package com.powers.wsexplorer.ws;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;

public class SoapSender {

	private static final String ANYACTION = "anyaction";
	private static final String SOAP_ACTION = "SOAPAction";
	public static final String ERROR_PREFIX = "Expletive! I Caught an exception: ";
	public static Exception CURRENT_EXCEPTION = null;
	private static final String SSL = "SSL";
	
	public static final String SET_COOKIE = "set-cookie";
	public static final String JSESSIONID = "JSESSIONID";
	public static final String COOKIE = "Cookie";
	public static final Map<String,String> EMPTY_HEADERS = Collections.emptyMap();
	
	public String jsessionId = StringUtils.EMPTY;
	public String cookie = StringUtils.EMPTY;
	public boolean maintainedSessionFromPreviousCall = false;
	
	public SoapSender(){}
	
	/**
	 * Convenience method to get a connection to send a SOAP message.
	 * @return a SOAPConnection or null if an exception was thrown.
	 */
	public SOAPConnection getConnection(){
		SOAPConnectionFactory soapConnFactory;
		SOAPConnection connection = null;
		
		try {
			soapConnFactory = SOAPConnectionFactory.newInstance();
			connection = soapConnFactory.createConnection();
		} catch (UnsupportedOperationException e) {
			return null;
		} catch (SOAPException e) {
			return null;
		}
		
		return connection;
	}
	/**
	 * Convenience method to send and receive a SOAP message.
	 * 
	 * @param endpoint
	 * @param soapMessage
	 * @param timeout
	 * @return
	 */
	public String sendAndReceiveSOAPMessage(String endpoint, String soapMessage){
		return sendAndReceiveSOAPMessage(endpoint, soapMessage, null, EMPTY_HEADERS, false);
	}
	
	public String sendAndReceiveSOAPMessage(String endpoint, String soapMessage, SOAPConnection connection, Map<String,String> requestHeaders, boolean maintainSessionFromPreviousCall){
		String response = null;
		
		try {
			
			if(connection == null){
				SOAPConnectionFactory soapConnFactory = SOAPConnectionFactory.newInstance();
				connection =  soapConnFactory.createConnection();
			}

			MessageFactory messageFactory = MessageFactory.newInstance();
	        SOAPMessage message = messageFactory.createMessage();
	        SOAPPart soapPart = message.getSOAPPart();
	        MimeHeaders mimeHeaders = message.getMimeHeaders();
	        
	        // for Axis web services that require a header
	        mimeHeaders.addHeader(SOAP_ACTION, ANYACTION);
	        
	        // add any other arbitrary headers
	 		if(!requestHeaders.isEmpty()){
	 			Set<String> keys = requestHeaders.keySet();
	 			for(String key : keys){
	 				mimeHeaders.addHeader(key, requestHeaders.get(key));
	 			}
	 		}
	 		
	 		this.maintainedSessionFromPreviousCall = maintainSessionFromPreviousCall;
	 		// will keep the session in the web server
	 		if(maintainSessionFromPreviousCall){
	 			mimeHeaders.addHeader(COOKIE, this.cookie);
	 		}
	        
	        StreamSource ss = new StreamSource(new StringReader(soapMessage));
	        soapPart.setContent(ss);
	        
	        // format to a URL
	        URL url = null;
			try {
				url = new URL(endpoint);
				
			} catch (MalformedURLException e1) {
				CURRENT_EXCEPTION = e1;
				e1.printStackTrace();
				return ERROR_PREFIX + "Endpoint given is not a URL";
			}
			
			SOAPMessage reply = connection.call(message, url);
	        
	        //Create the transformer
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	
	        Transformer transformer = null;
			try {
				transformer = transformerFactory.newTransformer();
			} catch (TransformerConfigurationException e) {
				e.printStackTrace();
				CURRENT_EXCEPTION = e;
				return ERROR_PREFIX + "Unable to create a Transformer";
			}
			
	        //Extract the content of the reply
	        Source sourceContent = reply.getSOAPPart().getContent();
	        MimeHeaders responseHeaders = reply.getMimeHeaders();
	        
	        StringWriter sw = new StringWriter();
	        //Set the output for the transformation
	        StreamResult result = new StreamResult(sw);
	        try {
				transformer.transform(sourceContent, result);
			} catch (TransformerException e) {
				e.printStackTrace();
				CURRENT_EXCEPTION = e;
				return ERROR_PREFIX + "Unable to transform the SOAP message into a textual response";
			}
        
			// change to a String to return
			response = sw.toString();
			
			// only get the cookie once per session after the first call
			if(StringUtils.isEmpty(cookie)){
				this.jsessionId = getJSessionId(responseHeaders);
				this.cookie = getCookie(responseHeaders);
			}
			
			
		} catch(SOAPException e){
			e.printStackTrace();
			CURRENT_EXCEPTION = e;
			response = ERROR_PREFIX + e.getMessage();
		}
		
		return response;
	}
	
	
	
	private String getJSessionId(MimeHeaders headers){
		String jSessionId = StringUtils.EMPTY;
		
		String cookieStr = getCookie(headers);
		
		if(StringUtils.isNotEmpty(cookieStr)){
			String[] cookieSplit = cookieStr.split(";");
			for(int i=0; i<cookieSplit.length; i++){
				if(cookieSplit[i].startsWith(JSESSIONID)){
					jSessionId = cookieSplit[i].replace(JSESSIONID+"=", StringUtils.EMPTY);
				}
			}
		} else {
			String[] jArray = headers.getHeader(JSESSIONID);
			if(jArray != null && jArray.length > 0){
				jSessionId = jArray[0];
			}
		}

		return jSessionId;
	}
	
	private String getCookie(MimeHeaders headers){
		String cookieStr = StringUtils.EMPTY;
		if(headers != null){
			String[] cookies = headers.getHeader(SET_COOKIE);

			if(cookies != null){
				cookieStr = cookies[0]; // get the value of the cookie
				// cookiesStr = "dcwsssn=3db50e6c7fa0762a76925401884a2442;Path=/;Version=1;Max-Age=20";
			}
		}
		return cookieStr;
	}
	
	public void clearSessionData(){
		this.jsessionId = StringUtils.EMPTY;
		this.cookie = StringUtils.EMPTY;
	}
	
	/**
	 * A 'hacky' way of attempting to ignore a SSL certificate.
	 * @throws Exception
	 */
	public void ignoreCertificates() throws Exception {
		TrustManager tm = new TrustManager();
		TrustManager[] trustAllCerts = {tm};
		
		// create an all trusting HostnameVerifier
		HostnameVerifier AllowAllHostnameVerifier = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				return true;
			}
		};
		
		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance(SSL);
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(AllowAllHostnameVerifier);
	}
	
	/**
	 * Used to attempt to ignore SSL certificates.
	 */
	static class TrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1)throws CertificateException {}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1)throws CertificateException {}

		@Override
		public X509Certificate[] getAcceptedIssuers() {return null;}
	}
}
