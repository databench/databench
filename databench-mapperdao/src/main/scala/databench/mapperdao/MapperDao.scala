package databench.mapperdao

import scala.Array.canBuildFrom
import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import com.googlecode.mapperdao._
import com.googlecode.mapperdao.SurrogateIntId
import com.googlecode.mapperdao.ValuesMap
import com.googlecode.mapperdao.jdbc.Transaction
import com.googlecode.mapperdao.utils.Setup
import com.googlecode.mapperdao.utils.SurrogateIntIdAll
import com.googlecode.mapperdao.utils._
import databench.database.PostgreSqlDatabase
import databench.AccountStatus
import databench.Bank
import databench.database.PostgreSqlDatabase.defaultBoneCPDataSource
import databench.database.Database
import java.sql.BatchUpdateException
import java.sql.Connection

case class Account(id: Int, balance: Int, transfers: String) {
    def transferValues =
        transfers.split(',').tail.map(Integer.parseInt(_).intValue)

    def update(value: Int) = {
        val balance = this.balance + value
        val transfers = this.transfers + "," + value
        copy(balance = balance, transfers = transfers)
    }
}

object AccountEntity extends Entity[Int, NaturalIntId, Account] {
    val id = key("id") to (_.id)
    val balance = column("balance") to (_.balance)
    val transfers = column("transfers") to (_.transfers)
    def constructor(implicit m: ValuesMap) =
        new Account(id, balance, transfers) with NaturalIntId
}

abstract class AccountDao
        extends NaturalIntIdCRUD[Account] {
    val entity = AccountEntity
}

class MapperDaoPostgreSubject extends Bank[Integer] {

    import PostgreSqlDatabase._

    val dataSource = defaultBoneCPDataSource
    val (jdbc, md, qd, txM) = Setup.postGreSql(dataSource, List(AccountEntity))

    val accountDao = new AccountDao {
        val (mapperDao, queryDao, txManager) = (md, qd, txM)
    }

    def setUp(numberOfAccounts: Integer) = {
        PostgreSqlDatabase.setDatabaseDefaultTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
        createTable
        val ids = withTransaction {
            for (i <- 0 until numberOfAccounts) yield {
                accountDao.create(new Account(i, 0, "")).id
            }
        }
        ids.map(new Integer(_)).toArray
    }

    def warmUp = {}

    override def additionalVMParameters(forMultipleVMs: Boolean) = ""

    def tearDown = {}

    def transfer(from: Integer, to: Integer, value: Int) =
        withTransactionRetry {
            val fromAccount = accountById(from)
            val toAccount = accountById(to)
            accountDao.update(fromAccount, fromAccount.update(-value))
            accountDao.update(toAccount, toAccount.update(value))
        }

    def getAccountStatus(id: Integer) =
        withTransactionRetry {
            val account = accountById(id)
            new AccountStatus(
                account.balance,
                account.transferValues.toArray)
        }

    private def createTable = {
        val con = dataSource.getConnection
        val stmt = con.createStatement
        stmt.executeUpdate(
            """CREATE TABLE account(
        		id int NOT NULL,
        		transfers varchar NOT NULL, 
        		balance int NOT NULL, 
        		PRIMARY KEY (id)
        	)""")
        stmt.executeUpdate("commit")
        stmt.close
        con.close
    }

    private def accountById(id: Integer) =
        accountDao.retrieve(id).get

    private def withTransaction[R](f: => R): R = {
        val tx = Transaction.get(txM, Transaction.Propagation.Required, Transaction.Isolation.RepeatableRead, -1)
        tx(() => f)
    }

    @tailrec
    private def withTransactionRetry[R](f: => R): R =
        Try(withTransaction(f)) match {
            case Success(result) =>
                result
            case Failure(exception) if (
                exception.getMessage.contains("ERROR: could not serialize access")) =>
                withTransactionRetry(f)
        }
}