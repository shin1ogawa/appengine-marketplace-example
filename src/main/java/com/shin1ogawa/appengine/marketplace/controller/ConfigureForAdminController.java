package com.shin1ogawa.appengine.marketplace.controller;

import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;
import org.slim3.controller.Navigation;

/**
 * when "Additional Configuration" at cpanel.
 * <p>necessary to pass the parameter {@literal domain}.</p>
 * @author shin1ogawa
 */
public class ConfigureForAdminController extends MarketplaceController {

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
		w.println("<h1>when 'Additional Configuration' at cpanel</h1>");
		w.println(getDebugInformationHtml(request));
		w.print("<a href=\"https://www.google.com/a/cpanel/");
		w.print(request.getParameter("domain"));
		w.print("/PikeplaceAppSettings?appId=");
		w.print(MARKETPLACE_APPID);
		w.println("&licenseNamespace=PACKAGE_GAIAID\">");
		w.println("complete configuration and back to apps cpanel.</a>");
		w.println("</body></html>");
		response.flushBuffer();
		return null;
	}
}
