package databench.task

import scala.collection.immutable.Seq
import databench.AccountStatus
import databench.Bank
import databench.runner.ThreadTasks

case class GetAccountStatusTask(
    account: Int)
        extends Task {

    def perform(bank: Bank[Any], idsMap: Map[Int, Any]) = {
        GetAccountStatusResponse(
            this, bank.getAccountStatus(idsMap(account)))
    }
}

case class GetAccountStatusResponse(
    task: GetAccountStatusTask,
    accountStatus: AccountStatus)
        extends TaskResponse {

    override def resultFor(tasksGroups: Seq[ThreadTasks]) = {
        val transfersOk = transfersValuesAreOk(tasksGroups)
        val balanceOk = balanceIsOk
        if (!transfersOk && !balanceOk)
            AccountStatusTransfersAndBalanceNok
        else if (!transfersOk)
            AccountStatusTransfersNok
        else if (!balanceOk)
            AccountStatusBalanceNok
        else
            AccountStatusOk
    }

    private def transfersValuesAreOk(tasksGroups: Seq[ThreadTasks]) = {
        val values = accountStatus.transferredAmounts.toList
        val groups = tasksGroups
            .map(_.flatTransferTasksValuesIndexesByAccount(task.account))
        val groupsValuesIndexes =
            groups.map(group => values.map(group.get(_)).flatten)
        values.size == groupsValuesIndexes.map(_.size).sum &&
            groupsValuesIndexes.forall { groupIndexes =>
                groupIndexes.toList == (0 until groupIndexes.size).toList
            }
    }

    private def balanceIsOk =
        accountStatus.transferredAmounts.sum == accountStatus.balance
}

case object AccountStatusOk
        extends TaskResult {
    def name = "OkAccountStatus"
}

case object AccountStatusBalanceNok
        extends TaskResult {
    def name = "NOkAccountStatus"
}

case object AccountStatusTransfersNok
        extends TaskResult {
    def name = "NOkAccountStatus"
}

case object AccountStatusTransfersAndBalanceNok
        extends TaskResult {
    def name = "NOkAccountStatus"
}

object getAccountStatusGenerator
        extends Generator {

    override def possibleResultsNames =
        Set(
            AccountStatusOk.name,
            AccountStatusBalanceNok.name,
            AccountStatusTransfersNok.name,
            AccountStatusTransfersAndBalanceNok.name)

    override protected def taskFor(numberOfAccounts: Int, index: Int) =
        GetAccountStatusTask(randomAccountNumber(numberOfAccounts))

}

