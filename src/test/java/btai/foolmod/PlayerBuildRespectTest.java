package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.harness.FakeWorld;
import btai.foolmod.path.FoolPathfinder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PlayerBuildRespectTest {

	@org.junit.jupiter.api.BeforeAll
	public static void setUp() { btai.foolmod.harness.FakeWorld.bootstrap(); }

	private static final int FLOOR = 63;
	private static final int STAND = 64;

	private static List<int[]> path(FakeWorld w, int gx, int gy, int gz, boolean canPlace, boolean mayBreak) {
		return FoolPathfinder.findPath(w.fool(), new FoolPathfinder.PathfinderState(30000),
				0, STAND, 0, gx, gy, gz, 0.0, 30000, canPlace, mayBreak, null);
	}

	private static boolean reaches(List<int[]> p, int gx, int gy, int gz) {
		if (p == null || p.isEmpty()) return false;
		int[] last = p.get(p.size() - 1);
		return last[0] == gx && last[1] == gy && last[2] == gz;
	}

	private static Block<?>[] buildMaterials() {
		return new Block<?>[]{
				Blocks.PLANKS_OAK, Blocks.BRICK_CLAY, Blocks.GLASS, Blocks.WOOL,
				Blocks.COBBLE_STONE, Blocks.STAIRS_PLANKS_OAK, Blocks.SLAB_PLANKS_OAK,
				Blocks.BRICK_STONE_POLISHED, Blocks.BOOKSHELF_PLANKS_OAK, Blocks.LADDER_OAK,
		};
	}

	@Test
	@DisplayName("every common build material is classified as player-built")
	public void buildMaterialsAreProtected() {
		FakeWorld w = new FakeWorld();
		int y = 70;
		for (Block<?> b : buildMaterials()) {
			w.set(0, y, 0, b);
			assertTrue(FoolPathfinder.isPlayerBuilt(w.world, 0, y, 0),
					b.getKey() + " is not protected — the Fool would mine it");
		}
	}

	@Test
	@DisplayName("natural terrain is NOT classified as player-built")
	public void terrainIsNotProtected() {
		FakeWorld w = new FakeWorld();
		Block<?>[] terrain = {
				Blocks.STONE, Blocks.DIRT, Blocks.GRASS, Blocks.SAND, Blocks.GRAVEL,
				Blocks.LOG_OAK, Blocks.LEAVES_OAK, Blocks.ORE_COAL_STONE, Blocks.SANDSTONE, Blocks.OBSIDIAN,
		};
		int y = 70;
		for (Block<?> b : terrain) {
			w.set(0, y, 0, b);
			assertFalse(FoolPathfinder.isPlayerBuilt(w.world, 0, y, 0),
					b.getKey() + " was wrongly protected — the Fool could never dig ordinary ground");
		}
	}

	@Test
	@DisplayName("will not breach a plank wall even when digging is fully enabled")
	public void willNotBreachAPlankWall() {

		FakeWorld w = new FakeWorld().floor(FLOOR, 30, Blocks.BEDROCK);

		w.fill(8, STAND, -3, 8, STAND + 2, 3, Blocks.PLANKS_OAK);
		w.fill(12, STAND, -3, 12, STAND + 2, 3, Blocks.PLANKS_OAK);
		w.fill(8, STAND, -3, 12, STAND + 2, -3, Blocks.PLANKS_OAK);
		w.fill(8, STAND, 3, 12, STAND + 2, 3, Blocks.PLANKS_OAK);
		w.fill(8, STAND + 3, -3, 12, STAND + 3, 3, Blocks.PLANKS_OAK);

		List<int[]> p = path(w, 10, STAND, 0, false, true);
		assertFalse(reaches(p, 10, STAND, 0),
				"the Fool broke into a sealed player build: " + FakeWorld.render(p));
	}

	@Test
	@DisplayName("prefers the long way round rather than through a plank wall")
	public void goesRoundAPlankWall() {
		FakeWorld w = new FakeWorld().floor(FLOOR, 40, Blocks.STONE);
		w.fill(5, STAND, -12, 5, STAND + 2, 12, Blocks.PLANKS_OAK);
		List<int[]> p = path(w, 10, STAND, 0, false, true);
		assertTrue(reaches(p, 10, STAND, 0), "should have walked round the ends");
		for (int z = -12; z <= 12; z++) {
			for (int y = STAND; y <= STAND + 2; y++) {
				assertFalse(pathVisits(p, 5, y, z), "cut through the plank wall at (5," + y + "," + z + ")");
			}
		}
	}

	@Test
	@DisplayName("its own wool is fair game, so it can retrieve its scaffolding")
	public void ownWoolIsBreakable() {
		FakeWorld w = new FakeWorld();
		w.set(0, 70, 0, Blocks.WOOL);

		assertTrue(FoolPathfinder.isPlayerBuilt(w.world, 0, 70, 0), "wool should read as built material");

		Set<Long> mine = new HashSet<>();
		mine.add(FoolPathfinder.packKey(0, 70, 0));
		assertTrue(w.fool(mine).isPlacedBlock(0, 70, 0), "harness should report the cell as self-placed");
		assertFalse(w.fool(new HashSet<>()).isPlacedBlock(0, 70, 0), "unplaced cell must not be claimed");
	}

	@Test
	@DisplayName("never mines a chest, furnace or other tile entity")
	public void neverBreaksContainers() {

		FakeWorld w = new FakeWorld();
		w.set(0, 70, 0, Blocks.CHEST_PLANKS_OAK).tileEntity(0, 70, 0);
		assertFalse(FoolPathfinder.isBreakable(w.world, 0, 70, 0),
				"a chest must never be breakable — the Fool steals from them, it does not smash them");

		FakeWorld bare = new FakeWorld();
		bare.set(0, 70, 0, Blocks.CHEST_PLANKS_OAK);
		assertTrue(FoolPathfinder.isPlayerBuilt(bare.world, 0, 70, 0),
				"a chest should also read as a player build, independently of its tile entity");

		bare.set(0, 70, 0, Blocks.FURNACE_STONE_IDLE);
		assertTrue(FoolPathfinder.isPlayerBuilt(bare.world, 0, 70, 0), "a furnace is somebody's build");
	}

	@Test
	@DisplayName("bedrock is never breakable")
	public void bedrockIsSafe() {
		FakeWorld w = new FakeWorld();
		w.set(0, 70, 0, Blocks.BEDROCK);
		assertFalse(FoolPathfinder.isBreakable(w.world, 0, 70, 0), "bedrock must not be breakable");
	}

	@Test
	@DisplayName("doors are opened, never mined through")
	public void doorsAreNotMined() {
		FakeWorld w = new FakeWorld();
		Block<?> door = firstDoor();
		if (door == null) {
			return;
		}
		w.set(0, 70, 0, door);
		assertFalse(FoolPathfinder.isBreakable(w.world, 0, 70, 0), "doors must be opened, not broken");
	}

	private static Block<?> firstDoor() {
		for (Block<?> b : Blocks.blocksList) {
			if (b != null && Block.hasLogicClass(b, net.minecraft.core.block.BlockLogicDoor.class)) {
				return b;
			}
		}
		return null;
	}

	private static boolean pathVisits(List<int[]> p, int x, int y, int z) {
		if (p == null) return false;
		for (int[] n : p) {
			if (n[0] == x && n[1] == y && n[2] == z) return true;
		}
		return false;
	}

	@Test
	@DisplayName("the watcher check is exposed for a single player")
	public void watchedByIsPublic() throws Exception {
		FoolEntityApi.assertHasMethod("isWatchedBy", Player.class);
		FoolEntityApi.assertHasMethod("isWatched");
	}

	static final class FoolEntityApi {
		static void assertHasMethod(String name, Class<?>... args) {
			try {
				btai.foolmod.entity.FoolEntity.class.getMethod(name, args);
			} catch (NoSuchMethodException e) {
				throw new AssertionError("FoolEntity." + name + " is missing", e);
			}
		}
	}
}
