package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.entity.WatchLatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WatchLatchTest {

	private static final int SPOT = 8;
	private static final int CLEAR = 30;

	private static WatchLatch latch() {
		return new WatchLatch(SPOT, CLEAR);
	}

	private static void feed(WatchLatch l, boolean watched, int ticks) {
		for (int i = 0; i < ticks; i++) {
			l.update(watched);
		}
	}

	@Test
	@DisplayName("a gaze that rests on it counts as being seen")
	void steadyLookSpooksIt() {
		WatchLatch l = latch();
		feed(l, true, SPOT - 1);
		assertFalse(l.believed(), "not yet — a look has to settle before it counts");
		l.update(true);
		assertTrue(l.believed(), "held for the full dwell: seen");
	}

	@Test
	@DisplayName("a camera sweeping past does not count")
	void sweepDoesNotSpookIt() {
		WatchLatch l = latch();
		feed(l, true, SPOT - 2);
		feed(l, false, 1);
		feed(l, true, SPOT - 2);
		assertFalse(l.believed(), "two glances that each fell short are still not a look");
	}

	@Test
	@DisplayName("it stays spooked for a while after the look moves away")
	void staysSpookedBriefly() {
		WatchLatch l = latch();
		feed(l, true, SPOT);
		feed(l, false, CLEAR - 1);
		assertTrue(l.believed(), "one tick short of the all-clear: still believes it is watched");
		l.update(false);
		assertFalse(l.believed(), "left alone long enough: bold again");
	}

	@Test
	@DisplayName("a glance back restarts the whole all-clear")
	void glanceBackResetsTheAllClear() {
		WatchLatch l = latch();
		feed(l, true, SPOT);
		feed(l, false, CLEAR - 2);
		l.update(true);
		feed(l, false, CLEAR - 2);
		assertTrue(l.believed(), "the all-clear starts over from the last glance, not the first");
	}

	@Test
	@DisplayName("a camera swinging about does not strobe it")
	void jitterDoesNotStrobe() {

		WatchLatch l = latch();
		boolean everBelieved = false;
		for (int i = 0; i < 200; i++) {
			l.update(i % 2 == 0);
			everBelieved |= l.believed();
		}
		assertFalse(everBelieved,
				"jitter alone made it believe it had been seen — that is the mob following the cursor");
	}

	@Test
	@DisplayName("jitter after a real sighting does not un-spook it either")
	void jitterDoesNotClearIt() {
		WatchLatch l = latch();
		feed(l, true, SPOT);
		for (int i = 0; i < 200; i++) {
			l.update(i % 2 == 0);
		}
		assertTrue(l.believed(), "it should not get its nerve back while the gaze keeps returning");
	}
}
