package btai.foolmod.client;

import btai.foolmod.entity.FoolEntity;
import java.awt.image.BufferedImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.PlayerSkinParser;
import net.minecraft.client.render.TextureManager;
import net.minecraft.client.render.entity.MobRendererBiped;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.client.render.texture.Texture;
import net.minecraft.core.util.helper.MathHelper;
import org.joml.Math;
import org.useless.dragonfly.models.entity.StaticEntityModel;

@Environment(EnvType.CLIENT)
public class FoolRenderer extends MobRendererBiped<FoolEntity> {

	private static final String SKIN_PATH = "/assets/foolmod/skin.png";
	private Texture skinTexture;
	private boolean skinTried;

	public FoolRenderer() {
		super(0.5f);
	}

	@Override
	public void loadEntityTexture(FoolEntity entity) {
		TextureManager tm = this.renderDispatcher.textureManager;
		if (!skinTried) {
			skinTried = true;
			BufferedImage img = tm.getImage(SKIN_PATH);
			if (img != null) {

				skinTexture = tm.loadBufferedTexture(PlayerSkinParser.instanceSteve.parseImage(img));
			}
		}
		if (skinTexture != null) {
			tm.bindTexture(skinTexture);
		} else {
			super.loadEntityTexture(entity);
		}
	}

	@Override
	protected StaticEntityModel getActiveModel(FoolEntity entity) {
		return this.getModel("main");
	}

	@Override
	protected void preRenderTransform(FoolEntity entity, double x, double y, double z, float yaw, float partialTick) {
		GLRenderer.modelM4f().translate((float) x, (float) y, (float) z);
		GLRenderer.modelM4f().rotateY(-this.getBodyYaw(entity, partialTick));
		GLRenderer.modelM4f().scale(0.062539086f, 0.062539086f, -0.062539086f);
		GLRenderer.modelM4f().scale(0.9375f, 0.9375f, 0.9375f);
		if (entity.deathTime > 0) {
			float rotationProgress = ((float) entity.deathTime + partialTick - 1.0f) / 20.0f * 1.6f;
			rotationProgress = MathHelper.sqrt_float(rotationProgress);
			if (rotationProgress > 1.0f) {
				rotationProgress = 1.0f;
			}
			GLRenderer.modelM4f().rotateZ(Math.toRadians(rotationProgress * this.getMaxDeathRotation(entity)));
		}
	}

	@Override
	protected void renderSpecials(TessellatorGeneral tessellator, FoolEntity entity, double x, double y, double z) {
	}
}
