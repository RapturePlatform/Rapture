import raptureAPI
import multipart
import json
import time
import base64
import random

# TODO These need to be parameters

repo = '//NightlyPython'

docRepoUri = 'document:'+repo
docu = repo+"/data1"
oper = repo+"/data2"

rapture = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
rapture = raptureAPI.raptureAPI(rapture, username, password)

if (not rapture.doDoc_DocRepoExists(repo)):
    rapture.doDoc_CreateDocRepo(repo, "NREP {} USING MEMORY {}")


def test_simpleOperation():
    rapture.doDoc_PutDoc(docu, '{"a":1,"b":2,"c":3,"incr":"this[\'b\'] = cast(this.b, \'integer\') + params.b; return this;"}')
    m = {"b":3}
    rapture.doOperation_InvokeSave(docu, 'incr', m)
    m['b'] = 4
    rapture.doOperation_InvokeSave(docu, 'incr', m)

    content = rapture.doDoc_GetDoc(docu)
    assert content == '{"a":1,"b":9,"c":3,"incr":"this[\'b\'] = cast(this.b, \'integer\') + params.b; return this;"}' 

def test_simpleAltOperation():
    # Start with a simple document
    rapture.doDoc_PutDoc(docu, '{"alpha":1,"bravo":2,"charlie":3}')
    # Put the operations in an Alterate document
    rapture.doDoc_PutDoc(oper, '{ "setVal" : "this[params[\'fieldToChange\']] = params[\'newFieldValue\']; return this;", "increment" : "this[params[\'fieldToIncrement\']] = cast(this[params[\'fieldToIncrement\']], \'integer\') + 1; println(this); return this;" }')

    m = {'fieldToChange':'bravo', 'newFieldValue': '5', 'fieldToIncrement': 'charlie'}
    rapture.doOperation_InvokeSaveAlt(docu, 'setVal', m, oper)
    content = rapture.doOperation_InvokeAlt(docu, 'increment', m, oper)
    ## So this is kinda wierd. bravo is String 5 but Charlie is Number 4
    ## This may be a bug.  
    assert content['bravo'] == "5" 
    assert content['charlie'] == 4 

def test_interfaceOperation():
    # simple document with $interface
    rapture.doDoc_PutDoc(docu, '{"alpha":1,"bravo":2,"charlie":3,"$interface":"'+oper+'"}')

    # Put the operations in $interface document
    rapture.doDoc_PutDoc(oper, '{ "setVal" : "this[params[\'fieldToChange\']] = params[\'newFieldValue\']; return this;", "increment" : "this[params[\'fieldToIncrement\']] = cast(this[params[\'fieldToIncrement\']], \'integer\') + 1; println(this); return this;" }')

    m = {'fieldToChange':'bravo', 'newFieldValue': '5', 'fieldToIncrement': 'charlie'}
    rapture.doOperation_InvokeSave(docu, 'setVal', m)
    content = rapture.doOperation_Invoke(docu, 'increment', m)
    ## bug?
    assert content['bravo'] == "5" 
    assert content['charlie'] == 4 

def test_parentOperation():
    # simple document with $parent
    rapture.doDoc_PutDoc(docu, '{"alpha":1,"bravo":2,"charlie":3,"$parent":"'+oper+'"}')

    # Put the operations in $parent document
    rapture.doDoc_PutDoc(oper, '{ "setVal" : "this[params[\'fieldToChange\']] = params[\'newFieldValue\']; return this;", "increment" : "this[params[\'fieldToIncrement\']] = cast(this[params[\'fieldToIncrement\']], \'integer\') + 1; println(this); return this;" }')

    m = {'fieldToChange':'bravo', 'newFieldValue': '5', 'fieldToIncrement': 'charlie'}
    rapture.doOperation_InvokeSave(docu, 'setVal', m)
    content = rapture.doOperation_Invoke(docu, 'increment', m)
    assert content['bravo'] == "5" 
    assert content['charlie'] == 4 
