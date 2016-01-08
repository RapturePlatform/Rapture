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
    <meta name="description" content="Rapture Login">
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
	<div class="container">
		<div class="content">
			<div class="row">
				<div class="login-form offset4 span4">
					<h2>Rapture Login</h2>
					<form id="loginForm" action="doLogin.jsp">
						<fieldset>
							<div class="clearfix">
								<input type="text" placeholder="Username" name="user"/>
							</div>
							<div class="clearfix">
								<input type="password" placeholder="Password" id="pass"/>
								<input type="hidden" name="password" id="hiddenpassword"/>
								<input type="hidden" name="return" value="<%=request.getParameter("back")%>"/>
							</div>
							<button class="btn primary" type="submit">Sign in</button>
						</fieldset>
					</form>
				</div>
			</div>
		</div>
	</div> <!-- /container -->
    <!-- Le javascript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
   <script src="../assets/js/jquery.js"></script>
     <script src="../assets/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="../assets/js/jquery.form.js"></script>  
    <script type="text/javascript" src="../assets/js/md5.js"></script>  
    
    <script>
    $('#loginForm').submit(function() {
    	var password = $('#pass').val();
    	var md5pass = MD5(password);
    	$('#hiddenpassword').val(md5pass);
    	return true;
    });
   
    </script>
  </body>
</html>
