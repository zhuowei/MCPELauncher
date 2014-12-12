package net.zhuoweizhang.mcpelauncher.ui;

import net.zhuoweizhang.mcpelauncher.R;
import de.ankri.views.Switch;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;

public class SwitchPreference extends Preference implements
		android.widget.CompoundButton.OnCheckedChangeListener {
	public Switch content = null;
	protected OnCheckedChangeListener listener = null;

	public SwitchPreference(Context context) {
		super(context);
		setWidgetLayoutResource(R.layout.switch_preference);
	}

	public SwitchPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setWidgetLayoutResource(R.layout.switch_preference);
	}

	public SwitchPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setWidgetLayoutResource(R.layout.switch_preference);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		content = (Switch) view.findViewById(R.id.switch_widget);
		if (content != null) {
			content.setChecked(getPersistedBoolean(false));
			content.setOnCheckedChangeListener(this);
		} else {
			System.err.println("SwitchPreference Switch is null");
		}
	}

	public void setListener(OnCheckedChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		persistBoolean(isChecked);
		if (listener != null) {
			listener.onCheckedChange(content);
		}
	}

	public interface OnCheckedChangeListener {
		public void onCheckedChange(Switch data);
	}
}
