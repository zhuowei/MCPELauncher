package com.mojang.android.net;

import java.io.*;
import java.net.*;
import java.nio.charset.*;

public class HTTPRequest {

	public String url, requestBody, cookieData, contentType;

	public void setURL(String url) {
		System.out.println("URL: " + url);
		this.url = url;
	}
	public void setRequestBody(String body) {
		System.out.println("Body: " + body);
		this.requestBody = body;
	}
	public void setCookieData(String cookie) {
		System.out.println("Cookie: " + cookie);
		this.cookieData = cookie;
	}

	public void setContentType(String contentType) {
		System.out.println("Content type: " + contentType);
		this.contentType = contentType;
	}

	public HTTPResponse send(String mode) {
		System.out.println("Send: " + mode);
		InputStream is = null;
		try {
			int status = 0;
			URL urlObj = new URL(this.url);

			HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
			conn.setRequestProperty("User-Agent", "BlockLauncher");
			conn.setRequestProperty("Cookie", cookieData);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", contentType);
			conn.setRequestMethod(mode);

			conn.connect();
			OutputStream os = null;
			os = conn.getOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(os);
			writer.write(this.requestBody);
			writer.close();

			try {
				status = conn.getResponseCode();
				is = conn.getInputStream();
			} catch (Exception e) {
				is = conn.getErrorStream();
			}

			if (is == null) throw new Exception("Null input stream");

			ByteArrayOutputStream realos = new ByteArrayOutputStream();

			byte[] buffer = new byte[4096];
			int count;
			while ((count = is.read(buffer)) != -1) {
				realos.write(buffer, 0, count);
			}
			String returnString = new String(realos.toByteArray(), "UTF-8");
			return new HTTPResponse(HTTPResponse.STATUS_SUCCESS, status, returnString);
		} catch (Exception e) {
			e.printStackTrace();
			return new HTTPResponse(HTTPResponse.STATUS_FAIL, 0, null);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public void abort() {
		System.out.println("Abort");
	}
}
