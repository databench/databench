package databench.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import databench.AccountStatus;

class GetAccountStatusWorker {

	private final PreparedStatement getAccountStatusPreparedStatement;

	public GetAccountStatusWorker(Connection connection) {
		try {
			getAccountStatusPreparedStatement = connection.prepareStatement(
					"SELECT balance, transfers FROM JDBCACCOUNT WHERE id=?",
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public AccountStatus getAccountStatus(int id) throws SQLException {
		getAccountStatusPreparedStatement.setInt(1, id);
		ResultSet rs = getAccountStatusPreparedStatement.executeQuery();
		rs.next();
		int balance = rs.getInt(1);
		String transfers = rs.getString(2);
		return new AccountStatus(balance, toArray(transfers));
	}

	private int[] toArray(String transfers) {
		String[] split = transfers.split(",");
		int[] transfersValues = new int[split.length - 1];
		for (int i = 1; i < split.length; i++) {
			transfersValues[i - 1] = Integer.parseInt(split[i]);
		}
		return transfersValues;
	}

}