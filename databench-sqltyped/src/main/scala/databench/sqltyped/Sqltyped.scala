package databench.sqltyped

import databench.Bank
import java.lang.{ Integer => JInt }
import databench.AccountStatus
import sqltyped._
import scala.slick.session.Database
import databench.database.PostgreSqlDatabase
import com.jolbox.bonecp.BoneCPConfig
import com.jolbox.bonecp.BoneCPDataSource

class SqltypedPostgreSubject extends Bank[JInt] {

    private val dataSource = {
        import PostgreSqlDatabase._
        PostgreSqlDatabase.loadDriver
        val config = new BoneCPConfig
        config.setJdbcUrl(url)
        config.setUsername(user)
        config.setPassword(password)
        config.setDefaultAutoCommit(true)
        config.setStatementsCacheSize(10)
        new BoneCPDataSource(config)
    }

    object Tables {}
    object Columns {}

    private val db = Database.forDataSource(dataSource)
    implicit val config = Configuration(Tables, Columns)
    implicit def conn = Database.threadLocalSession.conn

    val newAccount = sql("INSERT INTO sqltyped_account(id, balance, transfers) VALUES (?,?,?)")
    val updateAccount = sql("UPDATE sqltyped_account SET balance = balance + ?, transfers=(transfers || ',' || ?) WHERE id=?")
    val accountById = sql("SELECT balance, transfers FROM sqltyped_account WHERE id=?")

    def tearDown = dataSource.close

    def setUp(numberOfAccounts: JInt): Array[JInt] = {
        createSchema
        insertAccounts(numberOfAccounts).map(new JInt(_)).toArray
    }

    def warmUp = {}

    override def additionalVMParameters(forMultipleVMs: Boolean) = ""

    def transfer(from: JInt, to: JInt, value: Int) = db.withTransaction {
        def updateFrom = updateAccount(-value, (-value).toString, from)
        def updateTo = updateAccount(value, value.toString, to)
        executeOrderedUpdatesToAvoidDeadlock(from, to, updateFrom, updateTo)
    }

    def getAccountStatus(id: JInt) = db.withSession {
        accountById(id).map(_.values.tupled).map {
            case (balance, transfers) =>
                new AccountStatus(balance, transfers.split(',').tail.map(Integer.parseInt(_).intValue))
        }.head
    }

    private def insertAccounts(numberOfAccounts: Integer) = db.withTransaction {
        for (i <- (0 until numberOfAccounts)) yield {
            newAccount(i, 0, " ")
            i
        }
    }

    private def createSchema = db.withTransaction {
        val stmt = conn.createStatement
        stmt.executeUpdate(""" CREATE SCHEMA databench AUTHORIZATION postgres """)
        stmt.executeUpdate("""
      CREATE TABLE SQLTYPED_ACCOUNT(
        ID int NOT NULL,
        TRANSFERS varchar NOT NULL, 
        BALANCE int NOT NULL, 
        PRIMARY KEY (id)
      )""")
        stmt.close
    }

    private def executeOrderedUpdatesToAvoidDeadlock(
        fromAccount: JInt,
        toAccount: JInt,
        updateFromAccount: => Unit,
        updateToAccount: => Unit) =
        if (fromAccount < toAccount) {
            updateFromAccount
            updateToAccount
        } else {
            updateToAccount
            updateFromAccount
        }
}
