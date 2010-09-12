package com.shin1ogawa.appengine.marketplace.controller;

import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;
import org.slim3.controller.Navigation;

/**
 * when started from universal navigation.
 * <p>necessary to pass the parameter {@literal domain}.</p>
 * @author shin1ogawa
 */
public class TopPageController extends MarketplaceController {

	@Override
	protected Navigation setUp() {
		Navigation navigation = super.setUp();
		if (navigation != null) {
			return navigation;
		}
		if (StringUtils.equalsIgnoreCase(getLicenseState(request.getParameter("domain")), "ACTIVE") == false) {
			// if not licensed then redirect to listing page.
			return redirect(MARKETPLACE_LISTING_URL);
		}
		return null;
	}

	@Override
	protected Navigation run() throws Exception {
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		PrintWriter w = response.getWriter();
		w.println("<html><head><title>appengine-gdata-example</title></head><body>");
		w.println("<h1>started from universal navigation</h1>");
		w.println(getDebugInformationHtml(request));
		w.println("<h2>licensing notifications.</h2>");
		w.println("<div><form action=\"TopPage?method=showLicenseNotification\" method=\"get\">");
		w
			.println("<p><label>domain: </label><input type=\"text\" name=\"domain\" size=\"75\" /></p>");
		w
			.println("<p><label>date(yyyy-MM-dd): </label><input type=\"text\" name=\"datetime\" size=\"75\" /></p>");
		w.println("<p><input type=\"submit\" /></p>");
		w.println("</form></div>");
		w.println("</body></html>");
		response.flushBuffer();
		return null;
	}
}
