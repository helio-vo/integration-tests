package eu.heliovo.integrationtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.GenericXmlApplicationContext;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import eu.heliovo.clientapi.HelioClient;
import eu.heliovo.clientapi.query.HelioQueryResult;
import eu.heliovo.clientapi.query.QueryService;
import eu.heliovo.clientapi.utils.STILUtils;
import eu.heliovo.clientapi.workerservice.HelioWorkerServiceHandler.Phase;
import eu.heliovo.integrationtest.hec.HecIntegrationTest;
import eu.heliovo.registryclient.HelioServiceName;
import eu.heliovo.registryclient.ServiceCapability;

/**
 * Abstract base class for integration tests.
 * Sub classes should overwrite the default constructor and should provide a 
 * method with signature 
 * <pre>
 *     @Parameters public static Collection<Object[]> testData() {...}
 * </pre>
 * See {@link HecIntegrationTest} for an example.
 * @author MarcoSoldati
 *
 */
public abstract class AbstractIntegrationTest {
    
    /**
     *  the name of the service
     */
    private final HelioServiceName serviceName;

    private final String[] startTime;

    private final String[] endTime;

    private final String[] from;

    private final String where;

    private final String expectedResultFile;
    
    private HelioClient helioClient;
    
    @Before public void setup() {
        GenericXmlApplicationContext context = new GenericXmlApplicationContext("classpath:spring/clientapi-main.xml");        
        helioClient = (HelioClient) context.getBean("helioClient");        
    }
    
    /**
     * Create the integration test
     * @param startTime the start time list
     * @param endTime the end time list
     * @param from the list of from catalogs
     * @param where the where clause
     */
    public AbstractIntegrationTest(HelioServiceName serviceName, String[] startTime, String[] endTime, String[] from, String where, String expectedResultFile) {
        assertNotNull(serviceName);
        assertNotNull(startTime);
        assertNotNull(endTime);
        assertNotNull(from);
        assertNotNull(expectedResultFile);
        this.serviceName = serviceName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.from = from;
        this.where = where;
        this.expectedResultFile = expectedResultFile;
    }
    
    @Test public void testCatalog() throws Exception {
        HelioQueryResult result = testAsyncQuery(serviceName, Arrays.asList(startTime), Arrays.asList(endTime), Arrays.asList(from), where);
        assertNotNull(result);
        
        STILUtils stilUtils = new STILUtils();
        StarTable[] actualTables;
        try {
            actualTables = stilUtils.read(result.asURL());
        } catch (Exception e) {
            System.err.println("Invalid URL " + result.asURL());
            System.err.println("Failed to read " + Arrays.toString(this.from) + " | " + Arrays.toString(startTime) + " | " + Arrays.toString(endTime) + " | " + where + " | " + expectedResultFile);
            throw e;
        }
        
        InputStream expectedResult = getClass().getResourceAsStream(expectedResultFile);
        assertNotNull(expectedResult);
        StarTable[] expectedTables = stilUtils.read(expectedResult);
        assertEquals("Expected " + expectedTables.length + " tables, but got: " + actualTables.length + " tables", 
                expectedTables.length, actualTables.length);
        assertTrue(expectedTables.length >= 1);
        for (int i = 0; i < expectedTables.length; i++) {
            StarTable expectedTable = expectedTables[i];
            StarTable actualTable = actualTables[i];
            assertEquals(expectedTable.getRowCount(), actualTable.getRowCount());
            assertEquals(expectedTable.getColumnCount(), actualTable.getColumnCount());
            
            System.out.println(Arrays.toString(this.from) + " | " + Arrays.toString(startTime) + " | " + Arrays.toString(endTime) + " | " + where + " | " + expectedResultFile + " -> " + actualTable.getRowCount());
            if (actualTable.getRowCount() > 0) {
                int startTimeColumn = -1; 
                int endTimeColumn = -1; 
                for (int j = 0; j < actualTable.getColumnCount(); j++) {
                    ColumnInfo currentColumn = actualTable.getColumnInfo(j);
                    if ("time_start".equals(currentColumn.getName())) {
                        startTimeColumn = j;
                    } else if ("time_end".equals(currentColumn.getName())) {
                        endTimeColumn = j;
                    }
                }
                // handle tables with only start time
                if (startTimeColumn >= 0 && endTimeColumn < 0) {
                    endTimeColumn = startTimeColumn;
                }
                
                System.out.println("Covered date range: " + (startTimeColumn >= 0 ? actualTable.getRow(0)[startTimeColumn] : "n/a") + " - " +
                        (endTimeColumn >= 0 ? actualTable.getRow(actualTable.getRowCount()-1)[endTimeColumn] : "n/a")) ;
            }
            
            // ensure at least one row in result.
            assertTrue("Result should contain at least 1 row", actualTable.getRowCount() > 0);
            
            // ensure the tables are the same
            for (int j = 0; j < expectedTable.getRowCount(); j++) {
                Object[] actualRow = actualTable.getRow(j);
                Object[] expectedRow = expectedTable.getRow(j);
                
                for (int k = 0; k < expectedRow.length; k++) {
                    if ("hec_id".equals(actualTable.getColumnInfo(k).getName())) {
                        // ignore column
                    } else {
                        assertEquals(expectedRow[k], actualRow[k]);
                    }
                }
            }
        }
    }
    

    /**
     * Call a HQI service and wait for the result
     * @param serviceName the service to call
     * @param startTime the time of start
     * @param endTime the time of end
     * @param from the from clause
     * @param where the where clause
     * @return
     */
    protected HelioQueryResult testAsyncQuery(HelioServiceName serviceName, List<String> startTime, List<String> endTime, List<String> from, String where) {
        QueryService queryService = (QueryService) helioClient.getServiceInstance(serviceName, null, ServiceCapability.ASYNC_QUERY_SERVICE);
        HelioQueryResult result = queryService.query(startTime, endTime, from, 100, 0, null);
        
        //System.out.println(result);
        if (result != null) {
            // get the result in 1 minute
            assertNotNull(result.asURL(60, TimeUnit.SECONDS));
            assertEquals(Phase.COMPLETED, result.getPhase());
            return result;
        }
        return null;
    }
    
    /**
     * the toString method will be used as Test name
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("(").append(from).append(")");
        return sb.toString();
    }
    
    /**
     * Return the default timeout in seconds.
     * This method can be overloaded by implmeentations to increase the timeout.
     * @return timeout in seconds.
     */
    protected long getDefaultTimeout() {
        return 60;
    }
}
