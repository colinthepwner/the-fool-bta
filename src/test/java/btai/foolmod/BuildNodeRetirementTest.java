package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.entity.FoolEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BuildNodeRetirementTest {

	@Test
	@DisplayName("a rung is spent once the Fool is standing on top of it")
	void pillarDoneWhenStoodUpon() {
		assertTrue(FoolEntity.pillarNodeDone(true, 65.0, 65),
				"landed at the node's own height: the rung did its job");
	}

	@Test
	@DisplayName("a rung is not spent while the Fool is still at its foot")
	void pillarNotDoneFromBelow() {
		assertFalse(FoolEntity.pillarNodeDone(true, 64.0, 65),
				"a block below the node is where it started, not where it was going");
	}

	@Test
	@DisplayName("passing the height mid-hop does not count")
	void pillarNotDoneInMidAir() {

		assertFalse(FoolEntity.pillarNodeDone(false, 65.2, 65),
				"airborne over the rung is not standing on it");
	}

	@Test
	@DisplayName("landing a whisker low still counts")
	void pillarTolerance() {
		assertTrue(FoolEntity.pillarNodeDone(true, 64.95, 65),
				"floating-point settling must not strand the Fool on its own step");
	}

	@Test
	@DisplayName("a plank is spent once the Fool has walked out onto it")
	void bridgeDoneWhenStoodUpon() {
		assertTrue(FoolEntity.bridgeNodeDone(10.5, 64.0, 20.5, 10, 64, 20),
				"dead centre of the plank at its own height");
	}

	@Test
	@DisplayName("a plank is not spent from the bank")
	void bridgeNotDoneFromTheBank() {
		assertFalse(FoolEntity.bridgeNodeDone(9.5, 64.0, 20.5, 10, 64, 20),
				"a whole block short is still on the near side");
	}

	@Test
	@DisplayName("a plank is not spent from below it")
	void bridgeNotDoneFromBelow() {
		assertFalse(FoolEntity.bridgeNodeDone(10.5, 62.0, 20.5, 10, 64, 20),
				"fallen off the side is not crossed");
	}

	@Test
	@DisplayName("near enough to the middle counts, as it does for a walk node")
	void bridgeTolerance() {
		assertTrue(FoolEntity.bridgeNodeDone(10.2, 64.0, 20.8, 10, 64, 20),
				"the same 0.6 tolerance an ordinary waypoint gets");
	}
}
