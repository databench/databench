package databench.jpa.eclipselink;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

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
	protected Map<String, String> getEntityManagerFactoryProperties() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("javax.persistence.jdbc.driver",
				PostgreSqlDatabase.jdbcDriver());
		properties
				.put("javax.persistence.jdbc.user", PostgreSqlDatabase.user());
		properties.put("javax.persistence.jdbc.password",
				PostgreSqlDatabase.password());
		properties.put("javax.persistence.jdbc.url", PostgreSqlDatabase.url());
		return properties;
	}

	@Override
	protected String getPersistenceUnitName() {
		return "databench.jpa.eclipselink";
	}

}
