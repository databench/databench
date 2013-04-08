package databench.runner

import databench.Bank
import scala.util.Try
import java.lang.System.{ currentTimeMillis => now }

case class TurnVM(
        subject: BankSubject,
        groups: Seq[ThreadTasks],
        idsMap: Map[Int, Any]) {

    val bank = subject.instance

    bank.warmUp

    val threads =
        groups.map(TurnVMThread(_, bank, idsMap))

    val start = now

    threads.foreach(_.start)
    threads.foreach(_.join)

    val milis = now - start

    bank.tearDown

    val summary =
        new TurnVMSummary(this)

}

case class TurnVMThread(
        myThreadTasks: ThreadTasks,
        bank: Bank[Any],
        idsMap: Map[Int, Any]) extends Thread {

    lazy val responses =
        myThreadTasks.tasks.map { task =>
            Try(task.perform(bank, idsMap))
        }

    override def run =
        responses
}

class TurnVMSummary(turn: TurnVM) extends Serializable {

    val bankName = turn.subject.name

    val tasksResponses =
        turn.threads.map(_.responses).flatten

    val milis = turn.milis

}