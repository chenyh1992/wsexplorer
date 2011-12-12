/*
 *   Copyright 2008 Nick Powers.
 *   This file is part of WSExplorer.
 *
 *   WSExplorer is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   WSExplorer is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with WSExplorer.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.powers.wsexplorer.ws;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.MessageFactory;
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

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class WSUtil {

	public static final String ERROR_PREFIX = "Expletive! I Caught an exception: ";
	public static Exception CURRENT_EXCEPTION = null;
	private static final String SSL = "SSL";
	
	/**
	 * Convenience method to get a connection to send a SOAP message.
	 * @return a SOAPConnection or null if an exception was thrown.
	 */
	public static SOAPConnection getConnection(){
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
	public static String sendAndReceiveSOAPMessage(String endpoint, String soapMessage){
		return sendAndReceiveSOAPMessage(endpoint, soapMessage, null);
	}
	
	public static String sendAndReceiveSOAPMessage(String endpoint, String soapMessage, SOAPConnection connection){
		String response = null;
		
		try {
			
			if(connection == null){
				SOAPConnectionFactory soapConnFactory = SOAPConnectionFactory.newInstance();
				connection =  soapConnFactory.createConnection();
			}

			MessageFactory messageFactory = MessageFactory.newInstance();
	        SOAPMessage message = messageFactory.createMessage();
	        SOAPPart soapPart = message.getSOAPPart();
	        // for Axis web services that require a header
	        message.getMimeHeaders().addHeader("SOAPAction", "anyaction");
	        
	        StreamSource ss = new StreamSource(new StringReader(soapMessage));
	        soapPart.setContent(ss);
	        
//	        java.net.URLStreamHandler handler = new URLStreamHandler() {
//				
//				@Override
//				protected URLConnection openConnection(URL url) throws IOException {
//					HttpURLConnection conn = new HttpURLConnection(url, null);
//					conn.setConnectTimeout(2*1000); // 2 seconds
//					conn.setReadTimeout(3*1000); // 3 seconds
//					return conn;
//				}
//			};
			
	        
	        // format to a URL
	        URL url = null;
			try {
				url = new URL(endpoint);
				
				//URL protocol = new URL("http://");
				//url = new URL(protocol,endpoint, handler);
			} catch (MalformedURLException e1) {
				CURRENT_EXCEPTION = e1;
				e1.printStackTrace();
				return ERROR_PREFIX + "Endpoint given is not a URL";
			}
			
			SOAPMessage reply = connection.call(message, url);
	        
	        //Create the transformer
	        TransformerFactory transformerFactory = 
	                           TransformerFactory.newInstance();
	
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
			
		} catch(SOAPException e){
			e.printStackTrace();
			CURRENT_EXCEPTION = e;
			response = ERROR_PREFIX + e.getMessage();
		}
		
		return response;
	}
	
	/**
	 * Pretty print the XML string given to this method.
	 * 
	 * @param xml String to be pretty printed
	 * @return null if it's a malformed document, pretty printed XML otherwise
	 */
	public static String prettyPrint(String xml){
		Document document = null;
		SAXBuilder parser = new SAXBuilder();
		StringWriter sw = new StringWriter();
		
		try {
			document = parser.build(new StringReader(xml));
			XMLOutputter out = new XMLOutputter();
			out.setFormat(Format.getPrettyFormat());
			
			out.output(document, sw);
			
		} catch (Exception e) {
			return null;
		}
		
		return (sw == null ? null : sw.toString());
	}
	
	/**
	 * A 'hacky' way of attempting to ignore a SSL certificate.
	 * @throws Exception
	 */
	public static void ignoreCertificates() throws Exception {
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
