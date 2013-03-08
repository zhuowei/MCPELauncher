package net.zhuoweizhang.mcpelauncher;

import android.app.*;
import android.content.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

public class HoverCar extends PopupWindow {

	public Button mainButton;

	public HoverCar(Activity activity) {
		super(activity);
		setContentView(activity.getLayoutInflater().inflate(R.layout.hovercar, null));
		mainButton = (Button) getContentView().findViewById(R.id.hovercar_main_button);
		setBackgroundDrawable(new ColorDrawable(0x77ffffff));
		setWidth(100);
		setHeight(50);
		mainButton.setWidth(100);
		mainButton.setHeight(50);
	}

	public void show(View parentView) {
		showAtLocation(parentView, Gravity.RIGHT | Gravity.TOP, 0, 0);
	}

}
