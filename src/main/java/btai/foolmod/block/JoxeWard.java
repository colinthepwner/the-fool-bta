package btai.foolmod.block;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;

public final class JoxeWard {

	private static final int MAX_COLUMNS = 4096;

	private static final int DUST_STEP = 1;

	private JoxeWard() {
	}

	public static boolean isWarded(World world, int x, int y, int z) {
		return isWarded(world, x, y, z, joxeDustId());
	}

	static boolean isWarded(World world, int x, int y, int z, int dustId) {
		if (world == null || dustId <= 0) {
			return false;
		}
		Set<Long> seen = new HashSet<>();
		Deque<int[]> queue = new ArrayDeque<>();
		seen.add(key(x, z));

		queue.add(new int[]{x, z});
		boolean touchedDust = false;

		while (!queue.isEmpty()) {
			if (seen.size() > MAX_COLUMNS) {
				return false;
			}
			int[] col = queue.poll();
			for (int[] d : NEIGHBOURS) {
				int nx = col[0] + d[0], nz = col[1] + d[1];
				if (!world.isChunkLoaded(nx >> 4, nz >> 4)) {
					return false;
				}
				if (hasDust(world, nx, y, nz, dustId)) {
					touchedDust = true;
					continue;
				}
				if (blocksMovement(world, nx, y, nz)) {
					continue;
				}
				long k = key(nx, nz);
				if (seen.add(k)) {
					queue.add(new int[]{nx, nz});
				}
			}
		}

		return touchedDust;
	}

	private static final int[][] NEIGHBOURS = {
			{1, 0}, {-1, 0}, {0, 1}, {0, -1},
	};

	private static boolean hasDust(World world, int x, int y, int z, int dustId) {
		for (int dy = -DUST_STEP; dy <= DUST_STEP; dy++) {
			if (world.getBlockId(x, y + dy, z) == dustId) {
				return true;
			}
		}
		return false;
	}

	public static boolean isDust(World world, int x, int y, int z) {
		return world.getBlockId(x, y, z) == joxeDustId();
	}

	private static boolean blocksMovement(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0 || id >= Blocks.blocksList.length) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		return block != null && block.getMaterial().blocksMotion();
	}

	private static int joxeDustId() {
		return FoolBlocks.joxeDust == null ? -1 : FoolBlocks.joxeDust.id();
	}

	private static long key(int x, int z) {
		return (long) (x & 0x3FFFFFF) << 26 | (long) (z & 0x3FFFFFF);
	}
}
