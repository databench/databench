package databench.prevayler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;

import databench.AccountStatus;
import databench.Bank;
import databench.SingleVMBank;
import databench.database.FolderDatabase;

public class PrevaylerSubject implements Bank<Integer>, SingleVMBank {

	private static final long serialVersionUID = 1L;

	private final Prevayler<Map<Integer, PrevaylerAccount>> prevayler = createPrevayler();

	public Integer[] setUp(Integer numberOfAccounts) {
		return prevayler
				.execute(new CreateAccountsTransaction(numberOfAccounts));
	}

	@Override
	public void warmUp() {
	}

	@Override
	public String additionalVMParameters(boolean forMultipleVMs) {
		return "";
	}

	public void transfer(Integer from, Integer to, int amount) {
		prevayler.execute(new TransferTransaction(from, to, amount));
	}

	public AccountStatus getAccountStatus(Integer id) {
		try {
			return prevayler.execute(new GetAccountStatusQuery(id));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void tearDown() {
		try {
			prevayler.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Prevayler<Map<Integer, PrevaylerAccount>> createPrevayler() {
		try {
			return getFactory().create();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private PrevaylerFactory<Map<Integer, PrevaylerAccount>> getFactory() {
		PrevaylerFactory<Map<Integer, PrevaylerAccount>> factory = new PrevaylerFactory<Map<Integer, PrevaylerAccount>>();
		factory.configurePrevalentSystem(new HashMap<Integer, PrevaylerAccount>());
		factory.configurePrevalenceDirectory(FolderDatabase.path());
		return factory;
	}

}
