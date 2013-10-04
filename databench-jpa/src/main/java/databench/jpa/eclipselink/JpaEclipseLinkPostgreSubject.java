package databench.jpa.eclipselink;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import databench.database.PostgreSqlDatabase;
import databench.jpa.JpaAccount;
import databench.jpa.JpaSubject;

public class JpaEclipseLinkPostgreSubject extends JpaSubject {

	private static final long serialVersionUID = 1L;

	@Override
	protected JpaAccount newAccount(Integer id) {
		return new JpaEclipseLinkAccount(id);
	}

	@Override
	protected JpaEclipseLinkAccount accountById(final Integer id,
			EntityManager em) {
		return em.find(JpaEclipseLinkAccount.class, id);
	}
	
	@Override
	protected EntityManagerFactory createEntityManagerFactory()
			throws SQLException {
		createTable();
		registerDS();
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("eclipselink.ddl-generation", "create-tables");
		return Persistence.createEntityManagerFactory(getPersistenceUnitName(),
				properties);
	}
	
	public void createTable() throws SQLException {
		Connection conn = PostgreSqlDatabase.getConnection();
		try {
			conn.createStatement().execute(
					"CREATE TABLE IF NOT EXISTS JPAECLIPSELINKACCOUNT ("
							+ "    ID INTEGER PRIMARY KEY, "
							+ "    BALANCE INTEGER, "
							+ "    TRANSFERVALUES BYTEA, "
							+ "    VERSION INTEGER" + ")");
		} finally {
			conn.close();
		}
	}

	@Override
	protected String getPersistenceUnitName() {
		return "databench.jpa.eclipselink";
	}

}
