package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class NerdyStuffActivity extends Activity implements View.OnClickListener {

	private Button dumpLibMinecraftPeButton;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.nerdy_stuff);
		dumpLibMinecraftPeButton = (Button) findViewById(R.id.dump_libminecraftpe_button);
		dumpLibMinecraftPeButton.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v == dumpLibMinecraftPeButton) {
			dumpLib();
		}
	}

	public void dumpLib() {
		try {
			FileOutputStream os = new FileOutputStream("/sdcard/libminecraftpe.so.dump");
			FileChannel channel = os.getChannel();
			channel.write(com.mojang.minecraftpe.MainActivity.minecraftLibBuffer);
			channel.close();
			Toast.makeText(this, "/sdcard/libminecraftpe.so.dump", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
