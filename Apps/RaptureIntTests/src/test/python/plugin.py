import raptureAPI
import multipart
import json
import time
import base64
import zipfile

# TODO These need to be parameters

repo = '//nightly_python'

site = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
rapture = raptureAPI.raptureAPI(site, username, password)

pluginName ="testdoc";
description="Test workflow with docs";
testseries = "src/test/resources/plugin/nightly/testseries.zip"

# TODO The Java implementation has helper classes to make plugin creation easier.
# We may want to create something similar for Python

def test_plugin():
      zippy = zipfile.ZipFile(testseries)
      flowdata = zippy.read("content/testplugin/testseries/createsquaresandverify.workflow")
      jardata = zippy.read("content/testplugin/testseries/testSeries.jar")
      scriptdata = zippy.read("content/testplugin/testseries/verifydata.script")
      configdata = zippy.read("plugin.txt")
      
      flow = json.loads(flowdata);
      print flow['workflowURI']

      workflow = {"uri":flow['workflowURI'], "hash": ""}
      jar = { "uri": "jar://testplugin/testseries/testSeries", "hash": ""}
      script = { "uri": "script://testplugin/testseries/verifydata", "hash": ""}

      manifest= {"description": description, "plugin": pluginName, "contents": [{"uri":flow['workflowURI'],"hash":""}, {"uri":jar['uri'],"hash":""}, {"uri":script['uri'],"hash":""}]}
      
      workflow['content'] = base64.b64encode(flowdata)
      scriptcontent = "println('Hello, world!');"
      script['content'] = base64.b64encode(scriptcontent)
      jar['content'] = base64.b64encode(jardata)

      payload = { workflow['uri'] : workflow, jar['uri'] : jar, script['uri'] : script }

      rapture.doPlugin_InstallPlugin(manifest, payload)
      installed = rapture.doPlugin_GetInstalledPlugins()
      found = False
      for ins in installed:
        if (pluginName == ins['plugin']):
          assert(description == ins['description'])
          found = True;

      assert found

      # TODO Test that the plugin actually works? 
