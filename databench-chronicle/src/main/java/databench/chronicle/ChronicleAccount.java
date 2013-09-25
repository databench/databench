package databench.chronicle;

import databench.AccountStatus;
import gnu.trove.list.array.TIntArrayList;

/**
 * @author peter.lawrey
 */
public class ChronicleAccount {
	
    private int balance = 0;
    private final TIntArrayList transferValues = new TIntArrayList();

    public void transfer(int value) {
        transferValues.add(value);
        balance += value;
    }

    public AccountStatus getAccountStatus() {
        return new AccountStatus(balance, transferValues.toArray());
    }
}
