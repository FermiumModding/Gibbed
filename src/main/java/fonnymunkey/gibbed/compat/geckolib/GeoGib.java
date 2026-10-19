package fonnymunkey.gibbed.compat.geckolib;

import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.EntityGib;
import fonnymunkey.gibbed.client.gib.GibGenerator;
import fonnymunkey.gibbed.client.gib.RenderGib;
import fonnymunkey.gibbed.config.ConfigHandler;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.core.snapshot.BoneSnapshot;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class GeoGib extends AbstractGib {
	//scale prepScaleX prepScaleY prepScaleZ
	private static final float[] DEFAULT_SCALES = { 1.0F, 1.0F, 1.0F, 1.0F };
	//x y z u v nx ny nz
	private static final int VERTEX_SIZE = 8;
	private final GeoBone bone;
	private final boolean renderChildren;
	private final float textureWidth;
	private final float textureHeight;
	private final float[] vertexData;

	public GeoGib(GeoBone bone, boolean renderChildren, float textureWidth, float textureHeight) {
		super();
		this.bone = bone;
		this.renderChildren = renderChildren;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;

		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		applyParentRestTransforms(matrix, bone.parent);
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
	public float cX(float scale) {
		return this.cX * scale;
	}

	@Override
	public float cY(float scale) {
		return this.cY * scale;
	}

	@Override
	public float cZ(float scale) {
		return this.cZ * scale;
	}

	@Override
	public float dX(float scale) {
		return this.dX * scale;
	}

	@Override
	public float dY(float scale) {
		return this.dY * scale;
	}

	@Override
	public float dZ(float scale) {
		return this.dZ * scale;
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
	public float bbWidth(float scale, float prepScaleX, float prepScaleY, float prepScaleZ) {
		return Math.max(this.dX * prepScaleX, Math.max(this.dY * prepScaleY, this.dZ * prepScaleZ)) * scale;
	}

	@Override
	public float bbHeight(float scale, float prepScaleX, float prepScaleY, float prepScaleZ) {
		return Math.min(this.dX * prepScaleX, Math.min(this.dY * prepScaleY, this.dZ * prepScaleZ)) * scale;
	}

	@Override
	public Object getContextKey() {
		return this.bone;
	}

	@Override
	public float[] getDefaultScales() {
		return DEFAULT_SCALES;
	}

	@Override
	public boolean fadeOut() {
		return true;
	}

	@Override
	public boolean flipYaw() {
		return false;
	}

	@Override
	public boolean flipY() {
		return false;
	}

	@Override
	public boolean valid() {
		return super.valid() && this.vertexData != null && this.vertexData.length > 0;
	}

	@Override
	public void render(RenderGib renderGib, EntityGib entityGib, double x, double y, double z, float partialTicks, float[] scales) {
		float dX = this.dX(entityGib.scale);
		float dY = this.dY(entityGib.scale);
		float dZ = this.dZ(entityGib.scale);
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
		if(!entityGib.onGround) {
			rotAxisX = 1.0F;
			rotAxisY = 0.0F;
			rotAxisZ = 0.0F;
		}

		float alpha = 1.0F;
		if(entityGib.groundTime > ConfigHandler.PHYSICS.gibGroundLifetime - 20 || entityGib.ticksExisted > ConfigHandler.PHYSICS.gibLifetime - 20) {
			alpha = MathHelper.clamp(1.0F - ((float)Math.max(entityGib.groundTime - (ConfigHandler.PHYSICS.gibGroundLifetime - 20), entityGib.ticksExisted - (ConfigHandler.PHYSICS.gibLifetime - 20)) + partialTicks) / 20.0F, 0.0F, 1.0F);
		}

		GlStateManager.pushMatrix();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(180.0F - lerpRot(entityGib.prevRotationYaw, entityGib.rotationYaw, partialTicks), 0.0F, 1.0F, 0.0F);
		GlStateManager.enableRescaleNormal();
		GlStateManager.scale(entityGib.prepScaleX, entityGib.prepScaleY, entityGib.prepScaleZ);

		GlStateManager.rotate(lerpRot(entityGib.prevRotationPitch, entityGib.rotationPitch, partialTicks), rotAxisX, rotAxisY, rotAxisZ);
		GlStateManager.translate(-this.cX(entityGib.scale), -this.cY(entityGib.scale), -this.cZ(entityGib.scale));
		if(entityGib.onGround) {
			GlStateManager.translate(-rotAxisZ * (shortest / 2.0F + entityGib.zFightOffset), rotAxisY * (shortest / 2.0F + entityGib.zFightOffset), rotAxisX * (shortest / 2.0F + entityGib.zFightOffset));
		}
		GlStateManager.scale(entityGib.scale, entityGib.scale, entityGib.scale);

		if(ConfigHandler.TEXTURES.useGoreTextures && entityGib.baseTexture == GibGenerator.GORE_TEXTURE) {
			GlStateManager.pushMatrix();
			GlStateManager.matrixMode(5890);
			GlStateManager.loadIdentity();
			GlStateManager.scale(this.textureWidth / 16.0F, this.textureHeight / 16.0F, 1.0F);
			GlStateManager.matrixMode(5888);
		}

		renderGib.bindTexture(entityGib.baseTexture);
		this.drawVertices(alpha);
		if(entityGib.layerTexture != null) {
			renderGib.bindTexture(entityGib.layerTexture);
			this.drawVertices(alpha);
		}

		if(ConfigHandler.TEXTURES.useGoreTextures && entityGib.baseTexture == GibGenerator.GORE_TEXTURE) {
			GlStateManager.matrixMode(5890);
			GlStateManager.loadIdentity();
			GlStateManager.matrixMode(5888);
			GlStateManager.popMatrix();
		}

		GlStateManager.disableRescaleNormal();
		GlStateManager.color(1.0F, 1.0F, 1.0F,  1.0F);
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}

	private void drawVertices(float alpha) {
		BufferBuilder builder = Tessellator.getInstance().getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
		for(int i = 0; i < this.vertexData.length; i += VERTEX_SIZE) {
			builder.pos(this.vertexData[i], this.vertexData[i + 1], this.vertexData[i + 2])
					.tex(this.vertexData[i + 3], this.vertexData[i + 4])
					.color(1.0F, 1.0F, 1.0F, alpha)
					.normal(this.vertexData[i + 5], this.vertexData[i + 6], this.vertexData[i + 7])
					.endVertex();
		}
		Tessellator.getInstance().draw();
	}

	private static float lerpRot(float prev, float target, float partial) {
		float diff = ((target - prev + 540) % 360) - 180;
		return prev + diff * partial;
	}

	private void diveBones(GeoBone bone, Matrix4f parentMatrix, FloatArrayList data, boolean isRoot) {
		Matrix4f boneMatrix = new Matrix4f(parentMatrix);
		applyRestTransform(boneMatrix, bone);

		if(isRoot || !bone.isHidden()) {
			for(GeoCube cube : bone.childCubes) {
				Matrix4f cubeMatrix = new Matrix4f(boneMatrix);
				applyCubeTransform(cubeMatrix, cube);
				this.addCube(cube, cubeMatrix, data);
			}
		}
		if(this.renderChildren && !bone.childBonesAreHiddenToo()) {
			for(GeoBone childBone : bone.childBones) {
				this.diveBones(childBone, boneMatrix, data, false);
			}
		}
	}

	private void addCube(GeoCube cube, Matrix4f matrix, FloatArrayList data) {
		Matrix3f normalMatrix = new Matrix3f();
		matrix.getRotationScale(normalMatrix);
		for(GeoQuad quad : cube.quads) {
			if(quad == null) continue;
			Vector3f normal = new Vector3f(quad.normal.getX(), quad.normal.getY(), quad.normal.getZ());
			normalMatrix.transform(normal);
			if(normal.lengthSquared() > 0.0F) normal.normalize();

			if((cube.size.y == 0 || cube.size.z == 0) && normal.getX() < 0) normal.x *= -1;
			if((cube.size.x == 0 || cube.size.z == 0) && normal.getY() < 0) normal.y *= -1;
			if((cube.size.x == 0 || cube.size.y == 0) && normal.getZ() < 0) normal.z *= -1;

			for(GeoVertex vertex : quad.vertices) {
				Vector4f pos = new Vector4f(vertex.position.getX(), vertex.position.getY(), vertex.position.getZ(), 1.0F);
				matrix.transform(pos);

				this.minX = Math.min(this.minX, pos.x);
				this.minY = Math.min(this.minY, pos.y);
				this.minZ = Math.min(this.minZ, pos.z);
				this.maxX = Math.max(this.maxX, pos.x);
				this.maxY = Math.max(this.maxY, pos.y);
				this.maxZ = Math.max(this.maxZ, pos.z);

				data.add(pos.x);
				data.add(pos.y);
				data.add(pos.z);
				data.add(vertex.textureU);
				data.add(vertex.textureV);
				data.add(normal.x);
				data.add(normal.y);
				data.add(normal.z);
			}
		}
	}

	private static void applyParentRestTransforms(Matrix4f matrix, GeoBone parent) {
		if(parent == null) return;
		applyParentRestTransforms(matrix, parent.parent);
		applyRestTransform(matrix, parent);
	}

	private static void applyRestTransform(Matrix4f matrix, GeoBone bone) {
		BoneSnapshot snapshot = bone.getInitialSnapshot();
		float posX = snapshot != null ? snapshot.positionOffsetX : bone.getPositionX();
		float posY = snapshot != null ? snapshot.positionOffsetY : bone.getPositionY();
		float posZ = snapshot != null ? snapshot.positionOffsetZ : bone.getPositionZ();
		float rotX = snapshot != null ? snapshot.rotationValueX : bone.getRotationX();
		float rotY = snapshot != null ? snapshot.rotationValueY : bone.getRotationY();
		float rotZ = snapshot != null ? snapshot.rotationValueZ : bone.getRotationZ();
		float scaleX = snapshot != null ? snapshot.scaleValueX : bone.getScaleX();
		float scaleY = snapshot != null ? snapshot.scaleValueY : bone.getScaleY();
		float scaleZ = snapshot != null ? snapshot.scaleValueZ : bone.getScaleZ();

		translate(matrix, -posX / 16.0F, posY / 16.0F, posZ / 16.0F);
		translate(matrix, bone.rotationPointX / 16.0F, bone.rotationPointY / 16.0F, bone.rotationPointZ / 16.0F);
		if(rotZ != 0.0F) rotate(matrix, rotZ, 2);
		if(rotY != 0.0F) rotate(matrix, rotY, 1);
		if(rotX != 0.0F) rotate(matrix, rotX, 0);
		Matrix4f scale = new Matrix4f();
		scale.setIdentity();
		scale.setM00(scaleX);
		scale.setM11(scaleY);
		scale.setM22(scaleZ);
		matrix.mul(scale);
		translate(matrix, -bone.rotationPointX / 16.0F, -bone.rotationPointY / 16.0F, -bone.rotationPointZ / 16.0F);
	}

	private static void applyCubeTransform(Matrix4f matrix, GeoCube cube) {
		translate(matrix, cube.pivot.getX() / 16.0F, cube.pivot.getY() / 16.0F, cube.pivot.getZ() / 16.0F);
		rotate(matrix, cube.rotation.getZ(), 2);
		rotate(matrix, cube.rotation.getY(), 1);
		rotate(matrix, cube.rotation.getX(), 0);
		translate(matrix, -cube.pivot.getX() / 16.0F, -cube.pivot.getY() / 16.0F, -cube.pivot.getZ() / 16.0F);
	}

	private static void translate(Matrix4f matrix, float x, float y, float z) {
		Matrix4f translation = new Matrix4f();
		translation.setIdentity();
		translation.setTranslation(new Vector3f(x, y, z));
		matrix.mul(translation);
	}

	private static void rotate(Matrix4f matrix, float radians, int axis) {
		Matrix4f rotation = new Matrix4f();
		if(axis == 0) rotation.rotX(radians);
		else if(axis == 1) rotation.rotY(radians);
		else rotation.rotZ(radians);
		matrix.mul(rotation);
	}
}
