package btai.foolmod.act;

import btai.foolmod.entity.FoolEntity;
import btai.foolmod.path.FoolPathfinder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.util.helper.MathHelper;

public class WoolBuildAct implements FoolAct {

	private static final int TRAVEL_TIMEOUT = 300;
	private static final int PLACE_GAP = 7;
	private static final int MAX_BLOCKS = 14;

	private final List<int[]> plan = new ArrayList<>();
	private int cursor;
	private int gap;
	private int travelTicks;

	@Override
	public boolean seek(FoolEntity fool) {

		int[] base = fool.randomNearbyGround(4 + fool.rng().nextInt(6));
		if (base == null) {
			return false;
		}
		switch (fool.rng().nextInt(3)) {
			case 0 -> planTotem(fool, base);
			case 1 -> planArch(fool, base);
			default -> planWall(fool, base);
		}
		prunePlan(fool);
		return plan.size() >= 3;
	}

	private void planTotem(FoolEntity fool, int[] b) {
		int h = 3 + fool.rng().nextInt(3);
		for (int i = 0; i < h; i++) {
			plan.add(new int[]{b[0], b[1] + i, b[2]});
		}
		int top = b[1] + h - 1;
		plan.add(new int[]{b[0] + 1, top, b[2]});
		plan.add(new int[]{b[0] - 1, top, b[2]});
		plan.add(new int[]{b[0], top, b[2] + 1});
		plan.add(new int[]{b[0], top, b[2] - 1});
	}

	private void planArch(FoolEntity fool, int[] b) {
		boolean alongX = fool.rng().nextBoolean();
		int dx = alongX ? 1 : 0;
		int dz = alongX ? 0 : 1;
		int h = 3;
		for (int i = 0; i < h; i++) {
			plan.add(new int[]{b[0] - dx, b[1] + i, b[2] - dz});
			plan.add(new int[]{b[0] + dx, b[1] + i, b[2] + dz});
		}
		int top = b[1] + h;
		plan.add(new int[]{b[0] - dx, top, b[2] - dz});
		plan.add(new int[]{b[0], top, b[2]});
		plan.add(new int[]{b[0] + dx, top, b[2] + dz});
	}

	private void planWall(FoolEntity fool, int[] b) {
		boolean alongX = fool.rng().nextBoolean();
		int len = 3 + fool.rng().nextInt(2);
		int h = 2 + fool.rng().nextInt(2);
		for (int i = 0; i < len; i++) {
			for (int j = 0; j < h; j++) {
				int x = b[0] + (alongX ? i : 0);
				int z = b[2] + (alongX ? 0 : i);
				plan.add(new int[]{x, b[1] + j, z});
			}
		}
	}

	private void prunePlan(FoolEntity fool) {
		plan.removeIf(c -> !FoolPathfinder.chunkLoaded(fool.world, c[0], c[2])
				|| FoolPathfinder.blocksMotion(fool.world, c[0], c[1], c[2]));
		while (plan.size() > MAX_BLOCKS) {
			plan.remove(plan.size() - 1);
		}

		plan.sort((a, b) -> Integer.compare(a[1], b[1]));
	}

	@Override
	public boolean tick(FoolEntity fool) {
		if (gap > 0) {
			gap--;
			return false;
		}
		if (cursor >= plan.size()) {
			return true;
		}
		int[] cell = plan.get(cursor);

		if (FoolPathfinder.blocksMotion(fool.world, cell[0], cell[1], cell[2])) {
			cursor++;
			return false;
		}
		if (!fool.withinReach(cell[0], cell[1], cell[2])) {
			if (++travelTicks > TRAVEL_TIMEOUT) {
				return true;
			}
			if (!fool.navigateTo(cell[0], cell[1], cell[2], 2.0)) {
				cursor++;
			}
			return false;
		}
		fool.stopMoving();
		if (fool.placeWool(cell[0], cell[1], cell[2])) {
			gap = PLACE_GAP;
		}
		cursor++;
		travelTicks = 0;
		return false;
	}

	@Override
	public String name() {
		return "put up a folly";
	}
}
