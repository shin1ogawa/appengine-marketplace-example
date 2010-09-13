package com.shin1ogawa.appengine.marketplace.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slim3.datastore.Datastore;
import org.slim3.memcache.Memcache;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.shin1ogawa.appengine.marketplace.Configuration;
import com.shin1ogawa.appengine.marketplace.gdata.LicensingAPI;

/**
 * @author shin1ogawa
 */
public class Utils {

	/**
	 * retrieve a license status at specified domain.
	 * @param domain
	 * @return Map of Licensing State.
	 * <dl>
	 * <dt>enabled</dt>
	 * <dd>{@literal true} or {@literal false}</dd>
	 * <dt>state</dt>
	 * <dd>{@literal ACTIVE} or {@literal PENDING} or {@literal UNLICENSED}</dd>
	 * </dl>
	 */
	static Map<String, String> getLicenseState(String domain) {
		String memcacheKey = "getLicenseState\t" + domain;
		Map<String, String> map = Memcache.get(memcacheKey);
		if (map != null) {
			return map;
		}
		LicensingAPI api =
				new LicensingAPI(Configuration.get().getConsumerKey(), Configuration.get()
					.getConsumerSecret());
		map = api.retrieveLicenseStateOfDomain(Configuration.get().getMarketplaceAppId(), domain);
		Memcache.put(memcacheKey, map, Expiration.byDeltaSeconds(60 * 15)); // 15min
		return map;
	}

	static Key createAppsUserKey(User currentUser) {
		String federatedIdentity = currentUser.getFederatedIdentity();
		int lastIndexOf = federatedIdentity.lastIndexOf("openid?");
		Key appsUserKey =
				Datastore.createKey("AppsUser", federatedIdentity.substring(lastIndexOf
						+ "openid?".length()));
		return appsUserKey;
	}

	@SuppressWarnings("unchecked")
	static StringBuilder getHtmlForDebug(HttpServletRequest request) {
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
		b.append("<li><a href=\"").append(us.createLogoutURL("/Logout")).append(
				"\">logout.</a></li>");
		b.append("</ul>");
		return b;
	}

	static void responseRefresh(HttpServletResponse response, String redirectUrl, String message)
			throws IOException {
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		PrintWriter w = response.getWriter();
		w.println("<html><head>");
		w.println("<meta http-equiv=\"refresh\" content=\"3 ; URL=" + redirectUrl + "\">");
		w.println("<title>" + message + "</title></head><body>");
		w.println("<h1>" + message + "</h1>");
		w.println("<a href=\"" + redirectUrl + "\">redirecting to listing page...</a>");
		w.println("</body></html>");
		response.flushBuffer();
	}
}
