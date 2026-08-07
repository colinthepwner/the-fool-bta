package btai.foolmod.client;

import btai.foolmod.entity.FoolEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.EntityRendererDispatcher;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import net.minecraft.client.render.block.color.BlockColorDispatcher;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import turniplabs.halplibe.util.ModelEntrypoint;

@Environment(EnvType.CLIENT)
public class FoolModels implements ModelEntrypoint {

	@Override
	public void initEntityModels(EntityRendererDispatcher dispatcher) {

		dispatcher.assignRenderer(FoolEntity.class, new FoolRenderer());
	}

	@Override
	public void initBlockModels(BlockModelDispatcher dispatcher) {
	}

	@Override
	public void initItemModels(ItemModelDispatcher dispatcher) {
	}

	@Override
	public void initTileEntityModels(TileEntityRenderDispatcher dispatcher) {
	}

	@Override
	public void initBlockColors(BlockColorDispatcher dispatcher) {
	}
}
