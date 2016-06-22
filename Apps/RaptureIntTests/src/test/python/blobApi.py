import raptureAPI
import multipart
import json
import time
import base64

# TODO These need to be parameters

repo = '//nightly_python'

blobRepoUri = 'blob:'+repo
docRepoUri = 'document:'+repo
seriesRepoUri = 'series:'+repo

site = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
rapture = raptureAPI.raptureAPI(site, username, password)
user2 = "User2"

def test_entitlements():
    if(rapture.doBlob_BlobRepoExists(blobRepoUri)):
        rapture.doBlob_DeleteBlobRepo(blobRepoUri)
    rapture.doBlob_CreateBlobRepo(blobRepoUri, "BLOB {} USING MONGODB {prefix=\"nightly\"}", "NREP {} USING MONGODB {prefix=\"nightlymeta\"}")

    pdf = blobRepoUri + "/blob/thisisaPDF"
    csv = blobRepoUri + "/blob/thisisaCSV"

    rapture.doBlob_PutBlob(pdf, base64.b64encode("PDF"), "application/pdf")
    rapture.doBlob_PutBlob(csv, base64.b64encode("C,S,V"), "text/csv")

    pdfSize = rapture.doBlob_GetBlobSize(pdf)
    assert pdfSize == 3, "Expect size = 3"

    csvSize = rapture.doBlob_GetBlobSize(csv)
    assert csvSize == 5, "Expect size = 5 but was "+str(csvSize)

    metaData = rapture.doBlob_GetBlobMetaData(pdf)
    assert metaData['Content-Type'] == 'application/pdf'
    assert metaData['Content-Length'] == '3'

    metaData = rapture.doBlob_GetBlobMetaData(csv)
    assert metaData['Content-Type'] == 'text/csv'
    assert metaData['Content-Length'] == '5'
    assert metaData['createdTimestamp'] == metaData['modifiedTimestamp']
    
    with open("../resources/www-bbc-com.pdf", 'rb') as pdfFile:
        pdfData = pdfFile.read()
    rapture.doBlob_AddBlobContent(pdf, base64.b64encode(pdfData))
    pdfSize = rapture.doBlob_GetBlobSize(pdf)
    assert pdfSize == 91297, "Expect size = 91297 but was "+str(pdfSize)
    
    metaData = rapture.doBlob_GetBlobMetaData(pdf)
    assert metaData['Content-Type'] == 'application/pdf'
    assert metaData['Content-Length'] == '91297', "Expect size = 91297 but was "+str(metaData['Content-Length'])
    assert metaData['createdTimestamp'] != metaData['modifiedTimestamp']
    
    rapture.doBlob_PutBlob(pdf, base64.b64encode("XYZZY"), "text/plain")
    metaData = rapture.doBlob_GetBlobMetaData(pdf)
    assert metaData['Content-Type'] == 'text/plain'
    assert metaData['Content-Length'] == '5'
    assert metaData['createdTimestamp'] != metaData['modifiedTimestamp']
    
    rapture.doBlob_DeleteBlob(pdf)
    rapture.doBlob_DeleteBlob(csv)
    
    assert not rapture.doBlob_BlobExists(pdf)
    assert not rapture.doBlob_BlobExists(csv)

    metaData = rapture.doBlob_GetBlobMetaData(pdf)
    assert metaData == {}
