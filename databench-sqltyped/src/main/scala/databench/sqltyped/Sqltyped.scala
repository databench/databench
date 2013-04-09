package databench.sqltyped

import databench.Bank
import java.lang.{ Integer => JInt }
import databench.AccountStatus
import java.sql.Connection
import scala.annotation.tailrec
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import sqltyped._
import scala.slick.session.Database
import databench.database.PostgreSqlDatabase
import com.jolbox.bonecp.BoneCPConfig
import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.BoneCPDataSource

class SqltypedPostgreSubject extends Bank[JInt] {
  private val dataSource = {
    import PostgreSqlDatabase._
    PostgreSqlDatabase.loadDriver
    val config = new BoneCPConfig
    config.setJdbcUrl(url)
    config.setUsername(user)
    config.setPassword(password)
    new BoneCPDataSource(config)
  }

  object Tables { trait SQLTYPED_ACCOUNT }
  object Columns { }

  private val database = Database.forDataSource(dataSource)
  implicit val config = Configuration(Tables, Columns)
  implicit def conn = Database.threadLocalSession.conn

  val newAccount    = sql("INSERT INTO sqltyped_account(id, balance, transfers) VALUES (?,?,?)")
  val updateAccount = sql("UPDATE sqltyped_account SET balance = balance + ?, transfers=(transfers || ',' || ?) WHERE id=?")
  val accountById   = sql("SELECT balance, transfers FROM sqltyped_account WHERE id=?")

  def tearDown = dataSource.close

  @tailrec private def withTransactionRetry[R](f: => R): R =
    Try(withTransaction(f)) match {
      case Success(result) =>
        result
      case Failure(exception) if (
        exception.getMessage.startsWith("ERROR: could not serialize access")) =>
          withTransactionRetry(f)
    }

  def setUp(numberOfAccounts: JInt): Array[JInt] = {
    createSchema
    insertAccounts(numberOfAccounts).map(new JInt(_)).toArray
  }

  def warmUp = {}

  override def additionalVMParameters(forMultipleVMs: Boolean) = ""

  def transfer(from: JInt, to: JInt, value: Int) = withTransactionRetry {
    updateAccount(-value, (-value).toString, from)
    updateAccount(value, value.toString, to)
  }

  def getAccountStatus(id: JInt) = withTransactionRetry {
    accountById(id) map (_.values.tupled) map { case (balance, transfers) => 
      new AccountStatus(balance, transfers.split(',').tail.map(Integer.parseInt(_).intValue))
    } getOrElse sys.error("no such account " + id)
  }
  
  private def insertAccounts(numberOfAccounts: Integer) = withTransaction {
    for (i <- (0 until numberOfAccounts)) yield {
      newAccount(i, 0, " ")
      i
    }
  }

  private def createSchema = withTransaction {
    val stmt = conn.createStatement
    stmt.executeUpdate(""" CREATE SCHEMA databench AUTHORIZATION postgres """)
    stmt.executeUpdate("""
      create table SQLTYPED_ACCOUNT(
        ID int NOT NULL,
        TRANSFERS varchar NOT NULL, 
        BALANCE int NOT NULL, 
        PRIMARY KEY (id)
      )""")
    stmt.close
  }
  
  private def withTransaction[R](f: => R): R = database.withTransaction(f)
}
