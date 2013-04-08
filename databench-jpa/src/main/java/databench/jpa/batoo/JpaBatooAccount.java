package databench.jpa.batoo;

import java.util.ArrayList;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import databench.jpa.JpaAccount;

@Entity
public class JpaBatooAccount implements JpaAccount {

	@Id
	final Integer id;

	private String transferValuesString = "";

	private int balance = 0;

	@Version
	private int version = 0;

	public JpaBatooAccount() {
		id = null;
	}

	public JpaBatooAccount(Integer id) {
		super();
		this.id = id;
	}

	public void transfer(int value) {
		transferValuesString += "," + value;
		balance += value;
	}

	public ArrayList<Integer> getTransferValues() {
		String[] split = transferValuesString.split(",");
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for (int i = 1; i < split.length; i++)
			ret.add(Integer.parseInt(split[i]));
		return ret;
	}

	public int getBalance() {
		return balance;
	}

	public int getVersion() {
		return version;
	}

}
