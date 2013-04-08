package databench.syncbank

import java.lang.{ Integer => JInt }
import scala.collection.mutable.{ Map => MutableMap }
import databench.Bank
import scala.collection.mutable.SynchronizedMap
import databench.AccountStatus
import scala.util.Random
import databench.SingleVMBank

class SyncAccount {
    var balance = 0
    var transferValues = Array[Int]()
    def transfer(value: Int) = {
        transferValues = transferValues ++ Array(value)
        balance += value
    }
}

case class SyncBank(
    numberOfTransfers: Int,
    numberOfGetAccountStatus: Int,
    numberOfTransferFailures: Int = 0,
    numberOfGetAccountStatusFailures: Int = 0,
    numberOfInconsistentBalances: Int = 0,
    numberOfInconsistentTransfersValues: Int = 0,
    numberOfInconsistentTransfersValuesAndBalance: Int = 0)
        extends Bank[JInt] with SingleVMBank {

    require(numberOfTransferFailures <= numberOfTransfers)
    require(numberOfGetAccountStatusFailures <= numberOfGetAccountStatus)
    require(numberOfInconsistentBalances + numberOfInconsistentTransfersValues <= numberOfGetAccountStatus)

    val accounts = MutableMap[JInt, SyncAccount]()

    var transferNumber = -1
    var getAccountStatusNumber = -1

    val transferNumbersToFail =
        pick(n = numberOfTransferFailures,
            limit = numberOfTransfers)

    val getAccountStatusNumbersToFail =
        pick(n = numberOfGetAccountStatusFailures,
            limit = numberOfGetAccountStatus)

    val getAccountStatusNumbersInconsistentBalance =
        pick(n = numberOfInconsistentBalances,
            limit = numberOfGetAccountStatus,
            ignore = getAccountStatusNumbersToFail)

    val getAccountStatusNumbersInconsistentTransfersValues =
        pick(n = numberOfInconsistentTransfersValues,
            limit = numberOfGetAccountStatus,
            ignore = getAccountStatusNumbersInconsistentBalance ++
                getAccountStatusNumbersToFail)

    val getAccountStatusNumbersInconsistentTransfersValuesAndBalance =
        pick(n = numberOfInconsistentTransfersValuesAndBalance,
            limit = numberOfGetAccountStatus,
            ignore = getAccountStatusNumbersInconsistentTransfersValues ++
                getAccountStatusNumbersInconsistentBalance ++
                getAccountStatusNumbersToFail)

    def name = "syncBank"

    def setUp(numberOfAccounts: java.lang.Integer) = {
        val ids = (0 until numberOfAccounts).map(new JInt(_))
        ids.foreach { id =>
            accounts += id -> new SyncAccount
        }
        ids.toArray
    }

    def warmUp = {}

    override def additionalVMParameters(forMultipleVMs: Boolean) = ""

    def tearDown = {
        transferNumber = -1
        getAccountStatusNumber = -1
        accounts.clear
    }

    case object aException extends Exception

    def transfer(from: JInt, to: JInt, value: Int) =
        synchronized {
            transferNumber += 1
            accounts(from).transfer(-value)
            accounts(to).transfer(value)
            if (transferNumbersToFail.contains(transferNumber))
                throw aException
        }

    def getAccountStatus(id: JInt) =
        synchronized {
            getAccountStatusNumber += 1
            if (getAccountStatusNumbersToFail.contains(getAccountStatusNumber))
                throw aException
            val account = accounts(id)
            var balance = account.balance
            var transferValues = account.transferValues
            if (getAccountStatusNumbersInconsistentBalance.contains(getAccountStatusNumber))
                balance += Random.nextInt(100) + 1
            if (getAccountStatusNumbersInconsistentTransfersValues.contains(getAccountStatusNumber)) {
                transferValues = Array(321) ++ transferValues
                balance = transferValues.sum
            }
            if (getAccountStatusNumbersInconsistentTransfersValuesAndBalance.contains(getAccountStatusNumber))
                transferValues = Array(321) ++ transferValues
            new AccountStatus(
                balance,
                transferValues.toArray[Int])
        }

    private def pick(n: Int, limit: Int, ignore: Set[Int] = Set()) = {
        var res = Set[Int]()
        while (res.size < n) {
            val rand = Random.nextInt(limit)
            if (!ignore.contains(rand))
                res += rand
        }
        res
    }
}
