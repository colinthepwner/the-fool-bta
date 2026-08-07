package btai.foolmod.item;

import btai.foolmod.FoolMod;
import btai.foolmod.block.FoolBlocks;
import net.minecraft.core.enums.HumanArmorShape;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemArmor;
import net.minecraft.core.item.material.ArmorMaterial;
import net.minecraft.core.item.material.ToolMaterial;
import net.minecraft.core.item.tool.ItemToolAxe;
import net.minecraft.core.item.tool.ItemToolHoe;
import net.minecraft.core.item.tool.ItemToolPickaxe;
import net.minecraft.core.item.tool.ItemToolShovel;
import net.minecraft.core.item.tool.ItemToolSword;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import turniplabs.halplibe.helper.ItemBuilder;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryCategory;
import turniplabs.halplibe.helper.creativeInventory.CreativeInventoryPlacement;

public final class FoolItems {

	private static final int BASE_ID = 19400;

	public static final ToolMaterial FOOLS_GOLD_TOOL = new ToolMaterial()
			.setMiningLevel(3)
			.setDurability(1536)
			.setEfficiency(6.0f, 8.0f)
			.setDamage(4)
			.setBlockHitDelay(4);

	public static ArmorMaterial foolsGoldArmor;

	public static Item joxeDust;
	public static Item foolsGoldIngot;
	public static Item pickaxe, axe, shovel, hoe, sword;
	public static ItemArmor<?> helmet, chestplate, leggings, boots;

	private FoolItems() {
	}

	public static void register() {
		int id = BASE_ID;

		ItemBuilder drops = inTab(CreativeInventoryCategory.DROPS);
		ItemBuilder products = inTab(CreativeInventoryCategory.ORE_PRODUCTS);
		ItemBuilder tools = inTab(CreativeInventoryCategory.TOOLS);
		ItemBuilder armour = inTab(CreativeInventoryCategory.ARMOR);

		joxeDust = FoolBlocks.joxeDust.asItem();
		foolsGoldIngot = products.build(new Item("fools_gold_ingot", tex("fools_gold_ingot"), id++));

		pickaxe = tools.build(new ItemToolPickaxe("fools_gold_pickaxe", tex("fools_gold_pickaxe"), id++, FOOLS_GOLD_TOOL));
		axe = tools.build(new ItemToolAxe("fools_gold_axe", tex("fools_gold_axe"), id++, FOOLS_GOLD_TOOL));
		shovel = tools.build(new ItemToolShovel("fools_gold_shovel", tex("fools_gold_shovel"), id++, FOOLS_GOLD_TOOL));
		hoe = tools.build(new ItemToolHoe("fools_gold_hoe", tex("fools_gold_hoe"), id++, FOOLS_GOLD_TOOL));
		sword = tools.build(new ItemToolSword("fools_gold_sword", tex("fools_gold_sword"), id++, FOOLS_GOLD_TOOL));

		foolsGoldArmor = ArmorMaterial.register(
				new ArmorMaterial(NamespaceID.fromPool(FoolMod.MOD_ID, "fools_gold"), 800)
						.withProtectionPercentage(DamageType.COMBAT, 66.0f)
						.withProtectionPercentage(DamageType.BLAST, 66.0f)
						.withProtectionPercentage(DamageType.FIRE, 124.0f)
						.withProtectionPercentage(DamageType.FALL, 66.0f));

		helmet = armour.build(new ItemArmor<>("fools_gold_helmet", tex("fools_gold_helmet"), id++, foolsGoldArmor, HumanArmorShape.HEAD));
		chestplate = armour.build(new ItemArmor<>("fools_gold_chestplate", tex("fools_gold_chestplate"), id++, foolsGoldArmor, HumanArmorShape.CHEST));
		leggings = armour.build(new ItemArmor<>("fools_gold_leggings", tex("fools_gold_leggings"), id++, foolsGoldArmor, HumanArmorShape.LEGS));
		boots = armour.build(new ItemArmor<>("fools_gold_boots", tex("fools_gold_boots"), id++, foolsGoldArmor, HumanArmorShape.BOOTS));

		FoolMod.LOGGER.info("Registered {} of the Fool's belongings.", id - BASE_ID);
	}

	private static ItemBuilder inTab(CreativeInventoryCategory category) {
		return new ItemBuilder(FoolMod.MOD_ID)
				.setCreativeInventoryPlacement(new CreativeInventoryPlacement.Category(category));
	}

	private static String tex(String name) {
		return FoolMod.MOD_ID + ":item/" + name;
	}
}
