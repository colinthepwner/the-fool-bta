package btai.foolmod.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.harness.FakeWorld;
import java.util.Random;
import net.minecraft.core.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FoolBlockDecayTest {

	static {
		FakeWorld.bootstrap();
	}

	private static int tickUntilGone(FakeWorld w, int x, int y, int z, int limit) {
		for (int t = 1; t <= limit; t++) {
			FoolBlockDecay.tick(w.world);
			if (w.isAir(x, y, z)) {
				return t;
			}
		}
		return -1;
	}

	@Test
	@DisplayName("a placed block goes up in smoke inside three minutes")
	void decaysWithinTheWindow() {
		FakeWorld w = new FakeWorld();
		w.set(4, 70, -9, Blocks.WOOL);
		FoolBlockDecay.mark(w.world, 4, 70, -9, Blocks.WOOL.id(), new Random(1));

		int gone = tickUntilGone(w, 4, 70, -9, FoolBlockDecay.MAX_LIFETIME);
		assertNotEquals(-1, gone, "still standing after three minutes");
		assertTrue(gone >= FoolBlockDecay.MIN_LIFETIME,
				"went at tick " + gone + ", sooner than the floor that keeps scaffolding usable");
	}

	@Test
	@DisplayName("blocks laid together do not all go at once")
	void decaysInRandomIncrements() {
		FakeWorld w = new FakeWorld();
		Random random = new Random(7);
		for (int i = 0; i < 24; i++) {
			w.set(i, 70, 0, Blocks.WOOL);
			FoolBlockDecay.mark(w.world, i, 70, 0, Blocks.WOOL.id(), random);
		}

		int standing = 24;
		int momentsOfLoss = 0;
		for (int t = 0; t < FoolBlockDecay.MAX_LIFETIME; t++) {
			FoolBlockDecay.tick(w.world);
			int now = 0;
			for (int i = 0; i < 24; i++) {
				if (!w.isAir(i, 70, 0)) {
					now++;
				}
			}
			if (now < standing) {
				momentsOfLoss++;
				standing = now;
			}
		}
		assertEquals(0, standing, "everything should be gone by the end of the window");
		assertTrue(momentsOfLoss > 5, "came apart in only " + momentsOfLoss + " moments — that is a batch, not a dribble");
	}

	@Test
	@DisplayName("a cell someone else has changed is left alone")
	void doesNotEatSomebodyElsesBlock() {
		FakeWorld w = new FakeWorld();
		w.set(0, 70, 0, Blocks.WOOL);
		FoolBlockDecay.mark(w.world, 0, 70, 0, Blocks.WOOL.id(), new Random(3));

		w.set(0, 70, 0, Blocks.PLANKS_OAK);

		for (int t = 0; t < FoolBlockDecay.MAX_LIFETIME + 10; t++) {
			FoolBlockDecay.tick(w.world);
		}
		assertEquals(Blocks.PLANKS_OAK.id(), w.idAt(0, 70, 0),
				"decay must never take a block it did not put there");
	}

	@Test
	@DisplayName("the cell it books is the cell it clears, wherever in the world that is")
	void coordinatesSurviveTheRoundTrip() {

		int[][] cells = {
				{0, 0, 0}, {5, 70, 9}, {-5, 70, -9}, {-1, 1, -1},
				{1200, 120, -1200}, {-30000, 4, 30000},
		};
		for (int[] c : cells) {
			FakeWorld w = new FakeWorld();
			w.set(c[0], c[1], c[2], Blocks.WOOL);
			FoolBlockDecay.mark(w.world, c[0], c[1], c[2], Blocks.WOOL.id(), new Random(11));
			int gone = tickUntilGone(w, c[0], c[1], c[2], FoolBlockDecay.MAX_LIFETIME);
			assertNotEquals(-1, gone,
					"never cleared (" + c[0] + "," + c[1] + "," + c[2] + ") — unpacked to the wrong cell");
		}
	}

	@Test
	@DisplayName("the client is not allowed to decay anything")
	void clientSideIsLeftToTheServer() {
		FakeWorld w = new FakeWorld();
		w.set(0, 70, 0, Blocks.WOOL);
		FoolBlockDecay.mark(w.world, 0, 70, 0, Blocks.WOOL.id(), new Random(5));
		w.world.isClientSide = true;
		for (int t = 0; t < FoolBlockDecay.MAX_LIFETIME + 10; t++) {
			FoolBlockDecay.tick(w.world);
		}
		assertEquals(Blocks.WOOL.id(), w.idAt(0, 70, 0), "the server owns the world, not the client");
	}
}
