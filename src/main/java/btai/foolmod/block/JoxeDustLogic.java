package btai.foolmod.block;

import btai.foolmod.block.FoolBlocks;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;

public class JoxeDustLogic extends BlockLogic {

	private static final double THICKNESS = 1.0 / 16.0;

	public JoxeDustLogic(Block<?> block, Material material) {
		super(block, material);
		setBlockBounds(0.0, 0.0, 0.0, 1.0, THICKNESS, 1.0);
	}

	@Override
	public boolean isCubeShaped() {
		return false;
	}

	@Override
	public org.joml.primitives.AABBdc getCollisionAABB(net.minecraft.core.world.WorldSource source, TilePosc pos) {
		return null;
	}

	public static boolean connectsTo(net.minecraft.core.world.WorldSource source, int x, int y, int z) {
		return FoolBlocks.joxeDust != null
				&& source.getBlockId(x, y, z) == FoolBlocks.joxeDust.id();
	}

	@Override
	public boolean isSolidRender() {
		return false;
	}

	@Override
	public boolean canPlaceAt(World world, TilePosc pos) {
		TilePos below = new TilePos(pos.x(), pos.y() - 1, pos.z());
		return world.isBlockNormalCube(below.x(), below.y(), below.z());
	}

	@Override
	public void onNeighborChanged(World world, TilePosc pos, Block<?> neighbor) {
		if (!canPlaceAt(world, pos)) {
			dropBlockWithCause(world, net.minecraft.core.enums.EnumDropCause.WORLD,
					pos.x(), pos.y(), pos.z(), world.getBlockMetadata(pos.x(), pos.y(), pos.z()), null, null);
			world.setBlockWithNotify(pos.x(), pos.y(), pos.z(), 0);
		}
	}
}
