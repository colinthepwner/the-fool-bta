package btai.foolmod.block;

import btai.foolmod.FoolMod;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.material.Materials;
import net.minecraft.core.sound.BlockSounds;
import turniplabs.halplibe.helper.BlockBuilder;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryCategory;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryPlacement;

public final class FoolBlocks {

	private static final int JOXE_DUST_ID = 1180;
	private static final int FOOLS_GOLD_BLOCK_ID = 1181;

	public static Block<JoxeDustLogic> joxeDust;
	public static Block<BlockLogic> foolsGoldBlock;

	private FoolBlocks() {
	}

	public static void register() {
		joxeDust = new BlockBuilder(FoolMod.MOD_ID)
				.setBlockSound(BlockSounds.SAND)
				.setHardness(0.0f)
				.setLuminance(3)
				.setCreativeInventoryPlacement(new CreativeInventoryPlacement.Category(CreativeInventoryCategory.DROPS))

				.build("joxe_dust", "joxe_dust", JOXE_DUST_ID,
						b -> new JoxeDustLogic(b, Materials.DECORATION));

		foolsGoldBlock = new BlockBuilder(FoolMod.MOD_ID)
				.setBlockSound(BlockSounds.METAL)
				.setHardness(3.0f)
				.setResistance(10.0f)
				.setCreativeInventoryPlacement(

						new CreativeInventoryPlacement.Category(CreativeInventoryCategory.ORE_PRODUCTS))
				.build("fools_gold_block", "fools_gold_block", FOOLS_GOLD_BLOCK_ID,
						b -> new BlockLogic(b, Materials.METAL));
		FoolMod.LOGGER.info("Joxe Dust may be laid down.");
	}
}
