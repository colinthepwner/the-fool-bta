package btai.foolmod.path;

import btai.foolmod.entity.FoolEntity;
import btai.foolmod.util.BlockNames;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicDoor;
import net.minecraft.core.block.BlockLogicFallingBlock;
import net.minecraft.core.block.BlockLogicFence;
import net.minecraft.core.block.BlockLogicFenceGate;
import net.minecraft.core.block.BlockLogicFenceThin;
import net.minecraft.core.block.BlockLogicGravel;
import net.minecraft.core.block.BlockLogicTrapDoor;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.block.material.Materials;
import net.minecraft.core.world.World;

public final class FoolPathfinder {

	public static final int MOVE = 0;

	public static final int PILLAR = 1;

	public static final int BRIDGE = 2;

	private static final int WALK_COST = 10;
	private static final int DIAG_COST = 14;
	private static final int STEP_UP_COST = 18;
	private static final int DIAG_UP_COST = 22;
	private static final int FALL_BASE_COST = 10;
	private static final int FALL_PER_BLOCK = 4;
	private static final int MAX_FALL = 3;

	private static final int DIG_DOWN_COST = 26;
	private static final int BREAK_COST = 14;
	private static final int PILLAR_COST = 16;
	private static final int BRIDGE_COST = 18;
	private static final int DIAG_BRIDGE_COST = 24;
	private static final int WATER_PENALTY = 12;

	private static final int SWIM_COST = 60;
	private static final int SWIM_MAX_DEPTH = 5;
	private static final int HAZARD_NEAR_PENALTY = 40;

	private static final int DOOR_COST = 2;
	private static final int AVOID_PENALTY = 600;
	private static final double HEURISTIC_WEIGHT = 2.0;

	private FoolPathfinder() {
	}

	public static List<int[]> findPath(FoolEntity fool, PathfinderState state, int sx, int sy, int sz,
			int gx, int gy, int gz, double acceptRadius, int maxNodes, boolean canPlace, boolean mayBreak, Set<Long> avoid) {
		Search s = beginSearch(fool, state, sx, sy, sz, gx, gy, gz, acceptRadius, maxNodes, canPlace, mayBreak, avoid);
		s.advance(Integer.MAX_VALUE);
		return s.result();
	}

	public static Search beginSearch(FoolEntity fool, PathfinderState state, int sx, int sy, int sz,
			int gx, int gy, int gz, double acceptRadius, int maxNodes, boolean canPlace, boolean mayBreak, Set<Long> avoid) {
		return new Search(fool, state, sx, sy, sz, gx, gy, gz, acceptRadius, maxNodes, canPlace, mayBreak, avoid);
	}

	private static void expand(FoolEntity fool, World world, PathfinderState state, Node cur,
			int gx, int gy, int gz, boolean canPlace, boolean mayBreak, Set<Long> avoid) {
		boolean submerged = isWater(world, cur.x, cur.y, cur.z) || isWater(world, cur.x, cur.y + 1, cur.z);

		for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			int nx = cur.x + d[0];
			int nz = cur.z + d[1];

			if (!chunkLoaded(world, nx, nz)) continue;

			double enter = enterCost(fool, world, nx, cur.y, nz, mayBreak);
			if (enter >= 0.0 && isStandable(world, nx, cur.y, nz)) {
				relax(avoid, state, cur, nx, cur.y, nz, WALK_COST + enter, MOVE, gx, gy, gz);
			}

			double up = enterCost(fool, world, nx, cur.y + 1, nz, mayBreak);
			double head = breakCost(fool, world, cur.x, cur.y + 2, cur.z, mayBreak);
			if (up >= 0.0 && head >= 0.0 && isStandable(world, nx, cur.y + 1, nz)) {
				relax(avoid, state, cur, nx, cur.y + 1, nz, STEP_UP_COST + up + head, MOVE, gx, gy, gz);
			}

			if (enter == 0.0 && !isStandable(world, nx, cur.y, nz)) {
				int fy = cur.y;
				for (int drop = 1; drop <= MAX_FALL && isClear(world, nx, fy - 1, nz); ++drop) {

					if (!isStandable(world, nx, --fy, nz) || isHazard(world, nx, fy, nz)) continue;
					relax(avoid, state, cur, nx, fy, nz, FALL_BASE_COST + FALL_PER_BLOCK * drop, MOVE, gx, gy, gz);
					break;
				}
			}

			if (isSwimSurface(world, nx, cur.y, nz)) {
				relax(avoid, state, cur, nx, cur.y, nz, SWIM_COST, MOVE, gx, gy, gz);
			}

			boolean liquidBelow = isWater(world, nx, cur.y - 1, nz) || isLava(world, nx, cur.y - 1, nz);
			boolean shallowAirGap = isAir(world, nx, cur.y - 1, nz) && hasSolidWithin(world, nx, cur.y - 1, nz, 4);
			boolean bodyDry = isClear(world, nx, cur.y, nz) && !isWater(world, nx, cur.y, nz)
					&& isClear(world, nx, cur.y + 1, nz) && !isWater(world, nx, cur.y + 1, nz);

			boolean bridgeSupport = isSolid(world, cur.x, cur.y - 1, cur.z) || cur.action == BRIDGE;
			if (canPlace && (liquidBelow || shallowAirGap) && bodyDry && bridgeSupport) {
				relax(avoid, state, cur, nx, cur.y, nz, BRIDGE_COST, BRIDGE, gx, gy, gz);
			}
		}

		for (int[] d : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
			int nx = cur.x + d[0];
			int nz = cur.z + d[1];
			if (!chunkLoaded(world, nx, nz)) continue;

			boolean cornerLow = isBodyClear(world, cur.x + d[0], cur.y, cur.z) && isBodyClear(world, cur.x, cur.y, cur.z + d[1]);
			boolean cornerHigh = isBodyClear(world, cur.x + d[0], cur.y + 1, cur.z) && isBodyClear(world, cur.x, cur.y + 1, cur.z + d[1]);

			if (cornerLow && isPassableBody(fool, world, nx, cur.y, nz, mayBreak) && isBodyClear(world, nx, cur.y, nz)
					&& isStandable(world, nx, cur.y, nz) && !isWater(world, nx, cur.y + 1, nz)) {

				double dpen = (isWater(world, nx, cur.y, nz) ? WATER_PENALTY : 0.0)
						+ (touchesHazard(world, nx, cur.y, nz) ? HAZARD_NEAR_PENALTY : 0.0);
				relax(avoid, state, cur, nx, cur.y, nz, DIAG_COST + dpen, MOVE, gx, gy, gz);
			}
			if (cornerLow && isClear(world, nx, cur.y, nz) && !isStandable(world, nx, cur.y, nz)) {
				int fy = cur.y;
				for (int drop = 1; drop <= MAX_FALL && isClear(world, nx, fy - 1, nz); ++drop) {
					if (!isStandable(world, nx, --fy, nz) || isHazard(world, nx, fy, nz)) continue;
					relax(avoid, state, cur, nx, fy, nz, DIAG_COST + FALL_PER_BLOCK * drop, MOVE, gx, gy, gz);
					break;
				}
			}
			boolean liquidBelow = isWater(world, nx, cur.y - 1, nz) || isLava(world, nx, cur.y - 1, nz);
			boolean shallowAirGap = isAir(world, nx, cur.y - 1, nz) && hasSolidWithin(world, nx, cur.y - 1, nz, 4);
			boolean bodyDry = isClear(world, nx, cur.y, nz) && !isWater(world, nx, cur.y, nz)
					&& isClear(world, nx, cur.y + 1, nz) && !isWater(world, nx, cur.y + 1, nz);
			boolean cornerSupports = isSolid(world, cur.x + d[0], cur.y - 1, cur.z) || isSolid(world, cur.x, cur.y - 1, cur.z + d[1]);
			if (canPlace && cornerLow && cornerSupports && (liquidBelow || shallowAirGap) && bodyDry
					&& isSolid(world, cur.x, cur.y - 1, cur.z)) {
				relax(avoid, state, cur, nx, cur.y, nz, DIAG_BRIDGE_COST, BRIDGE, gx, gy, gz);
			}
			if (!cornerHigh) continue;
			double dup = enterCost(fool, world, nx, cur.y + 1, nz, mayBreak);
			double dhead = breakCost(fool, world, cur.x, cur.y + 2, cur.z, mayBreak);
			if (dup >= 0.0 && dhead >= 0.0 && isStandable(world, nx, cur.y + 1, nz)) {
				relax(avoid, state, cur, nx, cur.y + 1, nz, DIAG_UP_COST + dup + dhead, MOVE, gx, gy, gz);
			}
		}

		if (!submerged && isClear(world, cur.x, cur.y - 1, cur.z)) {
			int fy = cur.y;
			for (int drop = 1; drop <= MAX_FALL && isClear(world, cur.x, fy - 1, cur.z); ++drop) {
				if (!isStandable(world, cur.x, --fy, cur.z) || isHazard(world, cur.x, fy, cur.z)) continue;
				relax(avoid, state, cur, cur.x, fy, cur.z, FALL_BASE_COST + FALL_PER_BLOCK * drop, MOVE, gx, gy, gz);
				break;
			}
		}

		boolean liquidDanger = hasAdjacentLiquid(world, cur.x, cur.y, cur.z) || hasAdjacentLiquid(world, cur.x, cur.y - 1, cur.z);
		double below = submerged || liquidDanger ? -1.0 : breakCost(fool, world, cur.x, cur.y - 1, cur.z, mayBreak);
		if (below > 0.0) {
			for (int ly = cur.y - 2; ly >= cur.y - 1 - MAX_FALL; --ly) {
				if (isSolid(world, cur.x, ly, cur.z)) {
					int extraFall = cur.y - 2 - ly;
					relax(avoid, state, cur, cur.x, cur.y - 1, cur.z,
							DIG_DOWN_COST + below + FALL_PER_BLOCK * extraFall, MOVE, gx, gy, gz);
					break;
				}
				if (!isClear(world, cur.x, ly, cur.z)) break;
			}
		}

		if (canPlace && isClear(world, cur.x, cur.y + 1, cur.z) && isClear(world, cur.x, cur.y + 2, cur.z)
				&& isSolid(world, cur.x, cur.y - 1, cur.z)) {
			relax(avoid, state, cur, cur.x, cur.y + 1, cur.z, PILLAR_COST, PILLAR, gx, gy, gz);

			if (isClear(world, cur.x, cur.y + 3, cur.z)) {
				Node n1 = state.getNode(cur.x, cur.y + 1, cur.z);
				if (n1 != null) {
					relax(avoid, state, n1, cur.x, cur.y + 2, cur.z, PILLAR_COST, PILLAR, gx, gy, gz);
					if (isClear(world, cur.x, cur.y + 4, cur.z)) {
						Node n2 = state.getNode(cur.x, cur.y + 2, cur.z);
						if (n2 != null) {
							relax(avoid, state, n2, cur.x, cur.y + 3, cur.z, PILLAR_COST, PILLAR, gx, gy, gz);
						}
					}
				}
			}
		}
	}

	private static double enterCost(FoolEntity fool, World world, int x, int y, int z, boolean mayBreak) {
		double feet = breakCost(fool, world, x, y, z, mayBreak);
		double head = breakCost(fool, world, x, y + 1, z, mayBreak);
		if (feet < 0.0 || head < 0.0) {
			return -1.0;
		}
		if (isWater(world, x, y + 1, z)) {
			return -1.0;
		}
		double water = isWater(world, x, y, z) ? WATER_PENALTY : 0.0;
		double hazard = touchesHazard(world, x, y, z) ? HAZARD_NEAR_PENALTY : 0.0;
		return feet + head + water + hazard;
	}

	private static double breakCost(FoolEntity fool, World world, int x, int y, int z, boolean mayBreak) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return 0.0;
		}
		Block<?> block = Blocks.blocksList[id];
		if (block == null) {
			return 0.0;
		}
		Material material = block.getMaterial();
		if (material == Materials.LAVA || material == Materials.CACTUS || material == Materials.FIRE) {
			return -1.0;
		}
		if (!material.blocksMotion()) {
			return 0.0;
		}
		if (isOperableDoor(world, x, y, z)) {
			return isDoorOpen(world, x, y, z) ? 0.0 : DOOR_COST;
		}

		if (isFenceLike(world, x, y, z)) {
			return -1.0;
		}

		boolean own = fool != null && fool.isPlacedBlock(x, y, z);
		if (!own && isPlayerBuilt(world, x, y, z)) {
			return -1.0;
		}
		if (!mayBreak) {
			return -1.0;
		}
		if (!isBreakable(world, x, y, z)) {
			return -1.0;
		}
		double cost = BREAK_COST;
		if (isFallingBlock(world, x, y + 1, z)) {
			cost += 50000.0;
		}
		return cost;
	}

	public static int breakCost() {
		return BREAK_COST;
	}

	private static boolean[] playerBuilt;

	private static final Set<String> CRAFTED_TOKENS = Set.of(
			"planks", "plank", "brick", "bricks", "glass", "wool", "cobble", "bookshelf", "workbench",
			"furnace", "lamp", "bed", "carpet", "stairs", "slab", "ladder", "chest", "sign", "rail",
			"tnt", "jukebox", "dispenser", "piston", "fence", "pane", "panes", "door", "doors",
			"trapdoor", "painting", "banner", "polished", "chiseled", "smooth", "carved", "capstone",
			"pillar", "column", "torchholder", "bars", "gate");

	public static boolean isPlayerBuilt(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		boolean[] cache = playerBuilt;
		if (cache == null) {
			cache = new boolean[Blocks.blocksList.length];
			for (int i = 0; i < cache.length; i++) {
				Block<?> b = Blocks.blocksList[i];
				cache[i] = b != null && BlockNames.hasAnyToken(b, CRAFTED_TOKENS);
			}
			playerBuilt = cache;
		}
		return id < cache.length && cache[id];
	}

	public static boolean chunkLoaded(World world, int x, int z) {
		return world.isChunkLoaded(x >> 4, z >> 4);
	}

	public static boolean isFenceLike(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		if (block == null) {
			return false;
		}
		return Block.hasLogicClass(block, BlockLogicFence.class)
				|| Block.hasLogicClass(block, BlockLogicFenceThin.class);
	}

	public static boolean isHazard(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		if (block == null) {
			return false;
		}
		Material m = block.getMaterial();
		return m == Materials.CACTUS || m == Materials.FIRE || m == Materials.LAVA;
	}

	private static boolean touchesHazard(World world, int x, int y, int z) {
		for (int dy = 0; dy <= 1; ++dy) {
			if (isHazard(world, x + 1, y + dy, z) || isHazard(world, x - 1, y + dy, z)
					|| isHazard(world, x, y + dy, z + 1) || isHazard(world, x, y + dy, z - 1)) {
				return true;
			}
		}
		return isHazard(world, x, y - 1, z);
	}

	public static boolean isFallingBlock(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		return block != null && (Block.hasLogicClass(block, BlockLogicFallingBlock.class)
				|| Block.hasLogicClass(block, BlockLogicGravel.class));
	}

	public static boolean isBreakable(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		if (block == null || block.getHardness() < 0.0f) {
			return false;
		}
		if (Block.hasLogicClass(block, BlockLogicDoor.class)
				|| Block.hasLogicClass(block, BlockLogicTrapDoor.class)
				|| Block.hasLogicClass(block, BlockLogicFenceGate.class)) {
			return false;
		}
		return world.getTileEntity(x, y, z) == null;
	}

	public static boolean isOperableDoor(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		if (block == null) {
			return false;
		}
		if (!Block.hasLogicClass(block, BlockLogicDoor.class)
				&& !Block.hasLogicClass(block, BlockLogicTrapDoor.class)
				&& !Block.hasLogicClass(block, BlockLogicFenceGate.class)) {
			return false;
		}
		Material material = block.getMaterial();
		return material != Materials.METAL && material != Materials.STEEL;
	}

	public static boolean isDoorOpen(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		if (block == null) {
			return false;
		}
		int meta = world.getBlockMetadata(x, y, z);
		if (Block.hasLogicClass(block, BlockLogicDoor.class)) {
			return BlockLogicDoor.isOpen(meta);
		}
		if (Block.hasLogicClass(block, BlockLogicTrapDoor.class)) {
			return BlockLogicTrapDoor.isTrapdoorOpen(meta);
		}
		if (Block.hasLogicClass(block, BlockLogicFenceGate.class)) {
			return BlockLogicFenceGate.isOpen(meta);
		}
		return false;
	}

	public static boolean blocksMotion(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		return block != null && block.getMaterial().blocksMotion();
	}

	public static boolean isSolid(World world, int x, int y, int z) {
		return blocksMotion(world, x, y, z);
	}

	private static boolean isAir(World world, int x, int y, int z) {
		return world.getBlockId(x, y, z) <= 0;
	}

	private static boolean isSwimSurface(World world, int x, int y, int z) {
		return isWater(world, x, y, z)
				&& isClear(world, x, y + 1, z) && !isWater(world, x, y + 1, z)
				&& hasSolidWithin(world, x, y - 1, z, SWIM_MAX_DEPTH);
	}

	private static boolean hasSolidWithin(World world, int x, int yTop, int z, int depth) {
		for (int i = 0; i < depth; ++i) {
			if (isLava(world, x, yTop - i, z)) {
				return false;
			}
			if (isSolid(world, x, yTop - i, z)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isClear(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return true;
		}
		Block<?> block = Blocks.blocksList[id];
		if (block == null) {
			return true;
		}
		Material material = block.getMaterial();
		return !material.blocksMotion() && material != Materials.LAVA;
	}

	private static boolean isBodyClear(World world, int x, int y, int z) {
		return isClear(world, x, y, z) && isClear(world, x, y + 1, z);
	}

	private static boolean isPassableBody(FoolEntity fool, World world, int x, int y, int z, boolean mayBreak) {
		return breakCost(fool, world, x, y, z, mayBreak) >= 0.0
				&& breakCost(fool, world, x, y + 1, z, mayBreak) >= 0.0;
	}

	public static boolean isWater(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		return block != null && block.getMaterial() == Materials.WATER;
	}

	private static boolean isLava(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return false;
		}
		Block<?> block = Blocks.blocksList[id];
		return block != null && block.getMaterial() == Materials.LAVA;
	}

	public static boolean isLiquid(World world, int x, int y, int z) {
		return isWater(world, x, y, z) || isLava(world, x, y, z);
	}

	private static boolean hasAdjacentLiquid(World world, int x, int y, int z) {
		return isLiquid(world, x + 1, y, z) || isLiquid(world, x - 1, y, z)
				|| isLiquid(world, x, y, z + 1) || isLiquid(world, x, y, z - 1)
				|| isLiquid(world, x, y + 1, z);
	}

	public static boolean isStandable(World world, int x, int y, int z) {
		return isSolid(world, x, y - 1, z) && !isHazard(world, x, y - 1, z) && !isFenceLike(world, x, y - 1, z);
	}

	private static void relax(Set<Long> avoid, PathfinderState state, Node cur, int x, int y, int z,
			double moveCost, int action, int gx, int gy, int gz) {
		if (cur == null) return;
		Node next = state.getNode(x, y, z);
		if (next == null || next.closed) {
			return;
		}
		if (avoid != null && !avoid.isEmpty() && avoid.contains(packKey(x, y, z))) {
			moveCost += AVOID_PENALTY;
		}
		double g = cur.g + moveCost;
		if (g < next.g) {
			next.g = g;
			next.f = g + heuristic(x, y, z, gx, gy, gz);
			next.parent = cur;
			next.action = action;
			state.open.add(next);
		}
	}

	private static double heuristic(int x, int y, int z, int gx, int gy, int gz) {
		int dx = Math.abs(gx - x);
		int dz = Math.abs(gz - z);
		int dmin = Math.min(dx, dz);
		int dmax = Math.max(dx, dz);
		double horiz = DIAG_COST * dmin + WALK_COST * (dmax - dmin);
		int dy = gy - y;
		double vert = dy > 0 ? STEP_UP_COST * dy : FALL_PER_BLOCK * -dy;
		return (horiz + vert) * HEURISTIC_WEIGHT;
	}

	public static long packKey(int x, int y, int z) {
		return (long) (x & 0x3FFFFFF) << 38 | (long) (z & 0x3FFFFFF) << 12 | (long) (y & 0xFFF);
	}

	private static List<int[]> reconstruct(Node end) {
		ArrayList<int[]> path = new ArrayList<>();
		Node cur = end;
		while (cur != null) {
			path.add(new int[]{cur.x, cur.y, cur.z, cur.action});
			cur = cur.parent;
		}
		Collections.reverse(path);
		return path;
	}

	private static List<int[]> smooth(World world, List<int[]> path) {
		if (path.size() <= 2) {
			return path;
		}
		ArrayList<int[]> out = new ArrayList<>();
		int i = 0;
		out.add(path.get(0));
		while (i < path.size() - 1) {
			int j;
			for (j = path.size() - 1; j > i + 1 && !walkableStraight(world, path.get(i), path.get(j)); --j) {
			}
			out.add(path.get(j));
			i = j;
		}
		return out;
	}

	private static boolean walkableStraight(World world, int[] a, int[] b) {

		if (a[1] != b[1] || a[3] != MOVE || b[3] != MOVE) {
			return false;
		}
		int y = a[1];
		double x0 = a[0] + 0.5, z0 = a[2] + 0.5;
		double dx = (b[0] + 0.5) - x0, dz = (b[2] + 0.5) - z0;
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < 1.0E-4) {
			return true;
		}
		int samples = (int) Math.ceil(dist * 4.0);
		double r = 0.35;
		for (int s = 1; s <= samples; ++s) {
			double fx = x0 + dx * s / samples;
			double fz = z0 + dz * s / samples;
			for (int cx = (int) Math.floor(fx - r); cx <= (int) Math.floor(fx + r); ++cx) {
				for (int cz = (int) Math.floor(fz - r); cz <= (int) Math.floor(fz + r); ++cz) {

					if (isBodyClear(world, cx, y, cz) && isStandable(world, cx, y, cz) && !isHazard(world, cx, y, cz)) {
						continue;
					}
					return false;
				}
			}
		}
		return true;
	}

	public static final class Search {
		private final FoolEntity fool;
		private final World world;
		private final int gx;
		private final int gy;
		private final int gz;
		private final boolean canPlace;
		private final boolean mayBreak;
		private final Set<Long> avoid;
		private final int maxNodes;
		private final double acceptSq;
		private final PathfinderState state;
		private final Node start;
		private Node best;
		private double bestH;
		private int visited;
		private boolean done;
		private List<int[]> result;

		Search(FoolEntity fool, PathfinderState state, int sx, int sy, int sz, int gx, int gy, int gz,
				double acceptRadius, int maxNodes, boolean canPlace, boolean mayBreak, Set<Long> avoid) {
			this.fool = fool;
			this.world = fool.world;
			this.gx = gx;
			this.gy = gy;
			this.gz = gz;
			this.canPlace = canPlace;
			this.mayBreak = mayBreak;
			this.avoid = avoid;
			this.maxNodes = maxNodes;
			this.acceptSq = acceptRadius * acceptRadius;
			this.state = state;
			this.state.reset();

			this.start = chunkLoaded(this.world, sx, sz) ? this.state.getNode(sx, sy, sz) : null;
			if (this.start != null) {
				this.start.g = 0.0;
				this.start.f = heuristic(sx, sy, sz, gx, gy, gz);
				this.state.open.add(this.start);
				this.best = this.start;
				this.bestH = this.start.f;
			} else {
				this.done = true;
			}
		}

		public boolean advance(int budget) {
			if (this.done) {
				return true;
			}
			int steps = 0;
			while (!this.state.open.isEmpty() && this.visited < this.maxNodes && steps < budget) {
				Node cur = this.state.open.poll();
				if (cur.closed) continue;
				cur.closed = true;
				++this.visited;
				++steps;
				double ddx = cur.x - this.gx, ddy = cur.y - this.gy, ddz = cur.z - this.gz;
				if (ddx * ddx + ddy * ddy + ddz * ddz <= this.acceptSq
						|| (cur.x == this.gx && cur.y == this.gy && cur.z == this.gz)) {
					this.result = smooth(this.world, reconstruct(cur));
					this.done = true;
					return true;
				}
				double h = heuristic(cur.x, cur.y, cur.z, this.gx, this.gy, this.gz);
				if (h < this.bestH) {
					this.bestH = h;
					this.best = cur;
				}
				expand(this.fool, this.world, this.state, cur, this.gx, this.gy, this.gz,
						this.canPlace, this.mayBreak, this.avoid);
			}
			if (this.state.open.isEmpty() || this.visited >= this.maxNodes) {

				this.result = this.best == this.start ? null : smooth(this.world, reconstruct(this.best));
				this.done = true;
			}
			return this.done;
		}

		public boolean isDone() {
			return this.done;
		}

		public List<int[]> result() {
			return this.result;
		}
	}

	public static final class Node implements Comparable<Node> {
		int x;
		int y;
		int z;
		double g = Double.MAX_VALUE;
		double f = Double.MAX_VALUE;
		Node parent;
		int action = MOVE;
		boolean closed;

		Node() {
		}

		void init(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.g = Double.MAX_VALUE;
			this.f = Double.MAX_VALUE;
			this.parent = null;
			this.action = MOVE;
			this.closed = false;
		}

		@Override
		public int compareTo(Node other) {
			return Double.compare(this.f, other.f);
		}
	}

	public static final class PathfinderState {
		public final int capacity;
		private final Node[] pool;
		private int poolIdx;
		private final long[] mapKeys;
		private final Node[] mapValues;
		private final int mapMask;
		public final PriorityQueue<Node> open = new PriorityQueue<>();

		public PathfinderState(int capacity) {
			this.capacity = capacity;
			this.pool = new Node[capacity];
			for (int i = 0; i < capacity; i++) this.pool[i] = new Node();
			int cap = 1;
			while (cap < capacity * 2) cap <<= 1;
			this.mapKeys = new long[cap];
			this.mapValues = new Node[cap];
			this.mapMask = cap - 1;
		}

		public void reset() {
			poolIdx = 0;
			java.util.Arrays.fill(mapValues, null);
			open.clear();
		}

		public Node getNode(int x, int y, int z) {
			long key = packKey(x, y, z);
			int i = (int) (murmurHash3(key) & mapMask);
			while (mapValues[i] != null) {
				if (mapKeys[i] == key) return mapValues[i];
				i = (i + 1) & mapMask;
			}
			if (poolIdx >= capacity) return null;
			Node n = pool[poolIdx++];
			n.init(x, y, z);
			mapKeys[i] = key;
			mapValues[i] = n;
			return n;
		}

		private static long murmurHash3(long x) {
			x ^= x >>> 33; x *= 0xff51afd7ed558ccdL;
			x ^= x >>> 33; x *= 0xc4ceb9fe1a85ec53L;
			x ^= x >>> 33; return x;
		}
	}
}
