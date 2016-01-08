package rapture.server.web.<sdkname>.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import rapture.server.web.servlet.BaseServlet;
import rapture.server.web.servlet.StandardCallInfo;
import rapture.common.DispatchReturn;

import rapture.<sdkname>.common.shared.<apitype; format="lower">.Dispatch<apitype>Function;

@WebServlet(urlPatterns="/<sdkname>/<apitype; format="lower">")
@MultipartConfig
public class <apitype>Servlet extends BaseServlet {
    
    private static final long serialVersionUID = -654951209810427391L;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        StandardCallInfo call = processFunctionalRequest(req);

        Dispatch<apitype>Function <apitype>Dispatch = Dispatch<apitype>Function.valueOf(call.getFunctionName());
        DispatchReturn response;
        try {
            response = <apitype>Dispatch.executeDispatch(call.getContent(), req, resp);
        } catch (Exception e) {
            response = handleUnexpectedException(e);
        }
        sendResponseAppropriately(response.getContext(), req, resp, response.getResponse());
    }

}
