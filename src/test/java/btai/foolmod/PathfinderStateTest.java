package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.path.FoolPathfinder;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PathfinderStateTest {

	@Test
	@DisplayName("the same cell always resolves to the same node")
	public void nodeIdentityIsStable() {
		FoolPathfinder.PathfinderState s = new FoolPathfinder.PathfinderState(1000);
		FoolPathfinder.Node a = s.getNode(4, 64, -9);
		FoolPathfinder.Node b = s.getNode(4, 64, -9);
		assertSame(a, b, "same coordinates handed back different nodes");
		assertNotSame(a, s.getNode(5, 64, -9), "different coordinates shared a node");
	}

	@Test
	@DisplayName("the pool hands back null when spent, rather than overrunning")
	public void poolExhaustionReturnsNull() {
		FoolPathfinder.PathfinderState s = new FoolPathfinder.PathfinderState(16);
		for (int i = 0; i < 16; i++) {
			assertNotNull(s.getNode(i, 64, 0), "node " + i + " should still come from the pool");
		}
		assertNull(s.getNode(999, 64, 0), "a spent pool must return null, not throw or wrap around");
	}

	@Test
	@DisplayName("reset makes the whole pool available again")
	public void resetReusesThePool() {
		FoolPathfinder.PathfinderState s = new FoolPathfinder.PathfinderState(8);
		for (int i = 0; i < 8; i++) s.getNode(i, 64, 0);
		assertNull(s.getNode(100, 64, 0));
		s.reset();
		assertNotNull(s.getNode(100, 64, 0), "reset should have freed the pool");
	}

	@Test
	@DisplayName("packKey is collision-free across a realistic world volume")
	public void packKeyIsInjective() {
		Set<Long> seen = new HashSet<>();
		int collisions = 0;

		for (int x = -600; x <= 600; x += 7) {
			for (int z = -600; z <= 600; z += 7) {
				for (int y = 0; y < 256; y += 11) {
					if (!seen.add(FoolPathfinder.packKey(x, y, z))) {
						collisions++;
					}
				}
			}
		}
		assertEquals(0, collisions, collisions + " coordinate collisions in packKey");
		assertTrue(seen.size() > 100000, "test did not cover a meaningful volume: " + seen.size());
	}

	@Test
	@DisplayName("packKey separates neighbours on every axis")
	public void packKeySeparatesNeighbours() {
		long base = FoolPathfinder.packKey(10, 64, 10);
		assertTrue(base != FoolPathfinder.packKey(11, 64, 10), "x neighbour collided");
		assertTrue(base != FoolPathfinder.packKey(10, 65, 10), "y neighbour collided");
		assertTrue(base != FoolPathfinder.packKey(10, 64, 11), "z neighbour collided");
		assertTrue(base != FoolPathfinder.packKey(-10, 64, 10), "sign flip collided");
	}
}
