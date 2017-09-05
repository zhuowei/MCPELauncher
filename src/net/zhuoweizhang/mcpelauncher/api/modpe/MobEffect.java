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
	public static int fatalPoison;
	public static int levitation;
	public static Map<Integer, String> effectIds = new HashMap<Integer, String>();

	public static void initIds() {
		saturation = populate("saturation", "SATURATION");
		absorption = populate("absorption", "ABSORPTION");
		healthBoost = populate("healthBoost", "HEALTH_BOOST");
		wither = populate("wither", "WITHER");
		poison = populate("poison", "POISON");
		weakness = populate("weakness", "WEAKNESS");
		hunger = populate("hunger", "HUNGER");
		nightVision = populate("nightVision", "NIGHT_VISION");
		blindness = populate("blindness", "BLINDNESS");
		invisibility = populate("invisibility", "INVISIBILITY");
		waterBreathing = populate("waterBreathing", "WATER_BREATHING");
		fireResistance = populate("fireResistance", "FIRE_RESISTANCE");
		damageResistance = populate("damageResistance", "DAMAGE_RESISTANCE");
		regeneration = populate("regeneration", "REGENERATION");
		confusion = populate("confusion", "CONFUSION");
		jump = populate("jump", "JUMP");
		harm = populate("harm", "HARM");
		heal = populate("heal", "HEAL");
		damageBoost = populate("damageBoost", "DAMAGE_BOOST");
		digSlowdown = populate("digSlowdown", "DIG_SLOWDOWN");
		digSpeed = populate("digSpeed", "DIG_SPEED");
		movementSlowdown = populate("movementSlowdown", "MOVEMENT_SLOWDOWN");
		movementSpeed = populate("movementSpeed", "MOVEMENT_SPEED");
		fatalPoison = populate("fatalPoison", "FATAL_POISON");
		levitation = populate("levitation", "LEVITATION");
	}

	public static native int nativePopulate(String a);

	public static int populate(String a, String b) {
		int id = nativePopulate("_ZN9MobEffect" + b.length() + b + "E");
		effectIds.put(id, a);
		//System.out.println("MobEffect " + a + ": " + id);
		return id;
	}
}
