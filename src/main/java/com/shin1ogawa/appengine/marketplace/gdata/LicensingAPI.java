package com.shin1ogawa.appengine.marketplace.gdata;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gdata.client.appsforyourdomain.AppsPropertyService;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.authn.oauth.OAuthParameters.OAuthType;
import com.google.gdata.data.DateTime;
import com.shin1ogawa.appengine.marketplace.gdata.GDataAPIUtil.GDataAPIException;

/**
 * Service class to use Licensing API.
 * @see <a href="http://code.google.com/googleapps/marketplace/licensing.html">Licensing API - Google Apps Marketplace - Google Code</a>
 * @author shin1ogawa
 */
public class LicensingAPI {

	static final Logger logger = Logger.getLogger(LicensingAPI.class.getName());

	static final String EQUAL = "%3D"; // "="

	static final String OPEN_SQUARE = "%5B"; // "["

	static final String CLOSE_BRACE = "%5D"; // "]"

	final GoogleOAuthParameters oauthParams;


	/**
	 * the constructor.
	 * @param consumerKey
	 * @param consumerSecret
	 * @category constructor
	 */
	public LicensingAPI(String consumerKey, String consumerSecret) {
		this.oauthParams = new GoogleOAuthParameters();
		oauthParams.setOAuthConsumerKey(consumerKey);
		oauthParams.setOAuthConsumerSecret(consumerSecret);
		oauthParams.setOAuthType(OAuthType.TWO_LEGGED_OAUTH);
	}

	/**
	 * retrieve the domain's licensing state.
	 * @param appId ID of application to do listing to Marketplace
	 * @param domain domain name to be investigated
	 * @return a map of domain's licensing state
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>enabled</code></li>
	 * <li><code>state</code>: ACTIVE or UNLICENSED or PENDING</li>
	 * </ul>
	 * @see <a href="http://code.google.com/googleapps/marketplace/licensing.html#licensed">Check that a Domain is Licensed</a>
	 * @throws GDataAPIException 
	 */
	public Map<String, String> retrieveLicenseStateOfDomain(String appId, String domain) {
		try {
			String url =
					"http://feedserver-enterprise.googleusercontent.com/license?bq=" + OPEN_SQUARE
							+ "appid" + EQUAL + appId + CLOSE_BRACE + OPEN_SQUARE + "domain"
							+ EQUAL + domain + CLOSE_BRACE;
			AppsPropertyService service =
					new AppsPropertyService(GDataAPIUtil.getApplicationName());
			service.setOAuthCredentials(oauthParams, new OAuthHmacSha1Signer());
			List<Map<String, String>> states =
					parseLicenseFeed(GDataAPIUtil.getXmlWithRetry(service, new URL(url)));
			if (Iterables.size(states) != 1) {
				throw new IllegalStateException("Too many state.");
			}
			return states.get(0);
		} catch (MalformedURLException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		} catch (SAXException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		} catch (IOException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		} catch (ParserConfigurationException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		} catch (OAuthException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		}
	}

	/**
	 * retrieve the licensing notifications.
	 * @param appId ID of application to do listing to Marketplace
	 * @param date datetime to be investigated
	 * @return a list of map of domain's licensing state
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>domainname</code></li>
	 * <li><code>installeremail</code></li>
	 * <li><code>lastchangetime</code></li>
	 * <li><code>productconfigid</code></li>
	 * <li><code>state</code>: ACTIVE or UNLICENSED or PENDING</li>
	 * </ul>
	 * @throws GDataAPIException 
	 * @see <a href="http://code.google.com/googleapps/marketplace/licensing.html#notifications">Check for License Notifications</a>
	 */
	public List<Map<String, String>> retrieveLicenseNotifications(String appId, Date date)
			throws GDataAPIException {
		try {
			String startdatetime = new DateTime(date).toString();
			String maxresults = "100";
			String url =
					" http://feedserver-enterprise.googleusercontent.com/licensenotification?bq="
							+ OPEN_SQUARE + "appid" + EQUAL + appId + CLOSE_BRACE + OPEN_SQUARE
							+ "startdatetime" + EQUAL + startdatetime + CLOSE_BRACE + OPEN_SQUARE
							+ "max-results" + EQUAL + maxresults + CLOSE_BRACE;
			AppsPropertyService service =
					new AppsPropertyService(GDataAPIUtil.getApplicationName());
			service.setOAuthCredentials(oauthParams, new OAuthHmacSha1Signer());
			return parseLicenseFeed(GDataAPIUtil.getXmlWithRetry(service, new URL(url)));
		} catch (MalformedURLException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		} catch (IOException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		} catch (SAXException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		} catch (ParserConfigurationException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		} catch (OAuthException e) {
			throw new GDataAPIUtil.GDataAPIException(e);
		}
	}

	static List<Map<String, String>> parseLicenseFeed(InputStream is) throws SAXException,
			IOException, ParserConfigurationException {
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		final List<Map<String, String>> list = Lists.newArrayList();
		parser.parse(is, new DefaultHandler() {

			boolean entity = false;

			String currentElement;

			Map<String, String> map;


			@Override
			public void characters(char[] ch, int start, int length) {
				if (entity) {
					map.put(currentElement, new String(ch, start, length));
				}
			}

			@Override
			public void startElement(String uri, String localName, String qName,
					Attributes attributes) {
				if (entity == false && StringUtils.equals(qName, "entity")) {
					entity = true;
					map = Maps.newHashMap();
					list.add(map);
					return;
				}
				currentElement = qName;
			}

			@Override
			public void endElement(String uri, String localName, String qName) {
				if (entity && StringUtils.equals(qName, "entity")) {
					entity = false;
				}
			}
		});
		return list;
	}
}
