package btai.foolmod.act;

import btai.foolmod.entity.FoolEntity;
import btai.foolmod.path.FoolPathfinder;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.block.Block;
import btai.foolmod.util.BlockNames;
import net.minecraft.core.block.Blocks;

public class BreakTorchAct implements FoolAct {

	private static final int SEARCH_RADIUS = 20;
	private static final int SEARCH_VERTICAL = 6;
	private static final int TRAVEL_TIMEOUT = 300;
	private static final int SNUFF_GAP = 12;

	private static Set<Integer> torchIds;

	private int[] target;
	private int travelTicks;
	private int gap;
	private int taken;
	private int quota;
	private final Set<Long> skip = new HashSet<>();

	public static Set<Integer> torchIds() {
		Set<Integer> ids = torchIds;
		if (ids == null) {
			ids = new HashSet<>();
			for (Block<?> b : Blocks.blocksList) {
				if (b != null && BlockNames.hasToken(b, "torch")) {
					ids.add(b.id());
				}
			}
			torchIds = ids;
		}
		return ids;
	}

	@Override
	public boolean seek(FoolEntity fool) {
		quota = 2 + fool.rng().nextInt(4);
		target = fool.findNearestBlock(torchIds(), SEARCH_RADIUS, SEARCH_VERTICAL, skip);
		return target != null;
	}

	@Override
	public boolean tick(FoolEntity fool) {
		if (gap > 0) {
			gap--;
			return false;
		}
		if (taken >= quota) {
			return true;
		}
		if (target == null) {
			target = fool.findNearestBlock(torchIds(), SEARCH_RADIUS, SEARCH_VERTICAL, skip);
			travelTicks = 0;
			if (target == null) {
				return true;
			}
		}

		if (!torchIds().contains(fool.world.getBlockId(target[0], target[1], target[2]))) {
			target = null;
			return false;
		}
		if (!fool.withinReach(target[0], target[1], target[2]) || !fool.canSeeBlock(target[0], target[1], target[2])) {
			if (++travelTicks > TRAVEL_TIMEOUT) {
				skip.add(FoolPathfinder.packKey(target[0], target[1], target[2]));
				target = null;
				return false;
			}
			if (!fool.navigateTo(target[0], target[1], target[2], 1.8)) {
				skip.add(FoolPathfinder.packKey(target[0], target[1], target[2]));
				target = null;
			}
			return false;
		}
		fool.stopMoving();
		fool.faceBlock(target[0], target[1], target[2]);
		if (fool.breakAndPocket(target[0], target[1], target[2])) {
			taken++;
			gap = SNUFF_GAP;
		}
		skip.add(FoolPathfinder.packKey(target[0], target[1], target[2]));
		target = null;
		return false;
	}

	@Override
	public String name() {
		return "snuff torches";
	}
}
