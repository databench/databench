package databench.runner

import scala.util.Failure
import scala.util.Success

class TurnSummary(turn: Turn) extends Serializable {

    val bankName = turn.subject.name

    val tasksResponses =
        turn.vmsSummaries
            .map(_.tasksResponses)
            .flatten

    val exceptions =
        tasksResponses.collect {
            case Failure(exception) =>
                exception
        }

    val failures = exceptions.size

    @transient
    val groups =
        turn.vmsTasks.flatten

    val vms = turn.vmsTasks.size

    val threadsPerVM = turn.vmsTasks.head.size

    val milis =
        turn.vmsSummaries.map(_.milis).max

    val tps = groups.map(_.tasks.size).sum * 1000 / milis

    val results =
        tasksResponses.par.collect {
            case Success(response) =>
                response.resultFor(groups)
        }.to[Seq]

    lazy val numberOfResultsByName =
        results.groupBy(_.name)
            .mapValues(_.size)

    override lazy val toString = {
        "\n******************************************" +
            s"\nBank $bankName" +
            s"\n	TPS: $tps" +
            s"\n	VMs: $vms" +
            s"\n	Threads/VM: $threadsPerVM" +
            s"\n	Milis: $milis" +
            s"\n	Failures: $failures" +
            (for ((name, value) <- numberOfResultsByName) yield s"\n	$name: $value").mkString("") +
            "\n******************************************"
    }
}
