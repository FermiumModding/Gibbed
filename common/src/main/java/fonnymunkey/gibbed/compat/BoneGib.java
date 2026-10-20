package fonnymunkey.gibbed.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.Gib;
import fonnymunkey.gibbed.util.UVScalingVertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.List;

public abstract class BoneGib<B> extends AbstractGib {
	//scale prepScaleX prepScaleY prepScaleZ
	private static final float[] BAKED_DEFAULT_SCALES = { 1.0F, 1.0F, 1.0F, 1.0F };
	//x y z u v nx ny nz
	private static final int VERTEX_SIZE = 8;
	protected final B bone;
	private final float[] vertexData;
	private FloatArrayList bakingData;

	public BoneGib(B bone, boolean renderChildren) {
		super(renderChildren);
		this.bone = bone;

		Matrix4f matrix = new Matrix4f();
		this.applyParentRestTransforms(matrix, this.getParent(bone));
		this.bakingData = new FloatArrayList();
		this.diveBones(bone, matrix, true);
		this.vertexData = this.bakingData.toFloatArray();
		this.bakingData = null;
		if(!this.valid()) return;

		this.cX = (this.maxX + this.minX) / 2.0F;
		this.cY = (this.maxY + this.minY) / 2.0F;
		this.cZ = (this.maxZ + this.minZ) / 2.0F;
		this.dX = this.maxX - this.minX;
		this.dY = this.maxY - this.minY;
		this.dZ = this.maxZ - this.minZ;
	}

	@Nullable
	protected abstract B getParent(B bone);

	protected abstract List<B> getChildren(B bone);

	protected abstract boolean isHidden(B bone);

	protected abstract boolean isHidingChildren(B bone);

	protected abstract void applyRestTransform(Matrix4f matrix, B bone);

	protected abstract void bakeCubes(B bone, Matrix4f boneMatrix);

	protected void addVertex(Vector4f position, float u, float v, Vector3f normal) {
		this.minX = Math.min(this.minX, position.x());
		this.minY = Math.min(this.minY, position.y());
		this.minZ = Math.min(this.minZ, position.z());
		this.maxX = Math.max(this.maxX, position.x());
		this.maxY = Math.max(this.maxY, position.y());
		this.maxZ = Math.max(this.maxZ, position.z());

		this.bakingData.add(position.x());
		this.bakingData.add(position.y());
		this.bakingData.add(position.z());
		this.bakingData.add(u);
		this.bakingData.add(v);
		this.bakingData.add(normal.x());
		this.bakingData.add(normal.y());
		this.bakingData.add(normal.z());
	}

	@Override
	public float offsetX(float scale, float prepScaleX) {
		return -this.cX * scale * prepScaleX;
	}

	@Override
	public float offsetY(float scale, float prepScaleY) {
		return 0.01F + this.cY * scale * prepScaleY;
	}

	@Override
	public float offsetZ(float scale, float prepScaleZ) {
		return this.cZ * scale * prepScaleZ;
	}

	@Override
	public Object getContextKey() {
		return this.bone;
	}

	@Override
	public float[] getDefaultScales() {
		return BAKED_DEFAULT_SCALES;
	}

	@Override
	public boolean valid() {
		return super.valid() && this.vertexData != null && this.vertexData.length > 0;
	}

	@Override
	public void render(Gib gib, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks) {
		float dX = this.dX(gib.scale);
		float dY = this.dY(gib.scale);
		float dZ = this.dZ(gib.scale);
		float shortest;
		float rotAxisX = 0.0F;
		float rotAxisY = 0.0F;
		float rotAxisZ = 0.0F;
		if(dY < dX && dY < dZ) {
			shortest = dY;
			rotAxisY = 1.0F;
		}
		else if(dZ <= dX && dZ <= dY) {
			shortest = dZ;
			rotAxisX = 1.0F;
		}
		else {
			shortest = dX;
			rotAxisZ = 1.0F;
		}
		if(!gib.onGround) {
			rotAxisX = 1.0F;
			rotAxisY = 0.0F;
			rotAxisZ = 0.0F;
		}

		float alpha = this.fadeOut() ? gib.getAlpha(partialTicks) : 1.0F;

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - gib.getYRot(partialTicks)));
		poseStack.scale(gib.prepScaleX, gib.prepScaleY, gib.prepScaleZ);

		float pitch = gib.getXRot(partialTicks);
		if(rotAxisX != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
		else if(rotAxisY != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(pitch));
		else poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
		poseStack.translate(-this.cX(gib.scale), -this.cY(gib.scale), -this.cZ(gib.scale));
		if(gib.onGround) {
			poseStack.translate(-rotAxisZ * (shortest / 2.0F + gib.zFightOffset), rotAxisY * (shortest / 2.0F + gib.zFightOffset), rotAxisX * (shortest / 2.0F + gib.zFightOffset));
		}
		poseStack.scale(gib.scale, gib.scale, gib.scale);

		this.drawVertices(poseStack, buffer, gib.baseTexture, gib, packedLight, alpha);
		if(gib.layerTexture != null) {
			this.drawVertices(poseStack, buffer, gib.layerTexture, gib, packedLight, alpha);
		}

		poseStack.popPose();
	}

	private void drawVertices(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, Gib gib, int packedLight, float alpha) {
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
		if(gib.goreScaleU != 1.0F || gib.goreScaleV != 1.0F) {
			consumer = new UVScalingVertexConsumer(consumer, gib.goreScaleU, gib.goreScaleV);
		}
		Matrix4f pose = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		Vector4f position = new Vector4f();
		Vector3f vertexNormal = new Vector3f();
		int color = FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);
		for(int i = 0; i < this.vertexData.length; i += VERTEX_SIZE) {
			pose.transform(position.set(this.vertexData[i], this.vertexData[i + 1], this.vertexData[i + 2], 1.0F));
			normal.transform(vertexNormal.set(this.vertexData[i + 5], this.vertexData[i + 6], this.vertexData[i + 7]));
			consumer.addVertex(position.x(), position.y(), position.z(), color,
							this.vertexData[i + 3], this.vertexData[i + 4], OverlayTexture.NO_OVERLAY, packedLight,
							vertexNormal.x(), vertexNormal.y(), vertexNormal.z());
		}
	}

	private void diveBones(B bone, Matrix4f parentMatrix, boolean isRoot) {
		Matrix4f boneMatrix = new Matrix4f(parentMatrix);
		this.applyRestTransform(boneMatrix, bone);

		if(isRoot || !this.isHidden(bone)) {
			this.bakeCubes(bone, boneMatrix);
		}
		if(this.renderChildren && !this.isHidingChildren(bone)) {
			for(B childBone : this.getChildren(bone)) {
				this.diveBones(childBone, boneMatrix, false);
			}
		}
	}

	private void applyParentRestTransforms(Matrix4f matrix, @Nullable B parent) {
		if(parent == null) return;
		this.applyParentRestTransforms(matrix, this.getParent(parent));
		this.applyRestTransform(matrix, parent);
	}
}