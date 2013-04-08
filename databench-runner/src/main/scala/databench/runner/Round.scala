package databench.runner

import databench.util.JvmFork
import databench.util.Logging
import databench.database.Database
import databench.task.Task
import scala.collection.immutable.Seq

case class Round(
    subjects: Seq[BankSubject],
    tasks: Seq[Task],
    numberOfAccounts: Int,
    memory: Int,
    numberOfVMs: Int,
    reporter: Reporter)
        extends Logging {

    info(s"Round start for $numberOfVMs VMs.")

    lazy val vmsTasksStream = ThreadTasks.generatorStreamFor(tasks, numberOfVMs)

    subjects.map(bestTurnSummary(_))

    info("Round end.")

    private def bestTurnSummary(
        subject: BankSubject,
        lastTurnSummaryOption: Option[TurnSummary] = None,
        vmsTasksStreamIterator: Iterator[Seq[Seq[ThreadTasks]]] = vmsTasksStream.iterator): TurnSummary = {

        Database.recreateAll

        val currentTurnSummary =
            turn(subject, vmsTasksStreamIterator.next, numberOfAccounts, memory)

        info(currentTurnSummary.toString)

        lastTurnSummaryOption
            .filter(_.tps > currentTurnSummary.tps)
            .map(reporter.report)
            .getOrElse(bestTurnSummary(subject.copy(), Some(currentTurnSummary), vmsTasksStreamIterator))
    }

    private def turn(
        subject: BankSubject,
        vmsTasks: Seq[Seq[ThreadTasks]],
        numberOfAccounts: Int,
        memory: Int) = {

        info(s"Turn start for $subject. ${vmsTasks.size} VM(s) " +
            s"with ${vmsTasks.map(_.size).sum / vmsTasks.size} thread(s) each.")

        JvmFork.runForked(mx = memory) {
            Turn(
                subject,
                vmsTasks,
                numberOfAccounts,
                memory).summary
        }
    }

}