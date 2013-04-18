package databench.chronicle;

import databench.AccountStatus;
import gnu.trove.list.array.TIntArrayList;

/**
 * @author peter.lawrey
 */
public class ChronicleAccount {
    private int balance = 0;
    private final TIntArrayList transferValues = new TIntArrayList();
    private AccountStatus accountStatus = null;

    public int getBalance() {
        return balance;
    }

    public TIntArrayList getTransferValues() {
        return transferValues;
    }

    public void transfer(int value) {
        transferValues.add(value);
        balance += value;
        accountStatus = null;
    }

    public AccountStatus getAccountStatus() {
        if (accountStatus == null)
            accountStatus = new AccountStatus(balance, transferValues.toArray());
        return accountStatus;
    }
}
