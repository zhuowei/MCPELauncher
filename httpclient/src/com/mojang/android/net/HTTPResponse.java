package com.mojang.android.net;

import org.apache.http.Header;

import static com.mojang.android.net.HTTPRequest.debugNet;

public class HTTPResponse {
	public static final int STATUS_FAIL = 0;
	public static final int STATUS_SUCCESS = 1;

	private int status, responseCode;
	private String body;
	private Header[] headers;

	public HTTPResponse(int status, int responseCode, String body, Header[] headers) {
		this.status = status;
		this.responseCode = responseCode;
		this.body = body;
		this.headers = headers;
	}

	public int getStatus() {
		if (debugNet) System.out.println("get status");
		return status;
	}

	public String getBody() {
		if (debugNet) System.out.println("get response " + body);
		return body;
	}

	public int getResponseCode() {
		if (debugNet) System.out.println("get response code");
		return responseCode;
	}

	public Header[] getHeaders() {
		if (debugNet) System.out.println("get headers");
		return headers;
	}
}
