package databench.chronicle;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import databench.AccountStatus;
import databench.Bank;
import databench.SingleVMBank;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntProcedure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author peter.lawrey
 */
public class ChronicleSubject implements Bank<Integer>, SingleVMBank {
	private static boolean s_warmup = false;
	private static final String TMP = System.getProperty("java.io.tmpdir");
	private final List<ChronicleAccount> accounts = new ArrayList<ChronicleAccount>();
	private final IndexedChronicle chronicle;
	private final Excerpt excerpt;

	public ChronicleSubject() throws IOException {
		this("subject");
	}

	public ChronicleSubject(String name) throws IOException {
		String basePath = TMP + "/chronicle-" + name;
		ChronicleTools.deleteOnExit(basePath);
		chronicle = new IndexedChronicle(basePath);
		chronicle.useUnsafe(true);
		excerpt = chronicle.createExcerpt();
	}

	@Override
	public Integer[] setUp(Integer numberOfAccounts) {
		Integer[] accountIds = new Integer[numberOfAccounts];
		for (int i = 0; i < numberOfAccounts; i++) {
			accountIds[i] = i;
			accounts.add(new ChronicleAccount());
		}
		return accountIds;
	}

	@Override
	public void warmUp() {
		warmUp0();
	}

	private static void warmUp0() {
		if (s_warmup)
			return;
		s_warmup = true;
		try {
			int transactions = 20000;
			Random rand = new Random();
			ChronicleSubject subject = new ChronicleSubject("warmup");

			long start = System.nanoTime();
			Integer[] ids = subject.setUp(500);
			for (int t = 0; t < transactions; t++) {
				int from = rand.nextInt(ids.length);
				int to = rand.nextInt(ids.length - 1);
				if (to >= from)
					to++;
				subject.transfer(from, to, t);
			}
			// check results
			TIntIntHashMap countAmounts = new TIntIntHashMap();
			for (Integer id : ids) {
				AccountStatus accountStatus = subject.getAccountStatus(id);
				for (int amount : accountStatus.transferredAmounts)
					countAmounts.adjustOrPutValue(amount, 1, 1);
			}
			assert transactions == countAmounts.size();
			countAmounts.forEachValue(new TIntProcedure() {
				@Override
				public boolean execute(int count) {
					assert 2 == count;
					return true;
				}
			});
			subject.tearDown();
			long time = System.nanoTime() - start;
		} catch (IOException e) {
			throw new AssertionError(e);
		}

	}

	@Override
	public String additionalVMParameters(boolean forMultipleVMs) {
		return "";
	}

	@Override
	public synchronized void transfer(Integer from, Integer to, int amount) {
//		synchronized (this) {
			accounts.get(from).transfer(-amount);
			accounts.get(to).transfer(amount);

			excerpt.startExcerpt(12);
			excerpt.writeInt(from);
			excerpt.writeInt(to);
			excerpt.writeInt(amount);
			excerpt.finish();
//		}
	}

	@Override
	public AccountStatus getAccountStatus(Integer id) {
//		synchronized (this) {
			return accounts.get(id).getAccountStatus();
//		}
	}

	@Override
	public void tearDown() {
		chronicle.close();
	}
}
