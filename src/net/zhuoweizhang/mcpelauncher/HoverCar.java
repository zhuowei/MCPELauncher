package net.zhuoweizhang.mcpelauncher;

import android.content.Context;
import android.view.*;
import android.widget.*;

public class HoverCar extends PopupWindow {

	public ImageButton mainButton;

	// public View contentView;

	// private Drawable buttonBackground;

	public HoverCar(Context con) {
		super(con);
		LayoutInflater inf = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setContentView(inf.inflate(R.layout.hovercar, null));
		mainButton = (ImageButton) getContentView().findViewById(R.id.hovercar_main_button);
		float myWidth = con.getResources().getDimension(R.dimen.hovercar_width);
		float myHeight = con.getResources().getDimension(R.dimen.hovercar_height);
		setWidth((int) myWidth);
		setHeight((int) myHeight);
		setBackgroundDrawable(null);
		// buttonBackground = mainButton.getBackground();
		// setBackgroundDrawable(new ColorDrawable(0x77ffffff));
		// mainButton.setMaxWidth((int) myWidth);
		// mainButton.setMaxHeight((int) myHeight);
		// mainButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
	}

	public void show(View parentView) {
		showAtLocation(parentView, Gravity.CENTER | Gravity.TOP, 0, 0);
	}

	/**
	 * Sets visibility of hovercar
	 * 
	 * @param visible
	 *            If true, hovercar will be visible
	 */
	public void setVisible(boolean visible) {
		// View popupRoot = (contentView.getParent() != null &&
		// contentView.getParent() instanceof View? (View)
		// contentView.getParent() : contentView);
		// popupRoot.setBackgroundColor(visible? 0x77ffffff: Color.TRANSPARENT);
		// mainButton.setBackgroundDrawable(visible? buttonBackground: new
		// ColorDrawable(Color.TRANSPARENT));
		mainButton.setAlpha(visible ? 0xff : 0);
		this.update();
	}

}
