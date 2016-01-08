<%@ page import="java.util.*"%>
<%@ page import="rapture.kernel.*"%>
<%@ page import="rapture.common.*"%>
<%@ page import="rapture.common.model.*"%>
<%@ page import="org.ocpsoft.pretty.time.PrettyTime"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Rapture Admin, from Incapture</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="Rapture Runner Status">
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
	<%@ include file="include/sec.jsp"%>


	<div class="container">

		<div class="row">
			<a class="btn" href="runner.jsp"><i class="icon-refresh"></i></a>
			<div class="span12">
				<h2>Active Servers</h2>
				<%
				    List<String> servers = Kernel.getRunner().getRunnerServers(context);
				%>
				<table class="table table-striped  table-condensed">
					<thead>
						<tr>
							<th>Server</th>
							<th>Status</th>
						</tr>
					</thead>
					<tbody>
						<%
						    for (String server : servers) {
						%>
						<tr>
							<td><%=server%></td>
							<%
							    RaptureRunnerStatus status = Kernel.getRunner().getRunnerStatus(context, server);
							        Map<String, RaptureRunnerInstanceStatus> statusByName = status.getStatusByInstanceName();
							%>
							<td>
								<table class="table table-striped  table-condensed">
									<thead>
										<tr>
											<th>Instance</th>
											<th>Server Group</th>
											<th>Status</th>
											<th>Last Seen</th>
										</tr>
									</thead>
									<tbody>
										<%
										    for (Map.Entry<String, RaptureRunnerInstanceStatus> entry : statusByName.entrySet()) {
										%>
										<tr>
											<td><%=entry.getValue().getAppInstance()%></td>
											<td><%=entry.getValue().getServerGroup()%></td>
											<td><%=entry.getValue().getStatus()%></td>
											<td><%=entry.getValue().getLastSeen().toString()%></td>
										</tr>
										<%
										    }
										%>
									</tbody>
								</table>
							</td>
						</tr>
						<%
						    }
						%>
					</tbody>
				</table>
			</div>
		</div>
		<div class="row">
			<h2>Applications</h2>
			<div class="span12">
				<table class="table table-striped  table-condensed">
					<thead>
						<tr>
							<th>Name</th>
							<th>Description</th>
							<th>Version</th>
						</tr>
					</thead>
					<tbody>
						<%
						    List<String> appDefNames = Kernel.getRunner().getApplicationDefinitions(context);
						    for (String appDef : appDefNames) {
						        RaptureApplicationDefinition definition = Kernel.getRunner().retrieveApplicationDefinition(context, appDef);
						%>
						<tr>
							<td><%=definition.getName()%></td>
							<td><%=definition.getDescription()%></td>
							<td><%=definition.getVersion()%></td>
						</tr>
						<%
						    }
						%>
					</tbody>
				</table>
			</div>
		</div>
		<div class="row">
			<h2>Server Groups</h2>
			<div class="span12">
				<table class="table table-striped  table-condensed">
					<thead>
						<tr>
							<th>Name</th>
							<th>Description</th>
							<th>Inclusions</th>
							<th>Exclusions</th>
							<th>Applications</th>
						</tr>
					</thead>
					<tbody>
						<%
						    List<String> serverGroups = Kernel.getRunner().getServerGroups(context);
						    for (String serverGroup : serverGroups) {
						        RaptureServerGroup group = Kernel.getRunner().getServerGroup(context, serverGroup);
						%>
						<tr>
							<td><%=serverGroup%></td>
							<td><%=group.getDescription()%></td>
							<td><%=group.getInclusions().toString()%></td>
							<td><%=group.getExclusions().toString()%></td>
							<td>
								<table class="table table-striped  table-condensed">
									<thead>
										<tr>
											<th>Name</th>
											<th>Application</th>
											<th>Description</th>
											<th>TimeRange</th>
										</tr>
									</thead>
									<tbody>
										<%
										    List<String> appsInGroup = Kernel.getRunner().getApplicationsForServerGroup(context, serverGroup);
										        for (String appInGroup : appsInGroup) {
										            RaptureApplicationInstance inst = Kernel.getRunner().retrieveApplicationInstance(context, appInGroup, serverGroup);
										%>
										<tr>
											<td><%=appInGroup%></td>
											<td><%=inst.getAppName()%></td>
											<td><%=inst.getDescription()%></td>
											<td><%=inst.getTimeRangeSpecification()%></td>
										</tr>
										<%
										    }
										%>
									</tbody>
								</table>
							</td>
						</tr>
						<%
						    }
						%>
					</tbody>
				</table>
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
	<script src="../assets/js/bootstrap.min.js"></script>
	<script type="text/javascript"
		src="../assets/js/jquery.sparkline.min.js"></script>

</body>
</html>
