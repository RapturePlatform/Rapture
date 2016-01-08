<%@ page import="java.util.*" %>
<%@ page import="rapture.kernel.*" %>
<%@ page import="rapture.stat.*" %>
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
    
    <!-- Sparklines charts for history -->
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">
      google.load("visualization", "1", {packages:["imagesparkline"]});
      google.setOnLoadCallback(drawChart);

      function drawChart() {
      var data = new google.visualization.DataTable();
  		// Add columns
 <%
        List<List<? extends BaseStat>> vals = new ArrayList<List<? extends BaseStat>>();
        
        for(String k : Kernel.getKernel().getStat().getKeys()) {
        	vals.add(Kernel.getKernel().getStat().getHistory(k));
 %>
            data.addColumn('number', '<%= k %>');
 <%
        }
 %>
        data.addRows(<%= vals.get(0).size() %>);
        // And add the rows
 <%
        for(int i=0; i< vals.size(); i++) {
           for(int j=0; j< vals.get(i).size(); j++) {
%>
           data.setCell(<%= j %>, <%= i %>, <%= Double.valueOf(vals.get(i).get(j).toString()) %>);
<%
           }
        }
%>

        var chart = new google.visualization.ImageSparkLine(document.getElementById('chart_div'));
        chart.draw(data, {width: 120, height: 40, showAxisLines: true,  showValueLabels: false, labelPosition: 'left'});
      }
    </script>
  </head>

  <body>

     <%@ include file="include/menu.jsp" %>


    <div class="container">

      <div class="row">
        <div class="span4">
        <h2>Rapture Stats</h2>
 <%
 		Map<String, BaseStat> currentStats = Kernel.getKernel().getStat().getCurrentStats();
 %>       
        <table class="table table-striped">
        <thead><tr><th>Key</th><th>Value</th></tr></thead>
        <tbody>
<%
        for(Map.Entry<String, BaseStat> p : currentStats.entrySet()) {
%>
        <tr>
        <td><strong><%= p.getKey() %></strong></td>
<%
            BaseStat x = p.getValue();
            if (x != null) {
%>
                <td><%= x.toString() %></td>
<%
           }
%>
		</tr>
<%
        }
%>
        </tbody>
        </table>
        </div>
       <div class="span4">
  	   <h2>History </h2>
            <div id="chart_div"></div>
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
    <script src="../assets/js/bits.js"></script>
    <%@ include file="include/sec.jsp" %>
     <script src="../assets/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="../assets/js/jquery.sparkline.min.js"></script>  

  </body>
</html>
