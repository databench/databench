package databench.prevayler;

import java.util.ArrayList;

public class PrevaylerAccount {

	private Integer balance = 0;
	private ArrayList<Integer> transferValues = new ArrayList<Integer>();

	public Integer getBalance() {
		return balance;
	}

	public ArrayList<Integer> getTransferValues() {
		return transferValues;
	}

	public void transfer(int value) {
		transferValues.add(value);
		balance += value;
	}

}
