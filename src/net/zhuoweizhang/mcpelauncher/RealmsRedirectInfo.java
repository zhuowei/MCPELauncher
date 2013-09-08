package net.zhuoweizhang.mcpelauncher;

import java.util.*;

import com.mojang.minecraftpe.MainActivity;

public class RealmsRedirectInfo {

	public String loginUrl = null;
	public String accountUrl;
	public String peoUrl = "NONE";

	public RealmsRedirectInfo(String peoUrl, String accountUrl, String loginUrl) {
		this.peoUrl = peoUrl;
		this.accountUrl = accountUrl;
		this.loginUrl = loginUrl;
	}

	public static Map<String, RealmsRedirectInfo> targets = new HashMap<String, RealmsRedirectInfo>();

	private static void add(RealmsRedirectInfo info) {
		targets.put(info.peoUrl, info);
	}

	static {
		add(new RealmsRedirectInfo("NONE", null, MainActivity.MOJANG_ACCOUNT_LOGIN_URL));
		//add(new RealmsRedirectInfo("peoapi.pocketmine.net", "account.pocketmine.net", "https://account.pocketmine.net/m/login"));
		//add(new RealmsRedirectInfo("peoapi.minepocket.com", "peoapi.minepocket.com", "https://peoapi.minepocket.com/m/login?app=mcpe"));
	}
}
