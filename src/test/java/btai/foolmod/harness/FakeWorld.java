package btai.foolmod.harness;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import btai.foolmod.entity.FoolEntity;
import btai.foolmod.path.FoolPathfinder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.block.Block;
import net.minecraft.core.world.World;

public final class FakeWorld {

	static {
		bootstrap();
	}

	public static void bootstrap() {
		net.minecraft.core.block.Blocks.init();
	}

	public final World world;
	private final Map<Long, Integer> blocks = new HashMap<>();
	private final Set<Long> unloadedChunks = new HashSet<>();
	private final Map<Long, net.minecraft.core.block.entity.TileEntity> tiles = new HashMap<>();

	public FakeWorld() {

		world = mock(World.class, withSettings().stubOnly().lenient());
		when(world.getBlockId(anyInt(), anyInt(), anyInt())).thenAnswer(inv ->
				blocks.getOrDefault(key(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)), 0));
		when(world.getBlockMetadata(anyInt(), anyInt(), anyInt())).thenReturn(0);
		when(world.getTileEntity(anyInt(), anyInt(), anyInt())).thenAnswer(inv ->
				tiles.get(key(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2))));
		when(world.isChunkLoaded(anyInt(), anyInt())).thenAnswer(inv -> {
			int cx = inv.getArgument(0), cz = inv.getArgument(1);
			return !unloadedChunks.contains(chunkKey(cx, cz));
		});

		when(world.setBlockWithNotify(anyInt(), anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
			setId(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2), inv.getArgument(3));
			return true;
		});
	}

	private static long key(int x, int y, int z) {
		return FoolPathfinder.packKey(x, y, z);
	}

	private static long chunkKey(int cx, int cz) {
		return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
	}

	public FakeWorld set(int x, int y, int z, Block<?> block) {
		if (block == null) {
			blocks.remove(key(x, y, z));
		} else {
			blocks.put(key(x, y, z), block.id());
		}
		return this;
	}

	public FakeWorld setId(int x, int y, int z, int id) {
		if (id == 0) {
			blocks.remove(key(x, y, z));
		} else {
			blocks.put(key(x, y, z), id);
		}
		return this;
	}

	public FakeWorld fill(int x0, int y0, int z0, int x1, int y1, int z1, Block<?> block) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					set(x, y, z, block);
				}
			}
		}
		return this;
	}

	public FakeWorld floor(int y, int r, Block<?> block) {
		return fill(-r, y, -r, r, y, r, block);
	}

	public FakeWorld tileEntity(int x, int y, int z) {
		tiles.put(key(x, y, z), mock(net.minecraft.core.block.entity.TileEntity.class,
				withSettings().stubOnly().lenient()));
		return this;
	}

	public FakeWorld unloadChunkAt(int x, int z) {
		unloadedChunks.add(chunkKey(x >> 4, z >> 4));
		return this;
	}

	public int idAt(int x, int y, int z) {
		return blocks.getOrDefault(key(x, y, z), 0);
	}

	public boolean isAir(int x, int y, int z) {
		return idAt(x, y, z) == 0;
	}

	public FoolEntity fool() {
		return fool(new HashSet<>());
	}

	public FoolEntity fool(Set<Long> placedCells) {
		FoolEntity f = mock(FoolEntity.class, withSettings().stubOnly().lenient());
		f.world = world;
		when(f.isPlacedBlock(anyInt(), anyInt(), anyInt())).thenAnswer(inv ->
				placedCells.contains(key(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2))));
		return f;
	}

	public static void assertPathContiguous(java.util.List<int[]> path) {
		for (int i = 1; i < path.size(); i++) {
			int[] a = path.get(i - 1), b = path.get(i);
			int dx = Math.abs(b[0] - a[0]), dy = Math.abs(b[1] - a[1]), dz = Math.abs(b[2] - a[2]);

			boolean sane = (dy == 0) || (dx <= 1 && dz <= 1 && dy <= 4);
			if (!sane) {
				throw new AssertionError("non-contiguous step " + fmt(a) + " -> " + fmt(b));
			}
		}
	}

	public static String fmt(int[] n) {
		return "(" + n[0] + "," + n[1] + "," + n[2] + ")a" + n[3];
	}

	public static String render(java.util.List<int[]> path) {
		StringBuilder sb = new StringBuilder();
		for (int[] n : path) sb.append(fmt(n)).append(' ');
		return sb.toString().trim();
	}
}
