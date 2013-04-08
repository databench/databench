package databench.ebean;

import java.util.List;

import javax.persistence.OptimisticLockException;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.TxCallable;
import com.avaje.ebean.TxRunnable;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.core.DefaultServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;

import databench.AccountStatus;
import databench.Bank;
import databench.database.PostgreSqlDatabase;

public class EbeanPostgreSubject implements Bank<Integer> {

	private static final long serialVersionUID = 1L;

	private EbeanServer ebean = EbeanServerFactory.create(buildConfig());

	@Override
	public Integer[] setUp(Integer numberOfAccounts) {
		createSchema();
		createAccounts(numberOfAccounts);
		return accountsIds();
	}

	private Integer[] accountsIds() {
		List<Object> ids = ebean.find(EbeanAccount.class).findIds();
		return toArray(ids);
	}

	private Integer[] toArray(List<Object> ids) {
		Integer[] array = new Integer[ids.size()];
		for (int i = 0; i < ids.size(); i++)
			array[i] = (Integer) ids.get(i);
		return array;
	}

	private void createSchema() {
		DefaultServer defaultServer = (DefaultServer) ebean;
		DdlGenerator ddlGenerator = defaultServer.getDdlGenerator();
		String createDdl = ddlGenerator.generateCreateDdl();
		ddlGenerator.runScript(false, createDdl);
	}

	@Override
	public void warmUp() {
	}

	@Override
	public void transfer(final Integer from, final Integer to, final int amount) {
		run(new TxCallable<Object>() {

			@Override
			public AccountStatus call() {
				transfer(from, -amount);
				transfer(to, amount);
				return null;
			}

			private void transfer(Integer id, int value) {
				EbeanAccount account = getAccountById(id);
				account.setTransferValues(account.getTransferValues() + ","
						+ value);
				account.setBalance(account.getBalance() + value);
				ebean.update(account);
			}
		});
	}

	@Override
	public AccountStatus getAccountStatus(final Integer id) {
		return run(new TxCallable<AccountStatus>() {
			@Override
			public AccountStatus call() {
				EbeanAccount account = getAccountById(id);
				String[] transferValues = account.getTransferValues()
						.split(",");
				return new AccountStatus(account.getBalance(),
						toArray(transferValues));
			}

			private int[] toArray(String[] transferValues) {
				int[] array = new int[transferValues.length - 1];
				for (int i = 1; i < transferValues.length; i++)
					array[i - 1] = Integer.parseInt(transferValues[i]);
				return array;
			}
		});
	}

	@Override
	public void tearDown() {
	}

	@Override
	public String additionalVMParameters(boolean forMultipleVMs) {
		return "";
	}

	private <T> T run(TxCallable<T> tx) {
		try {
			return ebean.execute(tx);
		} catch (OptimisticLockException e) {
			return run(tx);
		}
	}

	private EbeanAccount getAccountById(Integer id) {
		return ebean.find(EbeanAccount.class, id);
	}

	private void createAccounts(final Integer numberOfAccounts) {
		ebean.execute(new TxRunnable() {
			@Override
			public void run() {
				for (int i = 0; i < numberOfAccounts; i++) {
					ebean.insert(new EbeanAccount());
				}
			}
		});
	}

	private ServerConfig buildConfig() {
		ServerConfig config = new ServerConfig();
		config.setName("databench");
		config.setDataSourceConfig(buildDataSource());
		config.setDefaultServer(false);
		config.setRegister(false);
		config.addClass(EbeanAccount.class);
		return config;
	}

	private DataSourceConfig buildDataSource() {
		DataSourceConfig postgresDb = new DataSourceConfig();
		postgresDb.setDriver(PostgreSqlDatabase.jdbcDriver());
		postgresDb.setUsername(PostgreSqlDatabase.user());
		postgresDb.setPassword(PostgreSqlDatabase.password());
		postgresDb.setUrl(PostgreSqlDatabase.url());
		return postgresDb;
	}

}
