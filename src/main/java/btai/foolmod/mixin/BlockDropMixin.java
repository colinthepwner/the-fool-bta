package btai.foolmod.mixin;

import btai.foolmod.block.FoolBlockDecay;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockLogic.class)
public class BlockDropMixin {

	@Inject(method = "dropWithCause", at = @At("HEAD"), cancellable = true)
	private void foolmod$noSpoilsFromTheFool(World world, EnumDropCause cause, TilePosc pos, int data,
			TileEntity tileEntity, Player player, CallbackInfo ci) {
		if (FoolBlockDecay.claimAndPuff(world, pos.x(), pos.y(), pos.z())) {
			ci.cancel();
		}
	}
}
