package databench.prevayler;

import java.util.Date;
import java.util.Map;

import org.prevayler.SureTransactionWithQuery;

final class CreateAccountsTransaction implements
		SureTransactionWithQuery<Map<Integer, PrevaylerAccount>, Integer[]> {

	private static final long serialVersionUID = 1L;

	private final Integer numberOfAccounts;

	CreateAccountsTransaction(Integer numberOfAccounts) {
		this.numberOfAccounts = numberOfAccounts;
	}

	@Override
	public Integer[] executeAndQuery(Map<Integer, PrevaylerAccount> accounts,
			Date time) {
		Integer[] ids = createIds();
		for (Integer id : ids)
			accounts.put(id, new PrevaylerAccount());
		return ids;
	}

	private Integer[] createIds() {
		Integer[] ids = new Integer[numberOfAccounts];
		for (int i = 0; i < numberOfAccounts; i++)
			ids[i] = i;
		return ids;
	}

}