package net.zhuoweizhang.mcpelauncher.api.modpe;

public final class ParticleType {
	public static final int bubble = 1;
	public static final int crit = 2;
	public static final int smoke = 3;
	public static final int cloud = 4;
	public static final int largeexplode = 5;
	public static final int flame = 6;
	public static final int lava = 7;
	public static final int smoke2 = 8;
	public static final int redstone = 9;
	public static final int itemBreak = 10;
	public static final int snowballpoof = 11;
	public static final int hugeexplosionSeed = 12;
	public static final int hugeexplosion = 13;
	public static final int mobFlame = 14;
	public static final int heart = 15;
	public static final int terrain = 16;
	public static final int suspendedTown = 17;
	public static final int portal = 18;
	public static final int splash = 19;
	public static final int waterWake = 20;
	public static final int dripWater = 21;
	public static final int dripLava = 22;
	public static final int fallingDust = 23;
	public static final int spell = 24;
	public static final int spell2 = 25;
	public static final int spell3 = 26;
	public static final int ink = 27;
	public static final int slime = 28;
	public static final int rainSplash = 29;
	public static final int angryVillager = 30;
	public static final int happyVillager = 31;
	public static final int enchantmenttable = 32;
	//public static final int something = 33;
	public static final int note = 34;
	private ParticleType() {}
	public static boolean checkValid(int type, int data) {
		if (type < 0 || type > 34) {
			throw new RuntimeException("Invalid particle type: must be between 1 and 34");
		}
		if (type == itemBreak && data < 0x100) {
			throw new RuntimeException("Breaking item particle requires argument of id<<16|data");
		}
		if (type == smoke || type == smoke2 || type == ink) { // smoke and ink
			if (data < 100) {
				throw new RuntimeException("Size percent parameter for smoke particle must be 100 or above");
			}
		}
		return true;
	}
}
