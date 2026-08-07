package btai.foolmod.act;

import btai.foolmod.entity.FoolEntity;
import btai.foolmod.path.FoolPathfinder;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicDoor;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.util.helper.MathHelper;

public class BlockDoorAct implements FoolAct {

	private static final int SEARCH_RADIUS = 18;
	private static final int SEARCH_VERTICAL = 5;
	private static final int TRAVEL_TIMEOUT = 400;
	private static final int PLACE_GAP = 8;

	private static Set<Integer> doorIds;

	private int[] door;
	private int[] plugLow;
	private int travelTicks;
	private int gap;
	private int placed;
	private final Set<Long> skip = new HashSet<>();

	public static Set<Integer> doorIds() {
		Set<Integer> ids = doorIds;
		if (ids == null) {
			ids = new HashSet<>();
			for (Block<?> b : Blocks.blocksList) {
				if (b == null) continue;
				if (Block.hasLogicClass(b, BlockLogicDoor.class)) {
					ids.add(b.id());
				}
			}
			doorIds = ids;
		}
		return ids;
	}

	@Override
	public boolean seek(FoolEntity fool) {
		door = fool.findNearestBlock(doorIds(), SEARCH_RADIUS, SEARCH_VERTICAL, skip);
		return door != null;
	}

	@Override
	public boolean tick(FoolEntity fool) {
		if (gap > 0) {
			gap--;
			return false;
		}
		if (placed >= 2) {
			return true;
		}
		if (door == null) {
			return true;
		}
		if (!doorIds().contains(fool.world.getBlockId(door[0], door[1], door[2]))) {
			skip.add(FoolPathfinder.packKey(door[0], door[1], door[2]));
			door = null;
			return true;
		}
		if (plugLow == null) {
			plugLow = choosePlugCell(fool, door);
			if (plugLow == null) {
				skip.add(FoolPathfinder.packKey(door[0], door[1], door[2]));
				return true;
			}
		}
		if (!fool.withinReach(plugLow[0], plugLow[1], plugLow[2])) {
			if (++travelTicks > TRAVEL_TIMEOUT) {
				skip.add(FoolPathfinder.packKey(door[0], door[1], door[2]));
				return true;
			}
			if (!fool.navigateTo(plugLow[0], plugLow[1], plugLow[2], 1.6)) {
				skip.add(FoolPathfinder.packKey(door[0], door[1], door[2]));
				return true;
			}
			return false;
		}
		fool.stopMoving();

		int y = plugLow[1] + (placed == 0 ? 1 : 0);
		if (fool.placeWool(plugLow[0], y, plugLow[2])) {
			placed++;
			gap = PLACE_GAP;
		} else {

			if (++travelTicks > TRAVEL_TIMEOUT) {
				return true;
			}
			int[] step = backOff(fool, plugLow);
			if (step != null) {
				fool.navigateTo(step[0], step[1], step[2], 0.8);
			} else {
				return true;
			}
		}
		return false;
	}

	private int[] choosePlugCell(FoolEntity fool, int[] d) {
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		int[] best = null;
		double bestSq = Double.MAX_VALUE;
		for (int[] dir : dirs) {
			int x = d[0] + dir[0];
			int y = d[1];
			int z = d[2] + dir[1];
			if (FoolPathfinder.blocksMotion(fool.world, x, y, z)) continue;
			if (FoolPathfinder.blocksMotion(fool.world, x, y + 1, z)) continue;
			if (!FoolPathfinder.isStandable(fool.world, x, y, z)) continue;
			double dx = x + 0.5 - fool.x, dz = z + 0.5 - fool.z;
			double dSq = dx * dx + dz * dz;
			if (dSq < bestSq) {
				bestSq = dSq;
				best = new int[]{x, y, z};
			}
		}
		return best;
	}

	private int[] backOff(FoolEntity fool, int[] plug) {
		int fx = MathHelper.floor(fool.x), fz = MathHelper.floor(fool.z);
		int dx = fx - plug[0], dz = fz - plug[2];
		if (dx == 0 && dz == 0) {
			dx = 1;
		}
		int x = plug[0] + Integer.signum(dx) * 2;
		int z = plug[2] + Integer.signum(dz) * 2;
		int y = fool.groundAt(x, plug[1], z);
		return y == Integer.MIN_VALUE ? null : new int[]{x, y, z};
	}

	@Override
	public String name() {
		return "wall up a door";
	}
}
