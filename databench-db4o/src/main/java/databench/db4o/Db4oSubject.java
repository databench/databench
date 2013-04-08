package databench.db4o;

import java.util.List;

import com.db4o.ObjectContainer;

import databench.AccountStatus;
import databench.Bank;

public abstract class Db4oSubject implements Bank<Long> {

	private static final long serialVersionUID = 1L;

	protected abstract ObjectContainer db();

	@Override
	public Long[] setUp(Integer size) {
		Long[] res = new Long[size];
		for (int i = 0; i < size; i++) {
			res[i] = createAccount();
		}
		db().commit();
		return res;
	}

	@Override
	public void warmUp() {
		db();
	}

	@Override
	public String additionalVMParameters(boolean forMultipleVMs) {
		return "";
	}

	@Override
	public void transfer(final Long from, final Long to, final int amount) {
		run(new Transaction<Object>() {
			@Override
			public Object run(ObjectContainer session) {
				transfer(from, -amount, session);
				transfer(to, amount, session);
				return null;
			}

			private void transfer(final Long id, int value,
					ObjectContainer session) {
				Db4oAccount account = accountById(id, session);
				account.transfer(value);
				session.store(account);
			}

			@Override
			public Long[] accountsIdsToLock() {
				Long[] ret = { from, to };
				return ret;
			}
		});
	}

	@Override
	public AccountStatus getAccountStatus(final Long id) {
		return run(new Transaction<AccountStatus>() {
			@Override
			public AccountStatus run(ObjectContainer session) {
				Db4oAccount account = accountById(id, session);
				List<Integer> transferValues = account.getTransferValues();
				return new AccountStatus(account.getBalance(),
						toArray(transferValues));
			}

			private int[] toArray(List<Integer> transferValues) {
				int[] array = new int[transferValues.size()];
				for (int i = 0; i < transferValues.size(); i++)
					array[i] = transferValues.get(i);
				return array;
			}

			@Override
			public Long[] accountsIdsToLock() {
				Long[] ret = { id };
				return ret;
			}
		});
	}

	private Db4oAccount accountById(Long id, ObjectContainer session) {
		Db4oAccount account = session.ext().getByID(id);
		session.activate(account, 2);
		return account;
	}

	private interface Transaction<R> {
		Long[] accountsIdsToLock();

		R run(ObjectContainer session);
	}

	private <R> R run(Transaction<R> transaction) {
		ObjectContainer session = db().ext().openSession();
		try {
			try {
				if (lockAccounts(transaction, session))
					return transaction.run(session);
			} finally {
				unlockAccounts(transaction, session);
			}
		} finally {
			session.close(); // implicit commit
		}
		// Retry
		return run(transaction);
	}

	private <R> boolean lockAccounts(Transaction<R> transaction,
			ObjectContainer session) {
		for (Long id : transaction.accountsIdsToLock())
			if (!session.ext().setSemaphore(semaphoreName(id), 1000))
				return false;
		return true;
	}

	private <R> void unlockAccounts(Transaction<R> transaction,
			ObjectContainer session) {
		for (Long id : transaction.accountsIdsToLock())
			session.ext().releaseSemaphore(semaphoreName(id));
	}

	private String semaphoreName(Long id) {
		return "account: " + id;
	}

	private long createAccount() {
		Db4oAccount account = new Db4oAccount();
		db().store(account);
		return db().ext().getID(account);
	}
}
