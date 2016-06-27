import raptureAPI
import multipart
import json
import time
import random
import base64

# TODO These need to be parameters

repo = '//nightly_python'

blobRepoUri = 'blob:'+repo
docRepoUri = 'document:'+repo
seriesRepoUri = 'series:'+repo
scriptRepoUri = 'script:'+repo

site = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
rapture = raptureAPI.raptureAPI(site, username, password)
user2 = "User2"
config = " {} USING MONGODB {prefix=\"nightly\"}"

# testDocumentUpdate.rfx            testPutContentFireEventFromRepoTest.rfx
# testDocumentWithAutoID.rfx


def test_docoperations():
    if(rapture.doDoc_DocRepoExists(docRepoUri)):
        rapture.doDoc_DeleteDocRepo(docRepoUri)
    rapture.doDoc_CreateDocRepo(docRepoUri, "NREP "+config)

    docPath=docRepoUri+'/doc'+str(random.randint(1,1000000))

    content = '{"key1":"value1"}'
    rapture.doDoc_PutDoc(docPath,content)
    assert rapture.doDoc_DocExists(docPath)

    for i in range (1,5):
        newContent='{"key'+str(i)+'":"value'+str(i)+'"}'
        rapture.doDoc_PutDoc(docPath,newContent)
        assert rapture.doDoc_GetDoc(docPath) == newContent

    rapture.doDoc_DeleteDoc(docPath)
    assert not rapture.doDoc_DocExists(docPath)
    rapture.doDoc_DeleteDocRepo(docRepoUri)

def test_docindex():
    if(rapture.doDoc_DocRepoExists(docRepoUri)):
        rapture.doDoc_DeleteDocRepo(docRepoUri)
    rapture.doDoc_CreateDocRepo(docRepoUri, "NREP "+config)

    INDEXCFG = "tokenField(token) string, tsField(timestamp) string, keyField(key) string"
    testIndex = rapture.doIndex_CreateIndex(docRepoUri, INDEXCFG)

    doc1 = """{"token":"token1","timestamp":"ts1","key":"data1"}"""
    rapture.doDoc_PutDoc(docRepoUri+ '/doc1', doc1)
    
    doc2 = {"token":"token2","timestamp":"ts2","key":"data2"}
    rapture.doDoc_PutDoc(docRepoUri+ '/doc2', json.dumps(doc2))
    
    doc3 = {"token":"token2","timestamp":"ts2","key":"data3"}
    rapture.doDoc_PutDoc(docRepoUri+ '/doc3', json.dumps(doc3))
    
    query1 = "SELECT keyField WHERE tokenField=\"token1\""
    query2 = "SELECT tsField WHERE tokenField=\"token2\""
    query3 = "SELECT tokenField,tsField,keyField WHERE tokenField=\"token2\""
    query4 = "SELECT keyField WHERE tokenField=\"token2\" AND tsField=\"ts2\""
    
    res1 = rapture.doIndex_FindIndex(docRepoUri,query1)
    assert res1['columnNames'] == ['keyField']
    assert res1['rows'] == [['data1']]

    res2 = rapture.doIndex_FindIndex(docRepoUri,query2)
    assert res2['columnNames'] == ['tsField']
    assert res2['rows'] == [['ts2'], ['ts2']]

    res3 = rapture.doIndex_FindIndex(docRepoUri,query3)
    res3col = [u'tokenField', u'tsField', u'keyField']
    res3rowa = [[u'token2',u'ts2',u'data2'], [u'token2',u'ts2',u'data3']]
    res3rowb = [[u'token2',u'ts2',u'data3'], [u'token2',u'ts2',u'data2']]

    assert res3['columnNames'] == res3col
    assert (res3['rows'] == res3rowa) or (res3['rows'] == res3rowb)

    res4 = rapture.doIndex_FindIndex(docRepoUri,query4)
    assert (res4['rows'] == [[u'data3'], [u'data2']]) or (res4['rows'] == [[u'data2'], [u'data3']])
    rapture.doDoc_DeleteDocRepo(docRepoUri)

def test_listbyprefix():
    if(rapture.doDoc_DocRepoExists(docRepoUri)):
        rapture.doDoc_DeleteDocRepo(docRepoUri)
    rapture.doDoc_CreateDocRepo(docRepoUri, "NREP "+config)

    docURIf1d1=docRepoUri+'/folder1/doc1'
    docURIf1d2=docRepoUri+'/folder1/doc2'
    docURIf1d3=docRepoUri+'/folder1/doc3'
    docURIf2d1=docRepoUri+'/folder2/folder21/doc1'
    docURIf2d2=docRepoUri+'/folder2/folder21/doc2'
    docURIf3d1=docRepoUri+'/folder3/doc1'
    
    content="""{"key":"value"}"""
    rapture.doDoc_PutDoc(docURIf1d1,content)
    rapture.doDoc_PutDoc(docURIf1d2,content)
    rapture.doDoc_PutDoc(docURIf1d3,content)
    rapture.doDoc_PutDoc(docURIf2d1,content)
    rapture.doDoc_PutDoc(docURIf2d2,content)
    rapture.doDoc_PutDoc(docURIf3d1,content)

    assert rapture.doDoc_GetDoc(docURIf1d1) == content
    assert rapture.doDoc_GetDoc(docURIf1d2) == content
    assert rapture.doDoc_GetDoc(docURIf1d3) == content
    assert rapture.doDoc_GetDoc(docURIf2d1) == content
    assert rapture.doDoc_GetDoc(docURIf2d2) == content
    assert rapture.doDoc_GetDoc(docURIf3d1) == content
    
    assert len(rapture.doDoc_ListDocsByUriPrefix(docRepoUri+'/folder1',2)) == 3
    assert len(rapture.doDoc_ListDocsByUriPrefix(docRepoUri+'/folder1',1)) == 3
    assert len(rapture.doDoc_ListDocsByUriPrefix(docRepoUri+'/folder2',2)) == 3
    assert len(rapture.doDoc_ListDocsByUriPrefix(docRepoUri+'/folder2',1)) == 1
    assert len(rapture.doDoc_ListDocsByUriPrefix(docRepoUri+'/folder2',0)) == 3
    assert len(rapture.doDoc_ListDocsByUriPrefix(docRepoUri+'/folder3',0)) == 1
    
    rapture.doDoc_DeleteDoc(docURIf1d1)
    rapture.doDoc_DeleteDoc(docURIf3d1)
    assert len(rapture.doDoc_ListDocsByUriPrefix(docRepoUri+'/folder1',2)) == 2
    assert len(rapture.doDoc_ListDocsByUriPrefix(docRepoUri+'/folder1',1)) == 2
    try:
    	rapture.doDoc_ListDocsByUriPrefix(docRepoUri+'/folder3',0)
    	assert false
    except:
        print "Expected"
    rapture.doDoc_DeleteDocRepo(docRepoUri)
    
def test_docidgen():
    if (rapture.doDoc_DocRepoExists(docRepoUri)):
        rapture.doDoc_DeleteDocRepo(docRepoUri)
    rapture.doDoc_CreateDocRepo(docRepoUri, "NREP "+config)

    if (rapture.doIdGen_IdGenExists("idgen://documentRepo"+repo)):
        rapture.doIdGen_DeleteIdGen("idgen://documentRepo"+repo)

    docPath= docRepoUri+'/folder/#id'
    idgenCfg = 'IDGEN { base="10",length="5", prefix="TST"} USING MONGODB {prefix="testfountain.'+format(random.randint(1,1000000))+'"}'
    rapture.doDoc_SetDocRepoIdGenConfig(docRepoUri, idgenCfg);

    putData = '{"key1":"#id"}';
    for i in range (1,5):
    	putContentUri = rapture.doDoc_PutDoc(docPath, putData);
    	getContent = rapture.doDoc_GetDoc(putContentUri);
    	assert "TST0000"+str(i) in putContentUri
    	assert "TST0000"+str(i) in getContent
    rapture.doDoc_DeleteDocRepo(docRepoUri)

def test_update():
    if(rapture.doDoc_DocRepoExists(docRepoUri)):
        rapture.doDoc_DeleteDocRepo(docRepoUri)
    rapture.doDoc_CreateDocRepo(docRepoUri, "NREP "+config)

    docPath=docRepoUri+'/testdoc'
    rapture.doDoc_PutDoc(docPath, '{"key":"value"}')
    testDocUri = docRepoUri + '/testDoc'
    docVal='{"keytest":"valuetest"}'
    scriptText = "#doc.putDoc('"+testDocUri+"','"+docVal+"')"
    scriptName = 'script'+str(random.randint(1,1000000))
    eventName = 'data/update'
    scriptURI=scriptRepoUri+'/'+scriptName
    rapture.doScript_CreateScript(scriptURI, "REFLEX", "PROGRAM",scriptText)
    eventURI = docRepoUri + '/' + eventName
    rapture.doEvent_AddEventScript(eventURI, scriptURI, True)
    docURI2 = docRepoUri+'/folder1/documenttest2'
    rapture.doDoc_PutDoc(docURI2, '{"key1":"value1"}')
    s2 = rapture.doDoc_GetDoc(docURI2)
    assert s2 == '{"key1":"value1"}'
    time.sleep(3)
    getTestDoc = rapture.doDoc_GetDoc(testDocUri)
    assert getTestDoc is not None
    assert getTestDoc == docVal
    rapture.doDoc_DeleteDocRepo(docRepoUri)
