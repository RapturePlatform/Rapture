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
    status1 = rapture.doDecision_GetWorkOrderStatus(order1['uri'])
    status2 = rapture.doDecision_GetWorkOrderStatus(order1['uri'])
    assert not order2['isCreated'], str(order1) + str(status1) + str(order2) + str(status2)

    time.sleep(3)
    status1 = rapture.doDecision_GetWorkOrderStatus(order1['uri'])
    assert status1['status'] == 'FINISHED'
    order2 = rapture.doDecision_CreateWorkOrderP(flowURI, {}, "")
    assert order2['isCreated'], order2
    time.sleep(5)
    status2 = rapture.doDecision_GetWorkOrderStatus(order2['uri'])
    print status2['status']
    assert status2['status'] == 'FINISHED'
