//
// Too slow! See: https://github.com/sorm/sorm/issues/26
//
//package databench.sorm
//
//import sorm._
//import databench.database.PostgreSqlDatabase
//import databench.Bank
//import java.sql.Connection
//import java.lang.{ Long => JLong }
//import databench.AccountStatus
//import scala.annotation.tailrec
//import scala.util.Try
//import scala.util.Failure
//import scala.util.Success
//
//case class Account(balance: Int, transfers: String) {
//    def transferValues =
//        transfers.split(',').tail.map(Integer.parseInt(_).intValue)
//
//    def update(value: Int) = {
//        val balance = this.balance + value
//        val transfers = this.transfers + "," + value
//        copy(balance = balance, transfers = transfers)
//    }
//}
//
//object Db extends Instance(
//    entities = Set(Entity[Account]()),
//    url = PostgreSqlDatabase.url,
//    user = PostgreSqlDatabase.user,
//    password = PostgreSqlDatabase.password,
//    initMode = InitMode.Create,
//    poolSize = PostgreSqlDatabase.defaultPoolSize)
//
//class SormPostgreSubject extends Bank[JLong] {
//
//    def setUp(numberOfAccounts: Integer) = {
//        PostgreSqlDatabase.setDatabaseDefaultTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
//        val ids = for (_ <- (0 until numberOfAccounts).toArray) yield Db.save(Account(0, "")).id
//        ids.map(new JLong(_))
//    }
//
//    def warmUp = {}
//
//    def transfer(from: JLong, to: JLong, amount: Int) =
//        withTransactionRetry {
//            val fromAccount = Db.fetchById[Account](from)
//            val toAccount = Db.fetchById[Account](to)
//            Db.save(fromAccount.update(-amount))
//            Db.save(toAccount.update(amount))
//        }
//
//    def getAccountStatus(id: JLong) = {
//        val account = Db.fetchById[Account](id)
//        new AccountStatus(
//            account.balance,
//            account.transferValues.toArray)
//    }
//
//    def tearDown = {}
//
//    def additionalVMParameters(forMultipleVMs: Boolean) = ""
//
//    @tailrec private def withTransactionRetry[R](f: => R): R =
//        Try(Db.transaction(f)) match {
//            case Success(result) =>
//                result
//            case Failure(exception) if (
//                exception.getMessage.startsWith("ERROR: could not serialize access")) =>
//                withTransactionRetry(f)
//        }
//}