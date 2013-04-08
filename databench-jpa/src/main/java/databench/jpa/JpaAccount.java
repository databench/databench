package databench.jpa;

import java.util.ArrayList;

public interface JpaAccount {

	public void transfer(int value);

	public ArrayList<Integer> getTransferValues();

	public int getBalance();

}
