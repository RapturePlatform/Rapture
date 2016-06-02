package rapture.httpapi.series;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.SeriesDouble;
import rapture.common.SeriesRepoConfig;
import rapture.common.SeriesString;
import rapture.common.client.HttpSeriesApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.RaptureException;

public class SeriesApiTest {
    String raptureUrl = null;
    private String raptureUser = null;
    private String rapturePass = null;
    private HttpLoginApi raptureLogin = null;
    private HttpSeriesApi seriesApi=null;
    
    @BeforeClass(groups={"series","cassandra", "nightly"})
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void beforeTest(@Optional("http://localhost:8665/rapture")String url, @Optional("rapture")String user, @Optional("rapture")String password)  {
        raptureUrl=url;
        raptureUser=user;
        rapturePass=password;
        raptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(raptureUser, rapturePass));

        try {
            raptureLogin.login();
            seriesApi = new HttpSeriesApi(raptureLogin);

        } catch (RaptureException e) {
            e.printStackTrace();
        }   
    }
    
    @Test (groups={"series","cassandra", "nightly"})
    public void testAddStringsToSeries () {
        int MAX_VALUES=50;
        String repoName="testSeries"+System.nanoTime();
        String config = "SREP {} USING CASSANDRA {keyspace=\""+repoName+"KS\", cf=\""+repoName+"CF\"}";
        seriesApi.createSeriesRepo("//"+repoName, config);
        List <String> pointKeys = new ArrayList <String>();
        List <String> pointValues = new ArrayList <String>();
        for (int i = 0; i < MAX_VALUES;i++) {
            pointKeys.add(new Integer (i).toString());
            pointValues.add("testValue"+i);
        }
        String newSeries="//"+repoName+"/addStrings"+System.nanoTime();
        Reporter.log("Adding "+pointKeys.size() + " points to "+newSeries, true);
        seriesApi.addStringsToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in "+newSeries, true);
        List <SeriesString> seriesList=seriesApi.getPointsAsStrings(newSeries);
        Assert.assertTrue(seriesList.size() > 0);
        for (SeriesString s : seriesList) {
            Assert.assertTrue(pointKeys.contains(s.getKey()));
            Assert.assertTrue(pointValues.contains(s.getValue()));
        }         
    }
    
    @Test (groups={"series","cassandra", "nightly"})
    public void testAddLongsToSeries () {
        int MAX_VALUES=50;
        String repoName="testSeries"+System.nanoTime();
        String config = "SREP {} USING CASSANDRA {keyspace=\""+repoName+"KS\", cf=\""+repoName+"CF\"}";
        seriesApi.createSeriesRepo("//"+repoName, config);
        List <String> pointKeys = new ArrayList <String>();
        List <Long> pointValues = new ArrayList <Long>();
        for (int i = 0; i < MAX_VALUES;i++) {
            pointKeys.add(new Integer (i).toString());
            pointValues.add(new Long(i));
        }
        String newSeries="//"+repoName+"/addLongs"+System.nanoTime();
        Reporter.log("Adding "+pointKeys.size() + " points to "+newSeries, true);
        seriesApi.addLongsToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in "+newSeries, true);
        List <SeriesDouble> seriesList=seriesApi.getPointsAsDoubles(newSeries);
        Assert.assertTrue(seriesList.size() > 0);
        for (SeriesDouble s : seriesList) {
            Assert.assertTrue(pointKeys.contains(s.getKey()));
            Assert.assertTrue(pointValues.contains(s.getValue().longValue()));
        }         
    }
    
    @Test (groups={"series","cassandra", "nightly"})
    public void testAddDoublesToSeries () {
        int MAX_VALUES=50;
        String repoName="testSeries"+System.nanoTime();
        String config = "SREP {} USING CASSANDRA {keyspace=\""+repoName+"KS\", cf=\""+repoName+"CF\"}";
        seriesApi.createSeriesRepo("//"+repoName, config);
        List <String> pointKeys = new ArrayList <String>();
        List <Double> pointValues = new ArrayList <Double>();
        for (int i = 0; i < MAX_VALUES;i++) {
            pointKeys.add(new Integer (i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries="//"+repoName+"/addDoubles"+System.nanoTime();
        Reporter.log("Adding "+pointKeys.size() + " points to "+newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in "+newSeries, true);
        List <SeriesDouble> seriesList=seriesApi.getPointsAsDoubles(newSeries);
        Assert.assertTrue(seriesList.size() > 0);
        for (SeriesDouble s : seriesList) {
            Assert.assertTrue(pointKeys.contains(s.getKey()));
            Assert.assertTrue(pointValues.contains(s.getValue().doubleValue()));
        }         
    }
    
    @Test (groups={"series","cassandra", "nightly"})
    public void testDoubleSeriesRange () {
        int MAX_VALUES=200;
        int OFFSET=1000;
        int LOW_VALUE=MAX_VALUES / 4;
        int HIGH_VALUE=3*(MAX_VALUES / 4);
        String repoName="testSeries"+System.nanoTime();
        String config = "SREP {} USING CASSANDRA {keyspace=\""+repoName+"KS\", cf=\""+repoName+"CF\"}";
        seriesApi.createSeriesRepo("//"+repoName, config);
        List <String> pointKeys = new ArrayList <String>();
        List <Double> pointValues = new ArrayList <Double>();
        for (int i = OFFSET; i < MAX_VALUES+OFFSET;i++) {
            pointKeys.add(new Integer (i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries="//"+repoName+"/getDoublesRanges"+System.nanoTime();
        Reporter.log("Adding "+pointKeys.size() + " points to "+newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in "+newSeries, true);
        List <SeriesDouble> seriesList= seriesApi.getPointsInRangeAsDoubles(newSeries, new Integer(LOW_VALUE+OFFSET).toString(), new Integer(HIGH_VALUE+OFFSET).toString(), MAX_VALUES);
        Assert.assertTrue(seriesList.size() > 0);
        for (SeriesDouble s :seriesList) {
            Assert.assertTrue (Integer.parseInt(s.getKey()) >=(LOW_VALUE+OFFSET) && Integer.parseInt(s.getKey()) <=(HIGH_VALUE+OFFSET),"Key "+ s.getKey() +" not in range");
        }         
    }
    
    @Test (groups={"series","cassandra", "nightly"})
    public void testDeleteSeriesByKey () {
        int MAX_VALUES=200;
        String repoName="testSeries"+System.nanoTime();
        String config = "SREP {} USING CASSANDRA {keyspace=\""+repoName+"KS\", cf=\""+repoName+"CF\"}";
        seriesApi.createSeriesRepo("//"+repoName, config);
        List <String> pointKeys = new ArrayList <String>();
        List <Double> pointValues = new ArrayList <Double>();
        for (int i = 0; i < MAX_VALUES;i++) {
            pointKeys.add(new Integer (i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries="//"+repoName+"/deleteSeriesByKey"+System.nanoTime();
        Reporter.log("Adding "+pointKeys.size() + " points to "+newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        
        for (int i=0;i<MAX_VALUES/2;i++)
            pointKeys.remove(0);
        seriesApi.deletePointsFromSeriesByPointKey(newSeries, pointKeys);
        for (SeriesDouble s : seriesApi.getPointsAsDoubles(newSeries)) {
            Assert.assertTrue(Integer.parseInt(s.getKey()) < (MAX_VALUES/2));
        } 
    }
    
    @Test (groups={"series","cassandra", "nightly"})
    public void testDeleteAllSeriesPoints () {
        int MAX_VALUES=200;
        String repoName="testSeries"+System.nanoTime();
        String config = "SREP {} USING CASSANDRA {keyspace=\""+repoName+"KS\", cf=\""+repoName+"CF\"}";
        seriesApi.createSeriesRepo("//"+repoName, config);
        List <String> pointKeys = new ArrayList <String>();
        List <Double> pointValues = new ArrayList <Double>();
        for (int i = 0; i < MAX_VALUES;i++) {
            pointKeys.add(new Integer (i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries="//"+repoName+"/deleteSeries"+System.nanoTime();
        Reporter.log("Adding "+pointKeys.size() + " points to "+newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        seriesApi.deletePointsFromSeries(newSeries);
        
        Assert.assertEquals(seriesApi.getPoints(newSeries).size(),0);
       
    }
    
    @AfterClass(groups={"series","cassandra", "nightly"})
    public void AfterTest(){
        //delete all repos
        List<SeriesRepoConfig> seriesRepositories = seriesApi.getSeriesRepoConfigs();
        
        for(SeriesRepoConfig repo:seriesRepositories ){
            if(repo.getAuthority().contains("testSeries") ){
                String uriToDelete = repo.getAuthority();
                Reporter.log("**** Deleting series repo: " + uriToDelete,true);
                seriesApi.deleteSeriesRepo(uriToDelete);
            }
        }
        
    }
}
