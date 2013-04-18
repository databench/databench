package databench.chronicle;

import databench.AccountStatus;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntProcedure;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author peter.lawrey
 */
public class ChronicleSubjectTest {
    @org.junit.Test
    public void testTransfer() throws Exception {
        int transactions = 400000;
        Random rand = new Random();

        ChronicleSubject subject = new ChronicleSubject();
        subject.warmUp();

        long start = System.nanoTime();
        Integer[] ids = subject.setUp(500);
        for (int t = 0; t < transactions; t++) {
            int from = rand.nextInt(ids.length);
            int to = rand.nextInt(ids.length - 1);
            if (to >= from) to++;
            subject.transfer(from, to, t);
        }
        // check results
        TIntIntHashMap countAmounts = new TIntIntHashMap();
        for (Integer id : ids) {
            AccountStatus accountStatus = subject.getAccountStatus(id);
            for (int amount : accountStatus.transferredAmounts)
                countAmounts.adjustOrPutValue(amount, 1, 1);
        }
        assertEquals(transactions, countAmounts.size());
        countAmounts.forEachValue(new TIntProcedure() {
            @Override
            public boolean execute(int count) {
                assertEquals(2, count);
                return true;
            }
        });
        subject.tearDown();
        long time = System.nanoTime() - start;
        System.out.printf("Perform and checked %,d transfers per second%n", transactions * 1000000000L / time);
    }
}
