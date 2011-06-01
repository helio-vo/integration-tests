package eu.heliovo.integrationtest.hec;

import java.util.Collection;

import org.junit.runner.RunWith;

import eu.heliovo.clientapi.model.service.HelioServiceName;
import eu.heliovo.integrationtest.AbstractIntegrationTest;
import eu.heliovo.integrationtest.DataReaderUtil;
import eu.heliovo.integrationtest.util.LabelledParameterized;
import eu.heliovo.integrationtest.util.LabelledParameterized.Parameters;

@RunWith(LabelledParameterized.class)
public class HecIntegrationTest extends AbstractIntegrationTest {
    /**
     * Create the integration test
     * @param startTime the start time list
     * @param endTime the end time list
     * @param from the list of from catalogs
     * @param where the where clause
     */
    public HecIntegrationTest(String[] startTime, String[] endTime, String[] from, String where, String expectedResultFile) {
        super(HelioServiceName.HEC, startTime, endTime, from, where, expectedResultFile);
    }
    
    @Parameters public static Collection<Object[]> testData() {
        DataReaderUtil reader = new DataReaderUtil(DataReaderUtil.class.getResourceAsStream("/hec/hec_data.txt"));
        return reader.getTestData();
    }    
}
