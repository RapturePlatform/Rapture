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
    <meta name="description" content="Rapture Blob">
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
  <script type="text/javascript" src="../assets/js/pdf.js"></script>
  <%@ include file="include/sec.jsp"%>
    <div class="container">
      <div class="row">
        <div class="span4">
        	<div class="alert alert-success"><b>Rapture<b></div>
        	  <div id="tree">
			  </div>
        </div>
        <div class="span8">
        	<div class="alert alert-info"><b id="contentName">Blob URI</b></div>
            <div class="brush: js" id="textContents">
           
            </div>
            <canvas id="pdf-canvas" style=border:1px solid black;"/>
        </div>
     </div>
   </div>
 
    <script src="../assets/js/jquery.js"></script>
    <script src="../assets/js/jquery-ui.js"></script>
    <script src="../assets/js/bootstrap.js"></script>
 	<script src="../assets/js/dynatree/jquery.dynatree.js"></script>
	  
  <script type="text/javascript">
    $(function(){
      $("#tree").dynatree({
        initAjax: {
               url: "../reflex/blobTree.rfx",
               data: { "key" : "/",
                       "mode" : "all"
                     }
               },
        onLazyRead: function(node){
        	node.appendAjax({
        				   url: "../reflex/blobTree.rfx",
                           data: { "key": node.data.key, // Optional url arguments
                                  "mode": "all"
                                  }
                          });
             },
        onActivate: function(node) {
        	if( !node.data.isFolder ) { 
        	    PDFJS.getDocument("/rapture/blob/" + node.data.key).then(function(pdf) {
        	         pdf.getPage(1).then(function(page) {
        	            var scale = 0.75;
        	            var viewport = page.getViewport(scale);
        	            var canvas = document.getElementById('pdf-canvas');
        	            var context = canvas.getContext('2d');
        	            canvas.height = viewport.height;
        	            canvas.width = viewport.width;
        	            
        	            var renderContext = {
        	                canvasContext: context,
        	                viewport : viewport
        	            };
        	            
        	            page.render(renderContext);
        	        });
        	   });
        	}
    	}
      });
    });
  </script>
   <script type="text/javascript" src="https://incapture.atlassian.net/s/en_UScc0y2g-418945332/6025/76/1.3.4/_/download/batch/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector.js?collectorId=f455c0db"></script>
  </body>
</html>
