/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.server.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import rapture.common.DispatchReturn;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.util.ResourceLoader;

/**
 * The menu servlet looks in a root folder and simply returns those menu files merged
 * together as a json doc.
 *
 * @author alan
 */

public class MenuConfigServlet extends BaseServlet {

	private String folder;
    private static Logger log = Logger.getLogger(MenuConfigServlet.class);
	
	@Override
	public void init() throws ServletException {
		log.info("INITIALIZING....");
		
		folder = getServletConfig().getInitParameter("folder");
		if (folder == null) {
			throw new ServletException("No folder parameter");
		} else if (!folder.endsWith("/")) {
			folder = folder + "/";
		}
	}
    /**
     *
     */
    private static final long serialVersionUID = 2930792109818985861L;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		DispatchReturn response;

		try {
			// Look in configured folder for .menu files, load them as json, merge and then return that
			// as a json string
			ServletContext context = getServletContext();
			Set<String> files = context.getResourcePaths(folder);
			log.info("Search for " + folder + " yielded " + files.toString());

			List<Object> configs = new ArrayList<Object>();
			for (String f : files) {
				log.info("Looking at " + f);
				InputStream is = context.getResourceAsStream(f);
				String content = ResourceLoader.getResourceFromInputStream(is);
				log.info("Content is " + content);
				List<?> menus = JacksonUtil.objectFromJson(content, List.class);
				configs.addAll(menus);
			}
			Collections.sort(configs, new Comparator<Object>() {

				@Override
				public int compare(Object o1, Object o2) {
					Map<String, Object> ao1 = (Map<String, Object>) o1;
					Map<String, Object> ao2 = (Map<String, Object>) o2;
					if (ao1.containsKey("rank") && ao2.containsKey("rank")) {
						return (Integer) ao1.get("rank") < (Integer) ao2.get("rank") ? -1 : 1;
					}
					return 1;
				}

			});
			String mergedContent = JacksonUtil.jsonFromObject(configs);
			resp.setStatus(HttpURLConnection.HTTP_OK);
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().append(mergedContent);
			resp.setContentType("text/plain");
			resp.flushBuffer();
		} catch (Exception e) {
			response = handleUnexpectedException(e);
			sendResponseAppropriately(response.getContext(), req, resp, response.getResponse());
		}
    }
}
