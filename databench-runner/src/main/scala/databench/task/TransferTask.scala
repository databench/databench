package databench.task

import scala.annotation.tailrec
import scala.collection.immutable.Seq
import databench.Bank
import databench.runner.ThreadTasks

case class TransferTask(
    from: Int,
    to: Int,
    amount: Int)
        extends Task {

    def perform(bank: Bank[Any], idsMap: Map[Int, Any]) = {
        bank.transfer(idsMap(from), idsMap(to), amount)
        TransferOkResponse
    }
}

case object TransferOkResponse
        extends TaskResponse {

    override def resultFor(tasksGroups: Seq[ThreadTasks]) =
        TransferOk
}

case object TransferOk extends TaskResult {

    override def name = "OkTransfer"
}

object transferTaskGenerator
        extends Generator {

    override def possibleResultsNames =
        Set(TransferOk.name)

    @tailrec override protected final def taskFor(numberOfAccounts: Int, index: Int): Task = {
        val from = randomAccountNumber(numberOfAccounts)
        val to = randomAccountNumber(numberOfAccounts)
        if (from != to)
            TransferTask(from, to, index)
        else
            taskFor(numberOfAccounts, index)
    }

}