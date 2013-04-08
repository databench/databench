package databench.runner
import scala.collection.immutable.Seq
import scala.collection.immutable.Stream.consWrapper
import databench.task.TransferTask
import databench.task.Task
import databench.properties

case class ThreadTasks(tasks: Seq[Task]) {

    @transient
    lazy val flatTransferTasksValuesIndexesByAccount = {

        val transferTasks =
            tasks.collect {
                case task: TransferTask => task
            }

        val flatTransferTasksValues =
            transferTasks.map(transfer =>
                List(
                    (transfer.from, -transfer.amount),
                    (transfer.to, transfer.amount)))
                .flatten

        val flatTransferTasksValuesByAccount =
            flatTransferTasksValues
                .groupBy(_._1)
                .mapValues(_.map(_._2))

        val lazyMap =
            flatTransferTasksValuesByAccount
                .mapValues(_.zipWithIndex.toMap)

        val result =
            (for ((key, value) <- lazyMap)
                yield (key, value)).toMap

        result.withDefault(i => Map[Int, Int]())
    }
}

object ThreadTasks {

    def generatorStreamFor(tasks: Seq[Task], numberOfVMs: Int) = {

        def groupsFor(numberOfThreads: Int) = {
            val vmsGroups =
                tasks.grouped((tasks.size / numberOfVMs) + 1).to[Seq]
            val threadsPerVM = numberOfThreads / numberOfVMs
            val vmsThreads =
                vmsGroups.map(t =>
                    t.grouped((t.size / threadsPerVM) + 1)
                        .map(new ThreadTasks(_)).to[Seq])
            val errorMessage = "Without enough tasks"
            require(vmsThreads.size == numberOfVMs, errorMessage)
            require(vmsThreads.map(_.size).sum == numberOfThreads, errorMessage)
            require(vmsThreads.map(_.map(_.tasks.size).sum).sum == tasks.size, errorMessage)
            vmsThreads
        }

        def loop(numberOfThreads: Int): Stream[Seq[Seq[ThreadTasks]]] = {
            val groups = groupsFor(numberOfThreads)
            groups #:: loop(groups.map(_.size).sum + (numberOfVMs * properties.threadsStep))
        }

        loop(numberOfVMs * properties.threadsStart)
    }
}