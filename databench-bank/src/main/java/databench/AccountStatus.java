package databench;

import java.io.Serializable;

public class AccountStatus implements Serializable {

	private static final long serialVersionUID = 1L;

	public final int balance;
	public final int[] transferredAmounts;

	public AccountStatus(int balance, int[] transferredAmounts) {
		this.balance = balance;
		this.transferredAmounts = transferredAmounts;
	}
}
