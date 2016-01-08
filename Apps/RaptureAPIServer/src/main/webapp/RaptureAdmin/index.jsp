<%@ page import="java.util.*" %>
<%@ page import="rapture.kernel.*" %>
<%@ page import="rapture.common.*" %>
<%@ page import="rapture.common.model.*" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Rapture Admin, from Incapture</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Rapture Admin landing page">
    <meta name="author" content="Alan Moore">

    <!-- Le styles -->
    <link href="../assets/css/bootstrap.css" rel="stylesheet">
    <style type="text/css">
      body {
        padding-top: 60px;
        padding-bottom: 40px;
      }
    </style>
    <link href="../assets/css/bootstrap-responsive.css" rel="stylesheet">

    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->


   
    <!-- Le fav and touch icons -->
    <link rel="shortcut icon" href="../assets/ico/favicon.ico">
    <link rel="apple-touch-icon-precomposed" sizes="144x144" href="../assets/ico/apple-touch-icon-144-precomposed.png">
    <link rel="apple-touch-icon-precomposed" sizes="114x114" href="../assets/ico/apple-touch-icon-114-precomposed.png">
    <link rel="apple-touch-icon-precomposed" sizes="72x72" href="../assets/ico/apple-touch-icon-72-precomposed.png">
    <link rel="apple-touch-icon-precomposed" href="../assets/ico/apple-touch-icon-57-precomposed.png">
  </head>

  <body>
   <%@ include file="include/menu.jsp" %>
    <div class="container">
      <div class="row">
        <div class="span4">
        	<h2>Scale Information</h2>       
        	<table class="table table-striped table-condensed table-bordered">
        		<thead><tr><th>Type</th><th>Value</th></tr></thead>
        		<tbody>
        			<tr><td>API Servers</td><td id="webapp"><span class="badge badge-success">0</span></td></tr>
        			<tr><td>Workers</td><td id="worker"><span class="badge badge-success">0</span></td></tr>
       		    </tbody>
            </table>
        </div>
        <div class="span4">
            <h2>Current Levels</h2> 
        	<table class="table table-striped table-condensed table-bordered">
        		<thead><tr><th>Type</th><th>Value</th><th>History</th></tr></thead>
        		<tbody>
        			<tr><td>Throughput</td><td id="throughput"><span class="badge badge-success">0</span></td><td><span id="throughputsparkline">Loading..</span></td></tr>
        			<tr><td>Users</td><td id="users"><span class="badge badge-success">0</span></td><td><span id="userssparkline">Loading..</span></td></tr>
        			<tr><td>Call Rate</td><td id="callcount"><span class="badge badge-success">0</span></td><td><span id="callsparkline">Loading..</span></td></tr>
        			<tr><td>Trigger Rate</td><td id="triggerrun"><span class="badge badge-success">0</span></td><td><span id="trigsparkline">Loading..</span></td></tr>
        			<tr><td>Event Rate</td><td id="runevent"><span class="badge badge-success">0</span></td><td><span id="runsparkline">Loading..</span></td></tr>
        			<tr><td>Script Rate</td><td id="scriptrate"><span class="badge badge-success">0</span></td><td><span id="scriptsparkline">Loading..</span></td></tr>
        			<tr><td>Pipeline Rate</td><td id="pipelinerate"><span class="badge badge-success">0</span></td><td><span id="pipelinesparkline">Loading..</span></td></tr>
        		</tbody>
        	</table>
        </div>
        <div class="span4">
        	<h2>Counts</h2>             
        	<table class="table table-striped table-condensed table-bordered">
        		<thead><tr><th>Type</th><th>Value</th></tr></thead>
        		<tbody>
        			<tr><td>Call Count</td><td id="totalcalls"><span class="badge badge-success">0</span></td></tr>
        			<tr><td>Events Fired</td><td id="eventsfired"><span class="badge badge-success">0</span></td></tr>
        			<tr><td>Scripts Run</td><td id="scriptsrun"><span class="badge badge-success">0</span></td></tr>
        			<tr><td>Pipeline Total</td><td id="pipelinetotal"><span class="badge badge-success">0</span></td></tr>
        			<tr><td>Pipeline Waiting</td><td id="pipelinewait"><span class="badge badge-success">0</span></td></tr>
        			<tr><td>Pipeline Running</td><td id="pipelinerun"><span class="badge badge-success">0</span></td></tr>
       			</tbody>
       		 </table>
        </div>
    </div>
    <div class="row">
        <div class="span6">
        	<h2>Cloud Servers</h2>
        	<table id="cloudServers" class="table table-striped table-condensed">
        	</table>
        </div>
        <div class="span6">
        	 <h2>Installed Plugins</h2>
             <table id="pluginList" class="table table-striped table-condensed">
        	 </table>
     	</div>
    </div>
      <hr>
      <footer>
        <p>&copy; Incapture Technologies 2012-2013</p>
      </footer>
    </div> <!-- /container -->

    <!-- Le javascript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="../assets/js/jquery.js"></script>
    <script src="../assets/js/bootstrap.js"></script>
    <script type="text/javascript" src="../assets/js/jquery.sparkline.min.js"></script>  
    <script src="../assets/js/bits.js"></script>
      <%@ include file="include/sec.jsp" %>
    <script type="text/javascript">
    // Need to retrieve the stats using the restful location
    
    var thrupoints = [];
    var callpoints = [];
    var trigpoints = [];
    var runpoints = [];
    var userpoints = [];
    var scriptpoints = [];
    var pipelinepoints = [];

    var points_max = 30;
    
    
    refresh = function() {
    $.ajax({
  		url: "../r/stats/$current",
  		dataType: 'json',
  		success: function(data) {
  		   $('td#webapp span').text(data.content["server/webapp"].value);
  		   $('td#worker span').text(data.content["server/worker"].value);
  		   
  		   $('td#throughput span').text(data.content["stat/throughput"].value);  		   
  		   thrupoints.push(data.content["stat/throughput"].value);
  		   if (thrupoints.length > points_max)
                thrupoints.splice(0,1); 		   
	       $('#throughputsparkline').sparkline(thrupoints);
	       
   		   $('td#users span').text(data.content["stat/users"].value);
   		   userpoints.push(data.content["stat/users"].value);
  		   if (userpoints.length > points_max)
                userpoints.splice(0,1); 		   
	       $('#userssparkline').sparkline(userpoints, { type: 'bar' });
   		   
   		   $('td#callcount span').text(data.content["stat/callcount"].value);
   		   callpoints.push(data.content["stat/callcount"].value);
  		   if (callpoints.length > points_max)
                callpoints.splice(0,1); 		   
	       $('#callsparkline').sparkline(callpoints);
   		   
   		   $('td#triggerrun span').text(data.content["stat/triggerrun"].value);
  		   trigpoints.push(data.content["stat/triggerrun"].value);
  		   if (trigpoints.length > points_max)
                trigpoints.splice(0,1); 		   
	       $('#trigsparkline').sparkline(trigpoints);
   		   		   
   		   $('td#runevent span').text(data.content["stat/runevent"].value);
   		   runpoints.push(data.content["stat/runevent"].value);
  		   if (runpoints.length > points_max)
                runpoints.splice(0,1); 		   
	       $('#runsparkline').sparkline(runpoints);
   		     		   
   		   $('td#scriptrate span').text(data.content["stat/scriptrate"].value);   
   		   scriptpoints.push(data.content["stat/scriptrate"].value);
  		   if (scriptpoints.length > points_max)
                scriptpoints.splice(0,1); 		   
	       $('#scriptsparkline').sparkline(scriptpoints);
 
   		   $('td#pipelinerate span').text(data.content["stat/pipelinerate"].value);   
   		   pipelinepoints.push(data.content["stat/pipelinerate"].value);
  		   if (pipelinepoints.length > points_max)
                pipelinepoints.splice(0,1); 		   
	       $('#pipelinesparkline').sparkline(pipelinepoints);
	       
   		   $('td#totalcalls span').text(data.content["count/totalcalls"].value);
   		   $('td#eventsfired span').text(data.content["count/eventsfired"].value);   
   		   $('td#scriptsrun span').text(data.content["count/scriptsrun"].value);
   		   
   		   $('td#pipelinetotal span').text(data.content["count/pipelinecount"].value);
   		   $('td#pipelinewait span').text(data.content["count/pipelinewait"].value);
   		   $('td#pipelinerun span').text(data.content["count/pipelinerunning"].value);
   		   
   		}
	});
   }
   
         $.ajax({
  		url: "../reflex/pluginList.rfx",
  		dataType: 'json',
  		success: function(data) {
   			var oldTable = document.getElementById('pluginList'),
        	newTable = oldTable.cloneNode(false),tr,td;
        	var thead = document.createElement('thead');
        	addHeader(thead, 'Plugin');
        	addHeader(thead, 'Version');
        	addHeader(thead, 'Description');
        	newTable.appendChild(thead);
        	
  		    var tbody = document.createElement('tbody');
  		    for(var i=0; i< data.length; i++) {
  		       	tr = document.createElement('tr'); 
  		       	addCell(tr,data[i].plugin);
  		       	addCell(tr,data[i].version);
  		       	addCell(tr,data[i].description);
  		        	tbody.appendChild(tr);		        
  		    }
  		    newTable.appendChild(tbody);
  		    oldTable.parentNode.replaceChild(newTable, oldTable);
  
  		}
        });
   
        $.ajax({
           url: "../reflex/statusList.rfx",
           dataType: 'json',
           success: function(data) {
          		 [{"serverName":"Rapture.local","instance":{"serverGroup":"localRun","appInstance":"RaptureWebServlet-1.0.0-LATEST.jar","appName":"unknown","status":"OK","capabilities":{"PresentsAPI":true},"lastSeen":1358736680414}}]
          		 var oldTable = document.getElementById('cloudServers'),
          		 newTable = oldTable.cloneNode(false),tr,td;
          		 var thead = document.createElement('thead');
          		 addHeader(thead, 'Server');
          		 addHeader(thead, 'App');
          		 addHeader(thead, 'Status');
          		 addHeader(thead, 'Last Seen');
          		 newTable.appendChild(thead);
          		 var tbody = document.createElement('tbody');
          		 for(var i=0; i< data.length; i++) {
          		    tr = document.createElement('tr');
          		    addCell(tr, data[i].serverName);
          		    addCell(tr, data[i].instance.appInstance);
          		    addCell(tr, data[i].instance.status);
          		    var ls = new Date(data[i].instance.lastSeen);
          		    addCell(tr, ls);
          		    tbody.appendChild(tr);
          		 }
  		    newTable.appendChild(tbody);
  		    oldTable.parentNode.replaceChild(newTable, oldTable);
           }
           });
           
         function addHeader(thead, name) {
        	var th = document.createElement('th');
        	th.appendChild(document.createTextNode(name));
        	thead.appendChild(th);
        }
  
        function addCell(tr, element) {
       	    var td = document.createElement('td');
       	    if (element == undefined) {
       	       td.appendChild(document.createTextNode(''));
       	    } else {
       	   	 td.appendChild(document.createTextNode(element));
       	   	}
       	    tr.appendChild(td);
        }
    
   refresh();
    window.setInterval(refresh, 10000);
  	</script>
   <script type="text/javascript" src="https://incapture.atlassian.net/s/en_UScc0y2g-418945332/6025/76/1.3.4/_/download/batch/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector.js?collectorId=f455c0db"></script>
  </body>
</html>
