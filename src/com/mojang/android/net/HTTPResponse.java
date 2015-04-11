package com.mojang.android.net;

public class HTTPResponse {

	public int getStatus() {
		System.out.println("get status");
		return 0;
	}

	public String getBody() {
		System.out.println("get response");
		return "Swag overload";
	}

	public int getResponseCode() {
		System.out.println("get response code");
		return 404;
	}
}
