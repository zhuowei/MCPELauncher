package com.microsoft.onlineid.interop;

import java.io.*;
import java.nio.charset.Charset;

import android.app.Activity;
import android.content.Context;

public class Interop {
	public static native void ticket_callback(String a, int b, int c, String d);
	public static native void auth_flow_callback(long a, int b);
	public static native void invoke_xb_login(long a, String b, XBLoginCallback callback);
	public static native void invoke_xb_logout(long a, XBLogoutCallback callback);
	public static native void invoke_x_token_acquisition(long a, Callback callback);
	public static native String get_live_x_token_callback(boolean what);
	public static native String get_x_token_callback(String what);
	public static native void sign_out_callback();
	public static void InvokeAuthFlow(long callback, Activity activity, Context context) {
		System.out.println("Invoking auth flow: " + Long.toString(callback, 16) + ":" + activity + ":" + context);
		auth_flow_callback(callback, 1);
	}
	public static void InvokeMSA(Activity activity, Context context, int theCode) {
		System.out.println("Invoking MSA: " + activity + ":" + context + ":" + theCode);
	}
	public static String ReadConfigFile(Context context) {
		try {
			InputStream is = context.getResources().openRawResource(net.zhuoweizhang.mcpelauncher.R.raw.xboxservices);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (true) {
				int len = is.read(buffer);
				if (len < 0) {
					break;
				}
				bout.write(buffer, 0, len);
			}
			byte[] retval = bout.toByteArray();

			return new String(retval, Charset.forName("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public static class Callback {
	}
	public static class XBLoginCallback extends Callback {
	}
	public static class XBLogoutCallback extends Callback {
	}
}
