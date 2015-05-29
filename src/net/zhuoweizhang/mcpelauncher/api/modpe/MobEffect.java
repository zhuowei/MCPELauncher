package net.zhuoweizhang.mcpelauncher.api.modpe;
import java.util.*;
public class MobEffect {
	public static int saturation;
	public static int absorption;
	public static int healthBoost;
	public static int wither;
	public static int poison;
	public static int weakness;
	public static int hunger;
	public static int nightVision;
	public static int blindness;
	public static int invisibility;
	public static int waterBreathing;
	public static int fireResistance;
	public static int damageResistance;
	public static int regeneration;
	public static int confusion;
	public static int jump;
	public static int harm;
	public static int heal;
	public static int damageBoost;
	public static int digSlowdown;
	public static int digSpeed;
	public static int movementSlowdown;
	public static int movementSpeed;
	public static Map<Integer, String> effectIds = new HashMap<Integer, String>();

	public static void initIds() {
		saturation = populate("saturation");
		absorption = populate("absorption");
		healthBoost = populate("healthBoost");
		wither = populate("wither");
		poison = populate("poison");
		weakness = populate("weakness");
		hunger = populate("hunger");
		nightVision = populate("nightVision");
		blindness = populate("blindness");
		invisibility = populate("invisibility");
		waterBreathing = populate("waterBreathing");
		fireResistance = populate("fireResistance");
		damageResistance = populate("damageResistance");
		regeneration = populate("regeneration");
		confusion = populate("confusion");
		jump = populate("jump");
		harm = populate("harm");
		heal = populate("heal");
		damageBoost = populate("damageBoost");
		digSlowdown = populate("digSlowdown");
		digSpeed = populate("digSpeed");
		movementSlowdown = populate("movementSlowdown");
		movementSpeed = populate("movementSpeed");
	}

	public static native int nativePopulate(String a);

	public static int populate(String a) {
		int id = nativePopulate("_ZN9MobEffect" + a.length() + a + "E");
		effectIds.put(id, a);
		System.out.println("MobEffect " + a + ": " + id);
		return id;
	}
}
