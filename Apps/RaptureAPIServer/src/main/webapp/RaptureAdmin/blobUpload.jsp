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
    <meta name="description" content="Rapture Blob Uploader">
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
    <link href="../assets/css/shCore.css" rel="stylesheet">
    <link href="../assets/css/shThemeDefault.css" rel="stylesheet">
    <link href="../assets/js/dynatree/skin/ui.dynatree.css" rel="stylesheet" type="text/css" id="skinSheet">

    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

	<script src="../assets/js/shCore.js"></script>
 	<script src="../assets/js/shBrushJScript.js"></script>	 

   
    <!-- Le fav and touch icons -->
    <link rel="shortcut icon" href="../assets/ico/favicon.ico">
    <link rel="apple-touch-icon-precomposed" sizes="144x144" href="../assets/ico/apple-touch-icon-144-precomposed.png">
    <link rel="apple-touch-icon-precomposed" sizes="114x114" href="../assets/ico/apple-touch-icon-114-precomposed.png">
    <link rel="apple-touch-icon-precomposed" sizes="72x72" href="../assets/ico/apple-touch-icon-72-precomposed.png">
    <link rel="apple-touch-icon-precomposed" href="../assets/ico/apple-touch-icon-57-precomposed.png">
  </head>

  <body>
  <%@ include file="include/menu.jsp" %>
  <script src="../assets/js/bits.js"></script>
  <%@ include file="include/sec.jsp"%>
    <div class="container">
      <div class="row">
      <h3>Rapture File --> Blob Uploader</h3>
        <form action="/rapture/blobupload" method="post" enctype="multipart/form-data" role="form">
        <div class="form-group">
        	<label for="BlobURI">Blob URI</label>        	
    		<input type="text" name="description" id="BlobURI" placeholder="Enter Blob URI"/>
    	</div>
        <div class="form-group">
        	<label for="BlobFile">File Input</label>        	
	    	<input type="file" name="file" id="BlobFile"/>
	    	<p class="help-block">Choose the file to upload to the Blob in Rapture</p>
    	</div>
    	<button type="submit" class="btn btn-default">Upload</button>
 		</form>
     </div>
   </div>
 
    <script src="../assets/js/jquery.js"></script>
    <script src="../assets/js/jquery-ui.js"></script>
    <script src="../assets/js/bootstrap.js"></script>
 	<script src="../assets/js/dynatree/jquery.dynatree.js"></script>
	  
  
   <script type="text/javascript" src="https://incapture.atlassian.net/s/en_UScc0y2g-418945332/6025/76/1.3.4/_/download/batch/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector.js?collectorId=f455c0db"></script>
  </body>
</html>
