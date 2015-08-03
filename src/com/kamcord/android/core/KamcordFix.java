package com.kamcord.android.core;

import java.lang.reflect.*;

import com.kamcord.android.server.model.sdk.DimensionsModel;
import com.kamcord.android.core.KC_g;
import com.kamcord.android.core.KC_j;
import com.kamcord.android.core.KC_k;

public class KamcordFix implements KC_g {
	public KC_g wrap;
	public boolean hasRun = false;
	public KamcordFix(KC_g wrap) {
		System.out.println("KamcordFix: Wrapping " + wrap);
		this.wrap = wrap;
	}

	public DimensionsModel lastModel;
	public DimensionsModel a() {
		if (!hasRun) {
			nativeAttachDestructor();
			hasRun = true;
		}
		DimensionsModel model = wrap.a();
		lastModel = model;
		return model;
	}

	public KC_j b() {
		return wrap.b();
	}

	public int c() {
		return wrap.c();
	}

	public static native void nativeAttachDestructor();

	public static void install() throws Exception {
		Class<KC_k> clazz = KC_k.class;
		Field f = clazz.getDeclaredField("a");
		f.setAccessible(true);
		f.set(null, new KamcordFix((KC_g) f.get(null)));
	}
}
