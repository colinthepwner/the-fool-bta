package btai.foolmod.block;

import btai.foolmod.FoolMod;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.material.Materials;
import net.minecraft.core.sound.BlockSounds;
import turniplabs.halplibe.helper.BlockBuilder;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryCategory;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryPlacement;

public final class FoolBlocks {

	private static final int JOXE_DUST_ID = 1180;

	public static Block<JoxeDustLogic> joxeDust;

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
		FoolMod.LOGGER.info("Joxe Dust may be laid down.");
	}
}
