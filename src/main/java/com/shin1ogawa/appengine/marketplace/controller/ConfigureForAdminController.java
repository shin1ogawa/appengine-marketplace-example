package com.shin1ogawa.appengine.marketplace.controller;

import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.shin1ogawa.appengine.marketplace.Configuration;
import com.shin1ogawa.appengine.marketplace.gdata.IncreaseURLFetchDeadlineDelegate;

/**
 * when "Additional Configuration" at cpanel.
 * <p>necessary to pass the parameter {@literal domain}.</p>
 * @author shin1ogawa
 */
public class ConfigureForAdminController extends Controller {

	static final Logger logger = Logger.getLogger(ConfigureForAdminController.class.getName());

	/** for backup an original {@link ApiProxy#getDelegate()} */
	Delegate<Environment> delegate;

	String domain;


	@Override
	protected Navigation setUp() {
		domain = asString("domain");
		if (StringUtils.isEmpty(domain)) {
			return redirect(Configuration.get().getMarketplaceListingUrl());
		}
		UserService us = UserServiceFactory.getUserService();
		if (us.isUserLoggedIn() == false) {
			// if user had not been authenticated then send redirect to login url.
			String callbackURL = request.getRequestURL() + "?domain=" + domain;
			logger.log(Level.INFO, "had not been authenticated: callback=" + callbackURL);
			return redirect(us.createLoginURL(callbackURL, null, domain, null));
		}
		if (StringUtils.contains(us.getCurrentUser().getFederatedIdentity(), domain) == false) {
			// if user had been authenticated but invalid domain then send redirect to logout url.
			String callbackURL = request.getRequestURL() + "?domain=" + domain;
			logger.log(Level.INFO, "invalid domain: callback=" + callbackURL);
			return redirect(us.createLogoutURL(callbackURL, domain));
		}
		delegate = IncreaseURLFetchDeadlineDelegate.install();
		NamespaceManager.set(domain);
		return super.setUp();
	}

	@Override
	protected Navigation run() throws Exception {
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		PrintWriter w = response.getWriter();
		w.println("<html><head><title>appengine-gdata-example</title></head><body>");
		w.println("<h1>appengine-gdata-example</h1>");
		w.println("<p>from 'Additional Configuration' at cpanel</p>");
		w
			.println("<p><a target=\"_blank\" href=\"http://github.com/shin1ogawa/appengine-marketplace-example/blob/master/src/main/java/com/shin1ogawa/appengine/marketplace/controller/ConfigureForAdminController.java\">");
		w.println("source code</a></p>");
		w.println(Utils.getHtmlForDebug(request));
		w.print("<a href=\"https://www.google.com/a/cpanel/");
		w.print(domain);
		w.print("/PikeplaceAppSettings?appId=");
		w.print(Configuration.get().getMarketplaceAppId());
		w.println("&licenseNamespace=PACKAGE_GAIAID\">");
		w.println("complete configuration and back to apps cpanel.</a>");
		w.println("</body></html>");
		response.flushBuffer();
		return null;
	}

	@Override
	protected void tearDown() {
		if (delegate != null) {
			IncreaseURLFetchDeadlineDelegate.uninstall(delegate);
		}
		super.tearDown();
	}
}
