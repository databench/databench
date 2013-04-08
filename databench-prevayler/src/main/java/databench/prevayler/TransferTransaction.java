package databench.prevayler;

import java.util.Date;
import java.util.Map;

import org.prevayler.Transaction;

final class TransferTransaction implements
		Transaction<Map<Integer, PrevaylerAccount>> {

	private static final long serialVersionUID = 1L;

	final Integer from;
	final Integer to;
	final int amount;

	public TransferTransaction(Integer from, Integer to, int amount) {
		super();
		this.from = from;
		this.to = to;
		this.amount = amount;
	}

	@Override
	public void executeOn(Map<Integer, PrevaylerAccount> accounts, Date time) {
		accounts.get(from).transfer(-amount);
		accounts.get(to).transfer(amount);
	}
}
