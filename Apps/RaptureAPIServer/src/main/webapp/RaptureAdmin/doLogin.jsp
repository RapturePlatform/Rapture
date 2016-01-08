<%@ page import="rapture.server.*" %>
<%@ page import="rapture.common.exception.*" %>
<%@ page import="rapture.common.*" %>
<%@ page import="rapture.kernel.*" %>
<%
	String userName = request.getParameter("user");
	String password = request.getParameter("password");
	String ret = request.getParameter("return");
	try {
	CallingContext context = Kernel.getLogin().loginWithHash(userName, password, null);
	if (context != null) {
		Cookie cookie = new Cookie("raptureContext", context.getContext());
		cookie.setPath("/");
		response.addCookie(cookie);
	}
	} catch(RaptureException e) {
	}
	response.sendRedirect(ret);
%>
