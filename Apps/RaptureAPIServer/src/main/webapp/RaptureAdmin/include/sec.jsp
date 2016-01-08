<!-- Check security -->
<%@ page import="rapture.server.*" %>
<%@ page import="rapture.common.exception.*" %>
<%
    CallingContext context = null;
    try {
		context = BaseDispatcher.validateSession(request);
%>
<script>
        updateUser("<%=context.getUser()%>");
</script>
<%
	} catch(RaptNotLoggedInException e) {
	   String back = request.getContextPath() + request.getServletPath();
	   response.sendRedirect("login.jsp?back=" + back);
	}
%>
