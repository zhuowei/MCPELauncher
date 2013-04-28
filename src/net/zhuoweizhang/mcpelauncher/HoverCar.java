package net.zhuoweizhang.mcpelauncher;

import android.app.*;
import android.content.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

public class HoverCar extends PopupWindow {

	public ImageButton mainButton;

	public HoverCar(Activity activity) {
		super(activity);
		setContentView(activity.getLayoutInflater().inflate(R.layout.hovercar, null));
		mainButton = (ImageButton) getContentView().findViewById(R.id.hovercar_main_button);
		setBackgroundDrawable(new ColorDrawable(0x77ffffff));
		float myWidth = activity.getResources().getDimension(R.dimen.hovercar_width);
		float myHeight = activity.getResources().getDimension(R.dimen.hovercar_height);
		setWidth((int) myWidth);
		setHeight((int) myHeight);
		mainButton.setMaxWidth((int) myWidth);
		mainButton.setMaxHeight((int) myHeight);
		mainButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
	}

	public void show(View parentView) {
		showAtLocation(parentView, Gravity.CENTER | Gravity.TOP, 0, 0);
	}

}
