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
			addFromUpdateBatch(from, value);
			addToUpdateBatch(to, value);
		} else {
			addToUpdateBatch(to, value);
			addFromUpdateBatch(from, value);
		}
	}

	private void addToUpdateBatch(int to, int value) throws SQLException {
		updateAccountPreparedStatement.setInt(1, value);
		updateAccountPreparedStatement.setInt(2, value);
		updateAccountPreparedStatement.setInt(3, to);
		updateAccountPreparedStatement.addBatch();
	}

	private void addFromUpdateBatch(int from, int value) throws SQLException {
		updateAccountPreparedStatement.setInt(1, -value);
		updateAccountPreparedStatement.setInt(2, -value);
		updateAccountPreparedStatement.setInt(3, from);
		updateAccountPreparedStatement.addBatch();
	}
}