package databench.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.ConnectionHandle;

import databench.AccountStatus;
import databench.Bank;
import databench.database.PostgreSqlDatabase;

public class JdbcPostgreSubject implements Bank<Integer> {

	private static final long serialVersionUID = 1L;

	private final BoneCP boneCP;

	private Map<Connection, TransferWorker> transferWorkers = Collections
			.synchronizedMap(new HashMap<Connection, TransferWorker>());

	private Map<Connection, GetAccountStatusWorker> getAccountStatusWorkers = Collections
			.synchronizedMap(new HashMap<Connection, GetAccountStatusWorker>());

	public JdbcPostgreSubject() throws SQLException {
		BoneCPConfig config = PostgreSqlDatabase.defaultBoceCPConfig();
		config.setDefaultAutoCommit(true);
		boneCP = new BoneCP(config);
	}

	@Override
	public String additionalVMParameters(boolean forMultipleVMs) {
		return "";
	}

	@Override
	public AccountStatus getAccountStatus(Integer id) {
		ConnectionHandle conn = getConnection();
		try {
			GetAccountStatusWorker worker = getGetAccountStatusWorker(conn);
			return worker.getAccountStatus(id);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public Integer[] setUp(Integer numberOfAccounts) {
		ConnectionHandle conn = getConnection();
		try {
			Statement stmt = conn.createStatement();
			createSchema(stmt);
			return createAccounts(numberOfAccounts, stmt);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public void tearDown() {
		boneCP.shutdown();
	}

	@Override
	public void transfer(Integer from, Integer to, int amount) {
		ConnectionHandle conn = getConnection();
		try {
			TransferWorker worker = getTransferWorker(conn);
			worker.transfer(from, to, amount);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public void warmUp() {
	}

	private Integer[] createAccounts(Integer numberOfAccounts, Statement stmt)
			throws SQLException {
		Integer[] ids;
		ids = new Integer[numberOfAccounts];
		for (int i = 0; i < numberOfAccounts; i++) {
			stmt.executeUpdate("INSERT INTO JDBCACCOUNT (ID, BALANCE, TRANSFERS) VALUES ("
					+ i + ", 0, '')");
			ids[i] = i;
		}
		return ids;
	}

	private void createSchema(Statement stmt) throws SQLException {
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS JDBCACCOUNT ("
				+ "    ID INTEGER PRIMARY KEY, BALANCE INTEGER, "
				+ "    TRANSFERS VARCHAR )");
	}

	private ConnectionHandle getConnection() {
		try {
			return (ConnectionHandle) boneCP.getConnection();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private GetAccountStatusWorker getGetAccountStatusWorker(
			ConnectionHandle handle) {
		Connection conn = handle.getInternalConnection();
		GetAccountStatusWorker worker = getAccountStatusWorkers.get(conn);
		if (worker == null) {
			worker = new GetAccountStatusWorker(conn);
			getAccountStatusWorkers.put(conn, worker);
		}
		return worker;
	}

	private TransferWorker getTransferWorker(ConnectionHandle handle) {
		Connection conn = handle.getInternalConnection();
		TransferWorker worker = transferWorkers.get(conn);
		if (worker == null) {
			worker = new TransferWorker(conn);
			transferWorkers.put(conn, worker);
		}
		return worker;
	}

	private void releaseConnection(Connection conn) {
		try {
			conn.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
