package btai.foolmod.mixin;

import btai.foolmod.block.FoolBlockDecay;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class WorldTickMixin {

	@Inject(method = "updateEntities", at = @At("HEAD"))
	private void foolmod$decayPlacedBlocks(CallbackInfo ci) {
		FoolBlockDecay.tick((World) (Object) this);
	}

	@Inject(method = "saveWorldIndirectly", at = @At("HEAD"))
	private void foolmod$sweepBeforeSaving(CallbackInfo ci) {
		FoolBlockDecay.sweep((World) (Object) this);
	}
}
