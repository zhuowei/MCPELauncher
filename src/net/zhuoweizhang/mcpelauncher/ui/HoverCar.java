package net.zhuoweizhang.mcpelauncher.ui;

import net.zhuoweizhang.mcpelauncher.R;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupWindow;

public class HoverCar extends PopupWindow {

	public ImageButton mainButton;
	private Context theContext;

	public HoverCar(Context con, boolean safeMode) {
		super(con);
		this.theContext = con;
		LayoutInflater inf = (LayoutInflater) con
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setContentView(inf.inflate(R.layout.hovercar, null));
		mainButton = (ImageButton) getContentView().findViewById(
				R.id.hovercar_main_button);
		float myWidth = con.getResources().getDimension(R.dimen.hovercar_width);
		float myHeight = con.getResources().getDimension(
				R.dimen.hovercar_height);
		setWidth((int) myWidth);
		setHeight((int) myHeight);
		setBackgroundDrawable(safeMode? new ColorDrawable(0x80ff0000) : null);
	}

	public void show(View parentView) {
		showAtLocation(parentView, Gravity.CENTER | Gravity.TOP,
			(int) (theContext.getResources().getDimension(R.dimen.hovercar_width)*1.5), 0);
	}

	/**
	 * Sets visibility of hovercar
	 * 
	 * @param visible
	 *            If true, hovercar will be visible
	 */
	@SuppressWarnings("deprecation")
	public void setVisible(boolean visible) {
		mainButton.setAlpha(visible ? 0xff : 0);
		this.update();
	}

}
