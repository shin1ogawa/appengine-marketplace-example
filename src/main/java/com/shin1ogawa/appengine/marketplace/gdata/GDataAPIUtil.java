package com.shin1ogawa.appengine.marketplace.gdata;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.slim3.util.AppEngineUtil;

import com.google.apphosting.api.ApiProxy;
import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.data.IEntry;
import com.google.gdata.data.IFeed;
import com.google.gdata.util.ServiceException;

/**
 * Utilities for using gdata-api at appengine.
 * @author shin1ogawa
 */
public class GDataAPIUtil {

	static int MAX_RETRY_COUNT = 3;

	static int MAX_RETRY_MILLIS = 20000;

	static int TIMEOUT_MILLIS =
			AppEngineUtil.isProduction() || AppEngineUtil.isDevelopment() ? 10000 : 20000;

	static Level LOG_LEVEL = Level.FINE;

	static String applicationId = null;


	private GDataAPIUtil() {
	}


	static final Logger logger = Logger.getLogger(GDataAPIUtil.class.getName());


	/**
	 * @author shin1ogawa
	 */
	@SuppressWarnings("serial")
	public static class GDataAPIException extends RuntimeException {

		GDataAPIException(Exception e) {
			super(e);
		}
	}

	/**
	 * @author shin1ogawa
	 */
	@SuppressWarnings("serial")
	public static class GDataAPITimeoutException extends RuntimeException {

		GDataAPITimeoutException(URL url, long start, int retryCount) {
			super(String.format("gave up the retring: retry=%d, spent=%d, url=%s", retryCount,
					(System.currentTimeMillis() - start), url.toString()));
		}
	}


	/**
	 * @return application name for initialization of {@link GoogleService}
	 */
	public static String getApplicationName() {
		if (StringUtils.isNotEmpty(applicationId)) {
			return applicationId;
		}
		if (StringUtils.equalsIgnoreCase(System
			.getProperty("com.google.appengine.runtime.environment"), "Production")) {
			applicationId = ApiProxy.getCurrentEnvironment().getAppId();
		} else {
			applicationId = GDataAPIUtil.class.getName();
		}
		return applicationId;
	}

	/**
	 * execute {@link GDataRequest#execute()} with retrying.
	 * @param service
	 * @param url
	 * @return StringBuilder
	 * @throws GDataAPIException
	 */
	public static InputStream getXmlWithRetry(GoogleService service, URL url)
			throws GDataAPIException {
		try {
			return _getXmlWithRetry(service, url);
		} catch (ServiceException e) {
			throw new GDataAPIException(e);
		}
	}

	/**
	 * execute {@link GoogleService#getFeed(URL, Class)} with retrying.
	 * @param <T> {@link IFeed}
	 * @param service
	 * @param url
	 * @param feedClass
	 * @return T
	 * @throws GDataAPIException
	 * @see GoogleService#getFeed(URL, Class)
	 */
	public static <T extends IFeed>T getFeedsWithRetry(GoogleService service, URL url,
			Class<T> feedClass) throws GDataAPIException {
		try {
			return _getFeedsWithRetry(service, url, feedClass);
		} catch (ServiceException e) {
			throw new GDataAPIException(e);
		}
	}

	/**
	 * execute {@link GoogleService#getEntry(URL, Class)} with retrying.
	 * @param <T> {@link IEntry}
	 * @param service
	 * @param url
	 * @param entryClass
	 * @return T
	 * @throws GDataAPIException
	 * @see GoogleService#getEntry(URL, Class)
	 */
	public static <T extends IEntry>T getEntryWithRetry(GoogleService service, URL url,
			Class<T> entryClass) throws GDataAPIException {
		try {
			return _getEntryWithRetry(service, url, entryClass);
		} catch (ServiceException e) {
			throw new GDataAPIException(e);
		}
	}

	static InputStream _getXmlWithRetry(GoogleService service, URL url) throws ServiceException {
		service.setConnectTimeout(TIMEOUT_MILLIS);
		service.setReadTimeout(TIMEOUT_MILLIS);
		service.useSsl();
		long start = System.currentTimeMillis();
		if (logger.isLoggable(LOG_LEVEL)) {
			logger.log(LOG_LEVEL, "start:" + url);
		}
		int retryCount = 0;
		do {
			try {
				GDataRequest request = service.createFeedRequest(url);
				request.execute();
				InputStream inputStream = request.getParseSource().getInputStream();
				if (logger.isLoggable(LOG_LEVEL)) {
					logger.log(LOG_LEVEL, "end:" + url);
				}
				return inputStream;
			} catch (IOException ex) {
				retryCount++;
				if (logger.isLoggable(LOG_LEVEL)) {
					logger.log(LOG_LEVEL, "timeout: failureCount=" + retryCount + ", url=" + url);
				}
			}
		} while (System.currentTimeMillis() - start < MAX_RETRY_MILLIS
				&& retryCount <= MAX_RETRY_COUNT);
		throw new GDataAPITimeoutException(url, start, retryCount);
	}

	static <T extends IFeed>T _getFeedsWithRetry(GoogleService service, URL url, Class<T> feedClass)
			throws ServiceException {
		service.setConnectTimeout(TIMEOUT_MILLIS);
		service.setReadTimeout(TIMEOUT_MILLIS);
		service.useSsl();
		long start = System.currentTimeMillis();
		if (logger.isLoggable(LOG_LEVEL)) {
			logger.log(LOG_LEVEL, "start:" + url);
		}
		int retryCount = 0;
		do {
			try {
				T feed = service.getFeed(url, feedClass);
				if (logger.isLoggable(LOG_LEVEL)) {
					logger.log(LOG_LEVEL, "end:" + url);
				}
				return feed;
			} catch (RuntimeException ex) {
				if (ex.getCause() instanceof IOException == true
						&& StringUtils.contains(ex.getCause().getMessage(), "Timeout")) {
					retryCount++;
					if (logger.isLoggable(LOG_LEVEL)) {
						logger.log(LOG_LEVEL, "timeout: failureCount=" + retryCount + ", url="
								+ url);
					}
				} else {
					throw ex;
				}
			} catch (IOException ex) {
				retryCount++;
				if (logger.isLoggable(LOG_LEVEL)) {
					logger.log(LOG_LEVEL, "timeout: failureCount=" + retryCount + ", url=" + url);
				}
			}
		} while (System.currentTimeMillis() - start < MAX_RETRY_MILLIS
				&& retryCount <= MAX_RETRY_COUNT);
		throw new GDataAPITimeoutException(url, start, retryCount);
	}

	static <T extends IEntry>T _getEntryWithRetry(GoogleService service, URL url,
			Class<T> entryClass) throws ServiceException {
		service.setConnectTimeout(TIMEOUT_MILLIS);
		service.setReadTimeout(TIMEOUT_MILLIS);
		service.useSsl();
		long start = System.currentTimeMillis();
		if (logger.isLoggable(LOG_LEVEL)) {
			logger.log(LOG_LEVEL, "start:" + url);
		}
		int retryCount = 0;
		do {
			try {
				T entry = service.getEntry(url, entryClass);
				if (logger.isLoggable(LOG_LEVEL)) {
					logger.log(LOG_LEVEL, "end:" + url);
				}
				return entry;
			} catch (RuntimeException ex) {
				if (ex.getCause() instanceof IOException == true
						&& StringUtils.contains(ex.getCause().getMessage(), "Timeout")) {
					retryCount++;
					if (logger.isLoggable(LOG_LEVEL)) {
						logger.log(LOG_LEVEL, "timeout: failureCount=" + retryCount + ", url="
								+ url);
					}
				} else {
					throw ex;
				}
			} catch (IOException ex) {
				retryCount++;
				if (logger.isLoggable(LOG_LEVEL)) {
					logger.log(LOG_LEVEL, "timeout: failureCount=" + retryCount + ", url=" + url);
				}
			}
		} while (System.currentTimeMillis() - start < MAX_RETRY_MILLIS
				&& retryCount <= MAX_RETRY_COUNT);
		throw new GDataAPITimeoutException(url, start, retryCount);
	}
}
