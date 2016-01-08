<%@page import="java.io.*" %> 
<%@page import="java.util.*" %> 
<%
    File dir = new File("webapp/raptureAPI");
    for (File f : dir.listFiles()) {
	String str = f.getName();
	if (str.endsWith("whl"))
            out.println("<a href="+str+">" + str + "</file>");
    }
%> 

