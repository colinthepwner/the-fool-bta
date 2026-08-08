package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.entity.FoolEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlipBehindTest {

	@Test
	@DisplayName("a point straight behind is behind, whichever way the player faces")
	void directlyAstern() {
		for (double yaw = -360.0; yaw <= 360.0; yaw += 15.0) {
			double[] p = FoolEntity.pointOnBearing(0.0, 0.0, yaw + 180.0, 8.0);
			assertTrue(FoolEntity.isBehind(0.0, 0.0, yaw, p[0], p[1]),
					"dead astern read as visible at yaw " + yaw);
		}
	}

	@Test
	@DisplayName("a point straight ahead is not behind")
	void directlyAhead() {
		for (double yaw = -360.0; yaw <= 360.0; yaw += 15.0) {
			double[] p = FoolEntity.pointOnBearing(0.0, 0.0, yaw, 8.0);
			assertFalse(FoolEntity.isBehind(0.0, 0.0, yaw, p[0], p[1]),
					"the spot in front of them read as behind at yaw " + yaw);
		}
	}

	@Test
	@DisplayName("every spot the fan can pick lands behind them")
	void theWholeFanIsAstern() {

		double half = FoolEntity.SLIP_FAN / 2.0;
		for (double yaw = -180.0; yaw <= 180.0; yaw += 10.0) {
			for (double spread = -half; spread <= half; spread += 5.0) {
				for (double dist = 6.0; dist <= 13.0; dist += 1.0) {
					double[] p = FoolEntity.pointOnBearing(100.5, -60.5, yaw + 180.0 + spread, dist);
					assertTrue(FoolEntity.isBehind(100.5, -60.5, yaw, p[0], p[1]),
							"yaw " + yaw + " spread " + spread + " landed in view");
				}
			}
		}
	}

	@Test
	@DisplayName("abeam does not count as behind")
	void abeamIsVisible() {

		double[] left = FoolEntity.pointOnBearing(0.0, 0.0, 90.0, 8.0);
		double[] right = FoolEntity.pointOnBearing(0.0, 0.0, -90.0, 8.0);
		assertFalse(FoolEntity.isBehind(0.0, 0.0, 0.0, left[0], left[1]), "abeam to port counted as behind");
		assertFalse(FoolEntity.isBehind(0.0, 0.0, 0.0, right[0], right[1]), "abeam to starboard counted as behind");
	}

	@Test
	@DisplayName("standing on the player is not behind them")
	void onTopOfThem() {
		assertFalse(FoolEntity.isBehind(10.0, 10.0, 45.0, 10.0, 10.0),
				"a zero-length bearing has no direction to judge");
	}
}
