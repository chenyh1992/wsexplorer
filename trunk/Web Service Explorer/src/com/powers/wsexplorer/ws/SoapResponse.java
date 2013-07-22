package com.powers.wsexplorer.ws;

import org.apache.commons.lang.StringUtils;

public class SoapResponse {

	public static SoapResponse EMPTY_SOAP_RESPONSE = new SoapResponse(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, false);
	public String response;
	public String jSessionId;
	public String cookie;
	public boolean maintainSessionFromPreviousCall;
	
	public SoapResponse(String response, String jSessionId, String cookie, boolean maintainSessionFromPreviousCall){
		this.response = response;
		this.jSessionId = jSessionId;
		this.cookie = cookie;
		this.maintainSessionFromPreviousCall = maintainSessionFromPreviousCall;
	}
}
