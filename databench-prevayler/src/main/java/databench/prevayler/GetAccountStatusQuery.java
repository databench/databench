package databench.prevayler;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.prevayler.Query;

import databench.AccountStatus;

final class GetAccountStatusQuery implements
		Query<Map<Integer, PrevaylerAccount>, AccountStatus> {

	private static final long serialVersionUID = 1L;

	private final Integer id;

	public GetAccountStatusQuery(Integer id) {
		this.id = id;
	}

	@Override
	public AccountStatus query(Map<Integer, PrevaylerAccount> accounts,
			Date time) {
		PrevaylerAccount account = accounts.get(id);
		ArrayList<Integer> transferValues = account.getTransferValues();
		return new AccountStatus(account.getBalance(), toArray(transferValues));
	}

	private int[] toArray(ArrayList<Integer> transferValues) {
		int[] array = new int[transferValues.size()];
		for (int i = 0; i < transferValues.size(); i++)
			array[i] = transferValues.get(i);
		return array;
	}

}
