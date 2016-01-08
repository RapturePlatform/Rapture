<%@ page import="java.util.*" %>
<%@ page import="rapture.kernel.*" %>
<%@ page import="rapture.common.*" %>
<%@ page import="rapture.common.model.*" %>
<%@ page import="rapture.license.*" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Rapture Admin, from Incapture</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Rapture Config landing page">
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
  <%@ include file="include/sec.jsp"%>
    <div class="container">
      <div class="row">
        <div class="span8">
        	<h2>Network Information</h2>
<%
			RaptureNetwork info = Kernel.getEnvironment().getNetworkInfo(context);
%>      
            <div class="alert alert-success">Network is <b><%=info.getNetworkId()%></b> with name <b id="nName"><%=info.getNetworkName()%></b></div>
            <button class="btn btn-primary" data-toggle="modal" data-target="#setNetworkName">Set Network</button>
      <hr>
      <h2>Server Information</h2>
<%
			RaptureServerInfo sInfo = Kernel.getEnvironment().getThisServer(context);			
%>
            <div class="alert alert-success">Server is <b><%=sInfo.getServerId()%></b> with name <b id="sName"><%=sInfo.getName()%></b></div>
            <button class="btn btn-primary" data-toggle="modal" data-target="#setServerNameDlg">Set Server Name</button>
      <hr>
      <h2>License Information</h2>
<%
			LicenseInfo lInfo = Kernel.getLicenseInfo();		
%>
            <div class="alert alert-info">Rapture is licensed to <b><%=lInfo.getCompanyName()%></div>
<%
             if (lInfo.isDeveloper()) {
%>
            <div class="alert alert-warning">Developer License</div>
<%
             }
%>
      </div>      
      <div class="span4">
      <h3>Appliance Mode</h3>
        <div class="alert alert-info">Appliance mode is <b id="sApplianceState">...</b></div>
        <button id="toggleAppliance" class="btn btn-danger">Toggle Appliance Mode</button>
	  </div>
	  </div> <!-- row -->
      <footer>
        <p>&copy; Incapture Technologies 2012-2013</p>
      </footer>
 
    </div> <!-- /container -->
    
      	<div class="modal hide" id="setServerNameDlg">
  		<div class="modal-header">
    		<button type="button" class="close" data-dismiss="modal">x</button>
    		<h3>Update Server Name</h3>
 		 </div>
  		<div class="modal-body">
		  <label>Server Name</label>
 		 <input id="serverNameId" type="text" class="span3" placeholder="name...">
  		 <span class="help-block">Please enter the name of this Rapture Server</span>
  		</div>
  		<div class="modal-footer">
    	<a href="#" class="btn" data-dismiss="modal">Close</a>
    	<a id="submitSetServerName" href="#" class="btn btn-primary">Update</a>
  	    </div>
  	  </div>
  	
    <div class="modal hide" id="setNetworkName">
  		<div class="modal-header">
    		<button type="button" class="close" data-dismiss="modal">x</button>
    		<h3>Update Network Name</h3>
 		 </div>
  		<div class="modal-body">
		  <label>Network Name</label>
 		 <input id="networkNameId" type="text" class="span3" placeholder="name...">
  		 <span class="help-block">Please enter the name of this Rapture network</span>
  		</div>
  		<div class="modal-footer">
    	<a href="#" class="btn" data-dismiss="modal">Close</a>
    	<a id="submitSetNetworkName" href="#" class="btn btn-primary">Update</a>
  	</div>
  	</div>
  	


    <!-- Le javascript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="../assets/js/jquery.js"></script>
    <script src="../assets/js/bootstrap.js"></script>
    <script type="text/javascript" src="../assets/js/jquery.sparkline.min.js"></script>  
    <script src="../assets/js/bits.js"></script>
    <script type="text/javascript">
          $('#submitSetNetworkName').click(function() {
          var networkName = $('#networkNameId').val();
        	$.ajax({
  				url: "../reflex/setNetwork.rfx?id=" + networkName,
  				dataType: 'json',
  				success: function(data) {
  						updateNetworkName(networkName);
 			           $('#setNetworkName').modal('hide');
  				}
  			});
  			
       });
       
       $('#toggleAppliance').click(function() {
        	$.ajax({
  				url: "../reflex/toggleApplianceState.rfx",
  				dataType: 'json',
  				success: function(data) {
  						updateApplianceInfo();
  				}
  			});
  			
       });
       
       function updateNetworkName(networkName) {      		
  			$('#nName').text(networkName);
  	   }
       $('#submitSetServerName').click(function() {
          var serverName = $('#serverNameId').val();
        	$.ajax({
  				url: "../reflex/setServer.rfx?id=" + serverName,
  				dataType: 'json',
  				success: function(data) {
  						updateServerName(serverName);
 			           $('#setServerNameDlg').modal('hide');
  				}
  			});
  			
       });
       function updateServerName(serverName) {      		
  			$('#sName').text(serverName);
  	   }  
  	   function updateApplianceInfo() {
  	   		$.ajax({
  	   		     url: "../reflex/getApplianceState.rfx",
  				dataType: 'json',
  				success: function(data) {
  				        status = data['status'];
  				        if (status == "true") {
  							$('#sApplianceState').text("ON");
  						}
  						else {
  							$('#sApplianceState').text("OFF");
  						}
  				}
  			});
  	   }
  	   
  	   updateApplianceInfo();
    </script>
   <script type="text/javascript" src="https://incapture.atlassian.net/s/en_UScc0y2g-418945332/6025/76/1.3.4/_/download/batch/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector.js?collectorId=f455c0db"></script>
	<script src="../assets/js/bits.js"></script>
  </body>
</html>
