package databench.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class TransferWorker {

	private final PreparedStatement updateAccountPreparedStatement;

	public TransferWorker(Connection connection) {
		try {
			updateAccountPreparedStatement = connection
					.prepareStatement("UPDATE JDBCACCOUNT SET balance = balance + ?, transfers=(transfers || ',' || ?) WHERE id=?");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void transfer(int from, int to, int value) throws SQLException {
		performOrderedUpdatesToAvoidDeadLock(from, to, value);
		updateAccountPreparedStatement.executeBatch();
	}

	private void performOrderedUpdatesToAvoidDeadLock(int from, int to,
			int value) throws SQLException {
		if (from < to) {
			addUpdateBatch(from, -value);
			addUpdateBatch(to, value);
		} else {
			addUpdateBatch(to, value);
			addUpdateBatch(from, -value);
		}
	}

	private void addUpdateBatch(int id, int value) throws SQLException {
		updateAccountPreparedStatement.setInt(1, value);
		updateAccountPreparedStatement.setInt(2, value);
		updateAccountPreparedStatement.setInt(3, id);
		updateAccountPreparedStatement.addBatch();
	}

}