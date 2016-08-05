import raptureAPI
import multipart
import json
from datetime import datetime
import time
import random

# TODO These need to be parameters

repo = '//nightly_python'

scriptRepoUri = 'script:'+repo
flowRepoUri = 'workflow:'+repo

site = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
rapture = raptureAPI.raptureAPI(site, username, password)

def test_workflow_lock():
    scriptName = 'script'+str(random.randint(1,1000000))
    scriptURI=scriptRepoUri+'/'+scriptName
    if (not rapture.doScript_DoesScriptExist(scriptURI)):
        scriptText = "println(\"Thread sleeping\");\nsleep(4000);\nprintln(\"Thread waking\");\nreturn 'next';"
        rapture.doScript_CreateScript(scriptURI, "REFLEX", "PROGRAM",scriptText)

    flowURI = flowRepoUri+"/flow"+str(random.randint(1,1000000))
    flow =  { "workflowURI": flowURI, "semaphoreType": "WORKFLOW_BASED", "semaphoreConfig" : "{\"maxAllowed\":1, \"timeout\":5 }", "steps": [{ "name": "first", "description": "first step", "executable": scriptURI, "view": {}, "transitions": []}], "startStep": "first", "view": {}, "expectedArguments": [] }

    rapture.doDecision_PutWorkflow(flow)

    order1 = rapture.doDecision_CreateWorkOrderP(flowURI, {}, "")
    order2 = rapture.doDecision_CreateWorkOrderP(flowURI, {}, "")
    assert not order2['isCreated'], "Order should not have been created due to the lock"

    time.sleep(3)
    status1 = rapture.doDecision_GetWorkOrderStatus(order1['uri'])
    assert status1['status'] == 'FINISHED', "expected Finished but is "+str(status1['status'])
    order2 = rapture.doDecision_CreateWorkOrderP(flowURI, {}, "")
    assert order2['isCreated'], order2
    time.sleep(5)
    status2 = rapture.doDecision_GetWorkOrderStatus(order2['uri'])
    print status2['status']
    assert status2['status'] == 'FINISHED'

def test_property_lock():
    scriptName = 'script'+str(random.randint(1,1000000))
    scriptURI=scriptRepoUri+'/'+scriptName
    if (not rapture.doScript_DoesScriptExist(scriptURI)):
        scriptText = "println(\"Thread sleeping\");\nsleep(4000);\nprintln(\"Thread waking\");\nreturn 'next';"
        rapture.doScript_CreateScript(scriptURI, "REFLEX", "PROGRAM",scriptText)

    flowURI = flowRepoUri+"/flow"+str(random.randint(1,1000000))
    flow =  { "workflowURI": flowURI, "semaphoreType": "PROPERTY_BASED", "semaphoreConfig" : "{\"maxAllowed\":1, \"propertyName\":\"FOO\" }", "steps": [{ "name": "first", "description": "first step", "executable": scriptURI, "view": {}, "transitions": []}], "startStep": "first", "view": {}, "expectedArguments": [] }

    rapture.doDecision_PutWorkflow(flow)

    // Will fail because of property error
    order4 = rapture.doDecision_CreateWorkOrderP(flowURI, {"BAR":"FOO"}, "")
    // These should succeed
    order1 = rapture.doDecision_CreateWorkOrderP(flowURI, {"FOO":"BAR"}, "")
    order3 = rapture.doDecision_CreateWorkOrderP(flowURI, {"FOO":"BAZ"}, "")
    // Will fail because of property based lock
    order2 = rapture.doDecision_CreateWorkOrderP(flowURI, {"FOO":"BAR"}, "")

    assert not order2['isCreated'], "Order 2 should not have been created due to the lock"
    assert order3['isCreated'], "Order 3 should have been created because property FOO is set to BAZ not BAR"
    assert not order4['isCreated'], "Order 4 should not have been created because property FOO is not set"

    time.sleep(5)
    status1 = rapture.doDecision_GetWorkOrderStatus(order1['uri'])
    assert status1['status'] == 'FINISHED', "expected Finished but is "+str(status1['status'])
    order2 = rapture.doDecision_CreateWorkOrderP(flowURI, {"FOO":"BAR"}, "")
    assert order2['isCreated'], order2
    time.sleep(5)
    status2 = rapture.doDecision_GetWorkOrderStatus(order2['uri'])
    print status2['status']
    assert status2['status'] == 'FINISHED'


