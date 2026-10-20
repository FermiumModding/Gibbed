package fonnymunkey.gibbed.compat.geckolib;

import fonnymunkey.gibbed.compat.BoneGib;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.util.RenderUtil;

import java.util.List;

public class GeoGib extends BoneGib<GeoBone> {

	public GeoGib(GeoBone bone, boolean renderChildren) {
		super(bone, renderChildren);
	}

	@Override
	protected GeoBone getParent(GeoBone bone) {
		return bone.getParent();
	}

	@Override
	protected List<GeoBone> getChildren(GeoBone bone) {
		return bone.getChildBones();
	}

	@Override
	protected boolean isHidden(GeoBone bone) {
		return bone.isHidden();
	}

	@Override
	protected boolean isHidingChildren(GeoBone bone) {
		return bone.isHidingChildren();
	}

	@Override
	protected void applyRestTransform(Matrix4f matrix, GeoBone bone) {
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

	@Override
	protected void bakeCubes(GeoBone bone, Matrix4f boneMatrix) {
		Vector4f position = new Vector4f();
		for(GeoCube cube : bone.getCubes()) {
			Matrix4f cubeMatrix = new Matrix4f(boneMatrix);
			cubeMatrix.translate((float)cube.pivot().x() / 16.0F, (float)cube.pivot().y() / 16.0F, (float)cube.pivot().z() / 16.0F);
			cubeMatrix.rotateZ((float)cube.rotation().z());
			cubeMatrix.rotateY((float)cube.rotation().y());
			cubeMatrix.rotateX((float)cube.rotation().x());
			cubeMatrix.translate(-(float)cube.pivot().x() / 16.0F, -(float)cube.pivot().y() / 16.0F, -(float)cube.pivot().z() / 16.0F);
			Matrix3f normalMatrix = cubeMatrix.normal(new Matrix3f());

			for(GeoQuad quad : cube.quads()) {
				if(quad == null) continue;
				Vector3f normal = normalMatrix.transform(new Vector3f(quad.normal()));
				if(normal.lengthSquared() > 0.0F) normal.normalize();
				RenderUtil.fixInvertedFlatCube(cube, normal);

				for(GeoVertex vertex : quad.vertices()) {
					cubeMatrix.transform(position.set(vertex.position(), 1.0F));
					this.addVertex(position, vertex.texU(), vertex.texV(), normal);
				}
			}
		}
	}
}