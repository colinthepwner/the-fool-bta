package btai.foolmod.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.harness.FakeWorld;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JoxeWardTest {

	private static final int DUST = 1179;

	private static final int GROUND = 63;
	private static final int FEET = 64;

	static {
		FakeWorld.bootstrap();
	}

	private static FakeWorld outdoors() {
		FakeWorld w = new FakeWorld();
		w.floor(GROUND, 48, Blocks.STONE);
		return w;
	}

	private static void ring(FakeWorld w, int r) {
		for (int i = -r; i <= r; i++) {
			dust(w, i, -r);
			dust(w, i, r);
			dust(w, -r, i);
			dust(w, r, i);
		}
	}

	private static void dust(FakeWorld w, int x, int z) {
		dust(w, x, FEET, z);
	}

	private static void dust(FakeWorld w, int x, int y, int z) {
		w.setId(x, y, z, DUST);
	}

	private static boolean warded(FakeWorld w, int x, int y, int z) {
		return JoxeWard.isWarded(w.world, x, y, z, DUST);
	}

	@Test
	@DisplayName("a ring drawn on open ground wards what is inside it")
	void ringOnOpenGround() {
		FakeWorld w = outdoors();
		ring(w, 5);
		assertTrue(warded(w, 0, FEET, 0), "the middle of a closed ring is inside the ward");
		assertTrue(warded(w, 4, FEET, 4), "so is a corner of it, right up against the dust");
	}

	@Test
	@DisplayName("outside the same ring is not warded")
	void outsideTheRing() {
		FakeWorld w = outdoors();
		ring(w, 5);
		assertFalse(warded(w, 12, FEET, 0), "open ground beyond the ring is nobody's ward");
	}

	@Test
	@DisplayName("a ring with one pinch missing wards nothing")
	void brokenRing() {
		FakeWorld w = outdoors();
		ring(w, 5);
		w.setId(0, FEET, -5, 0);
		assertFalse(warded(w, 0, FEET, 0), "a boundary with a gap in it is not a boundary");
	}

	@Test
	@DisplayName("being inside is a matter of where it stands, not how it arrived")
	void insideIsInsideHowever() {
		FakeWorld w = outdoors();

		ring(w, 4);
		assertTrue(warded(w, 0, FEET, 0), "a ward closed around something still holds it");
	}

	@Test
	@DisplayName("a line of dust across the doorway wards the room behind it")
	void dustAcrossADoorway() {
		FakeWorld w = outdoors();

		for (int i = -3; i <= 3; i++) {
			w.set(i, FEET, -3, Blocks.PLANKS_OAK);
			w.set(-3, FEET, i, Blocks.PLANKS_OAK);
			w.set(3, FEET, i, Blocks.PLANKS_OAK);
			if (i != 0) {
				w.set(i, FEET, 3, Blocks.PLANKS_OAK);
			}
		}
		assertFalse(warded(w, 0, FEET, 0), "an open door is a hole in it, dust or no dust");
		dust(w, 0, 3);
		assertTrue(warded(w, 0, FEET, 0), "walls plus dust is still a closed boundary");
	}

	@Test
	@DisplayName("a sealed room with no dust in it is not a ward")
	void sealedRoomWithoutDust() {
		FakeWorld w = outdoors();
		for (int i = -3; i <= 3; i++) {
			w.set(i, FEET, -3, Blocks.PLANKS_OAK);
			w.set(i, FEET, 3, Blocks.PLANKS_OAK);
			w.set(-3, FEET, i, Blocks.PLANKS_OAK);
			w.set(3, FEET, i, Blocks.PLANKS_OAK);
		}
		assertFalse(warded(w, 0, FEET, 0), "every cellar in the world would qualify otherwise");
	}

	@Test
	@DisplayName("a ring that steps up a slope still closes")
	void ringOverAStep() {
		FakeWorld w = outdoors();
		ring(w, 5);

		for (int i = -5; i <= 5; i++) {
			w.set(5, FEET, i, Blocks.STONE);
			dust(w, 5, FEET + 1, i);
		}
		assertTrue(warded(w, 0, FEET, 0), "a step up is how the dust joins on -- it is one line");
	}

	@Test
	@DisplayName("an area too vast to be a ward is left alone")
	void tooBigToWard() {
		FakeWorld w = new FakeWorld();
		w.floor(GROUND, 200, Blocks.STONE);
		ring(w, 60);
		assertFalse(warded(w, 0, FEET, 0), "the flood must bail rather than crawl a whole world");
	}

	@Test
	@DisplayName("terrain that has not loaded cannot be judged")
	void unloadedTerrain() {
		FakeWorld w = outdoors();
		ring(w, 5);
		w.unloadChunkAt(3, 3);
		assertFalse(warded(w, 0, FEET, 0), "an unread chunk might hold the gap");
	}
}
