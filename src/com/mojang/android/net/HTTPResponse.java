package com.mojang.android.net;

import static com.mojang.android.net.HTTPRequest.debugNet;

public class HTTPResponse {
	public static final int STATUS_FAIL = 0;
	public static final int STATUS_SUCCESS = 1;

	private int status, responseCode;
	private String body;

	public HTTPResponse(int status, int responseCode, String body) {
		this.status = status;
		this.responseCode = responseCode;
		this.body = body;
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
}
