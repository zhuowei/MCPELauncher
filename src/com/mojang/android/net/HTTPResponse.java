package com.mojang.android.net;

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
		System.out.println("get status");
		return status;
	}

	public String getBody() {
		System.out.println("get response " + body);
		return body;
	}

	public int getResponseCode() {
		System.out.println("get response code");
		return responseCode;
	}
}
