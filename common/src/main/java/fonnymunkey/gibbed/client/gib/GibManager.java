package fonnymunkey.gibbed.client.gib;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class GibManager {
	private static final double MAX_RENDER_DISTANCE_SQ = 128.0D * 128.0D;
	private static final List<Gib> GIBS = new ArrayList<>();
	private static ClientLevel currentLevel = null;

	public static void addGib(Gib gib) {
		GIBS.add(gib);
	}

	public static void clear() {
		GIBS.clear();
	}

	public static void tick(ClientLevel level) {
		if(level != currentLevel) {
			GIBS.clear();
			currentLevel = level;
		}
		if(level == null) return;

		Iterator<Gib> iter = GIBS.iterator();
		while(iter.hasNext()) {
			Gib gib = iter.next();
			if(!gib.isRemoved()) gib.tick(level, GIBS);
			if(gib.isRemoved()) iter.remove();
		}
	}

	public static void render(PoseStack poseStack, Camera camera, float partialTicks) {
		if(GIBS.isEmpty()) return;
		ClientLevel level = Minecraft.getInstance().level;
		if(level == null || level != currentLevel) return;

		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		Vec3 cameraPos = camera.getPosition();
		for(Gib gib : GIBS) {
			if(gib.isRemoved()) continue;
			double x = gib.getX(partialTicks);
			double y = gib.getY(partialTicks);
			double z = gib.getZ(partialTicks);
			if(cameraPos.distanceToSqr(x, y, z) > MAX_RENDER_DISTANCE_SQ) continue;

			int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));
			poseStack.pushPose();
			poseStack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
			gib.gib.render(gib, poseStack, bufferSource, packedLight, partialTicks);
			poseStack.popPose();
		}
		bufferSource.endLastBatch();
	}
}