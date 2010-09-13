package com.shin1ogawa.appengine.marketplace;

import java.io.IOException;
import java.util.Properties;

/**
 * @author shin1ogawa
 */
public class Configuration {

	static final private Configuration instance = new Configuration();

	final String consumerKey;

	final String consumerSecret;

	final String marketplaceAppId;

	final String marketplaceListingId;

	final String marketplaceListingUrl;


	private Configuration() {
		Properties properties = new Properties();
		try {
			properties.load(Configuration.class.getResourceAsStream("/marketplace.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		consumerKey = properties.getProperty("consumerKey");
		consumerSecret = properties.getProperty("consumerSecret");
		marketplaceAppId = properties.getProperty("marketplaceAppId");
		marketplaceListingId = properties.getProperty("marketplaceListingId");
		marketplaceListingUrl = properties.getProperty("marketplaceListingUrl");
	}

	/**
	 * @return instance
	 */
	public static Configuration get() {
		return instance;
	}

	/**
	 * @return the consumerKey
	 * @category accessor
	 */
	public String getConsumerKey() {
		return consumerKey;
	}

	/**
	 * @return the consumerSecret
	 * @category accessor
	 */
	public String getConsumerSecret() {
		return consumerSecret;
	}

	/**
	 * @return the marketplaceAppId
	 * @category accessor
	 */
	public String getMarketplaceAppId() {
		return marketplaceAppId;
	}

	/**
	 * @return the marketplaceListingId
	 * @category accessor
	 */
	public String getMarketplaceListingId() {
		return marketplaceListingId;
	}

	/**
	 * @return the marketplaceListingUrl
	 * @category accessor
	 */
	public String getMarketplaceListingUrl() {
		return marketplaceListingUrl;
	}
}
