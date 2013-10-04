package databench.jpa;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;

import databench.AccountStatus;
import databench.Bank;
import databench.database.PostgreSqlDatabase;

public abstract class JpaSubject implements Bank<Integer> {

	private static final long serialVersionUID = 1L;

	private final EntityManagerFactory entityManagerFactory;

	public JpaSubject() {
		try {
			entityManagerFactory = createEntityManagerFactory();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Integer[] setUp(Integer numberOfAccounts) {
		return createAccounts(createIds(numberOfAccounts));
	}

	@Override
	public void warmUp() {
	}

	@Override
	public String additionalVMParameters(boolean forMultipleVMs) {
		return "";
	}

	@Override
	public void transfer(final Integer from, final Integer to, final int amount) {
		try {
			run(new Transaction<Object>() {
				@Override
				public Object run(EntityManager em) {
					transfer(from, em, -amount);
					transfer(to, em, amount);
					return null;
				}

				private void transfer(final Integer from, EntityManager em,
						int value) {
					JpaAccount fromAccount = accountById(from, em);
					fromAccount.transfer(value);
					em.persist(fromAccount);
				}
			});
		} catch (Throwable ex) {
			if (isOptimisticLockException(ex))
				transfer(from, to, amount);
			else
				throw new RuntimeException(ex);
		}
	}

	protected boolean isOptimisticLockException(Throwable ex) {
		return ex.getCause() instanceof OptimisticLockException;
	}

	@Override
	public AccountStatus getAccountStatus(final Integer id) {
		return run(new Transaction<AccountStatus>() {
			@Override
			public AccountStatus run(EntityManager em) {
				JpaAccount account = accountById(id, em);
				ArrayList<Integer> transferValues = account.getTransferValues();
				return new AccountStatus(account.getBalance(),
						toArray(transferValues));
			}

			private int[] toArray(ArrayList<Integer> transferValues) {
				int[] array = new int[transferValues.size()];
				for (int i = 0; i < transferValues.size(); i++)
					array[i] = transferValues.get(i);
				return array;
			}
		});
	}

	@Override
	public void tearDown() {
		entityManagerFactory.close();

	}

	private Integer[] createIds(Integer numberOfAccounts) {
		Integer[] ids = new Integer[numberOfAccounts];
		for (int i = 1; i <= numberOfAccounts; i++)
			ids[i - 1] = i;
		return ids;
	}

	private Integer[] createAccounts(final Integer[] ids) {
		return run(new Transaction<Integer[]>() {
			@Override
			public Integer[] run(EntityManager em) {
				for (int i = 0; i < ids.length; i++) {
					em.persist(newAccount(ids[i]));
				}
				return ids;
			}
		});
	}

	protected abstract JpaAccount newAccount(Integer id);

	protected abstract String getPersistenceUnitName();

	protected abstract JpaAccount accountById(final Integer id, EntityManager em);

	private interface Transaction<R> {
		R run(EntityManager em);
	}

	private <R> R run(Transaction<R> transaction) {
		EntityManager entityManager = entityManagerFactory
				.createEntityManager();
		EntityTransaction emTransaction = entityManager.getTransaction();
		emTransaction.begin();
		try {
			R result = transaction.run(entityManager);
			emTransaction.commit();
			return result;
		} catch (RollbackException e) {
			throw e;
		} catch (Throwable e) {
			emTransaction.rollback();
			throw new RuntimeException(e);
		} finally {
			entityManager.close();
		}
	}

	protected EntityManagerFactory createEntityManagerFactory()
			throws SQLException {
		registerDS();
		return Persistence.createEntityManagerFactory(getPersistenceUnitName(),
				new HashMap<String, String>());
	}

	protected void registerDS() {
		try {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
					"org.apache.naming.java.javaURLContextFactory");
			System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
			(new InitialContext()).bind("PostgreDS",
					PostgreSqlDatabase.defaultBoneCPDataSource());
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}

}
