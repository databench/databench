package databench.jpa.hibernate;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

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
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("hibernate.connection.driver_class",
				PostgreSqlDatabase.jdbcDriver());
		properties.put("hibernate.connection.username",
				PostgreSqlDatabase.user());
		properties.put("hibernate.connection.password",
				PostgreSqlDatabase.password());
		properties.put("hibernate.connection.url", PostgreSqlDatabase.url());
		return properties;
	}

	@Override
	protected String getPersistenceUnitName() {
		return "databench.jpa.hibernate";
	}
}
