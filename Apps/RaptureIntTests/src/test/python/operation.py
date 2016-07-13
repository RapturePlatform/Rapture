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

    m = {'fieldToChange':'bravo', 'newFieldValue': 'Five', 'fieldToIncrement': 'charlie'}
    rapture.doOperation_InvokeSaveAlt(docu, 'setVal', m, oper)
    content = rapture.doOperation_InvokeAlt(docu, 'increment', m, oper)
    assert content['bravo'] == "Five" 
    assert content['charlie'] == 4 

def test_interfaceOperation():
    # simple document with $interface
    rapture.doDoc_PutDoc(docu, '{"alpha":1,"bravo":2,"charlie":3,"$interface":"'+oper+'"}')

    # Put the operations in $interface document
    rapture.doDoc_PutDoc(oper, '{ "setVal" : "this[params[\'fieldToChange\']] = params[\'newFieldValue\']; return this;", "increment" : "this[params[\'fieldToIncrement\']] = cast(this[params[\'fieldToIncrement\']], \'integer\') + 1; println(this); return this;" }')

    m = {'fieldToChange':'bravo', 'newFieldValue': 'Five', 'fieldToIncrement': 'charlie'}
    rapture.doOperation_InvokeSave(docu, 'setVal', m)
    content = rapture.doOperation_Invoke(docu, 'increment', m)
    ## bug?
    assert content['bravo'] == "Five" 
    assert content['charlie'] == 4 

def test_parentOperation():

    parent1 = repo+"/parent1"
    parent2 = repo+"/parent2"
    parent3 = repo+"/parent3"

    # simple document with $parent
    rapture.doDoc_PutDoc(docu, '{"alpha":1,"bravo":2,"charlie":3,"$parent":"'+parent1+'"}')

    # Put the operations in $parent document
    rapture.doDoc_PutDoc(parent1, '{ "setVal" : "this[\'alpha\'] = params[\'newFieldValue\']; return this;", "$parent" : "'+parent2+'" }')
    rapture.doDoc_PutDoc(parent2, '{ "setVal" : "this[\'bravo\'] = params[\'newFieldValue\']; return this;", "$parent" : "'+parent3+'" }')
    rapture.doDoc_PutDoc(parent3, '{ "increment" : "this[params[\'fieldToIncrement\']] = cast(this[params[\'fieldToIncrement\']], \'integer\') + 1; println(this); return this;"}');

    m = {'newFieldValue': 'Five', 'fieldToIncrement': 'charlie'}
    rapture.doOperation_InvokeSave(docu, 'setVal', m)
    content = rapture.doOperation_Invoke(docu, 'increment', m)
    assert content['alpha'] == "Five" 
    assert content['bravo'] == 2
    assert content['charlie'] == 4 

    rapture.doDoc_PutDoc(docu, '{"alpha":1,"bravo":2,"charlie":3,"$parent":"'+oper+'", "$interface" : "'+parent2+'"}')
    ## This will call setVal from parent2 because although parent1 is imported via the $PARENT clause
    ## parent2 is imported via the $INTERFACE clause, which takes precedence.

    m = {'newFieldValue': 'Five', 'fieldToIncrement': 'charlie'}
    rapture.doOperation_InvokeSave(docu, 'setVal', m)
    content = rapture.doOperation_Invoke(docu, 'increment', m)
    assert content['alpha'] == 1
    assert content['bravo'] == "Five"
    assert content['charlie'] == 4 

