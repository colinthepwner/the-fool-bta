package btai.foolmod.command;

import btai.foolmod.entity.FoolEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilderLiteral;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.net.command.CommandManager;
import net.minecraft.core.net.command.CommandSource;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;

public class FoolCommand implements CommandManager.CommandRegistry {

	private static final int MIN_DISTANCE = 24;
	private static final int MAX_DISTANCE = 44;
	private static final int ATTEMPTS = 400;

	@Override
	public void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
				ArgumentBuilderLiteral.<CommandSource>literal("fool")
						.requires(CommandSource::hasAdmin)
						.executes(c -> spawn(c.getSource())));
	}

	private static int spawn(CommandSource source) {
		Entity sender = source.getSender();
		if (sender == null || sender.world == null) {
			source.sendMessage("§cNo world to put one in.");
			return 0;
		}
		World world = sender.world;
		if (world.isClientSide) {
			return 0;
		}

		FoolEntity fool = new FoolEntity(world);
		int[] spot = findSpawnSpot(world, fool, sender);
		if (spot == null) {
			source.sendMessage("§eNowhere round here suits one. It wants open ground out of doors, "
					+ "within sight of something you built, and at least " + MIN_DISTANCE + " blocks off.");
			return 0;
		}
		fool.moveTo(spot[0] + 0.5, spot[1], spot[2] + 0.5, world.rand.nextFloat() * 360.0f, 0.0f);
		fool.spawnInit();
		world.entityJoinedWorld(fool);
		double dist = Math.sqrt(sender.distanceToSqr(spot[0] + 0.5, spot[1], spot[2] + 0.5));
		source.sendMessage(String.format("§aA Fool is abroad — %d, %d, %d (%.0f blocks off).",
				spot[0], spot[1], spot[2], dist));
		return 1;
	}

	private static int[] findSpawnSpot(World world, FoolEntity fool, Entity near) {
		for (int i = 0; i < ATTEMPTS; i++) {
			double angle = world.rand.nextDouble() * Math.PI * 2.0;
			double radius = MIN_DISTANCE + world.rand.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
			int x = MathHelper.floor(near.x + Math.cos(angle) * radius);
			int z = MathHelper.floor(near.z + Math.sin(angle) * radius);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			int y = world.getHeightValue(x, z);
			if (y < 2) {
				continue;
			}
			fool.moveTo(x + 0.5, y, z + 0.5, 0.0f, 0.0f);
			if (fool.hasValidSpawnSurroundings()) {
				return new int[]{x, y, z};
			}
		}
		return null;
	}
}
