package databench.jpa.batoo;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import org.batoo.jpa.jdbc.OptimisticLockFailedException;

import databench.database.PostgreSqlDatabase;
import databench.jpa.JpaAccount;
import databench.jpa.JpaSubject;

public class JpaBatooPostgreSubject extends JpaSubject {

	private static final long serialVersionUID = 1L;
	
	@Override
	protected JpaAccount newAccount(Integer id) {
		return new JpaBatooAccount(id);
	}

	@Override
	protected JpaBatooAccount accountById(final Integer id, EntityManager em) {
		return em.find(JpaBatooAccount.class, id);
	}

	@Override
	protected boolean isOptimisticLockException(Throwable ex) {
		return ex.getCause() instanceof OptimisticLockFailedException;
	}

	@Override
	public Integer[] setUp(Integer numberOfAccounts) {
		try {
			createTable();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return super.setUp(numberOfAccounts);
	}

	public void createTable() throws SQLException {
		Connection conn = PostgreSqlDatabase.getConnection();
		try {
			conn.createStatement().execute(
					"CREATE TABLE IF NOT EXISTS JPABATOOACCOUNT ("
							+ "    ID INTEGER PRIMARY KEY, "
							+ "    BALANCE INTEGER, "
							+ "    TRANSFERVALUESSTRING VARCHAR, "
							+ "    VERSION INTEGER" + ")");
		} finally {
			conn.close();
		}
	}


	@Override
	protected String getPersistenceUnitName() {
		return "databench.jpa.batoo";
	}

}
