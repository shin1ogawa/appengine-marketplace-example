package com.shin1ogawa.appengine.marketplace.gdata;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.google.appengine.api.urlfetch.URLFetchServicePb.URLFetchRequest;
import com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

/**
 * the {@link ApiProxy.Delegate} to set 10 seconds as deadline to URLFetch service
 * @author shin1ogawa
 */
public class IncreaseURLFetchDeadlineDelegate implements ApiProxy.Delegate<Environment> {

	static final Logger logger = Logger.getLogger(IncreaseURLFetchDeadlineDelegate.class.getName());

	final ApiProxy.Delegate<Environment> delegate;

	static Level logLevel = Level.FINEST;


	/**
	 * install new {@link IncreaseURLFetchDeadlineDelegate} to {@link ApiProxy}.
	 * @return previous {@link ApiProxy.Delegate}
	 */
	public static ApiProxy.Delegate<Environment> install() {
		@SuppressWarnings("unchecked")
		Delegate<Environment> originalDelegate = ApiProxy.getDelegate();
		if (originalDelegate instanceof IncreaseURLFetchDeadlineDelegate == false) {
			ApiProxy.setDelegate(new IncreaseURLFetchDeadlineDelegate(originalDelegate));
		}
		return originalDelegate;
	}

	/**
	 * uninstall present {@link IncreaseURLFetchDeadlineDelegate} from {@link ApiProxy}.
	 * @param originalDelegate previous {@link ApiProxy.Delegate}
	 */
	public static void uninstall(Delegate<Environment> originalDelegate) {
		ApiProxy.setDelegate(originalDelegate);
	}

	/**
	 * the constructor.
	 * @param delegate
	 * @category constructor
	 */
	public IncreaseURLFetchDeadlineDelegate(Delegate<Environment> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void log(Environment env, LogRecord logRecord) {
		delegate.log(env, logRecord);
	}

	@Override
	public Future<byte[]> makeAsyncCall(Environment env, String service, String method,
			byte[] requestBytes, ApiConfig config) {
		if (StringUtils.equalsIgnoreCase("urlfetch", service) == false) {
			return delegate.makeAsyncCall(env, service, method, requestBytes, config);
		}
		if (StringUtils.equalsIgnoreCase("fetch", method) == false) {
			return delegate.makeAsyncCall(env, service, method, requestBytes, config);
		}
		try {
			config.setDeadlineInSeconds(10000.0);
			byte[] newRequestBytes = increaseDeadline(requestBytes);
			return delegate.makeAsyncCall(env, service, method, newRequestBytes, config);
		} catch (InvalidProtocolBufferException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] makeSyncCall(Environment env, String service, String method, byte[] requestBytes)
			throws ApiProxyException {
		if (StringUtils.equalsIgnoreCase("urlfetch", service) == false) {
			return delegate.makeSyncCall(env, service, method, requestBytes);
		}
		if (StringUtils.equalsIgnoreCase("fetch", method) == false) {
			return delegate.makeSyncCall(env, service, method, requestBytes);
		}
		try {
			byte[] newRequestBytes = increaseDeadline(requestBytes);
			return delegate.makeSyncCall(env, service, method, newRequestBytes);
		} catch (InvalidProtocolBufferException e) {
			throw new RuntimeException(e);
		}
	}

	static byte[] increaseDeadline(byte[] orignialRequestBytes)
			throws InvalidProtocolBufferException {
		URLFetchRequest requestPB = URLFetchRequest.parseFrom(orignialRequestBytes);
		URLFetchRequest newRequestPB = requestPB.toBuilder().setDeadline(10000.0).build();
		byte[] newRequestBytes = newRequestPB.toByteArray();
		if (logger.isLoggable(logLevel)) {
			logger.log(logLevel, "newRequest=" + newRequestPB);
		}
		return newRequestBytes;
	}
}
