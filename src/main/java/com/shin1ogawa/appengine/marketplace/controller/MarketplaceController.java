package com.shin1ogawa.appengine.marketplace.controller;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.shin1ogawa.appengine.marketplace.gdata.IncreaseURLFetchDeadlineDelegate;
import com.shin1ogawa.appengine.marketplace.gdata.LicensingAPI;

/**
 * Base class for {@link Controller} of Marketplace.
 * @author shin1ogawa
 */
public abstract class MarketplaceController extends Controller {

	static final Logger logger = Logger.getLogger(SetupForAdminController.class.getName());

	static final String CONSUMER_KEY;

	static final String CONSUMER_SECRET;

	static final String MARKETPLACE_APPID;

	static final String MARKETPLACE_LISTING_ID;

	static final String MARKETPLACE_LISTING_URL;

	private Delegate<Environment> delegate;

	static {
		Properties properties = new Properties();
		try {
			properties.load(MarketplaceController.class
				.getResourceAsStream("/marketplace.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		CONSUMER_KEY = properties.getProperty("consumerKey");
		CONSUMER_SECRET = properties.getProperty("consumerSecret");
		MARKETPLACE_APPID = properties.getProperty("marketplaceAppId");
		MARKETPLACE_LISTING_ID = properties.getProperty("marketplaceListingId");
		MARKETPLACE_LISTING_URL = properties.getProperty("marketplaceListingUrl");
	}


	@Override
	protected Navigation setUp() {
		delegate = IncreaseURLFetchDeadlineDelegate.install();
		String domain = request.getParameter("domain");
		if (StringUtils.isEmpty(domain)) {
			return redirect(MARKETPLACE_LISTING_URL);
		}
		UserService us = UserServiceFactory.getUserService();
		if (us.isUserLoggedIn() == false) {
			// if user had not been authenticated then send redirect to login url.
			String callbackURL = request.getRequestURL() + "?" + request.getQueryString();
			logger.log(Level.INFO, "callback=" + callbackURL);
			return redirect(us.createLoginURL(callbackURL, null, domain, null));
		}
		if (StringUtils.contains(us.getCurrentUser().getFederatedIdentity(), domain) == false) {
			// if user had been authenticated but invalid domain then send redirect to logout url.
			String callbackURL = request.getRequestURL() + "?" + request.getQueryString();
			logger.log(Level.INFO, "callback=" + callbackURL);
			return redirect(us.createLogoutURL(callbackURL, domain));
		}
		NamespaceManager.set(domain);
		return super.setUp();
	}

	@Override
	protected void tearDown() {
		if (delegate != null) {
			IncreaseURLFetchDeadlineDelegate.uninstall(delegate);
		}
		super.tearDown();
	}

	@SuppressWarnings("unchecked")
	static StringBuilder getDebugInformationHtml(HttpServletRequest request) {
		StringBuilder b = new StringBuilder();
		b.append("<h2>Request Headers</h2><ul>");
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String value = request.getHeader(name);
			b.append("<li>").append(name).append("=").append(value).append("</li>");
		}
		b.append("</ul>");
		b.append("<h2>Request Parameters</h2><ul>");
		names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String[] values = request.getParameterValues(name);
			for (String value : values) {
				b.append("<li>").append(name).append("=").append(value).append("</li>");
			}
		}
		b.append("</ul>");
		b.append("<h2>CurrentUser</h2><ul>");
		UserService us = UserServiceFactory.getUserService();
		User currentUser = us.getCurrentUser();
		if (currentUser == null) {
			b.append("<li>currentUser=null</li>");
			b.append("</ul>");
			return b;
		}
		b.append("<li>authDomain=").append(currentUser.getAuthDomain()).append("</li>");
		b.append("<li>email=").append(currentUser.getEmail()).append("</li>");
		b.append("<li>federatedIdentity=").append(currentUser.getFederatedIdentity()).append(
				"</li>");
		b.append("<li>nickname=").append(currentUser.getNickname()).append("</li>");
		b.append("<li>userId=").append(currentUser.getUserId()).append("</li>");
		b.append("<li><a href=\"").append(
				us.createLogoutURL(request.getRequestURL() + "?" + request.getQueryString()))
			.append("\">login to another user.</a></li>");
		b.append("</ul>");
		return b;
	}

	/**
	 * retrieve a license status at specified domain.
	 * @param domain
	 * @return {@literal ACTIVE} or {@literal PENDING} or {@literal UNLICENSED}
	 */
	static String getLicenseState(String domain) {
		LicensingAPI api = new LicensingAPI(CONSUMER_KEY, CONSUMER_SECRET);
		Map<String, String> map = api.retrieveLicenseStateOfDomain(MARKETPLACE_APPID, domain);
		return map.get("state");
	}
}
