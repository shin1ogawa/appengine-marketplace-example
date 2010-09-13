package com.shin1ogawa.appengine.marketplace.controller;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;
import org.slim3.datastore.Datastore;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.shin1ogawa.appengine.marketplace.Configuration;
import com.shin1ogawa.appengine.marketplace.gdata.IncreaseURLFetchDeadlineDelegate;

/**
 * when started from universal navigation.
 * <p>necessary to pass the parameter {@literal domain}.</p>
 * @author shin1ogawa
 */
public class TopPageController extends Controller {

	static final Logger logger = Logger.getLogger(TopPageController.class.getName());

	Delegate<Environment> delegate;

	protected String domain;

	private User currentUser;


	@Override
	protected Navigation setUp() {
		domain = asString("domain");
		UserService us = UserServiceFactory.getUserService();
		currentUser = us.getCurrentUser();
		if (currentUser == null) {
			if (StringUtils.isEmpty(domain)) {
				return redirect(Configuration.get().getMarketplaceListingUrl());
			}
			// if user had not been authenticated then send redirect to login url.
			String callbackURL = request.getRequestURL() + "?domain=" + domain;
			logger.log(Level.INFO, "had not been authenticated: callback=" + callbackURL);
			return redirect(us.createLoginURL(callbackURL, null, domain, null));
		} else {
			if (StringUtils.isEmpty(domain)) {
				String id = currentUser.getFederatedIdentity();
				int beginIndex = id.indexOf("://") + "://".length();
				int endIndex = id.indexOf('/', beginIndex);
				domain = id.substring(beginIndex, endIndex);
			} else if (StringUtils.contains(currentUser.getFederatedIdentity(), domain) == false) {
				// if user had been authenticated but invalid domain then send redirect to logout url.
				String callbackURL = request.getRequestURL() + "?domain=" + domain;
				logger.log(Level.INFO, "invalid domain: callback=" + callbackURL);
				return redirect(us.createLogoutURL(callbackURL, domain));
			}
		}
		delegate = IncreaseURLFetchDeadlineDelegate.install();
		NamespaceManager.set(domain);
		return super.setUp();
	}

	@Override
	protected Navigation run() throws Exception {
		Map<String, String> licenseState = Utils.getLicenseState(domain);
		boolean notLicensed =
				StringUtils.equalsIgnoreCase(licenseState.get("state"), "ACTIVE") == false
						|| StringUtils.equalsIgnoreCase(licenseState.get("enabled"), "true") == false;
		if (notLicensed) {
			Utils.responseRefresh(response, Configuration.get().getMarketplaceAppId(),
					"not licensed(or not enabled).");
			return null;
		}
		Key appsUserKey = Utils.createAppsUserKey(currentUser);
		Entity appsUser = Datastore.getOrNull(appsUserKey);
		if (appsUser == null) {
			appsUser = new Entity(appsUserKey);
			appsUser.setProperty("email", currentUser.getEmail());
			appsUser.setProperty("createdAt", new Date());
		}
		appsUser.setProperty("updatedAt", new Date());
		Datastore.put(appsUser);

		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		PrintWriter w = response.getWriter();
		w.println("<html><head><title>appengine-gdata-example</title></head><body>");
		w.println("<h1>appengine-gdata-example</h1>");
		w.println("<p>from universal navigation</p>");
		w
			.println("<p><a target=\"_blank\" href=\"http://github.com/shin1ogawa/appengine-marketplace-example/blob/master/src/main/java/com/shin1ogawa/appengine/marketplace/controller/TopPageController.java\">");
		w.println("source code</a></p>");
		w.println(Utils.getHtmlForDebug(request));
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
