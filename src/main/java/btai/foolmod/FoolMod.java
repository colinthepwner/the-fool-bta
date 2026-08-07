package btai.foolmod;

import btai.foolmod.entity.FoolEntity;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.data.registry.Registries;
import net.minecraft.core.data.registry.Registry;
import net.minecraft.core.entity.SpawnListEntry;
import net.minecraft.core.enums.MobCategory;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.world.biome.Biome;
import net.minecraft.core.world.biome.Biomes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import turniplabs.halplibe.HalpLibe;
import turniplabs.halplibe.helper.EntityHelper;
import turniplabs.halplibe.util.GameStartEntrypoint;

public class FoolMod implements ModInitializer, GameStartEntrypoint {

	public static final String MOD_ID = HalpLibe.registerMod("foolmod", true);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final int SPAWN_WEIGHT = 1;

	@Override
	public void onInitialize() {
		new EntityHelper().createEntity(FoolEntity.class, NamespaceID.getPermanent(MOD_ID, "fool"), FoolEntity::new);
		LOGGER.info("The Fool is abroad.");
	}

	@Override
	public void beforeGameStart() {
	}

	@Override
	public void afterGameStart() {
		try {
			Biomes.init();
			Registry<Biome> biomes = Registries.BIOMES;
			int count = 0;
			for (Biome biome : biomes.values()) {
				if (addFoolTo(biome)) {
					count++;
				}
			}
			biomes.addCallback((registry, biome) -> addFoolTo(biome));
			LOGGER.info("The Fool may turn up in {} biomes, day or night.", count);
		} catch (Throwable t) {

			LOGGER.error("Could not register the Fool's spawn entries", t);
		}
	}

	private static boolean addFoolTo(Biome biome) {
		if (biome == null) {
			return false;
		}
		var list = biome.getSpawnableList(MobCategory.MONSTER);
		for (SpawnListEntry entry : list) {
			if (entry.entityClass == FoolEntity.class) {
				return false;
			}
		}
		list.add(new SpawnListEntry(FoolEntity.class, SPAWN_WEIGHT));
		return true;
	}
}
