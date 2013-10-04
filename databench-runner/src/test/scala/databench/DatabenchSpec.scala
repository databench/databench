 package databench

 import org.scalatest.matchers.ShouldMatchers
 import org.scalatest.FlatSpec
 import databench.syncbank.SyncBank
 import databench.task.getAccountStatusGenerator
 import databench.task.transferTaskGenerator
 import scala.collection.immutable.Seq
 import databench.task.TransferOk
 import databench.task.AccountStatusOk
 import databench.task.AccountStatusBalanceNok
 import databench.task.AccountStatusTransfersNok
 import databench.task.AccountStatusTransfersAndBalanceNok
 import databench.runner.Benchmark
 import databench.runner.BankSubject
 import databench.runner.Reporter
 import databench.runner.TurnSummary
 import scala.concurrent.Future

 class DatabenchSpec extends FlatSpec with ShouldMatchers {

     "Databench" should "detect failures" in
         test(new SyncBank(
             numberOfTransfers = 1000,
             numberOfGetAccountStatus = 4000,
             numberOfTransferFailures = 1,
             numberOfGetAccountStatusFailures = 1))

     it should "process response results" in
         test(new SyncBank(
             numberOfTransfers = 1000,
             numberOfGetAccountStatus = 4000,
             numberOfInconsistentBalances = 4,
             numberOfInconsistentTransfersValues = 5,
             numberOfInconsistentTransfersValuesAndBalance = 9))

     private def test(bank: SyncBank): Unit = {
         test(bank, fork = false)
         test(bank, fork = true)
     }

     private def test(bank: SyncBank, fork: Boolean) = {
         val numberOfTasks = bank.numberOfTransfers + bank.numberOfGetAccountStatus
         val getAccountStatusPercentage = (bank.numberOfGetAccountStatus * 100) / numberOfTasks
         val generatorsPercentages = Seq(
             getAccountStatusPercentage -> getAccountStatusGenerator,
             100 - getAccountStatusPercentage -> transferTaskGenerator)
         val mockReporter =
             new Reporter {
                 var turnSummary: Option[TurnSummary] = None
                 override def report(turn: TurnSummary) = {
                     turnSummary = Some(turn)
                     turn
                 }
             }
         Benchmark(
             subjects =
                 List(new BankSubject(bank)),
             generatorsPercentages,
             numberOfTasks = numberOfTasks,
             numberOfAccounts = 500,
             memory = 1000,
             reporter = mockReporter)
         verifySummary(bank, mockReporter.turnSummary.get)
     }

     private def verifySummary(bank: SyncBank, turn: TurnSummary) = {
         turn.bankName should equal("SyncBank")
         verifyFailures(bank, turn)
         verifyResponsesResults(bank, turn)
     }

     private def verifyFailures(bank: SyncBank, turn: TurnSummary) = {
         val exceptions = turn.exceptions
         val numberOfFailures = bank.numberOfGetAccountStatusFailures + bank.numberOfTransferFailures
         exceptions.size should equal(numberOfFailures)
         if (exceptions.nonEmpty)
             exceptions.toSet.size should equal(1)
         turn.failures should equal(numberOfFailures)
         turn.results.size should equal(turn.tasksResponses.size - numberOfFailures)
     }

     private def verifyResponsesResults(bank: SyncBank, turn: TurnSummary) = {
         val resultsByName = turn.numberOfResultsByName.toMap.withDefault(name => 0)
         resultsByName(TransferOk.name) should equal(bank.numberOfTransfers - bank.numberOfTransferFailures)
         resultsByName(AccountStatusOk.name) should
             equal(bank.numberOfGetAccountStatus - bank.numberOfGetAccountStatusFailures -
                 bank.numberOfInconsistentBalances - bank.numberOfInconsistentTransfersValues -
                 bank.numberOfInconsistentTransfersValuesAndBalance)
         resultsByName(AccountStatusBalanceNok.name) should equal(bank.numberOfInconsistentBalances)
         resultsByName(AccountStatusTransfersNok.name) should equal(bank.numberOfInconsistentTransfersValues)
         resultsByName(AccountStatusTransfersAndBalanceNok.name) should
             equal(bank.numberOfInconsistentTransfersValuesAndBalance)
     }
 }