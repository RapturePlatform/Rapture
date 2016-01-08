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
        <div class="pagination">
 			<ul id="workflowList">
			</ul>
       </div>
      </div>
       <div class="row hide rapViewInfo">
        <div class="span12">
        <div class="row">
        <a class="btn" data-toggle="modal" href="#startWorkflow" >Start Workflow</a>
        <h2 id="workflowDetailsHeader">Workflow details</h2>
        <ul class="nav nav-tabs">
  			<li><a href="#wdetails" data-toggle="tab">Details</a></li>
  			<li><a href="#wsteps" data-toggle="tab">Steps</a></li>
  			<li><a href="#wruns" data-toggle="tab">Runs</a></li>
 		</ul>
 		<div class="tab-content">
 		   <div class="tab-pane active" id="wdetails">
    				<table id="workflowDetails" class="table table-striped table-condensed">
        			<tbody>
       				 <tr><td>Name</td><td id="wdName"></td></tr>
        			<tr><td>Description</td><td id="wdDescription"></td></tr>
        			</tbody>
       			 </table>             
  		   </div>
 		   <div class="tab-pane" id="wsteps">
       			<table id="workflowStepDetails" class="table table-striped table-condensed table-bordered">
        			</table>
  		   </div>
 		   <div class="tab-pane" id="wruns">
        		<table id="workflowRunList" class="table table-striped table-condensed">
        		</table>
   		   </div>
 		</div>  
        </div>
        <div class="row hide rapViewInfo2">
          <h2>Workflow run</h2>
              <div class="span3">
               <h3>Summary</h3>
                <table class="table table-striped table-condensed">
               <tbody>
               	<tr><td>Status</td><td id="wrStatus"></td></tr>
               	<tr><td>Progress</td><td><div id="wrProgress"></div></td></tr>
               	<tr><td>Action</td><td><a id="" href="#" class="btn btn-mini rapBump">Bump</a></td></tr>
               </tbody>
               </table>
              </div>
              <div class="span3">
               <h3>Initial parameters</h3>
               <table id="wrInitParams" class="table table-striped table-condensed">
               </table>
              </div>              
              <div class="span3">
               <h3>Running parameters</h3>
               <table id="wrRunningParams" class="table table-striped table-condensed">
                </table>              
              </div>              
         </div>
         <div class="row hide rapViewInfo2">
                    <h2>Run progress</h2>
             		<table id="wrRunProgress" class="table table-striped table-condensed">
             		</table>
         </div>
        </div>
      </div>
      <div class="row">
        <div class="span12">
 
            <div class="row">
               </div>
               <div class="span9">
               </div>
            </div>
            <table id="workflowRun" class="table table-striped table-condensed">
            </table>
        </div>
      </div>
      <div class="modal hide" id="startWorkflow">
  		<div class="modal-header">
    		<button type="button" class="close" data-dismiss="modal">x</button>
    		<h3>Start Workflow</h3>
 		 </div>
  		<div class="modal-body">
		  <label>Workflow Name</label>
 		 <input id="runWorkflowID" type="text" class="span3" placeholder="name...">
  		 <span class="help-block">Please enter the name of a workflow</span>
		  <label>Workflow Parameters</label>
 		 <input id="runWorkflowParameters" type="text" class="span3" placeholder="name...">
  		 <span class="help-block">Please enter any parameters for the workflow, in the form x=y,a=b</span>
  		</div>
  		<div class="modal-footer">
    	<a href="#" class="btn" data-dismiss="modal">Close</a>
    	<a id="submitRunWorkflow" href="#" class="btn btn-primary">Run</a>
  	</div>
  	</div>
    </div> <!-- /container -->

    <!-- Le javascript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
   <script src="../assets/js/jquery.js"></script>
    <script src="../assets/js/bits.js"></script>
       <%@ include file="include/sec.jsp" %>
 
      <script src="../assets/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="../assets/js/jquery.sparkline.min.js"></script>  

    <script type="text/javascript">
    // This will be the code to get the data and display it above
       var g_combinedId;
       var g_refreshId;
       
       $('#submitRunWorkflow').click(function() {
          var workflowId = $('#runWorkflowID').val();
          var params = $('#runWorkflowParameters').val();
        	$.ajax({
  				url: "../reflex/runWorkflow.rfx?id=" + workflowId + "&params=" + params,
  				dataType: 'json',
  				success: function(data) {
  						// data.id will be the workflow id
  						updateWorkflowStatus(workflowId + "-" + data.id);
 			           $('#startWorkflow').modal('hide');
  				}
  			});
  			
       });
       
       $('.rapBump').live('click', function(event) {
       		// Bump the current workflowRun
      		$.ajax({
  				url: "../reflex/bumpWorkflow.rfx?id=" + g_combinedId,
  				dataType: 'json',
  				success: function(data) {
  						// data.id will be the workflow id
  						updateWorkflowStatus(g_combinedId);
  				}
  			});
       });
       
       $('.rapView').live('click', function(event) {
           // With this.id, update the right hand side with details about this workflow
           var o = $('#runWorkflowID');
           
           $('#runWorkflowID').val(this.id);
           updateRHS(this.id);
           updateWorkflowRun(this.id);
           $('.rapViewInfo').show();
           clearTimeout(g_refreshId);
           $('.rapViewInfo2').hide();
       });
       
       $('.rapStatusDelete').live('click', function(event) {
           // Remove the combined id, then redo the RHS
  		   removeWorkflowStatus(this.id);
       });

      $('.rapStatusView').live('click', function(event) {
           // With this.id, update the right hand side with details about this workflow
           updateWorkflowStatus(this.id);
       });
         
       $.ajax({
  		url: "../reflex/workflowList.rfx",
  		dataType: 'json',
  		success: function(data) {
    		var oldUL = document.getElementById('workflowList');
        	var newUL = oldUL.cloneNode(true);
        	
  		     for(var i=0; i< data.length; i++) {
  		        li = document.createElement('li');
  		        var link = document.createElement('a');
    			link.className = 'rapView';
    			link.id = data[i].name;
    			link.appendChild(document.createTextNode(data[i].name));
 				li.appendChild(link);
 				newUL.appendChild(li);
  		    }
  		    oldUL.parentNode.replaceChild(newUL, oldUL);
        }
        });
        
        function updateWorkflowRun(workflowId) {
      		$.ajax({
  				url: "../reflex/workflowRunList.rfx?id=" + workflowId,
  				dataType: 'json',
  				success: function(data) {
  					// Update and replace the table
  					var oldTable = document.getElementById('workflowRunList'),
        			newTable = oldTable.cloneNode(false),
       				 tr,td;
  		     		var tbody = document.createElement('tbody');
  		     		for(var i=0; i< data.length; i++) {
  		       		tr = document.createElement('tr'); 
  		       		addCell(tr,data[i].id);
  		       		addStatusCell(tr, data[i].status);		        
  		         		        
  		            // Fill in an action button...  		        
  		        	td = document.createElement('td');  		        
    				var link = document.createElement('a');
    				link.className = 'btn btn-mini rapStatusView';
    				link.id = workflowId + "-" + data[i].id;
    				link.appendChild(document.createTextNode('View'));   			
  		        	td.appendChild(link);
  		        	tr.appendChild(td);
 
 		            // Fill in an action button...  		        
  		        	td = document.createElement('td');  		        
    				var link = document.createElement('a');
    				link.className = 'btn btn-mini btn-danger rapStatusDelete';
    				link.id = workflowId + "-" + data[i].id;
    				link.appendChild(document.createTextNode('Delete'));   			
  		        	td.appendChild(link);
  		        	tr.appendChild(td);
  		       
  		        	tbody.appendChild(tr);		        
  		    }
  		    newTable.appendChild(tbody);
  		    oldTable.parentNode.replaceChild(newTable, oldTable);
  
  				}
			});        
        }
        
        function removeWorkflowStatus(combinedId) {
      		$.ajax({
  				url: "../reflex/removeWorkflowStatus.rfx?id=" + combinedId,
  				dataType: 'json',
  				success: function(data) { 				
  				               updateWorkflowRun(data.id);
 
				}
			});        
        }
        
        function updateWorkflowStatus(combinedId) {
     		g_combinedId = undefined;
      		$.ajax({
  				url: "../reflex/workflowRunDetails.rfx?id=" + combinedId,
  				dataType: 'json',
  				success: function(data) {
  					// Need to completely replace the table if it doesn't have the id
  					// of this combined id, otherwise, just update the cells of the
  					// dates, times and status
     				// workflowId
     				// status
     				// statusId
     				// initParams (a table of keys and values)
     				// runningParams
     				// entries for steps (a second table)
     				$('#wrStatus').text(data.status);
					// Need something like the below to wrProgress to 
					// show how far along we are.
					var progress = data.nextStep * 100 / data.workflowStepStatus.length;
					var progressComponent = document.getElementById('wrProgress'); 		
					var divComponent = progressComponent.cloneNode(false);
					
					if (progress < 100) {
						divComponent.className = "progress progress-striped active";
					} else {
					    if (data.status == "COMPLETED") {
							divComponent.className = "progress progress-success";
						} else {
						    divComponent.className = "progress progress-error";
						}
					}
					var divBar = document.createElement('div');
					divBar.className = "bar";
					divBar.style.width = progress + "%";
					//divBar.style = "width: " + progress + "%;";
					divComponent.appendChild(divBar);
					progressComponent.parentNode.replaceChild(divComponent, progressComponent);

     				cloneTable('wrInitParams', data.initParams);
     				cloneTable('wrRunningParams', data.runningParams);
     				
     				var oldTable = document.getElementById('wrRunProgress'),
        			newTable = oldTable.cloneNode(false),
       			    tr,td;
       			    
       			    var thead = document.createElement('thead');
       			    addHeader(thead, 'Name');
       			    addHeader(thead, 'Area');
       			    addHeader(thead, 'Start');
       			    addHeader(thead, 'End');
       			    addHeader(thead, 'Status');
       			    addHeader(thead, 'Result');
  				    newTable.appendChild(thead);
  				    
  				    var tbody = document.createElement('tbody');
  				    for(var i=0; i< data.workflowStepStatus.length; i++) {
  				    	addStatusRow(tbody, data.workflowStepStatus[i]);
  				   	}
  				   	newTable.appendChild(tbody);
  				   	oldTable.parentNode.replaceChild(newTable, oldTable); 
     				g_combinedId = combinedId;
     				g_refreshId = setTimeout(statusRefresh, 1000);
          			 $('.rapViewInfo2').show(); 
  				}
  			});  
        }
        
        function statusRefresh() {
        	if (g_combinedId == undefined) {
        	
        	} else {
        		updateWorkflowStatus(g_combinedId);
        	}
        }
        
        function cloneTable(tableId, infoMap) {
   				    var oldTable = document.getElementById(tableId),
        			newTable = oldTable.cloneNode(false),
       			    tr,td;
        			var tbody = document.createElement('tbody');
        			for(var key in infoMap) {
        				if (infoMap.hasOwnProperty(key)) {
        					tr = document.createElement('tr');
        					td = document.createElement('td');
        					td.appendChild(document.createTextNode(key));
        					tr.appendChild(td);
        					td = document.createElement('td');
        					td.appendChild(document.createTextNode(infoMap[key]));
        					tr.appendChild(td);
        					tbody.appendChild(tr);
        				}
        			}
        			newTable.appendChild(tbody);
   				   	oldTable.parentNode.replaceChild(newTable, oldTable); 
       }
        
        function updateRHS(workflowId) {
      		$.ajax({
  				url: "../reflex/workflowDetails.rfx?id=" + workflowId,
  				dataType: 'json',
  				success: function(data) {
  				    $('#workflowDetailsHeader').text("Workflow details for " + data.name);
  				    // Now add the details
  				    $('#wdName').text(data.name);
  				    $('#wdDescription').text(data.description);
  				    
  				    var oldTable = document.getElementById('workflowStepDetails'),
        			newTable = oldTable.cloneNode(false),
       			    tr,td;
       			    
       			    var thead = document.createElement('thead');
       			    addHeader(thead, 'Name');
       			    addHeader(thead, 'Area');
       			    addHeader(thead, 'Description');
       			    addHeader(thead, 'Authority');
       			    addHeader(thead, 'Script');
       			    addHeader(thead, 'Repeat Count');
       			    addHeader(thead, 'Interval');
       			    addHeader(thead, 'Fail on Error');
  				    newTable.appendChild(thead);
  				    
  				    var tbody = document.createElement('tbody');
  				    for(var i=0; i< data.steps.length; i++) {
  				    	addRow(tbody, data.steps[i]);
  				   	}
  				   	newTable.appendChild(tbody);
  				   	oldTable.parentNode.replaceChild(newTable, oldTable); 
         		}
         	});
        }
        
        function addHeader(thead, name) {
        	var th = document.createElement('th');
        	th.appendChild(document.createTextNode(name));
        	thead.appendChild(th);
        }
        
        function addRow(tbody, step) {
        	var tr = document.createElement('tr');
        	addCell(tr, step.name);
        	addAreaCell(tr, step.area);
        	addCell(tr, step.description);
        	addCell(tr, step.authority);
        	addCell(tr, step.scriptName);
        	addCell(tr, step.repeatCount);
        	addCell(tr, step.delayInterval);
        	addCell(tr, step.failOnError);
        	tbody.appendChild(tr);
       	}
       	
       	function addStatusRow(tbody, step) {
        	var tr = document.createElement('tr');
        	addCell(tr, step.stepName);
        	addAreaCell(tr, step.area);
        	if (step.startTime != undefined) {
        		var d = new Date(step.startTime);
        		addCell(tr, d.toDateString() + " " + d.toTimeString());
        	} else {
        		addCell(tr, "");
        	}
        	if (step.endTime != undefined) {       	
        		d = new Date(step.endTime);
         		addCell(tr, d.toDateString() + " " + d.toTimeString());
         	} else {
        		addCell(tr, "");
        	}
        	addStatusCell(tr, step.stepStatus);
        	addCell(tr, step.stepStatusDescription);
        	tbody.appendChild(tr);
       	}
       	
       	function addAreaCell(tr, element) {
       	    var span = document.createElement('span');
       	    span.className = "label ";
       	    if (element == "core") {
       	       span.className = "label label-success";
       	    } else if (element == "feed") {
       	       span.className = "label label-important";
       	    } else if (element == "research") {
       	       span.className = "label label-info";
       	    } 
       	    span.appendChild(document.createTextNode(element));
       	    var td = document.createElement('td');
       	    td.appendChild(span);
       	    tr.appendChild(td);       	
       	}
       	
       	function addStatusCell(tr, element) {
       	    var span = document.createElement('span');
       	    span.className = "label ";
       	    if (element == "COMPLETED") {
       	       span.className = "label label-success";
       	    } else if (element == "FAILED") {
       	       span.className = "label label-important";
       	    } else if (element == "RUNNING") {
       	       span.className = "label label-info";
       	    } else if (element == "WAITING") {
       	       span.className = "label label-warning";
       	    }
       	    span.appendChild(document.createTextNode(element));
       	    var td = document.createElement('td');
       	    td.appendChild(span);
       	    tr.appendChild(td);
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
    
  	</script>
  </body>
</html>
