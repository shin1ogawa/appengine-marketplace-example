package com.shin1ogawa.appengine.marketplace.controller;

import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;
import org.slim3.controller.Navigation;

/**
 * when "Add it now" from listing page.
 * <p>necessary to pass the parameter {@literal domain}.</p>
 * <p>necessary to pass the parameter {@literal callback}.</p>
 * @author shin1ogawa
 */
public class SetupForAdminController extends MarketplaceController {

	@Override
	protected Navigation run() throws Exception {
		StringBuilder debugInformation = getDebugInformationHtml(request);
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		PrintWriter w = response.getWriter();
		w.println("<html><head><title>appengine-gdata-example</title></head><body>");
		w.println("<h1>when 'Add it now' from listing page</h1>");
		w.println(debugInformation);
		if (StringUtils.isNotEmpty(request.getParameter("callback"))) {
			w.print("<a href=\"");
			w.print(request.getParameter("callback"));
			w.println("\">complete setup and back to apps cpanel.</a>");
		}
		w.println("</body></html>");
		response.flushBuffer();
		return null;
	}
}
