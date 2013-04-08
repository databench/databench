package databench.db4o;

import java.util.ArrayList;
import java.util.List;

public class Db4oAccount {

	private int balance = 0;
	private List<Integer> transferValues = new ArrayList<Integer>();

	public void transfer(int value) {
		transferValues.add(value);
		balance += value;
	}

	public int getBalance() {
		return balance;
	}

	public List<Integer> getTransferValues() {
		return transferValues;
	}

}