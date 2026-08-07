package btai.foolmod.client;

import btai.foolmod.FoolMod;
import btai.foolmod.entity.FoolEntity;
import btai.foolmod.item.FoolItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.EntityRendererDispatcher;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import net.minecraft.client.render.block.color.BlockColorDispatcher;
import btai.foolmod.block.FoolBlocks;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.block.model.BlockModelStandard;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.item.Item;
import turniplabs.halplibe.util.ModelEntrypoint;

@Environment(EnvType.CLIENT)
public class FoolModels implements ModelEntrypoint {

	@Override
	public void initEntityModels(EntityRendererDispatcher dispatcher) {

		dispatcher.assignRenderer(FoolEntity.class, new FoolRenderer());
	}

	@Override
	public void initItemModels(ItemModelDispatcher dispatcher) {
		bind(dispatcher, FoolItems.joxeDust, "joxe_dust", false);
		bind(dispatcher, FoolItems.foolsGoldIngot, "fools_gold_ingot", false);
		bind(dispatcher, FoolItems.pickaxe, "fools_gold_pickaxe", true);
		bind(dispatcher, FoolItems.axe, "fools_gold_axe", true);
		bind(dispatcher, FoolItems.shovel, "fools_gold_shovel", true);
		bind(dispatcher, FoolItems.hoe, "fools_gold_hoe", true);
		bind(dispatcher, FoolItems.sword, "fools_gold_sword", true);
		bind(dispatcher, FoolItems.helmet, "fools_gold_helmet", false);
		bind(dispatcher, FoolItems.chestplate, "fools_gold_chestplate", false);
		bind(dispatcher, FoolItems.leggings, "fools_gold_leggings", false);
		bind(dispatcher, FoolItems.boots, "fools_gold_boots", false);
	}

	private static void bind(ItemModelDispatcher dispatcher, Item item, String texture, boolean handheld) {
		if (item == null) {
			return;
		}
		try {

			ItemModelStandard model = new ItemModelStandard(item, false);
			model.icon = TextureRegistry.getTexture(FoolMod.MOD_ID + ":item/" + texture);
			if (handheld) {
				model.setDisplayPos("firstperson_righthand", ItemModelDispatcher.HANDHELD_FIRST_PERSON_RIGHT_HAND)
						.setDisplayPos("firstperson_lefthand", ItemModelDispatcher.HANDHELD_FIRST_PERSON_LEFT_HAND)
						.setDisplayPos("thirdperson_righthand", ItemModelDispatcher.HANDHELD_THIRD_PERSON_RIGHT_HAND)
						.setDisplayPos("thirdperson_lefthand", ItemModelDispatcher.HANDHELD_THIRD_PERSON_LEFT_HAND);
			}
			dispatcher.addDispatch(model);
		} catch (Throwable t) {
			FoolMod.LOGGER.warn("Could not bind the icon for {}", texture, t);
		}
	}

	@Override
	public void initBlockModels(BlockModelDispatcher dispatcher) {
		if (FoolBlocks.joxeDust != null) {

			dispatcher.addDispatch(new JoxeDustModel(FoolBlocks.joxeDust));
		}
	}

	@Override
	public void initTileEntityModels(TileEntityRenderDispatcher dispatcher) {
	}

	@Override
	public void initBlockColors(BlockColorDispatcher dispatcher) {
	}
}
