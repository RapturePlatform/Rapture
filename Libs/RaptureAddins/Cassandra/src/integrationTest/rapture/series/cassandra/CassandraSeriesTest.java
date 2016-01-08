package rapture.series.cassandra;

import java.util.Map;

import com.google.common.collect.Maps;

import rapture.repo.SeriesRepo;
import rapture.series.SeriesStore;

public class CassandraSeriesTest extends SeriesContract {
    @Override
    public SeriesRepo makeRepo() {
        Map<String,String> config = Maps.newHashMap();
        config.put("keyspace", "testSeriesContract");
        config.put("cf", "testSeries");
        CassandraSeriesStore result = new CassandraSeriesStore();
        result.setInstanceName("CassSeriesTest");
        result.setConfig(config);
        result.drop();
        return new SeriesRepo(result);
    }
}
