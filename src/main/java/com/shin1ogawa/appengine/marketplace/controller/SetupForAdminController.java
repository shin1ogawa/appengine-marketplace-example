package com.shin1ogawa.appengine.marketplace.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;
import org.slim3.datastore.Datastore;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.shin1ogawa.appengine.marketplace.Configuration;
import com.shin1ogawa.appengine.marketplace.gdata.IncreaseURLFetchDeadlineDelegate;

/**
 * when "Add it now(3 External configuration)" from listing page.
 * <p>necessary to pass the parameter {@literal domain}.</p>
 * <p>necessary to pass the parameter {@literal callback}.</p>
 * @author shin1ogawa
 */
public class SetupForAdminController extends Controller {

	static final Logger logger = Logger.getLogger(SetupForAdminController.class.getName());

	/** for backup an original {@link ApiProxy#getDelegate()} */
	Delegate<Environment> delegate;

	String domain;

	String callback;


	@Override
	protected Navigation setUp() {
		domain = asString("domain");
		callback = asString("callback");
		if (StringUtils.isEmpty(callback)) {
			callback = sessionScope("callback");
			removeSessionScope("callback");
		}
		if (StringUtils.isEmpty(domain) || StringUtils.isEmpty(callback)) {
			return redirect(Configuration.get().getMarketplaceListingUrl());
		}
		UserService us = UserServiceFactory.getUserService();
		if (us.isUserLoggedIn() == false) {
			// if user had not been authenticated then send redirect to login url.
			String callbackURL = request.getRequestURL() + "?domain=" + domain;
			sessionScope("callback", callback);
			logger.log(Level.INFO, "had not been authenticated: callback=" + callbackURL);
			return redirect(us.createLoginURL(callbackURL, null, domain, null));
		}
		if (StringUtils.contains(us.getCurrentUser().getFederatedIdentity(), domain) == false) {
			// if user had been authenticated but invalid domain then send redirect to logout url.
			sessionScope("callback", callback);
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
		setUpApplication();
		responseCompleted();
		return null;
	}

	@Override
	protected void tearDown() {
		if (delegate != null) {
			IncreaseURLFetchDeadlineDelegate.uninstall(delegate);
		}
		super.tearDown();
	}

	void responseCompleted() throws IOException {
		StringBuilder debugInformation = Utils.getHtmlForDebug(request);
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		PrintWriter w = response.getWriter();
		w.println("<html><head>");
		w.println("<meta http-equiv=\"refresh\" content=\"5 ; URL=" + callback + "\">");
		w.println("<title>appengine-gdata-example</title></head><body>");
		w.println("<h1>appengine-gdata-example</h1>");
		w
			.println("<p>from 'Add it now(3 External configuration)' at makretplace listing page.</p>");
		w
			.println("<p><a target=\"_blank\" href=\"http://github.com/shin1ogawa/appengine-marketplace-example/blob/master/src/main/java/com/shin1ogawa/appengine/marketplace/controller/SetupForAdminController.java\">");
		w.println("source code</a></p>");
		w.print("<p>completed successfully. <a href=\"");
		w.print(callback);
		w.println("\">redirecting to cpanel...</a></p>");
		w.println(debugInformation);
		w.println("</body></html>");
		response.flushBuffer();
	}

	void setUpApplication() {
		Entity entity = new Entity(Datastore.createKey("LicensedDomain", domain));
		entity.setProperty("timestamp", new Date());
		entity.setUnindexedProperty("appversion", ApiProxy.getCurrentEnvironment().getVersionId());
		Datastore.put(entity);
	}
}
