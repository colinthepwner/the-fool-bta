package btai.foolmod.client;

import btai.foolmod.FoolMod;
import btai.foolmod.block.FoolBlocks;
import btai.foolmod.block.JoxeDustLogic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.model.BlockModelStandard;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.enums.LightLayer;
import net.minecraft.core.world.WorldSource;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;

@Environment(EnvType.CLIENT)
public class JoxeDustModel extends BlockModelStandard<JoxeDustLogic> {

	private static final double LIFT = 1.0 / 64.0;

	private static final double STUB = 0.3125;

	private final IconCoordinate cross;
	private final IconCoordinate straight;

	public JoxeDustModel(Block<JoxeDustLogic> block) {
		super(block);
		cross = TextureRegistry.getTexture(FoolMod.MOD_ID + ":block/joxe_dust");
		straight = TextureRegistry.getTexture(FoolMod.MOD_ID + ":block/joxe_dust_straight");
		setAllTextures(FoolMod.MOD_ID + ":block/joxe_dust");
	}

	@Override
	public boolean render(TessellatorGeneral tessellator, WorldSource source, TilePosc pos) {
		int x = pos.x(), y = pos.y(), z = pos.z();
		tessellator.setLightmapCoord2i(source.getSavedLightValue(LightLayer.Block, pos),
				source.getSavedLightValue(LightLayer.Sky, pos));
		tessellator.setColorOpaque3f(1.0f, 1.0f, 1.0f);

		boolean openAbove = !source.isBlockOpaqueCube(new TilePos(x, y + 1, z));

		boolean west = connects(source, x - 1, y, z, openAbove);
		boolean east = connects(source, x + 1, y, z, openAbove);
		boolean north = connects(source, x, y, z - 1, openAbove);
		boolean south = connects(source, x, y, z + 1, openAbove);
		boolean westUp = openAbove && JoxeDustLogic.connectsTo(source, x - 1, y + 1, z);
		boolean eastUp = openAbove && JoxeDustLogic.connectsTo(source, x + 1, y + 1, z);
		boolean northUp = openAbove && JoxeDustLogic.connectsTo(source, x, y + 1, z - 1);
		boolean southUp = openAbove && JoxeDustLogic.connectsTo(source, x, y + 1, z + 1);

		double top = y + LIFT;
		boolean runsEastWest = (west || east) && !north && !south;
		boolean runsNorthSouth = (north || south) && !east && !west;

		if (runsEastWest) {
			quad(tessellator, x, x + 1, z, z + 1, top, straight, false);
		} else if (runsNorthSouth) {
			quad(tessellator, x, x + 1, z, z + 1, top, straight, true);
		} else if (!west && !east && !north && !south) {

			quad(tessellator, x, x + 1, z, z + 1, top, cross, false);
		} else {

			double w = west ? 0.0 : STUB;
			double e = east ? 0.0 : STUB;
			double n = north ? 0.0 : STUB;
			double s = south ? 0.0 : STUB;
			croppedQuad(tessellator, x + w, x + 1 - e, z + n, z + 1 - s, top, cross, w, 1.0 - e, n, 1.0 - s);
		}

		if (westUp) sideQuad(tessellator, x + LIFT, y, z, z + 1, true);
		if (eastUp) sideQuad(tessellator, x + 1 - LIFT, y, z, z + 1, true);
		if (northUp) sideQuad(tessellator, z + LIFT, y, x, x + 1, false);
		if (southUp) sideQuad(tessellator, z + 1 - LIFT, y, x, x + 1, false);
		return true;
	}

	private boolean connects(WorldSource source, int x, int y, int z, boolean openAbove) {
		if (JoxeDustLogic.connectsTo(source, x, y, z)) {
			return true;
		}
		if (JoxeDustLogic.connectsTo(source, x, y - 1, z)) {
			return true;
		}
		return openAbove && JoxeDustLogic.connectsTo(source, x, y + 1, z);
	}

	private void quad(TessellatorGeneral t, double x0, double x1, double z0, double z1, double y,
			IconCoordinate icon, boolean rotated) {
		double u0 = icon.getIconUMin(), u1 = icon.getIconUMax();
		double v0 = icon.getIconVMin(), v1 = icon.getIconVMax();
		if (rotated) {
			face(t, x0, x1, z0, z1, y, u0, v0, u1, v1, true);
		} else {
			face(t, x0, x1, z0, z1, y, u0, v0, u1, v1, false);
		}
	}

	private void croppedQuad(TessellatorGeneral t, double x0, double x1, double z0, double z1, double y,
			IconCoordinate icon, double uLo, double uHi, double vLo, double vHi) {
		double u0 = icon.getSubIconU(uLo), u1 = icon.getSubIconU(uHi);
		double v0 = icon.getSubIconV(vLo), v1 = icon.getSubIconV(vHi);
		face(t, x0, x1, z0, z1, y, u0, v0, u1, v1, false);
	}

	private void sideQuad(TessellatorGeneral t, double at, int y, double a0, double a1, boolean alongX) {
		double u0 = straight.getIconUMin(), u1 = straight.getIconUMax();
		double v0 = straight.getIconVMin(), v1 = straight.getIconVMax();
		double yb = y, yt = y + 1;

		if (alongX) {
			t.addVertexWithUV(at, yt, a1, u1, v1);
			t.addVertexWithUV(at, yb, a1, u0, v1);
			t.addVertexWithUV(at, yb, a0, u0, v0);
			t.addVertexWithUV(at, yt, a0, u1, v0);
			t.addVertexWithUV(at, yt, a0, u1, v0);
			t.addVertexWithUV(at, yb, a0, u0, v0);
			t.addVertexWithUV(at, yb, a1, u0, v1);
			t.addVertexWithUV(at, yt, a1, u1, v1);
		} else {
			t.addVertexWithUV(a1, yt, at, u1, v1);
			t.addVertexWithUV(a1, yb, at, u0, v1);
			t.addVertexWithUV(a0, yb, at, u0, v0);
			t.addVertexWithUV(a0, yt, at, u1, v0);
			t.addVertexWithUV(a0, yt, at, u1, v0);
			t.addVertexWithUV(a0, yb, at, u0, v0);
			t.addVertexWithUV(a1, yb, at, u0, v1);
			t.addVertexWithUV(a1, yt, at, u1, v1);
		}
	}

	private void face(TessellatorGeneral t, double x0, double x1, double z0, double z1, double y,
			double u0, double v0, double u1, double v1, boolean rotated) {
		if (rotated) {
			t.addVertexWithUV(x1, y, z1, u1, v0);
			t.addVertexWithUV(x1, y, z0, u0, v0);
			t.addVertexWithUV(x0, y, z0, u0, v1);
			t.addVertexWithUV(x0, y, z1, u1, v1);
			t.addVertexWithUV(x1, y, z1, u1, v0);
			t.addVertexWithUV(x0, y, z1, u1, v1);
			t.addVertexWithUV(x0, y, z0, u0, v1);
			t.addVertexWithUV(x1, y, z0, u0, v0);
			return;
		}
		t.addVertexWithUV(x1, y, z1, u1, v1);
		t.addVertexWithUV(x1, y, z0, u1, v0);
		t.addVertexWithUV(x0, y, z0, u0, v0);
		t.addVertexWithUV(x0, y, z1, u0, v1);
		t.addVertexWithUV(x1, y, z1, u1, v1);
		t.addVertexWithUV(x0, y, z1, u0, v1);
		t.addVertexWithUV(x0, y, z0, u0, v0);
		t.addVertexWithUV(x1, y, z0, u1, v0);
	}
}
