package fonnymunkey.gibbed.client.gib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fonnymunkey.gibbed.mixin.vanilla.ModelPartAccessor;
import fonnymunkey.gibbed.util.UVScalingVertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class BasicGib extends AbstractGib {
	private final ModelPart part;
	
	public BasicGib(ModelPart part, boolean renderChildren, Matrix4f parentTransform) {
		super(renderChildren);
		this.part = part;

		diveCubes(part, 0, 0, 0);
		if(!this.valid()) return;

		this.cX = (this.maxX + this.minX) / 2.0F;
		this.cY = (this.maxY + this.minY) / 2.0F;
		this.cZ = (this.maxZ + this.minZ) / 2.0F;
		this.dX = this.maxX - this.minX;
		this.dY = this.maxY - this.minY;
		this.dZ = this.maxZ - this.minZ;

		PartPose pose = part.getInitialPose();
		parentTransform.transformPosition(this.cX + pose.x, this.cY + pose.y, this.cZ + pose.z, this.restCenter);
	}

	public static void applyRestTransform(Matrix4f matrix, ModelPart part) {
		PartPose pose = part.getInitialPose();
		matrix.translate(pose.x, pose.y, pose.z);
		if(pose.xRot != 0.0F || pose.yRot != 0.0F || pose.zRot != 0.0F) {
			matrix.rotate(new Quaternionf().rotationZYX(pose.zRot, pose.yRot, pose.xRot));
		}
	}
	
	@Override
	public Object getContextKey() {
		return this.part;
	}
	
	@Override
	public void render(Gib gib, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks) {
		float alpha = this.fadeOut() ? gib.getAlpha(partialTicks) : 1.0F;

		poseStack.pushPose();
		this.applyModelRenderTransform(gib, poseStack, partialTicks);

		List<PartState> states = new ArrayList<>();
		states.add(new PartState(this.part));
		this.part.loadPose(PartPose.ZERO);
		this.part.visible = true;
		if(this.renderChildren) {
			this.part.getAllParts().skip(1).forEach(child -> {
				states.add(new PartState(child));
				child.resetPose();
			});
		}

		this.renderPart(poseStack, buffer, gib.baseTexture, gib, packedLight, alpha);
		if(gib.layerTexture != null) {
			this.renderPart(poseStack, buffer, gib.layerTexture, gib, packedLight, alpha);
		}

		for(PartState state : states) {
			state.restore();
		}

		poseStack.popPose();
	}

	private void renderPart(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, Gib gib, int packedLight, float alpha) {
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
		if(gib.goreScaleU != 1.0F || gib.goreScaleV != 1.0F) {
			consumer = new UVScalingVertexConsumer(consumer, gib.goreScaleU, gib.goreScaleV);
		}
		if(this.renderChildren) {
			this.part.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F));
		}
		else {
			poseStack.pushPose();
			this.part.translateAndRotate(poseStack);
			PoseStack.Pose pose = poseStack.last();
			for(ModelPart.Cube cube : ((ModelPartAccessor)(Object)this.part).gibbed$getCubes()) {
				cube.compile(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F));
			}
			poseStack.popPose();
		}
	}
	
	private void diveCubes(ModelPart part, float rotPointX, float rotPointY, float rotPointZ) {
		for(ModelPart.Cube cube : ((ModelPartAccessor)(Object)part).gibbed$getCubes()) {
			if(cube.minX + rotPointX < minX) minX = cube.minX + rotPointX;
			if(cube.maxX + rotPointX > maxX) maxX = cube.maxX + rotPointX;
			if(cube.minY + rotPointY < minY) minY = cube.minY + rotPointY;
			if(cube.maxY + rotPointY > maxY) maxY = cube.maxY + rotPointY;
			if(cube.minZ + rotPointZ < minZ) minZ = cube.minZ + rotPointZ;
			if(cube.maxZ + rotPointZ > maxZ) maxZ = cube.maxZ + rotPointZ;
		}
		if(this.renderChildren) {
			for(ModelPart childPart : ((ModelPartAccessor)(Object)part).gibbed$getChildren().values()) {
				PartPose pose = childPart.getInitialPose();
				diveCubes(childPart, rotPointX + pose.x, rotPointY + pose.y, rotPointZ + pose.z);
			}
		}
	}

	private static class PartState {
		private final ModelPart part;
		private final PartPose pose;
		private final float xScale;
		private final float yScale;
		private final float zScale;
		private final boolean visible;

		private PartState(ModelPart part) {
			this.part = part;
			this.pose = part.storePose();
			this.xScale = part.xScale;
			this.yScale = part.yScale;
			this.zScale = part.zScale;
			this.visible = part.visible;
		}

		private void restore() {
			this.part.loadPose(this.pose);
			this.part.xScale = this.xScale;
			this.part.yScale = this.yScale;
			this.part.zScale = this.zScale;
			this.part.visible = this.visible;
		}
	}
}