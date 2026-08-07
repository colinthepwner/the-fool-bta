package btai.foolmod.block;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;

public final class JoxeWard {

	private static final int MAX_CELLS = 4096;

	private JoxeWard() {
	}

	public static boolean isWarded(World world, int x, int y, int z) {
		if (world == null || joxeDustId() <= 0) {
			return false;
		}
		Set<Long> seen = new HashSet<>();
		Deque<int[]> queue = new ArrayDeque<>();
		long start = key(x, y, z);
		seen.add(start);
		queue.add(new int[]{x, y, z});
		boolean touchedDust = false;

		while (!queue.isEmpty()) {
			if (seen.size() > MAX_CELLS) {
				return false;
			}
			int[] cell = queue.poll();
			for (int[] d : NEIGHBOURS) {
				int nx = cell[0] + d[0], ny = cell[1] + d[1], nz = cell[2] + d[2];
				if (!world.isChunkLoaded(nx >> 4, nz >> 4)) {
					return false;
				}
				if (isDust(world, nx, ny, nz)) {
					touchedDust = true;
					continue;
				}
				if (blocksMovement(world, nx, ny, nz)) {
					continue;
				}
				long k = key(nx, ny, nz);
				if (seen.add(k)) {
					queue.add(new int[]{nx, ny, nz});
				}
			}
		}

		return touchedDust;
	}

	private static final int[][] NEIGHBOURS = {
			{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0},
	};

	public static boolean isDust(World world, int x, int y, int z) {
		return world.getBlockId(x, y, z) == joxeDustId();
	}

	private static boolean blocksMovement(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		return block != null && block.getMaterial().blocksMotion();
	}

	private static int joxeDustId() {
		return FoolBlocks.joxeDust == null ? -1 : FoolBlocks.joxeDust.id();
	}

	private static long key(int x, int y, int z) {
		return (long) (x & 0x3FFFFFF) << 38 | (long) (z & 0x3FFFFFF) << 12 | (long) (y & 0xFFF);
	}
}
