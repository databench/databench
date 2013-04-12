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
		updateAccountPreparedStatement.setInt(1, -value);
		updateAccountPreparedStatement.setInt(2, -value);
		updateAccountPreparedStatement.setInt(3, from);
		updateAccountPreparedStatement.addBatch();
		updateAccountPreparedStatement.setInt(1, value);
		updateAccountPreparedStatement.setInt(2, value);
		updateAccountPreparedStatement.setInt(3, to);
		updateAccountPreparedStatement.addBatch();
		updateAccountPreparedStatement.executeBatch();
	}
}