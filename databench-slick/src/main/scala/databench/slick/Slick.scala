package databench.slick

import databench.Bank
import java.lang.{ Integer => JInt }
import databench.AccountStatus
import java.sql.Connection
import scala.annotation.tailrec
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.slick.driver.ExtendedDriver
import scala.slick.driver.PostgresDriver.simple._
import javax.sql.DataSource
import databench.database.PostgreSqlDatabase
import Database.threadLocalSession
import com.jolbox.bonecp.BoneCPConfig
import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.BoneCPDataSource

case class Account(id: Int, balance: Int, transfers: String) {
    def transferValues =
        transfers.split(',').tail.map(Integer.parseInt(_).intValue)
}

object Accounts extends Table[Account]("SLICK_ACCOUNT") {
    def id = column[Int]("ID", O.PrimaryKey)
    def balance = column[Int]("BALANCE")
    def transfers = column[String]("TRANSFERS", O.DBType("VARCHAR"))
    def * = id ~ balance ~ transfers <> (Account.apply _, Account.unapply _)
}

class SlickPostgreSubject extends Bank[JInt] {

    private val dataSource = {
        import PostgreSqlDatabase._
        PostgreSqlDatabase.loadDriver
        val config = new BoneCPConfig
        config.setJdbcUrl(url)
        config.setUsername(user)
        config.setPassword(password)
        new BoneCPDataSource(config)
    }

    private val database =
        Database.forDataSource(dataSource)

    def tearDown =
        dataSource.close

    @tailrec private def withTransactionRetry[R](f: => R): R =
        Try(withTransaction(f)) match {
            case Success(result) =>
                result
            case Failure(exception) if (
                exception.getMessage.startsWith("ERROR: could not serialize access")) =>
                withTransactionRetry(f)
        }

    def setUp(numberOfAccounts: JInt): Array[JInt] = {
        PostgreSqlDatabase.setDatabaseDefaultTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
        createSchema
        insertAccounts(numberOfAccounts).map(new JInt(_)).toArray
    }

    def warmUp = {}

    override def additionalVMParameters(forMultipleVMs: Boolean) = ""

    def transfer(from: JInt, to: JInt, value: Int) =
        withTransactionRetry {
            val (fromAccount, toAccount) = accountsByIdsQuery(from, to).to[List].head
            def updateFromAccount = updateAccount(fromAccount, -value)
            def updateToAccount = updateAccount(toAccount, value)
            executeOrderedUpdatesToAvoidDeadlock(fromAccount, toAccount, updateFromAccount, updateToAccount)
        }

    private def updateAccount(account: Account, value: Int): Unit =
        updateAccount(
            account.id,
            account.balance + value,
            account.transfers + "," + value)

    private def updateAccount(id: Int, balance: Int, transfers: String) = {
        val query =
            for (account <- Accounts if (account.id === id))
                yield account.balance ~ account.transfers
        query.update(balance, transfers)
    }

    def getAccountStatus(id: JInt) =
        withTransactionRetry {
            val account = accountById(id)
            new AccountStatus(
                account.balance,
                account.transferValues.toArray)
        }

    private def accountById(id: Int) =
        accountByIdQuery(id).to[List].head

    private val accountByIdQuery =
        for (
            id <- Parameters[Int];
            account <- Accounts if (account.id === id)
        ) yield account

    private val accountsByIdsQuery = {
        (for (
            (from, to) <- Parameters[(Int, Int)];
            fromAccount <- Accounts.where(_.id is from);
            toAccount <- Accounts.where(_.id is to)
        ) yield (fromAccount, toAccount))
    }

    private def insertAccounts(numberOfAccounts: Integer) =
        withTransaction {
            for (i <- (0 until numberOfAccounts)) yield {
                Accounts.insert(Account(i, 0, " "))
                i
            }
        }

    private def createSchema =
        withTransaction {
            Accounts.ddl.create
        }

    private def executeOrderedUpdatesToAvoidDeadlock(
        fromAccount: Account,
        toAccount: Account,
        updateFromAccount: => Unit,
        updateToAccount: => Unit) =
        if (fromAccount.id < toAccount.id) {
            updateFromAccount
            updateToAccount
        } else {
            updateToAccount
            updateFromAccount
        }

    private def withTransaction[R](f: => R): R =
        database.withTransaction(f)

}

