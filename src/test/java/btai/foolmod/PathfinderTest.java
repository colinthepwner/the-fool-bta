package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.harness.FakeWorld;
import btai.foolmod.path.FoolPathfinder;
import java.util.List;
import net.minecraft.core.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PathfinderTest {

	@org.junit.jupiter.api.BeforeAll
	public static void setUp() { btai.foolmod.harness.FakeWorld.bootstrap(); }

	private static final int FLOOR = 63;
	private static final int STAND = 64;

	private static List<int[]> path(FakeWorld w, int sx, int sy, int sz, int gx, int gy, int gz,
			boolean canPlace, boolean mayBreak) {
		return FoolPathfinder.findPath(w.fool(), new FoolPathfinder.PathfinderState(20000),
				sx, sy, sz, gx, gy, gz, 0.0, 20000, canPlace, mayBreak, null);
	}

	private static boolean reaches(List<int[]> path, int gx, int gy, int gz) {
		if (path == null || path.isEmpty()) {
			return false;
		}
		int[] last = path.get(path.size() - 1);
		return last[0] == gx && last[1] == gy && last[2] == gz;
	}

	private static boolean visits(List<int[]> path, int x, int y, int z) {
		if (path == null) return false;
		for (int[] n : path) {
			if (n[0] == x && n[1] == y && n[2] == z) return true;
		}
		return false;
	}

	private static boolean hasAction(List<int[]> path, int action) {
		if (path == null) return false;
		for (int[] n : path) {
			if (n[3] == action) return true;
		}
		return false;
	}

	@Test
	@DisplayName("walks a straight line across flat ground")
	public void flatGround() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 30, Blocks.STONE);
		List<int[]> p = path(w, 0, STAND, 0, 10, STAND, 0, false, false);
		assertNotNull(p, "no path over flat ground");
		assertTrue(reaches(p, 10, STAND, 0), "did not arrive: " + FakeWorld.render(p));
		FakeWorld.assertPathContiguous(p);
	}

	@Test
	@DisplayName("routes around a natural wall when digging is not allowed")
	public void goesAroundWhenItCannotDig() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 30, Blocks.STONE);
		w.fill(5, STAND, -4, 5, STAND + 1, 4, Blocks.STONE);
		List<int[]> p = path(w, 0, STAND, 0, 10, STAND, 0, false, false);
		assertTrue(reaches(p, 10, STAND, 0), "should have gone round the ends");
		for (int z = -4; z <= 4; z++) {
			assertFalse(visits(p, 5, STAND, z), "walked through the wall at z=" + z);
		}
	}

	@Test
	@DisplayName("digs through natural stone when there is no way round")
	public void digsThroughTerrain() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 30, Blocks.STONE);
		w.fill(5, STAND, -30, 5, STAND + 1, 30, Blocks.STONE);
		List<int[]> dig = path(w, 0, STAND, 0, 10, STAND, 0, false, true);
		assertTrue(reaches(dig, 10, STAND, 0), "should have tunnelled through plain stone");

		List<int[]> noDig = path(w, 0, STAND, 0, 10, STAND, 0, false, false);
		assertFalse(reaches(noDig, 10, STAND, 0), "must not get through with digging disabled");
	}

	@Test
	@DisplayName("bridges over water rather than stopping at the bank")
	public void bridgesWater() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 30, Blocks.STONE);

		w.fill(4, FLOOR, -30, 7, FLOOR, 30, Blocks.FLUID_WATER_STILL);
		List<int[]> p = path(w, 0, STAND, 0, 12, STAND, 0, true, false);
		assertTrue(reaches(p, 12, STAND, 0), "should have crossed: " + FakeWorld.render(p));
		assertTrue(hasAction(p, FoolPathfinder.BRIDGE), "expected BRIDGE moves: " + FakeWorld.render(p));
	}

	@Test
	@DisplayName("pillars up to a ledge it cannot step onto")
	public void pillarsUp() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 30, Blocks.STONE);

		w.fill(6, FLOOR + 4, -10, 20, FLOOR + 4, 10, Blocks.STONE);
		List<int[]> p = path(w, 0, STAND, 0, 10, FLOOR + 5, 0, true, false);
		assertTrue(reaches(p, 10, FLOOR + 5, 0), "should have climbed: " + FakeWorld.render(p));
		assertTrue(hasAction(p, FoolPathfinder.PILLAR), "expected PILLAR moves: " + FakeWorld.render(p));
	}

	@Test
	@DisplayName("treats an unloaded chunk as a wall, not as open air")
	public void unloadedChunkIsAWall() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 60, Blocks.STONE);

		for (int x = 16; x < 48; x += 16) {
			w.unloadChunkAt(x, 0);
		}
		List<int[]> p = path(w, 0, STAND, 0, 40, STAND, 0, false, false);
		assertFalse(reaches(p, 40, STAND, 0), "planned a route through unloaded (phantom-air) terrain");
	}

	@Test
	@DisplayName("never routes a body through lava")
	public void avoidsLava() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 30, Blocks.STONE);
		w.fill(5, STAND, -6, 5, STAND, 6, Blocks.FLUID_LAVA_STILL);
		w.set(5, STAND, 7, null);
		List<int[]> p = path(w, 0, STAND, 0, 10, STAND, 0, false, true);
		assertTrue(reaches(p, 10, STAND, 0), "should have found the way round the lava");
		for (int z = -6; z <= 6; z++) {
			assertFalse(visits(p, 5, STAND, z), "routed into lava at z=" + z);
		}
	}

	@Test
	@DisplayName("survives node-pool exhaustion instead of crashing")
	public void poolExhaustionIsSafe() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 60, Blocks.STONE);

		FoolPathfinder.PathfinderState tiny = new FoolPathfinder.PathfinderState(48);
		List<int[]> p = FoolPathfinder.findPath(w.fool(), tiny, 0, STAND, 0, 200, STAND, 200,
				0.0, 100000, true, true, null);
		assertFalse(reaches(p, 200, STAND, 200), "cannot possibly have reached with a 48-node pool");
		if (p != null) {
			FakeWorld.assertPathContiguous(p);
		}
	}

	@Test
	@DisplayName("accepts arriving within the accept radius")
	public void acceptRadius() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 30, Blocks.STONE);
		List<int[]> p = FoolPathfinder.findPath(w.fool(), new FoolPathfinder.PathfinderState(20000),
				0, STAND, 0, 10, STAND, 0, 3.0, 20000, false, false, null);
		assertNotNull(p);
		int[] last = p.get(p.size() - 1);
		double d = Math.sqrt(Math.pow(last[0] - 10, 2) + Math.pow(last[2] - 0, 2));
		assertTrue(d <= 3.0 + 1e-6, "stopped " + d + " away, outside the 3.0 accept radius");
	}

	@Test
	@DisplayName("steps up a single block without needing to place anything")
	public void stepsUpOne() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 30, Blocks.STONE);
		w.fill(5, FLOOR + 1, -10, 20, FLOOR + 1, 10, Blocks.STONE);
		List<int[]> p = path(w, 0, STAND, 0, 10, FLOOR + 2, 0, false, false);
		assertTrue(reaches(p, 10, FLOOR + 2, 0), "should have stepped up: " + FakeWorld.render(p));
		assertFalse(hasAction(p, FoolPathfinder.PILLAR), "should not need to pillar for a one-block step");
	}
}
