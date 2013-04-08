package databench.ebean;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class EbeanAccount implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;

	@Column(columnDefinition = "VARCHAR")
	private String transferValues = "";

	private int balance = 0;

	@Version
	private int version = 0;

	public EbeanAccount() {
	}

	public int getBalance() {
		return balance;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setBalance(int balance) {
		this.balance = balance;
	}

	public String getTransferValues() {
		return transferValues;
	}

	public void setTransferValues(String transferValues) {
		this.transferValues = transferValues;
	}

}