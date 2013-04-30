package databench.jpa.hibernate;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;

import databench.database.PostgreSqlDatabase;
import databench.jpa.JpaAccount;
import databench.jpa.JpaSubject;

public class JpaHibernatePostgreSubject extends JpaSubject {

	private static final long serialVersionUID = 1L;

	@Override
	protected JpaAccount newAccount(Integer id) {
		return new JpaHibernateAccount(id);
	}

	@Override
	protected JpaHibernateAccount accountById(final Integer id, EntityManager em) {
		return em.find(JpaHibernateAccount.class, id);
	}

	@Override
	protected Map<String, String> getEntityManagerFactoryProperties() {
		registerDS();
		Map<String, String> properties = new HashMap<String, String>();
		return properties;
	}

	private void registerDS() {
		PostgreSqlDatabase.loadDriver();
		BoneCPConfig config = new BoneCPConfig();
		config.setJdbcUrl(PostgreSqlDatabase.url());
		config.setUsername(PostgreSqlDatabase.user());
		config.setPassword(PostgreSqlDatabase.password());
		try {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
					"org.apache.naming.java.javaURLContextFactory");
			System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
			(new InitialContext()).bind("PostgreDS", new BoneCPDataSource(
					config));
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected String getPersistenceUnitName() {
		return "databench.jpa.hibernate";
	}
}
