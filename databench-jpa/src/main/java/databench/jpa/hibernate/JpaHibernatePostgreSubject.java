package databench.jpa.hibernate;

import javax.persistence.EntityManager;

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
	protected String getPersistenceUnitName() {
		return "databench.jpa.hibernate";
	}
}
