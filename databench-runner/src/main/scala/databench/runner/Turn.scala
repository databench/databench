package databench.runner

import databench.util.JvmFork
import databench.util.Logging
import scala.collection.immutable.Seq

case class Turn(
    subject: BankSubject,
    vmsTasks: Seq[Seq[ThreadTasks]],
    numberOfAccounts: Int,
    memory: Int)
        extends Logging {

    val multipleVMs = vmsTasks.size != 1

    lazy val idsMap = {
        val ids = subject.instance.setUp(numberOfAccounts)
        (for (i <- 0 until ids.size)
            yield (i, ids(i))).toMap
    }

    val vmParameters =
        List(subject.instance.additionalVMParameters(multipleVMs))
            .filter(_.nonEmpty)

    if (multipleVMs || subject.acceptMultipleVMs) {
        idsMap
        subject.instance.tearDown()
    }

    val forks =
        vmsTasks.map(groups =>
            JvmFork.fork(mx = memory / vmsTasks.size, others = vmParameters) {
                TurnVM(subject, groups, idsMap).summary
            })

    forks.foreach(_.execute)
    forks.foreach(_.join)

    val vmsSummaries =
        forks.map(_.getResult)

    val summary = new TurnSummary(this)

}