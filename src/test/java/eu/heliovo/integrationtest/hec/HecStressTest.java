package eu.heliovo.integrationtest.hec;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import eu.heliovo.clientapi.query.HelioQueryResult;
import eu.heliovo.integrationtest.AbstractIntegrationTest;
import eu.heliovo.integrationtest.util.DataReaderUtil;
import eu.heliovo.registryclient.HelioServiceName;

public class HecStressTest {

    private static class HecStressTestImpl extends AbstractIntegrationTest {        
        public HecStressTestImpl(String[] startTime, String[] endTime, String[] from, String where, String expectedResultFile) {
            super(HelioServiceName.HEC, startTime, endTime, from, where, expectedResultFile);
        }
        
    }
    

    /**
     * Create the integration test
     * @param startTime the start time list
     * @param endTime the end time list
     * @param from the list of from catalogs
     * @param where the where clause
     */
    public Callable<HelioQueryResult> doHecStressTest(final String[] startTime, final String[] endTime, final String[] from, final String where, final String expectedResultFile) {
        Callable<HelioQueryResult> worker = new Callable<HelioQueryResult>() {
            @Override
            public HelioQueryResult call() throws Exception {
                HecStressTestImpl stressTest = new HecStressTestImpl(startTime, endTime, from, where, expectedResultFile);
                stressTest.testCatalog();
                return null;
            }
        };
        return worker;
    }
    
    @Ignore @Test public void testParallelHecCalls() throws Exception {
        DataReaderUtil reader = new DataReaderUtil(DataReaderUtil.class.getResourceAsStream("/hec/hec_data.txt"));
        Collection<Object[]> testData = reader.getTestData();
        Collection<Callable<HelioQueryResult>> workers = new ArrayList<Callable<HelioQueryResult>>();
        for (Object[] objects : testData) {
            Method method = getClass().getMethod("doHecStressTest", String[].class, String[].class, String[].class, String.class, String.class);
            assertNotNull(method);
            @SuppressWarnings("unchecked")
            Callable<HelioQueryResult> worker = (Callable<HelioQueryResult>)method.invoke(this, objects);
            workers.add(worker);
        }
        
        ExecutorService threadPool = Executors.newFixedThreadPool(30);
        List<Future<HelioQueryResult>> futures = threadPool.invokeAll(workers);
        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.MINUTES);
        for (Future<HelioQueryResult> future : futures) {
            future.get();
        }
    }
}
