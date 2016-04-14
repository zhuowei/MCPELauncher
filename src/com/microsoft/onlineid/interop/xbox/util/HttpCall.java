package com.microsoft.onlineid.interop.xbox.util;

public class HttpCall {
	public native void setRequestBody(String s);
	public native void setCustomHeader(String name, String value);
	public native void setRetryAllowed(boolean allowed);
	public native void setContentTypeHeaderValue(String value);
	public native void setXboxContractVersionHeaderValue(String value);
	public native void getResponseAsync(Callback theCallback);
	public native long create(String a, String b, String c);
	public native void delete(long ptr);
	public static class Callback {
	}
}
