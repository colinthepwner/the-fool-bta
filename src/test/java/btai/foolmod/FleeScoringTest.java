package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.entity.FoolEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FleeScoringTest {

	private static final double FOOL_X = 10, FOOL_Z = 0;
	private static final double PLAYER_X = 0, PLAYER_Z = 0;
	private static final double HEAD_X = 1, HEAD_Z = 0;
	private static final double CUR_DIST = 10;

	private static double score(double candX, double candZ, int radius, boolean hidden, boolean incumbent) {
		return FoolEntity.coverScore(FOOL_X, FOOL_Z, PLAYER_X, PLAYER_Z, HEAD_X, HEAD_Z,
				candX, candZ, CUR_DIST, radius, hidden, incumbent);
	}

	@Test
	@DisplayName("a spot behind the flee heading is rejected outright — it can never turn around")
	public void rejectsReversing() {

		assertEquals(Double.NEGATIVE_INFINITY, score(FOOL_X - 9, FOOL_Z, 9, true, false),
				"a candidate behind the heading must be rejected even when it offers perfect cover");

		assertEquals(Double.NEGATIVE_INFINITY, score(FOOL_X - 9, FOOL_Z, 9, true, true));
	}

	@Test
	@DisplayName("a spot that gives ground back to the pursuer is rejected")
	public void rejectsBacksliding() {

		double s = FoolEntity.coverScore(FOOL_X, FOOL_Z, PLAYER_X, PLAYER_Z, 0, 0,
				2.0, 0.0, CUR_DIST, 8, true, false);
		assertEquals(Double.NEGATIVE_INFINITY, s,
				"running toward your pursuer to reach cover is how you get caught");
	}

	@Test
	@DisplayName("cover beats open ground, decisively")
	public void coverBeatsDistance() {
		double hiddenClose = score(FOOL_X + 5, FOOL_Z, 5, true, false);
		double openFar = score(FOOL_X + 17, FOOL_Z, 17, false, false);
		assertTrue(hiddenClose > openFar,
				"a hiding place five blocks away should beat open ground seventeen away — only cover "
						+ "lets it vanish (hidden=" + hiddenClose + " open=" + openFar + ")");
	}

	@Test
	@DisplayName("with cover equal, further from the player wins")
	public void furtherWinsAllElseEqual() {
		assertTrue(score(FOOL_X + 13, FOOL_Z, 13, false, false) > score(FOOL_X + 5, FOOL_Z, 5, false, false),
				"on open ground it should still put distance between them");
	}

	@Test
	@DisplayName("the incumbent target survives a marginally better rival — the anti-swerve rule")
	public void incumbentResistsMarginalRivals() {

		double incumbent = score(FOOL_X + 9, FOOL_Z, 9, false, true);
		double rivalSlightlyBetter = score(FOOL_X + 8.5, FOOL_Z + 3, 9, false, false);
		assertTrue(incumbent > rivalSlightlyBetter,
				"a marginally better rival must not pull the Fool off its committed line — that swerve, "
						+ "repeated, is the circling bug");
	}

	@Test
	@DisplayName("but a genuinely better target (real cover) does win out")
	public void realCoverStillOverridesHysteresis() {
		double incumbentOpen = score(FOOL_X + 9, FOOL_Z, 9, false, true);
		double rivalHidden = score(FOOL_X + 7, FOOL_Z + 5, 9, true, false);
		assertTrue(rivalHidden > incumbentOpen,
				"hysteresis must not be so strong that actual cover is ignored");
	}

	@Test
	@DisplayName("alignment with the heading is rewarded, so flight stays roughly straight")
	public void prefersStraightAhead() {
		double straight = score(FOOL_X + 9, FOOL_Z, 9, false, false);
		double sideways = score(FOOL_X, FOOL_Z + 9, 9, false, false);
		assertTrue(straight > sideways,
				"straight along the heading should beat a right-angle turn of the same distance");
	}
}
