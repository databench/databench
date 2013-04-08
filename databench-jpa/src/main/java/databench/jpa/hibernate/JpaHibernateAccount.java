package databench.jpa.hibernate;

import java.util.ArrayList;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import databench.jpa.JpaAccount;

@Entity
public class JpaHibernateAccount implements JpaAccount {

	@Id
	final Integer id;
	private ArrayList<Integer> transferValues = new ArrayList<Integer>();
	private int balance = 0;

	@Version
	private int version = 0;

	public JpaHibernateAccount() {
		id = null;
	}

	public JpaHibernateAccount(Integer id) {
		super();
		this.id = id;
	}

	public void transfer(int value) {
		transferValues.add(value);
		balance += value;
	}

	public ArrayList<Integer> getTransferValues() {
		return transferValues;
	}

	public int getBalance() {
		return balance;
	}

	public int getVersion() {
		return version;
	}

}
