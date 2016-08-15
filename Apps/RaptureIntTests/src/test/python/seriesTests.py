import raptureAPI
import multipart
import json
import time
import base64

# TODO These need to be parameters

repo = '//nightly_python'

seriesRepoUri = 'series:'+repo

site = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
rapture = raptureAPI.raptureAPI(site, username, password)


def test_delete_all_series_points():
    MAX_VALUES=200;
      # Add string to series test
    config = " {} USING MONGODB {prefix=\"nightly\"}"
    if(rapture.doSeries_SeriesRepoExists(seriesRepoUri)):
        rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    rapture.doSeries_CreateSeriesRepo(seriesRepoUri, "SREP "+config)
      
    pointKeys = [];
    pointValues = [];
      
    for x in range(0, MAX_VALUES):
        pointKeys.append( x );
        pointValues.append(x);
      
    newSeries=seriesRepoUri+"/deleteSeries"+ str(int(time.time()))
    rapture.doSeries_AddDoublesToSeries(newSeries, pointKeys, pointValues)
    rapture.doSeries_DeletePointsFromSeries(newSeries)
    assert (len(rapture.doSeries_GetPoints(newSeries))==0)

def test_delete_and_update_series():
    MAX_VALUES=200;
    
      # Add string to series test
    config = " {} USING MONGODB {prefix=\"nightly\"}"
    if(rapture.doSeries_SeriesRepoExists(seriesRepoUri)):
        rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    rapture.doSeries_CreateSeriesRepo(seriesRepoUri, "SREP "+config)
      
    pointKeys = [];
    pointValues = [];
      
    for x in range(0, MAX_VALUES):
        pointKeys.append( x );
        pointValues.append(x);
      
    newSeries=seriesRepoUri+"/deleteUpdateSeries"+ str(int(time.time()))
    rapture.doSeries_AddDoublesToSeries(newSeries, pointKeys, pointValues)
    
    for x in range(0, MAX_VALUES/2):
        pointKeys.remove(x)
    rapture.doSeries_DeletePointsFromSeriesByPointKey(newSeries, pointKeys)
    seriesList=rapture.doSeries_GetPointsAsDoubles(newSeries);
    for s in seriesList:
            k=int(s['key'])
            assert (k < (MAX_VALUES/2));
    updateMap={};
    for s in seriesList:
        rapture.doSeries_AddDoubleToSeries(newSeries, str(s['key']), int(s['value'])+MAX_VALUES);        
        updateMap[str(s['key'])]=int(s['value'])+MAX_VALUES;
    seriesList=rapture.doSeries_GetPointsAsDoubles(newSeries);
    for s in seriesList:
        assert int(s['value'])==updateMap[str(s['key'])];

def test_delete_series_by_key():
    MAX_VALUES=200;
    
      # Add string to series test
    config = " {} USING MONGODB {prefix=\"nightly\"}"
    if(rapture.doSeries_SeriesRepoExists(seriesRepoUri)):
        rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    rapture.doSeries_CreateSeriesRepo(seriesRepoUri, "SREP "+config)
      
    pointKeys = [];
    pointValues = [];
      
    for x in range(0, MAX_VALUES):
        pointKeys.append( x );
        pointValues.append(int(x));
      
    newSeries=seriesRepoUri+"/deleteSeriesByKey"+ str(int(time.time()))
    rapture.doSeries_AddDoublesToSeries(newSeries, pointKeys, pointValues)
    
    for x in range(0, MAX_VALUES/2):
        pointKeys.remove(x)
    rapture.doSeries_DeletePointsFromSeriesByPointKey(newSeries, pointKeys)
    seriesList=rapture.doSeries_GetPointsAsStrings(newSeries);
    for s in seriesList:
            k=int(s['key'])
            assert (k < (MAX_VALUES/2));
    pointKeys=[];
    for s in seriesList:
        pointKeys.append(int(s['key']))
    rapture.doSeries_DeletePointsFromSeriesByPointKey(newSeries, pointKeys)
    assert (len(rapture.doSeries_GetPointsAsStrings(newSeries))==0);


def test_doubles_series_range():
    MAX_VALUES=200;
    OFFSET=1000;
    LOW_VALUE=MAX_VALUES / 4;
    HIGH_VALUE=3*(MAX_VALUES / 4);
      # Add string to series test
    config = " {} USING MONGODB {prefix=\"nightly\"}"
    if(rapture.doSeries_SeriesRepoExists(seriesRepoUri)):
        rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    rapture.doSeries_CreateSeriesRepo(seriesRepoUri, "SREP "+config)
      
    pointKeys = [];
    pointValues = [];
      
    for x in range(OFFSET, MAX_VALUES+OFFSET):
        pointKeys.append( x );
        pointValues.append(int(x));
      
    newSeries=seriesRepoUri+"/getDoublesRanges"+ str(int(time.time()))
    rapture.doSeries_AddDoublesToSeries(newSeries, pointKeys, pointValues)
    seriesList= rapture.doSeries_GetPointsInRangeAsDoubles(newSeries, LOW_VALUE+OFFSET, HIGH_VALUE+OFFSET, MAX_VALUES);
    for s in seriesList:
        k=int(s['key'])
        assert (k >=(LOW_VALUE+OFFSET) & k <=(HIGH_VALUE+OFFSET));

def test_add_strings_to_series_and_update():
    MAX_VALUES=50
    # Add string to series test
    config = " {} USING MONGODB {prefix=\"nightly\"}"
    if(rapture.doSeries_SeriesRepoExists(seriesRepoUri)):
        rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    rapture.doSeries_CreateSeriesRepo(seriesRepoUri, "SREP "+config)
    
    pointKeys = [];
    pointValues = [];
    
    for x in range(0, MAX_VALUES):
        pointKeys.append( x );
        pointValues.append(str(x));
    
    newSeries=seriesRepoUri+"/updateStrings"+ str(int(time.time()))
    rapture.doSeries_AddStringsToSeries(newSeries, pointKeys, pointValues)
    
    seriesList=rapture.doSeries_GetPointsAsStrings(newSeries);
    assert len(seriesList) > 0
    
    keySet = set()
    valueSet = set()
    
    for s in seriesList:
        keySet.add(int(s['key']))
        valueSet.add(str(s['value']))
    
    assert(keySet == set(pointKeys))
    assert(valueSet == set(pointValues))
    
    checkMap={}
    for i in range(0, MAX_VALUES):
        rapture.doSeries_AddStringToSeries(newSeries, i, str(i+MAX_VALUES));
        checkMap[i]=i+MAX_VALUES;
        
    seriesList=rapture.doSeries_GetPointsAsStrings(newSeries);
    for s in seriesList:
        assert str(s['value']) ==str(checkMap[int(s['key'])])


def test_add_doubles_to_series_and_update():
    MAX_VALUES=50
    # Add string to series test
    config = " {} USING MONGODB {prefix=\"nightly\"}"
    if(rapture.doSeries_SeriesRepoExists(seriesRepoUri)):
        rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    rapture.doSeries_CreateSeriesRepo(seriesRepoUri, "SREP "+config)
    
    pointKeys = [];
    pointValues = [];
    
    for x in range(0, MAX_VALUES):
        pointKeys.append( x );
        pointValues.append(int(x));
    
    newSeries=seriesRepoUri+"/updateDoubles"+ str(int(time.time()))
    rapture.doSeries_AddLongsToSeries(newSeries, pointKeys, pointValues)
    
    seriesList=rapture.doSeries_GetPointsAsDoubles(newSeries);
    assert len(seriesList) > 0
    
    keySet = set()
    valueSet = set()
    
    for s in seriesList:
        keySet.add(int(s['key']))
        valueSet.add(int(s['value']))
    
    assert(keySet == set(pointKeys))
    assert(valueSet == set(pointValues))
    
    checkMap={}
    for i in range(0, MAX_VALUES):
        rapture.doSeries_AddDoubleToSeries(newSeries, i, (i+MAX_VALUES));
        checkMap[i]=i+MAX_VALUES;
        
    seriesList=rapture.doSeries_GetPointsAsDoubles(newSeries);
    for s in seriesList:
        assert int(s['value']) ==checkMap[int(s['key'])]
    
def test_add_doubles_to_series():
    MAX_VALUES=50
    # Add string to series test
    config = " {} USING MONGODB {prefix=\"nightly\"}"
    if(rapture.doSeries_SeriesRepoExists(seriesRepoUri)):
        rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    rapture.doSeries_CreateSeriesRepo(seriesRepoUri, "SREP "+config)
    
    pointKeys = [];
    pointValues = [];
    
    for x in range(0, MAX_VALUES):
        pointKeys.append( x );
        pointValues.append(int(x));
    
    newSeries=seriesRepoUri+"/addLongs"+ str(int(time.time()))
    rapture.doSeries_AddLongsToSeries(newSeries, pointKeys, pointValues)
    
    seriesList=rapture.doSeries_GetPointsAsDoubles(newSeries);
    assert len(seriesList) > 0
    
    keySet = set()
    valueSet = set()
    
    for s in seriesList:
        keySet.add(int(s['key']))
        valueSet.add(int(s['value']))
    
    assert(keySet == set(pointKeys))
    assert(valueSet == set(pointValues))

def test_add_strings_to_series():
    MAX_VALUES=50
    # Add string to series test
    config = " {} USING MONGODB {prefix=\"nightly\"}"
    if(rapture.doSeries_SeriesRepoExists(seriesRepoUri)):
        rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    rapture.doSeries_CreateSeriesRepo(seriesRepoUri, "SREP "+config)
    
    pointKeys = [];
    pointValues = [];
    
    for x in range(0, MAX_VALUES):
        pointKeys.append( x );
        pointValues.append(str(x));
    
    newSeries=seriesRepoUri+"/addStrings"+ str(int(time.time()))
    rapture.doSeries_AddStringsToSeries(newSeries, pointKeys, pointValues)
    
    seriesList=rapture.doSeries_GetPointsAsStrings(newSeries);
    assert len(seriesList) > 0
    
    keySet = set()
    valueSet = set()
    
    for s in seriesList:
        keySet.add(int(s['key']))
        valueSet.add(str(s['value']))
    assert(keySet == set(pointKeys))
    assert(valueSet == set(pointValues))
    
        
    
