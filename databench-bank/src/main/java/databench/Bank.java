package databench;

import java.io.Serializable;

public interface Bank<ID> extends Serializable {

	/**
	 * Called first to create accounts.
	 */
	ID[] setUp(Integer numberOfAccounts);

	/**
	 * Called on each VM before the benchmark.
	 */
	void warmUp();

	void transfer(ID from, ID to, int amount);

	AccountStatus getAccountStatus(ID id);

	void tearDown();

	String additionalVMParameters(boolean forMultipleVMs);

}
