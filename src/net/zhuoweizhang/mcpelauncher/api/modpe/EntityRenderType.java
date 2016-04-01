package net.zhuoweizhang.mcpelauncher.api.modpe;
public final class EntityRenderType {
	public static final int
	tnt = 2,
	human = 3,
	item = 4,
	chicken = 5,
	cow = 6,
	mushroomCow = 7,
	pig = 8,
	sheep = 9,
	bat = 10,
	wolf = 11,
	villager = 12,
	zombie = 14,
	zombiePigman = 15,
	lavaSlime = 16,
	ghast = 17,
	blaze = 18,
	skeleton = 19,
	spider = 20,
	silverfish = 21,
	creeper = 22,
	slime = 23,
	enderman = 24,
	arrow = 25,
	fishHook = 26,
	player = 27,
	egg = 28,
	snowball = 29,
	unknownItem = 30,
	thrownPotion = 31,
	painting = 32,
	fallingTile = 33,
	minecart = 34,
	boat = 35,
	squid = 36,
	fireball = 37,
	smallFireball = 38,
	villagerZombie = 39,
	experienceOrb = 40,
	lightningBolt = 41,
	ironGolem = 42,
	ocelot = 43,
	snowGolem = 44,
	expPotion = 45,
	rabbit = 46,
	witch = 47,
	camera = 48,
	map = 50;
	private static final int[] alltypes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 50};
	private EntityRenderType() {}

	public static boolean isValidRenderType(int type) {
		return java.util.Arrays.binarySearch(alltypes, type) >= 0;
	}
}
