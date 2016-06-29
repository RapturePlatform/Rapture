import raptureAPI
import multipart
import json
import time
import base64
import random

# TODO These need to be parameters

repo = '//NightlyPython'

blobRepoUri = 'blob:'+repo
docRepoUri = 'document:'+repo
seriesRepoUri = 'series:'+repo

rapture = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
rapture = raptureAPI.raptureAPI(rapture, username, password)


premier = ["1,Leicester City,36,30,77", "2,Tottenham Hotspur,36,39,70", "3,Arsenal,36,25,67", "4,Manchester City,36,30,64", "5,Manchester Utd,35,12,60", 
 "6,West Ham Utd,35,17,59", "7,Southampton,36,14,57", "8,Liverpool,35,11,55", "9,Chelsea,35,7,48", "10,Stoke City,36,-14,48", 
 "11,Everton,35,6,44", "12,Watford,35,-6,44", "13,Swansea City,36,-13,43", "14,West Bromwich Albion,36,-14,41", "15,Bournemouth,36,-20,41",
 "16,Crystal Palace,36,-10,39", "17,Newcastle Utd,36,-25,33", "18,Sunder"+"land,35,-18,32", "19,Norwich City,35,-26,31", "20,Aston Villa,36,-45,16"]

championship = ["1,Burnley,45,34,90", "2,Middlesbrough,45,32,88", "3,Brighton & Hove Albion,45,30,88", "4,Hull City,45,30,80", 
 "5,Derby County,45,24,78", "6,Sheffield Wednesday,45,22,74", "7,Cardiff City,45,5,67", "8,Ipswich Town,45,1,66", "9,Birmingham City,45,4,62", 
 "10,Brentford,45,1,62", "11,Preston North End,45,0,61", "12,Leeds Utd,45,-8,58", "13,Queens Park Rangers,45,-1,57", "14,Wolverhampton Wanderers,45,-6,55", 
 "15,Blackburn Rovers,45,-2,52", "16,Reading,45,-5,52", "17,Nottingham Forest,45,-5,52", "18,Bristol City,45,-16,52", "19,Huddersfield Town,45,-7,51", 
 "20,Rotherham Utd,45,-14,49", "21,Fulham,45,-14,48", "22,Charlton Athletic,45,-37,40", "23,Milton Keynes Dons,45,-29,39", "24,Bolton Wanderers,45,-39,30"]

def test_login():
    if 'valid' in rapture.context and rapture.context['valid']:
        print 'Logged in successfully '
    else:
        print "Login unsuccessful"
        assert false 

    config = " {} USING MONGODB {prefix=\"Nightly\"}"
    if(rapture.doDoc_DocRepoExists(docRepoUri)):
        rapture.doDoc_DeleteDocRepo(docRepoUri)
    rapture.doDoc_CreateDocRepo(docRepoUri, "NREP {} USING MONGODB {prefix=\"NightlyDoc\"}"+config)

    if(rapture.doBlob_BlobRepoExists(blobRepoUri)):
        rapture.doBlob_DeleteBlobRepo(blobRepoUri)
    rapture.doBlob_CreateBlobRepo(blobRepoUri, "BLOB {} USING MONGODB {prefix=\"NightlyBlob\"}"+config, "NREP {} USING MONGODB {prefix=\"nightlyMeta\"}"+config)

    if(rapture.doSeries_SeriesRepoExists(seriesRepoUri)):
        rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    rapture.doSeries_CreateSeriesRepo(seriesRepoUri, "SREP {} USING MONGODB {prefix=\"NightlySer\"}"+config)

def test_series():

# Create series using different API calls - AddStringToSeries and AddStringsToSeries
# (Can add others)

    premUri = seriesRepoUri+"/English/Premier"
    champUri = seriesRepoUri+"/English/Championship"

    query1="Watford:*"
    srch1 = rapture.doSearch_Search(query1)
    query2 = "repo:"+repo[2:]
    srch2 = rapture.doSearch_Search(query2)
    query3 = "scheme:series AND parts:Eng*"
    srch3 = rapture.doSearch_Search(query3)
    query4="Brentford:*"
    srch4 = rapture.doSearch_Search(query4)
    
    premKeys = []
    premVars = []
    for pr in premier:
       d = pr.split(',')
       premKeys.append(d[1])
       premVars.append(d[4])
    rapture.doSeries_AddStringsToSeries(premUri, premKeys, premVars)

    for ch in championship:
       d = ch.split(',')
       rapture.doSeries_AddStringToSeries(champUri, d[1], d[4])
    time.sleep(2);

# Test simple search
    srch = rapture.doSearch_Search(query1)
    expect = srch1['total'] + 1;
    print srch
    assert (srch['total'] == expect), query1+" returns "+str(srch['total'])+" expected "+str(expect)

# Test Repo search
    srch = rapture.doSearch_Search(query2)
    expect = srch2['total'] + 2;
    assert (srch['total'] == expect), query2+" returns "+str(srch['total'])+" expected "+str(expect)

# Test URI search
    srch = rapture.doSearch_Search(query3)
    expect = srch3['total'] + 2;
    assert (srch['total'] == expect), query3+" returns "+str(srch['total'])+" expected "+str(expect)

# Test search works after DeletePointsFromSeriesByPointKey
    rapture.doSeries_DeletePointsFromSeriesByPointKey(premUri, premKeys[6:12])
    time.sleep(2);
    srch = rapture.doSearch_Search(query1)
    expect = srch1['total']
    assert (srch['total'] == expect), query1+" returns "+str(srch['total'])+" expected "+str(expect)

# Test search works after DeleteSeriesRepo
    srch = rapture.doSearch_Search(query4)
    expect = srch4['total'] +1
    assert (srch['total'] == expect), query4+" returns "+str(srch['total'])+" expected "+str(expect)

    rapture.doSeries_DeleteSeriesRepo(seriesRepoUri)
    time.sleep(2);
    srch = rapture.doSearch_Search(query4)
    expect = srch4['total']
    assert (srch['total'] == expect), query4+" returns "+str(srch['total'])+" expected "+str(expect)

def test_doc():

# Create doc using different API calls - AddStringToDoc and AddStringsToDoc
# (Can add others)

    premUri = docRepoUri+"/English/Premier"
    champUri = docRepoUri+"/English/Championship"

    if(rapture.doDoc_DocExists(premUri)):
        rapture.doDoc_DeleteDoc(premUri)
    if(rapture.doDoc_DocExists(champUri)):
        rapture.doDoc_DeleteDoc(champUri)

    query1="Watford:*"
    srch1 = rapture.doSearch_Search(query1)
    query2 = "repo:"+repo[2:]
    srch2 = rapture.doSearch_Search(query2)
    query3 = "scheme:document AND parts:Eng*"
    srch3 = rapture.doSearch_Search(query3)
    query4="Brentford:*"
    srch4 = rapture.doSearch_Search(query4)
    
    premDoc = "{"
    for pr in premier:
       d = pr.split(',')
       premDoc += '"' + d[1] + '":"' + d[4] + '"'
       if d[0] == "20":
          premDoc += '}'
       else:
          premDoc += ','
    rapture.doDoc_PutDoc(premUri, premDoc)

    champDoc = "{"
    for ch in championship:
       d = ch.split(',')
       champDoc += '"' + d[1] + '":"' + d[4] + '"'
       if d[0] == "24":
          champDoc += '}'
       else:
          champDoc += ','
    rapture.doDoc_PutDoc(champUri, champDoc)

    time.sleep(2);

# Test simple search

    srch = rapture.doSearch_Search(query1)
    expect = srch1['total'] + 1;
    assert (srch['total'] == expect), query1+" returns "+str(srch['total'])+" expected "+str(expect)

# Test Repo search
    srch = rapture.doSearch_Search(query2)
    expect = srch2['total'] + 2;
    assert (srch['total'] == expect), query2+" returns "+str(srch['total'])+" expected "+str(expect)

# Test URI search
    srch = rapture.doSearch_Search(query3)
    expect = srch3['total'] + 2;
    assert (srch['total'] == expect), query3+" returns "+str(srch['total'])+" expected "+str(expect)

# Test search works after DeleteDoc
    rapture.doDoc_DeleteDoc(premUri)
    time.sleep(2);
    srch = rapture.doSearch_Search(query1)
    expect = srch1['total']
    assert (srch['total'] == expect), query1+" returns "+str(srch['total'])+" expected "+str(expect)

# Test search works after DeleteDocRepo
    srch = rapture.doSearch_Search(query4)
    expect = srch4['total'] +1
    assert (srch['total'] == expect), query4+" returns "+str(srch['total'])+" expected "+str(expect)

    rapture.doDoc_DeleteDocRepo(docRepoUri)
    time.sleep(2);
    srch = rapture.doSearch_Search(query4)
    expect = srch4['total']
    assert (srch['total'] == expect), query4+" returns "+str(srch['total'])+" expected "+str(expect)
    
def test_Blob():

# Create blob using different API calls - AddStringToBlob and AddStringsToDoc
# (Can add others)

    premUri = blobRepoUri+"/English/Premier"
    champUri = blobRepoUri+"/English/Championship"

    if(rapture.doBlob_BlobExists(premUri)):
        rapture.doBlob_DeleteBlob(premUri)
    if(rapture.doBlob_BlobExists(champUri)):
        rapture.doBlob_DeleteBlob(champUri)

    query1="Watford*"
    srch1 = rapture.doSearch_Search(query1)
    query2 = "repo:"+repo[2:]
    srch2 = rapture.doSearch_Search(query2)
    query3 = "scheme:blob AND parts:Eng*"
    srch3 = rapture.doSearch_Search(query3)
    query4="Brentford*"
    srch4 = rapture.doSearch_Search(query4)
    
    rapture.doBlob_PutBlob(premUri, base64.b64encode(''.join(premier)), "text/plain");
    rapture.doBlob_PutBlob(champUri, base64.b64encode(''.join(championship)), "text/csv");

    time.sleep(2);

# Test simple search

    srch = rapture.doSearch_Search(query1)
    expect = srch1['total'] + 1;
    assert (srch['total'] == expect), query1+" returns "+str(srch['total'])+" expected "+str(expect)

# Test Repo search
    srch = rapture.doSearch_Search(query2)
    expect = srch2['total'] + 2;
    assert (srch['total'] == expect), query2+" returns "+str(srch['total'])+" expected "+str(expect)

# Test URI search
    srch = rapture.doSearch_Search(query3)
    expect = srch3['total'] + 2;
    assert (srch['total'] == expect), query3+" returns "+str(srch['total'])+" expected "+str(expect)

# Test search works after DeleteBlob
    rapture.doBlob_DeleteBlob(premUri)
    time.sleep(2);
    srch = rapture.doSearch_Search(query1)
    expect = srch1['total']
    assert (srch['total'] == expect), query1+" returns "+str(srch['total'])+" expected "+str(expect)

# Test search works after DeleteBlobRepo
    srch = rapture.doSearch_Search(query4)
    expect = srch4['total'] +1
    assert (srch['total'] == expect), query4+" returns "+str(srch['total'])+" expected "+str(expect)

    rapture.doBlob_DeleteBlobRepo(blobRepoUri)
    time.sleep(2);
    srch = rapture.doSearch_Search(query4)
    expect = srch4['total']
    assert (srch['total'] == expect), query4+" returns "+str(srch['total'])+" expected "+str(expect)

