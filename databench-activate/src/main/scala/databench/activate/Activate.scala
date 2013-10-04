package databench.activate

import net.fwbrasil.activate.storage.prevayler.PrevaylerStorage
import net.fwbrasil.activate.serialization.javaSerializer
import net.fwbrasil.activate.migration.Migration
import net.fwbrasil.activate.storage.relational.idiom.postgresqlDialect
import databench.database.PostgreSqlDatabase
import net.fwbrasil.activate.storage.mongo.MongoStorage
import net.fwbrasil.activate.migration.ManualMigration
import net.fwbrasil.activate.StoppableActivateContext
import databench.Bank
import net.fwbrasil.activate.storage.relational.PooledJdbcRelationalStorage
import net.fwbrasil.activate.storage.memory.TransientMemoryStorage
import net.fwbrasil.activate.entity.Entity
import databench.AccountStatus
import databench.database.FolderDatabase
import org.prevayler.PrevaylerFactory
import net.fwbrasil.activate.storage.prevayler.PrevaylerStorageSystem
import databench.database.MongoDatabase
import net.fwbrasil.activate.storage.relational.idiom.postgresqlDialect
import net.fwbrasil.activate.ActivateContext
import net.fwbrasil.activate.serialization.NamedSingletonSerializable
import java.sql.Connection
import scala.annotation.tailrec
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import net.fwbrasil.activate.storage.relational.JdbcStatementException
import net.fwbrasil.activate.OptimisticOfflineLocking
import net.fwbrasil.activate.migration.StorageVersion
import databench.SingleVMBank
import net.fwbrasil.activate.storage.prevalent.PrevalentStorage

class ActivateAccount
        extends Entity {

    var balance = 0
    var transfers = ""

    def transferValues =
        transfers.split(',').tail.map(Integer.parseInt(_))

    def transfer(value: Int) = {
        transfers += "," + value
        balance += value
    }
}

trait ActivateSubject
        extends StoppableActivateContext
        with Bank[String] {

    override protected val defaultSerializer = javaSerializer

    protected def createSchema =
        new ManualMigration {
            def up = {
                table[ActivateAccount]
                    .createTable(
                        _.column[Int]("balance"),
                        _.column[String]("transfers"),
                        _.column[Long]("version"))
                table[StorageVersion]
                    .addColumn(_.column[Long]("version"))
                    .ifNotExists
            }
        }.execute

    def setUp(numberOfAccounts: java.lang.Integer) = {
        start
        createSchema
        transactional {
            (0 until numberOfAccounts).map(_ => (new ActivateAccount).id).toArray
        }
    }

    def warmUp = {
        start
        initializeAllAccounts
    }

    override def additionalVMParameters(forMultipleVMs: Boolean) =
        if (forMultipleVMs)
            "-Dactivate.offlineLocking.enable=true"
        else
            ""

    def tearDown =
        stop

    final def transfer(from: String, to: String, value: Int): Unit =
        transactional {
            accountById(from).transfer(-value)
            accountById(to).transfer(value)
        }

    def getAccountStatus(id: String) =
        transactional {
            val account = accountById(id)
            new AccountStatus(
                account.balance,
                account.transferValues.toArray[Int])
        }

    private def initializeAllAccounts =
        transactional {
            all[ActivateAccount].foreach(_.balance)
        }

    private def accountById(id: String) =
        byId[ActivateAccount](id).get
}

class ActivateMongoSubject
        extends ActivateSubject {

    val storage = new MongoStorage {
        override val authentication = MongoDatabase.authentication
        override val host = MongoDatabase.host
        override val port = MongoDatabase.port
        override val db = MongoDatabase.db
        override val poolSize = MongoDatabase.defaultPoolSize
    }

}

class ActivatePostgreSubject
        extends ActivateSubject {
    
    val storage = new PooledJdbcRelationalStorage {
        override val jdbcDriver = PostgreSqlDatabase.jdbcDriver
        override val user = PostgreSqlDatabase.user
        override val password = PostgreSqlDatabase.password
        override val url = PostgreSqlDatabase.url
        override val dialect = postgresqlDialect
        override val poolSize = PostgreSqlDatabase.defaultPoolSize 
        override val batchLimit = 0
    }

}

class ActivatePrevaylerSubject
        extends ActivateSubject with SingleVMBank {

    val storage =
        new PrevaylerStorage(
            FolderDatabase.path + "/" + System.currentTimeMillis)
}

class ActivatePrevalentSubject
        extends ActivateSubject with SingleVMBank {

    val storage =
        new PrevalentStorage(
            FolderDatabase.path + "/" + System.currentTimeMillis)
}