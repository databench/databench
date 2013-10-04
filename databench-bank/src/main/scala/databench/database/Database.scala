package databench.database

import databench.properties
import java.sql.DriverManager
import com.mongodb.MongoClient
import scala.reflect.io.Directory
import java.io.File
import java.sql.Connection
import javax.naming.InitialContext
import com.jolbox.bonecp.BoneCPConfig
import com.jolbox.bonecp.BoneCPDataSource
import com.jolbox.bonecp.BoneCP

trait Database {
    def recreate: Unit
    def processName: Option[String]
    val defaultPoolSize = 100
}

object Database {
    val instances =
        List(FolderDatabase, MongoDatabase, PostgreSqlDatabase)
    def recreateAll =
        instances.foreach(_.recreate)
}

trait JdbcDatabase extends Database {

    val jdbcDriver: String
    protected val rootUser: String
    protected val rootPassword: String
    protected val host: String
    protected val databaseName: String
    protected val urlPrefix: String

    protected lazy val rootUrl = urlPrefix + "://" + host
    lazy val user = rootUser
    lazy val password = rootPassword
    lazy val url = rootUrl + "/" + databaseName

    def getConnection =
        DriverManager.getConnection(url, rootUser, rootPassword)

    private def getRootConnection =
        DriverManager.getConnection(rootUrl, rootUser, rootPassword)

    def loadDriver =
        Class.forName(jdbcDriver)

    override def recreate = {
        loadDriver
        executeStatements(
            prepareDropDatabaseCommand,
            dropDatabaseCommand,
            createDatabaseCommand)
        setDatabaseDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
    }

    def setDatabaseDefaultTransactionIsolation(level: Int) =
        executeWithConnection { con =>
            con.createStatement.execute(modifyDatabaseDefaultTransactionIsolationStatement(level))
        }

    private def executeStatements(statements: String*) =
        executeWithConnection { con =>
            statements.foreach(con.createStatement.execute)
        }

    protected def executeWithConnection[R](f: (Connection) => R) = {
        val con = getRootConnection
        try f(con)
        finally con.close
    }

    protected def modifyDatabaseDefaultTransactionIsolationStatement(level: Int): String
    protected def prepareDropDatabaseCommand: String
    protected def dropDatabaseCommand: String
    protected def createDatabaseCommand: String
}

object PostgreSqlDatabase extends JdbcDatabase {

    override val jdbcDriver = "org.postgresql.Driver"
    override protected val rootUser = properties.postgreUser
    override protected val rootPassword = properties.postgrePassword
    override protected val host = "localhost"
    override protected val databaseName = "databench"
    override protected val urlPrefix = "jdbc:postgresql"

    def processName = Some("postgres")

    def defaultBoneCP = 
        new BoneCP(defaultBoceCPConfig)
    
    def defaultBoneCPDataSource = 
        new BoneCPDataSource(defaultBoceCPConfig)
    
    def defaultBoceCPConfig = {
        Class.forName(jdbcDriver)
        val config = new BoneCPConfig
        config.setJdbcUrl(url)
        config.setUsername(user)
        config.setPassword(password)
        config.setLazyInit(true)
        config.setDisableConnectionTracking(true)
        config.setReleaseHelperThreads(0)
        val partitions = Runtime.getRuntime.availableProcessors
        config.setPartitionCount(partitions)
        config.setMaxConnectionsPerPartition(defaultPoolSize / partitions)
        config
    }

    override protected def prepareDropDatabaseCommand = {
        "SELECT" +
            "    pg_terminate_backend(pid) " +
            "FROM " +
            "    pg_stat_activity " +
            "WHERE " +
            "    pid != pg_backend_pid() " +
            s"    AND datname = '$databaseName'"
    }

    override protected def dropDatabaseCommand =
        s"DROP DATABASE IF EXISTS $databaseName"

    override protected def createDatabaseCommand =
        s"CREATE DATABASE $databaseName"

    override protected def modifyDatabaseDefaultTransactionIsolationStatement(level: Int) = {
        s"ALTER DATABASE $databaseName SET default_transaction_isolation = '${levelString(level)}'"
    }

    private def levelString(level: Int): String = {
        level match {
            case Connection.TRANSACTION_READ_COMMITTED =>
                "read committed"
            case Connection.TRANSACTION_READ_UNCOMMITTED =>
                "read uncommitted"
            case Connection.TRANSACTION_REPEATABLE_READ =>
                "repeatable read"
            case Connection.TRANSACTION_SERIALIZABLE =>
                "serializable"
        }
    }
}

object FolderDatabase extends Database {

    val folder = new File("databases")

    def processName = None

    override def recreate = {
        Directory(folder).deleteRecursively
        folder.mkdir
    }

    def path =
        folder.getPath
}

object MongoDatabase extends Database {

    val host = "localhost"
    val port = 27017
    val db = "databench"
    val authentication: Option[(String, String)] = None

    def processName = Some("mongod")

    override def recreate =
        new MongoClient(host, port)
            .getDB(db)
            .dropDatabase
}

