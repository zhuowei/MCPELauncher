package com.mojang.android.net;

public class HTTPRequest {

	public String url, requestBody, cookieData, contentType;

	public void setURL(String url) {
		System.out.println("URL: " + url);
	}
	public void setRequestBody(String body) {
		System.out.println("Body: " + body);
	}
	public void setCookieData(String cookie) {
		System.out.println("Cookie: " + cookie);
	}

	public void setContentType(String contentType) {
		System.out.println("Content type: " + contentType);
	}

	public HTTPResponse send(String mode) {
		System.out.println("Send: " + mode);
		return new HTTPResponse();
	}

	public void abort() {
		System.out.println("Abort");
	}
}
