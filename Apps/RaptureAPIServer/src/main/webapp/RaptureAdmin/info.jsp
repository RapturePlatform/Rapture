<%@ page import="java.util.*"%>
<%@ page import="rapture.kernel.*"%>

<%@ page import="rapture.server.*"%>

<%@ page import="rapture.common.exception.*"%>
<%@ page import="rapture.common.*"%>
<%@ page import="rapture.common.model.*"%>
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

	<%@ include file="include/sec.jsp"%>
	<%@ include file="include/menu.jsp"%>

	<div class="container">

		<div class="row">
			<div class="span8">
				<h2>Rapture System Config</h2>
				<%
				    Map<String, String> props = Kernel.getAdmin().getSystemProperties(context, new ArrayList<String>());
				%>
				<table class="table table-striped  table-condensed">
					<thead>
						<tr>
							<th>Key</th>
							<th>Value</th>
						</tr>
					</thead>
					<tbody>
						<%
						    for (Map.Entry<String, String> p : props.entrySet()) {
						%>
						<tr>
							<td><strong><%=p.getKey()%></strong></td>
							<td><code><%=p.getValue()%></code></td>
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
	<script src="../assets/js/bootstrap.min.js"></script>
	<script type="text/javascript"
		src="../assets/js/jquery.sparkline.min.js"></script>
	<script src="../assets/js/bits.js"></script>

</body>
</html>
