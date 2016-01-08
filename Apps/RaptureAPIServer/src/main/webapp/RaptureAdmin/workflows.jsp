<%@ page import="java.util.*"%>
<%@ page import="rapture.kernel.*"%>
<%@ page import="rapture.common.*"%>
<%@ page import="rapture.common.model.*"%>
<%@ page import="rapture.common.dp.*"%>
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
<link rel="apple-touch-icon-precomposed" sizes="144x144"
	href="../assets/ico/apple-touch-icon-144-precomposed.png">
<link rel="apple-touch-icon-precomposed" sizes="114x114"
	href="../assets/ico/apple-touch-icon-114-precomposed.png">
<link rel="apple-touch-icon-precomposed" sizes="72x72"
	href="../assets/ico/apple-touch-icon-72-precomposed.png">
<link rel="apple-touch-icon-precomposed"
	href="../assets/ico/apple-touch-icon-57-precomposed.png">
</head>

<body>

	<%@ include file="include/menu.jsp"%>


	<div class="container">

		<div class="row">
			<h2>Decision Process Workflows</h2>
			<%
			    List<Workflow> workflows = Kernel.getDecision().getAllWorkflows(ContextFactory.getKernelUser());
			%>
			<div class="span5">
				<table class="table table-striped table-condensed table-bordered">
					<thead>
						<tr>
							<th>Workflow URI</th>
							<th>View JSON</th>
							<th>View Graph</th>
						</tr>
					</thead>
					<tbody>
						<%
						    for (Workflow workflow : workflows) {
						%>
						<tr id="rapRow-<%=workflow.getWorkflowURI()%>">
							<td><%=workflow.getWorkflowURI()%></td>
							<td><a role="button"
								class="viewJson btn btn-primary btn-mini"
								id="<%=workflow.getWorkflowURI()%>">View JSON</a></td>
							<td><a role="button"
								class="viewGraph btn btn-primary btn-mini"
								id="<%=workflow.getWorkflowURI()%>">View Graph</a></td>
						</tr>
						<%
						    }
						%>
					</tbody>
				</table>
			</div>
			<div class="span7">
				<div class="alert alert-info">
					<b id="contentName"></b>
				</div>
				<div class="brush: js" id="textContents"></div>
				<div class="brush: js" id="svgContainer"></div>
			</div>

		</div>

		<hr>

		<footer>
			<p>&copy; Incapture Technologies 2012-2013</p>
		</footer>

	</div>
	<!-- /container -->

	<!-- Le javascript
    ================================================== -->
	<!-- Placed at the end of the document so the pages load faster -->
	<script src="../assets/js/jquery.js"></script>
	<script src="../assets/js/bits.js"></script>
	<script src="../assets/js/bootstrap.js"></script>
	<script type="text/javascript" src="../assets/js/md5.js"></script>
	<%@ include file="include/sec.jsp"%>
	<script>
		$('.viewJson').on('click', function(event) {
            var workflowURI = this.id;
			$.ajax({
				url : "../reflex/getWorkflow.rfx?workflowURI=" + workflowURI,
				dataType : 'json',
				success : function(data) {
					$('#contentName').text("JSON for " + workflowURI);
					$("#textContents").show();
                    $("#textContents").text(JSON.stringify(data));
					$("#svgContainer").hide();
				}
			});

		});

		$('.viewGraph').on('click', function(event) {
            var workflowURI = this.id;
            alert("Graphing not yet implemented!")
			$('#contentName').text("Graph for " + workflowURI);
            $("#textContents").hide();
            $("#svgContainer").html("");
            $("#svgContainer").show();
		});
	</script>

	<style type="text/css">
td,th {
	word-wrap: break-word;
}
</style>
</body>
</html>
