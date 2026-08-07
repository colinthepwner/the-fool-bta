package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.entity.FoolEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FleeScoringTest {

	private static final double FOOL_X = 10, FOOL_Y = 64, FOOL_Z = 0;
	private static final double PLAYER_X = 0, PLAYER_Z = 0;
	private static final double HEAD_X = 1, HEAD_Z = 0;
	private static final double CUR_DIST = 10;

	private static double score(double candX, double candZ, int radius, boolean hidden, boolean incumbent) {
		return scoreAt(candX, FOOL_Y, candZ, radius, hidden, incumbent);
	}

	private static double scoreAt(double candX, double candY, double candZ, int radius,
			boolean hidden, boolean incumbent) {
		return FoolEntity.coverScore(FOOL_X, FOOL_Y, FOOL_Z, PLAYER_X, PLAYER_Z, HEAD_X, HEAD_Z,
				candX, candY, candZ, CUR_DIST, radius, hidden, incumbent);
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

		double s = FoolEntity.coverScore(FOOL_X, FOOL_Y, FOOL_Z, PLAYER_X, PLAYER_Z, 0, 0,
				2.0, FOOL_Y, 0.0, CUR_DIST, 8, true, false);
		assertEquals(Double.NEGATIVE_INFINITY, s,
				"running toward your pursuer to reach cover is how you get caught");
	}

	@Test
	@DisplayName("a hiding place up a cliff is rejected, however good the cover")
	public void rejectsUnreachableClimbs() {

		assertEquals(Double.NEGATIVE_INFINITY, scoreAt(FOOL_X + 9, FOOL_Y + 8, FOOL_Z, 9, true, false),
				"an eight-block climb needs scaffolding, which means standing still while being chased");
		assertEquals(Double.NEGATIVE_INFINITY, scoreAt(FOOL_X + 9, FOOL_Y - 12, FOOL_Z, 9, true, false),
				"a twelve-block drop is a fall, not an escape route");
	}

	@Test
	@DisplayName("a small step up is still allowed, and level ground still preferred")
	public void mildSlopesAreFine() {
		double level = scoreAt(FOOL_X + 9, FOOL_Y, FOOL_Z, 9, false, false);
		double slight = scoreAt(FOOL_X + 9, FOOL_Y + 2, FOOL_Z, 9, false, false);
		assertTrue(slight > Double.NEGATIVE_INFINITY, "a two-block step should remain reachable");
		assertTrue(level > slight, "with cover equal, flat ground should beat a slope");
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
		double slight = score(FOOL_X + 8, FOOL_Z + 3, 9, false, false);
		assertTrue(straight > slight,
				"straight along the heading should beat even a mild deviation of the same distance");
	}

	@Test
	@DisplayName("cover at right angles is rejected outright — breaking off sideways is what circles")
	public void rejectsSidewaysCover() {

		assertEquals(Double.NEGATIVE_INFINITY, score(FOOL_X, FOOL_Z + 9, 9, true, false),
				"a right-angle turn for cover, repeated, is a circle");
	}
}
