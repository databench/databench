package databench.squeryl

import org.squeryl.KeyedEntity
import databench.Bank
import databench.AccountStatus
import org.squeryl.Schema
import java.sql.DriverManager
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.adapters.MySQLAdapter
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.internals.DatabaseAdapter
import org.squeryl.KeyedEntity
import org.squeryl.Schema
import org.squeryl.Session
import org.squeryl.SessionFactory
import scala.collection.JavaConversions._
import databench.database.PostgreSqlDatabase
import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.BoneCPConfig
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.Optimistic
import java.sql.Connection
import scala.annotation.tailrec
import org.squeryl.StaleUpdateException
import org.postgresql.util.PSQLException
import scala.util.Try
import scala.util.Failure
import scala.util.Success

class SquerylAccount(
    val id: Int)
        extends KeyedEntity[Int]
        with Optimistic {

    def this() = this(-1)

    var balance = 0
    var _transferValues = ""

    def transferValues =
        _transferValues.split(',')
            .tail.map(Integer.parseInt(_))

    def transfer(value: Int) = {
        _transferValues += "," + value
        balance += value
    }
}

object SquerylBankSchema extends Schema {
    val accounts = table[SquerylAccount]("squerylaccount")
    on(accounts)(s => declare(
        s._transferValues is (dbType("VARCHAR"))))
}

class SquerylPostgreSubject
        extends Bank[Integer] {

    private val connectionPool = {
        import PostgreSqlDatabase._
        Class.forName(jdbcDriver)
        val config = new BoneCPConfig
        config.setJdbcUrl(url)
        config.setUsername(user)
        config.setPassword(password)
        config.setLazyInit(true)
        new BoneCP(config)
    }

    def setUp(numberOfAccounts: Integer) = {
        prepareSessionFactory
        transaction {
            SquerylBankSchema.create
        }
        val ids = transaction {
            (0 until numberOfAccounts).map(i =>
                SquerylBankSchema.accounts.insert(new SquerylAccount()).id)
        }
        ids.map(new Integer(_)).toArray
    }

    def warmUp =
        prepareSessionFactory

    override def additionalVMParameters(forMultipleVMs: Boolean) = ""

    def tearDown =
        connectionPool.shutdown

    @tailrec private def transactionWithRetry[R](f: => R): R =
        Try(transaction(f)) match {
            case Failure(e: StaleUpdateException) =>
                transactionWithRetry(f)
            case Failure(e) =>
                throw e
            case Success(result) =>
                result
        }

    def transfer(from: Integer, to: Integer, value: Int) =
        transactionWithRetry {
            updateAccount(from, -value)
            updateAccount(to, value)
        }

    def getAccountStatus(id: Integer) =
        transaction {
            val account = from(SquerylBankSchema.accounts)(t => where(t.id === id.intValue) select (t)).head
            new AccountStatus(
                account.balance,
                account.transferValues.toArray[Int])
        }

    private def accountById(id: Int) =
        from(SquerylBankSchema.accounts)(t => where(t.id === id) select (t)).head

    private def prepareSessionFactory =
        SessionFactory.concreteFactory = Some(() =>
            Session.create(
                connectionPool.getConnection,
                new PostgreSqlAdapter))

    private def updateAccount(id: Integer, value: Int) = {
        val account = accountById(id)
        account.transfer(value)
        SquerylBankSchema.accounts.update(account)
    }

}