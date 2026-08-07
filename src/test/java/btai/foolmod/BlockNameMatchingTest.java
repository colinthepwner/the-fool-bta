package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.harness.FakeWorld;
import btai.foolmod.path.FoolPathfinder;
import btai.foolmod.util.BlockNames;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BlockNameMatchingTest {

	@BeforeAll
	public static void setUp() {
		FakeWorld.bootstrap();
	}

	@Test
	@DisplayName("BlockNames.of yields the clean underscore name, never the tile.* translation key")
	public void cleanNames() {
		assertEquals("stone", BlockNames.of(Blocks.STONE));
		assertEquals("planks_oak", BlockNames.of(Blocks.PLANKS_OAK));
		assertEquals("stairs_planks_oak", BlockNames.of(Blocks.STAIRS_PLANKS_OAK));
		assertEquals("bedrock", BlockNames.of(Blocks.BEDROCK));
		for (Block<?> b : Blocks.blocksList) {
			if (b == null) continue;
			assertFalse(BlockNames.of(b).startsWith("tile."),
					"BlockNames leaked a translation key: " + BlockNames.of(b));
		}
	}

	@Test
	@DisplayName("token matching respects word boundaries: bed is not bedrock")
	public void tokensRespectWordBoundaries() {
		assertTrue(BlockNames.hasToken(Blocks.BEDROCK, "bedrock"), "bedrock should match its own name");
		assertFalse(BlockNames.hasToken(Blocks.BEDROCK, "bed"),
				"'bed' must not match 'bedrock' — that is the substring trap this class exists to avoid");
		assertTrue(BlockNames.hasToken(Blocks.PLANKS_OAK, "planks"));
		assertTrue(BlockNames.hasToken(Blocks.PLANKS_OAK, "oak"));
		assertFalse(BlockNames.hasToken(Blocks.PLANKS_OAK, "plank"), "partial words must not match");
		assertFalse(BlockNames.hasToken(Blocks.STONE, "ston"), "partial words must not match");
	}

	@Test
	@DisplayName("the classifier does NOT protect everything — the original bug, pinned")
	public void doesNotProtectEverything() {
		FakeWorld w = new FakeWorld();
		int protectedCount = 0, total = 0;
		for (Block<?> b : Blocks.blocksList) {
			if (b == null) continue;
			total++;
			w.set(0, 70, 0, b);
			if (FoolPathfinder.isPlayerBuilt(w.world, 0, 70, 0)) protectedCount++;
		}
		assertTrue(protectedCount < total,
				"EVERY block is classified player-built (" + protectedCount + "/" + total
						+ ") — terrain breaking is dead, which is exactly the original bug");

		assertTrue(protectedCount > 20, "suspiciously few protected blocks: " + protectedCount + "/" + total);
		assertTrue(protectedCount < total * 0.8,
				"over 80% of blocks protected (" + protectedCount + "/" + total + ") — the rule is too broad");
		System.out.println("player-built classification: " + protectedCount + " of " + total + " blocks");
	}

	@Test
	@DisplayName("BTA's naturally-occurring rubyglass is not mistaken for crafted glass")
	public void rubyglassIsNotGlass() {
		FakeWorld w = new FakeWorld();
		for (Block<?> b : Blocks.blocksList) {
			if (b == null) continue;
			String n = BlockNames.of(b);
			if (!n.startsWith("rubyglass_")) continue;

			boolean worked = n.contains("brick") || n.contains("block") || n.contains("column");
			w.set(0, 70, 0, b);
			boolean isProtected = FoolPathfinder.isPlayerBuilt(w.world, 0, 70, 0);
			if (!worked) {
				assertFalse(isProtected, n + " is a natural formation but was classified as a player build");
			}
		}
	}
}
