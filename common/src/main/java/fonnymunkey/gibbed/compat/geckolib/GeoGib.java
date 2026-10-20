package fonnymunkey.gibbed.compat.geckolib;

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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.util.RenderUtils;

public class GeoGib extends AbstractGib {
	//scale prepScaleX prepScaleY prepScaleZ
	private static final float[] GEO_DEFAULT_SCALES = { 1.0F, 1.0F, 1.0F, 1.0F };
	//x y z u v nx ny nz
	private static final int VERTEX_SIZE = 8;
	private final GeoBone bone;
	private final float[] vertexData;

	public GeoGib(GeoBone bone, boolean renderChildren) {
		super(renderChildren);
		this.bone = bone;

		Matrix4f matrix = new Matrix4f();
		applyParentRestTransforms(matrix, bone.getParent());
		FloatArrayList data = new FloatArrayList();
		this.diveBones(bone, matrix, data, true);
		this.vertexData = data.toFloatArray();
		if(!this.valid()) return;

		this.cX = (this.maxX + this.minX) / 2.0F;
		this.cY = (this.maxY + this.minY) / 2.0F;
		this.cZ = (this.maxZ + this.minZ) / 2.0F;
		this.dX = this.maxX - this.minX;
		this.dY = this.maxY - this.minY;
		this.dZ = this.maxZ - this.minZ;
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
		return GEO_DEFAULT_SCALES;
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
		for(int i = 0; i < this.vertexData.length; i += VERTEX_SIZE) {
			pose.transform(position.set(this.vertexData[i], this.vertexData[i + 1], this.vertexData[i + 2], 1.0F));
			normal.transform(vertexNormal.set(this.vertexData[i + 5], this.vertexData[i + 6], this.vertexData[i + 7]));
			consumer.vertex(position.x(), position.y(), position.z(), 1.0F, 1.0F, 1.0F, alpha,
							this.vertexData[i + 3], this.vertexData[i + 4], OverlayTexture.NO_OVERLAY, packedLight,
							vertexNormal.x(), vertexNormal.y(), vertexNormal.z());
		}
	}

	private void diveBones(GeoBone bone, Matrix4f parentMatrix, FloatArrayList data, boolean isRoot) {
		Matrix4f boneMatrix = new Matrix4f(parentMatrix);
		applyRestTransform(boneMatrix, bone);

		if(isRoot || !bone.isHidden()) {
			for(GeoCube cube : bone.getCubes()) {
				Matrix4f cubeMatrix = new Matrix4f(boneMatrix);
				applyCubeTransform(cubeMatrix, cube);
				this.addCube(cube, cubeMatrix, data);
			}
		}
		if(this.renderChildren && !bone.isHidingChildren()) {
			for(GeoBone childBone : bone.getChildBones()) {
				this.diveBones(childBone, boneMatrix, data, false);
			}
		}
	}

	private void addCube(GeoCube cube, Matrix4f matrix, FloatArrayList data) {
		Matrix3f normalMatrix = matrix.normal(new Matrix3f());
		Vector4f position = new Vector4f();
		for(GeoQuad quad : cube.quads()) {
			if(quad == null) continue;
			Vector3f normal = normalMatrix.transform(new Vector3f(quad.normal()));
			if(normal.lengthSquared() > 0.0F) normal.normalize();
			RenderUtils.fixInvertedFlatCube(cube, normal);

			for(GeoVertex vertex : quad.vertices()) {
				matrix.transform(position.set(vertex.position(), 1.0F));

				this.minX = Math.min(this.minX, position.x());
				this.minY = Math.min(this.minY, position.y());
				this.minZ = Math.min(this.minZ, position.z());
				this.maxX = Math.max(this.maxX, position.x());
				this.maxY = Math.max(this.maxY, position.y());
				this.maxZ = Math.max(this.maxZ, position.z());

				data.add(position.x());
				data.add(position.y());
				data.add(position.z());
				data.add(vertex.texU());
				data.add(vertex.texV());
				data.add(normal.x());
				data.add(normal.y());
				data.add(normal.z());
			}
		}
	}

	private static void applyParentRestTransforms(Matrix4f matrix, GeoBone parent) {
		if(parent == null) return;
		applyParentRestTransforms(matrix, parent.getParent());
		applyRestTransform(matrix, parent);
	}

	private static void applyRestTransform(Matrix4f matrix, GeoBone bone) {
		BoneSnapshot snapshot = bone.getInitialSnapshot();
		float posX = snapshot != null ? snapshot.getOffsetX() : bone.getPosX();
		float posY = snapshot != null ? snapshot.getOffsetY() : bone.getPosY();
		float posZ = snapshot != null ? snapshot.getOffsetZ() : bone.getPosZ();
		float rotX = snapshot != null ? snapshot.getRotX() : bone.getRotX();
		float rotY = snapshot != null ? snapshot.getRotY() : bone.getRotY();
		float rotZ = snapshot != null ? snapshot.getRotZ() : bone.getRotZ();
		float scaleX = snapshot != null ? snapshot.getScaleX() : bone.getScaleX();
		float scaleY = snapshot != null ? snapshot.getScaleY() : bone.getScaleY();
		float scaleZ = snapshot != null ? snapshot.getScaleZ() : bone.getScaleZ();

		matrix.translate(-posX / 16.0F, posY / 16.0F, posZ / 16.0F);
		matrix.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
		if(rotZ != 0.0F) matrix.rotateZ(rotZ);
		if(rotY != 0.0F) matrix.rotateY(rotY);
		if(rotX != 0.0F) matrix.rotateX(rotX);
		matrix.scale(scaleX, scaleY, scaleZ);
		matrix.translate(-bone.getPivotX() / 16.0F, -bone.getPivotY() / 16.0F, -bone.getPivotZ() / 16.0F);
	}

	private static void applyCubeTransform(Matrix4f matrix, GeoCube cube) {
		matrix.translate((float)cube.pivot().x() / 16.0F, (float)cube.pivot().y() / 16.0F, (float)cube.pivot().z() / 16.0F);
		matrix.rotateZ((float)cube.rotation().z());
		matrix.rotateY((float)cube.rotation().y());
		matrix.rotateX((float)cube.rotation().x());
		matrix.translate(-(float)cube.pivot().x() / 16.0F, -(float)cube.pivot().y() / 16.0F, -(float)cube.pivot().z() / 16.0F);
	}
}