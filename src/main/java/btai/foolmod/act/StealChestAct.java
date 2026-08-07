package btai.foolmod.act;

import btai.foolmod.entity.FoolEntity;
import btai.foolmod.path.FoolPathfinder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.block.Block;
import btai.foolmod.util.BlockNames;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;

public class StealChestAct implements FoolAct {

	private static final int SEARCH_RADIUS = 20;
	private static final int SEARCH_VERTICAL = 5;
	private static final int TRAVEL_TIMEOUT = 400;

	private static final int MAX_SLOTS_ROBBED = 2;

	private static final int MAX_PER_SLOT = 4;

	private static Set<Integer> chestIds;

	private int[] target;
	private int travelTicks;
	private final Set<Long> skip = new HashSet<>();

	public static Set<Integer> chestIds() {
		Set<Integer> ids = chestIds;
		if (ids == null) {
			ids = new HashSet<>();
			for (Block<?> b : Blocks.blocksList) {
				if (b != null && BlockNames.hasToken(b, "chest")) {
					ids.add(b.id());
				}
			}
			chestIds = ids;
		}
		return ids;
	}

	@Override
	public boolean seek(FoolEntity fool) {
		target = findChest(fool);
		return target != null;
	}

	private int[] findChest(FoolEntity fool) {
		int[] found = fool.findNearestBlock(chestIds(), SEARCH_RADIUS, SEARCH_VERTICAL, skip);

		while (found != null && (fool.containerAt(found[0], found[1], found[2]) == null
				|| fool.isWardedCell(found[0], found[1], found[2]))) {
			skip.add(FoolPathfinder.packKey(found[0], found[1], found[2]));
			found = fool.findNearestBlock(chestIds(), SEARCH_RADIUS, SEARCH_VERTICAL, skip);
		}
		return found;
	}

	@Override
	public boolean tick(FoolEntity fool) {
		if (target == null) {
			return true;
		}
		Container chest = fool.containerAt(target[0], target[1], target[2]);
		if (chest == null) {
			skip.add(FoolPathfinder.packKey(target[0], target[1], target[2]));
			target = null;
			return true;
		}
		if (!fool.withinReach(target[0], target[1], target[2]) || !fool.canSeeBlock(target[0], target[1], target[2])) {
			if (++travelTicks > TRAVEL_TIMEOUT) {
				skip.add(FoolPathfinder.packKey(target[0], target[1], target[2]));
				return true;
			}
			if (!fool.navigateTo(target[0], target[1], target[2], 1.8)) {
				skip.add(FoolPathfinder.packKey(target[0], target[1], target[2]));
				return true;
			}
			return false;
		}
		fool.stopMoving();
		fool.faceBlock(target[0], target[1], target[2]);
		pilfer(fool, chest);
		fool.swing();
		skip.add(FoolPathfinder.packKey(target[0], target[1], target[2]));
		return true;
	}

	private void pilfer(FoolEntity fool, Container chest) {
		List<Integer> occupied = new ArrayList<>();
		int size = chest.getContainerSize();
		for (int i = 0; i < size; i++) {
			ItemStack s = chest.getItem(i);
			if (s != null && s.stackSize > 0 && !chest.locked(i)) {
				occupied.add(i);
			}
		}
		if (occupied.isEmpty()) {
			return;
		}
		java.util.Collections.shuffle(occupied, fool.rng());
		int slots = Math.min(MAX_SLOTS_ROBBED, occupied.size());
		boolean tookAnything = false;
		for (int n = 0; n < slots; n++) {
			int slot = occupied.get(n);
			ItemStack inSlot = chest.getItem(slot);
			if (inSlot == null || inSlot.stackSize <= 0) continue;
			int take = 1 + fool.rng().nextInt(MAX_PER_SLOT);
			ItemStack stolen = chest.removeItem(slot, Math.min(take, inSlot.stackSize));
			if (stolen != null && stolen.stackSize > 0) {
				fool.pocket(stolen);
				tookAnything = true;
			}
		}
		if (tookAnything) {
			chest.setChanged();
		}
	}

	@Override
	public String name() {
		return "rifle a chest";
	}
}
