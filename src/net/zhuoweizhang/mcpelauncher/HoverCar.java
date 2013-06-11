package net.zhuoweizhang.mcpelauncher;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

public class HoverCar extends PopupWindow {

	public ImageButton mainButton;

	public View contentView;

	private Drawable buttonBackground;

	public HoverCar(Activity activity) {
		super(activity);
		contentView = activity.getLayoutInflater().inflate(R.layout.hovercar, null);
		setContentView(contentView);
		mainButton = (ImageButton) getContentView().findViewById(R.id.hovercar_main_button);
		buttonBackground = mainButton.getBackground();
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

	public void setVisible(boolean visible) {
		View popupRoot = (contentView.getParent() != null && contentView.getParent() instanceof View? (View) contentView.getParent() : contentView);
		popupRoot.setBackgroundColor(visible? 0x77ffffff: Color.TRANSPARENT);
		mainButton.setBackgroundDrawable(visible? buttonBackground: new ColorDrawable(Color.TRANSPARENT));
		mainButton.setAlpha(visible? 0xff: 0);
		this.update();
	}

}
