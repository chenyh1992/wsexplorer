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

public class WSUtil {

	private static final String ERROR_PREFIX = "Caught an exception: ";
	
	public static String sendAndReceiveSOAPMessage(String endpoint, String soapMessage){
		String response = null;
		
		try {
			SOAPConnectionFactory soapConnFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection connection =  soapConnFactory.createConnection();
			MessageFactory messageFactory = MessageFactory.newInstance();
	        SOAPMessage message = messageFactory.createMessage();
	        SOAPPart soapPart = message.getSOAPPart();
	        
	        // for Axis web services that require a header
	        message.getMimeHeaders().addHeader("SOAPAction", "anyaction");
	        
	        StreamSource ss = new StreamSource(new StringReader(soapMessage));
	        soapPart.setContent(ss);
	        
	        // format to a URL
	        URL url = null;
			try {
				url = new URL(endpoint);
			} catch (MalformedURLException e1) {
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
				return ERROR_PREFIX + "Unable to transform the SOAP message into a textual response";
			}
        
			// change to a String to return
			response = sw.toString();
			
		} catch(SOAPException e){
			e.printStackTrace();
			response = ERROR_PREFIX + e.getMessage();
		}
		
		return response;
	}
}
