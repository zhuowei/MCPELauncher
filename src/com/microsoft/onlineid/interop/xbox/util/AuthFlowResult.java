package com.microsoft.onlineid.interop.xbox.util;

public class AuthFlowResult {
	public static native String getXToken(long ptr);
	public static native String getRpsTicket(long ptr);
	public static native String getUserId(long ptr);
	public static native String getAgeGroup(long ptr);
	public static native String getPrivileges(long ptr);
	public static native String getGamerTag(long ptr);
	public static native void delete(long ptr);
}
