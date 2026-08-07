package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.act.BlockDoorAct;
import btai.foolmod.act.BreakTorchAct;
import btai.foolmod.act.StealChestAct;
import btai.foolmod.harness.FakeWorld;
import btai.foolmod.path.FoolPathfinder;
import java.util.Set;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicDoor;
import net.minecraft.core.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ActTargetingTest {

	@org.junit.jupiter.api.BeforeAll
	public static void setUp() { btai.foolmod.harness.FakeWorld.bootstrap(); }

	@Test
	@DisplayName("torch targeting finds the real torches and nothing absurd")
	public void torchIds() {
		Set<Integer> ids = BreakTorchAct.torchIds();
		assertFalse(ids.isEmpty(), "no torches resolved — the Fool would never snuff anything");
		assertTrue(ids.contains(Blocks.TORCH_COAL.id()), "the ordinary coal torch must be a target");
		assertFalse(ids.contains(Blocks.STONE.id()), "stone must not be treated as a torch");
		assertFalse(ids.contains(Blocks.PLANKS_OAK.id()), "planks must not be treated as a torch");
	}

	@Test
	@DisplayName("a torch never blocks movement, so it is never a pathing obstacle")
	public void torchesAreNotObstacles() {
		FakeWorld w = new FakeWorld();
		w.set(0, 70, 0, Blocks.TORCH_COAL);
		assertFalse(FoolPathfinder.blocksMotion(w.world, 0, 70, 0),
				"a torch must be walk-through, or the planner would try to route around every one");
	}

	@Test
	@DisplayName("chest targeting finds real containers")
	public void chestIds() {
		Set<Integer> ids = StealChestAct.chestIds();
		assertFalse(ids.isEmpty(), "no chests resolved — the Fool could never steal");
		assertTrue(ids.contains(Blocks.CHEST_PLANKS_OAK.id()), "the plain oak chest must be a target");
		assertFalse(ids.contains(Blocks.STONE.id()), "stone must not be treated as a chest");
	}

	@Test
	@DisplayName("door targeting finds real doors and excludes gates and trapdoors")
	public void doorIds() {
		Set<Integer> ids = BlockDoorAct.doorIds();
		assertFalse(ids.isEmpty(), "no doors resolved — the Fool could never wall one up");
		for (int id : ids) {
			Block<?> b = Blocks.blocksList[id];
			assertTrue(Block.hasLogicClass(b, BlockLogicDoor.class),
					b.getKey() + " is in the door set but is not a door");
		}
		assertFalse(ids.contains(Blocks.STONE.id()), "stone must not be treated as a door");
	}

	@Test
	@DisplayName("wool is the block the Fool builds with, and it carries colour in metadata")
	public void woolIsMetadataColoured() {

		assertTrue(Blocks.WOOL.id() > 0, "no wool block");
		assertTrue(net.minecraft.core.util.helper.DyeColor.RED.blockMeta
						!= net.minecraft.core.util.helper.DyeColor.BLUE.blockMeta,
				"red and blue must be distinct metadata values");
	}

	@Test
	@DisplayName("fences are walls, never footing")
	public void fencesAreWalls() {
		FakeWorld w = new FakeWorld();
		Block<?> fence = null;
		for (Block<?> b : Blocks.blocksList) {
			if (b != null && Block.hasLogicClass(b, net.minecraft.core.block.BlockLogicFence.class)) {
				fence = b;
				break;
			}
		}
		if (fence == null) {
			return;
		}
		w.set(0, 69, 0, fence);
		assertTrue(FoolPathfinder.isFenceLike(w.world, 0, 69, 0), fence.getKey() + " not detected as a fence");
		assertFalse(FoolPathfinder.isStandable(w.world, 0, 70, 0),
				"a fence must never count as footing — standing on one wedges the body");
	}

	@Test
	@DisplayName("breaking is priced as one action, not by hardness, so tunnelling stays viable")
	public void breakingIsCheapEnoughToChoose() {

		int breakCost = FoolPathfinder.breakCost();
		assertTrue(breakCost > 10, "a break should cost a little more than a plain walk step: " + breakCost);
		assertTrue(breakCost < 30, "a break must stay within a few walk steps or it is never chosen: " + breakCost);
	}
}
