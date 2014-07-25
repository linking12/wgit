package com.pajk.wgit.core;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;

/**
 * httpclient 封装 提交http get/post请求
 * 
 * @author shiming.liu
 * 
 */
public class HttpInvoker {
	private static Log log = LogFactory.getLog(HttpInvoker.class);

	private final HttpClient httpclient;
	private final RequestConfig requestConfig;

	private final String charset = "UTF-8";

	private static final int MAX_CONNECTION_SIZE = 200;
	private static final int SOCKET_TIMEOUT = 30000;
	private static final int CONNECTION_TIMEOUT = 3000;
	private static final int CONNECTION_REQUEST_TIMEOUT = 30000;

	private static class Holder {
		private static HttpInvoker instance = new HttpInvoker();
	}

	private HttpInvoker() {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(MAX_CONNECTION_SIZE);
		cm.setDefaultMaxPerRoute(20);
		cm.setDefaultConnectionConfig(ConnectionConfig.custom()
				.setCharset(Consts.UTF_8).build());
		httpclient = HttpClients.custom().setConnectionManager(cm)
				.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
					public long getKeepAliveDuration(HttpResponse arg0,
							org.apache.http.protocol.HttpContext arg1) {
						return 30000;
					}
				}).build();
		requestConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES)
				.setSocketTimeout(SOCKET_TIMEOUT)
				.setConnectTimeout(CONNECTION_TIMEOUT)
				.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
				.setExpectContinueEnabled(false).setRedirectsEnabled(false)
				.build();
		// 代理dns设置
		// HttpHost proxy = new HttpHost("180.76.3.151", 80, "http");
		// 用户认证机制
		// CredentialsProvider credsProvider = new BasicCredentialsProvider();
		// credsProvider.setCredentials(new AuthScope("localhost", 8080),
		// new UsernamePasswordCredentials("username", "password"));
		// httpclient = HttpClients.custom().setProxy(proxy)
		// .setDefaultCredentialsProvider(credsProvider)
		// .setConnectionManager(connManager).build();
	}

	public static synchronized HttpInvoker getInstance() {
		return Holder.instance;
	}

	public String sendGetRequest(String url, Object requestObj) {
		HttpGet httpGet = new HttpGet(url);
		boolean isSuccess = parseRequestParamter(httpGet, requestObj);
		String response = null;
		if (isSuccess) {
			try {
				response = httpclient.execute(httpGet,
						new ResponseHandler<String>() {
							public String handleResponse(HttpResponse response)
									throws ClientProtocolException, IOException {
								int status = response.getStatusLine()
										.getStatusCode();
								if (status >= 200 && status < 300) {
									HttpEntity entity = response.getEntity();
									return entity != null ? EntityUtils
											.toString(entity,
													Charset.forName(charset))
											: null;
								} else {
									throw new ClientProtocolException(
											"Unexpected response status: "
													+ status);
								}
							}
						});
			} catch (ClientProtocolException e) {
				log.error(e.getMessage(), e);
				response = null;
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				response = null;
			}
		}
		return response;
	}

	public String sendPostRequest(String url, Object requestObj) {
		HttpPost httpPost = new HttpPost(url);
		boolean isSuccess = parseRequestParamter(httpPost, requestObj);
		String response = null;
		if (isSuccess) {
			try {
				response = httpclient.execute(httpPost,
						new ResponseHandler<String>() {
							public String handleResponse(HttpResponse response)
									throws ClientProtocolException, IOException {
								int status = response.getStatusLine()
										.getStatusCode();
								if (status >= 200 && status < 300) {
									HttpEntity entity = response.getEntity();
									return entity != null ? EntityUtils
											.toString(entity,
													Charset.forName(charset))
											: null;
								} else {
									throw new ClientProtocolException(
											"Unexpected response status: "
													+ status);
								}
							}
						});
			} catch (ClientProtocolException e) {
				log.error(e.getMessage(), e);
				response = null;
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				response = null;
			}
		}
		return response;
	}

	private boolean parseRequestParamter(HttpRequestBase httpRequest,
			Object requestObj) {
		List<NameValuePair> nvpsStr = new ArrayList<NameValuePair>();
		List<BasicFileValuePair> nvpsFile = new ArrayList<BasicFileValuePair>();
		if (requestObj == null)
			return true;
		Class<?> clazz = requestObj.getClass();
		try {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				String name = field.getName();
				PropertyDescriptor pd = new PropertyDescriptor(name, clazz);
				Method getMethod = pd.getReadMethod();// 获得get方法
				Object o = getMethod.invoke(requestObj);// 执行get方法返回一个Object
				if (o instanceof String) {
					nvpsStr.add(new BasicNameValuePair(name, String.valueOf(o)));
				} else if (o instanceof File) {
					nvpsFile.add(new BasicFileValuePair(name, (File) o));
				} else if (o instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, String> map = (Map<String, String>) o;
					for (Iterator<String> it = map.keySet().iterator(); it
							.hasNext();) {
						String key = it.next();
						String value = map.get(key);
						nvpsStr.add(new BasicNameValuePair(key, value));
					}

				}

			}
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
			return false;
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage(), e);
			return false;
		} catch (IntrospectionException e) {
			log.error(e.getMessage(), e);
			return false;
		} catch (IllegalAccessException e) {
			log.error(e.getMessage(), e);
			return false;
		} catch (InvocationTargetException e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			putValueIntoRequest(httpRequest, nvpsStr, nvpsFile);
		}
		return true;
	}

	private void putValueIntoRequest(HttpRequestBase httpRequest,
			List<NameValuePair> nvpsStr, List<BasicFileValuePair> nvpsFile) {
		httpRequest.setConfig(requestConfig);
		if (httpRequest instanceof HttpPost) {
			HttpPost httpPost = (HttpPost) httpRequest;
			if (nvpsFile.isEmpty()) {
				httpPost.setEntity(new UrlEncodedFormEntity(nvpsStr, Charset
						.forName((charset))));
			} else {
				MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder
						.create();
				multipartEntityBuilder
						.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				multipartEntityBuilder.setCharset(Charset.forName(charset));
				for (NameValuePair nameValue : nvpsStr) {
					multipartEntityBuilder.addTextBody(
							nameValue.getName(),
							nameValue.getValue(),
							ContentType.create("text/plain",
									Charset.forName(this.charset)));
				}
				for (BasicFileValuePair fileValue : nvpsFile) {
					multipartEntityBuilder.addBinaryBody(fileValue.getName(),
							fileValue.getValue());
				}
				HttpEntity httpEntity = multipartEntityBuilder.build();
				httpPost.setEntity(httpEntity);
			}
		} else {
			HttpGet httpGet = (HttpGet) httpRequest;
			String queryOld = httpGet.getURI().getQuery() != null ? httpGet
					.getURI().getQuery() : "";
			String queryNew;
			if (queryOld != null)
				queryNew = queryOld
						+ URLEncodedUtils.format(nvpsStr,
								Charset.forName((charset)));
			else
				queryNew = URLEncodedUtils.format(nvpsStr,
						Charset.forName((charset)));
			httpGet.setURI(URI.create(httpGet.getURI().toString() + "?"
					+ queryNew));
		}

	}

	public void close() {
		if (httpclient instanceof CloseableHttpClient)
			try {
				((CloseableHttpClient) httpclient).close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
	}

	private class BasicFileValuePair implements Cloneable, Serializable {

		private static final long serialVersionUID = -6437800749411518984L;

		private final String name;
		private final File value;

		public BasicFileValuePair(final String name, final File value) {
			super();
			this.name = Args.notNull(name, "Name");
			this.value = value;
		}

		public String getName() {
			return this.name;
		}

		public File getValue() {
			return this.value;
		}

	}
}
