package btai.foolmod.block;

import btai.foolmod.path.FoolPathfinder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.core.world.World;

public final class FoolBlockDecay {

	public static final int MAX_LIFETIME = 3600;

	public static final int MIN_LIFETIME = 400;

	private static final Map<World, Map<Long, int[]>> PENDING = new WeakHashMap<>();

	private FoolBlockDecay() {
	}

	public static void mark(World world, int x, int y, int z, int blockId, Random random) {
		if (world == null || world.isClientSide) {
			return;
		}
		int life = MIN_LIFETIME + random.nextInt(MAX_LIFETIME - MIN_LIFETIME + 1);
		synchronized (PENDING) {
			PENDING.computeIfAbsent(world, w -> new HashMap<>())
					.put(FoolPathfinder.packKey(x, y, z), new int[]{life, blockId});
		}
	}

	public static boolean claimAndPuff(World world, int x, int y, int z) {
		if (world == null || world.isClientSide) {
			return false;
		}
		Map<Long, int[]> pending;
		synchronized (PENDING) {
			pending = PENDING.get(world);
		}
		if (pending == null || pending.remove(FoolPathfinder.packKey(x, y, z)) == null) {
			return false;
		}
		puff(world, x, y, z);
		return true;
	}

	public static void tick(World world) {
		if (world == null || world.isClientSide) {
			return;
		}
		Map<Long, int[]> pending;
		synchronized (PENDING) {
			pending = PENDING.get(world);
			if (pending == null || pending.isEmpty()) {
				return;
			}
		}
		Iterator<Map.Entry<Long, int[]>> it = pending.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, int[]> entry = it.next();
			int[] state = entry.getValue();
			if (--state[0] > 0) {
				continue;
			}
			it.remove();
			long k = entry.getKey();
			int x = unpackX(k), y = unpackY(k), z = unpackZ(k);

			if (world.getBlockId(x, y, z) != state[1]) {
				continue;
			}
			puff(world, x, y, z);
			world.setBlockWithNotify(x, y, z, 0);
		}
	}

	private static final Random PUFF_RANDOM = new Random();

	private static void puff(World world, int x, int y, int z) {
		Random random = PUFF_RANDOM;
		for (int i = 0; i < 12; i++) {
			world.spawnParticle("largesmoke",
					x + random.nextDouble(),
					y + random.nextDouble(),
					z + random.nextDouble(),
					(random.nextDouble() - 0.5) * 0.04, 0.03, (random.nextDouble() - 0.5) * 0.04,
					0, false);
		}
	}

	private static int unpackX(long k) {
		return sign26((int) ((k >> 38) & 0x3FFFFFF));
	}

	private static int unpackZ(long k) {
		return sign26((int) ((k >> 12) & 0x3FFFFFF));
	}

	private static int unpackY(long k) {
		int y = (int) (k & 0xFFF);
		return (y & 0x800) != 0 ? y - 0x1000 : y;
	}

	private static int sign26(int v) {
		return (v & 0x2000000) != 0 ? v - 0x4000000 : v;
	}
}
