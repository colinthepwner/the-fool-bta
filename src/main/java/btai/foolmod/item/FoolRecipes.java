package btai.foolmod.item;

import btai.foolmod.FoolMod;
import btai.foolmod.block.FoolBlocks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.data.registry.Registries;
import net.minecraft.core.data.registry.recipe.RecipeSymbol;
import net.minecraft.core.data.registry.recipe.entry.RecipeEntryCraftingShaped;
import net.minecraft.core.data.registry.recipe.entry.RecipeEntryCraftingShapeless;
import net.minecraft.core.item.ItemStack;
import turniplabs.halplibe.util.RecipeEntrypoint;

public class FoolRecipes implements RecipeEntrypoint {

	@Override
	public void initNamespaces() {
	}

	@Override
	public void onRecipesReady() {
		if (FoolBlocks.foolsGoldBlock == null || FoolItems.foolsGoldIngot == null) {
			return;
		}
		ItemStack bar = new ItemStack(FoolItems.foolsGoldIngot);

		RecipeSymbol[] grid = new RecipeSymbol[9];
		Arrays.setAll(grid, i -> new RecipeSymbol('I', bar.copy()));
		Registries.RECIPES.WORKBENCH.register(FoolMod.MOD_ID + ":fools_gold_block",
				new RecipeEntryCraftingShaped(3, 3, grid, FoolBlocks.foolsGoldBlock.getDefaultStack()));

		List<RecipeSymbol> single = new ArrayList<>();
		single.add(new RecipeSymbol(FoolBlocks.foolsGoldBlock.getDefaultStack()));
		Registries.RECIPES.WORKBENCH.register(FoolMod.MOD_ID + ":fools_gold_ingot_from_block",
				new RecipeEntryCraftingShapeless(single, new ItemStack(FoolItems.foolsGoldIngot, 9)));

		FoolMod.LOGGER.info("Fool's Gold may be stacked and unstacked.");
	}
}
