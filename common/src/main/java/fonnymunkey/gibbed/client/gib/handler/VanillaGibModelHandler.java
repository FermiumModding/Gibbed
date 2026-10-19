package fonnymunkey.gibbed.client.gib.handler;

import fonnymunkey.gibbed.Constants;
import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.BasicGib;
import fonnymunkey.gibbed.mixin.vanilla.AgeableListModelInvoker;
import fonnymunkey.gibbed.mixin.vanilla.ModelPartAccessor;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class VanillaGibModelHandler implements IGibModelHandler {

	@Override
	public boolean canHandle(EntityRenderer<?> renderer) {
		return renderer instanceof LivingEntityRenderer;
	}

	@Override
	public Object getGibCacheKey(LivingEntity entity, EntityRenderer<?> renderer) {
		return renderer;
	}

	@Override
	public List<AbstractGib> generateGibs(Object cacheKey, LivingEntity entity, EntityRenderer<?> renderer, float maxBranchSizeRatio, float maxBranchesRatio) {
		if(!(renderer instanceof LivingEntityRenderer<?,?> livingRenderer)) return Collections.emptyList();
		Set<ModelPart> modelParts = collectModelParts(livingRenderer.getModel());
		if(modelParts.isEmpty()) return Collections.emptyList();

		Set<ModelPart> nestedParts = new HashSet<>();
		for(ModelPart part : modelParts) {
			part.getAllParts().skip(1).forEach(nestedParts::add);
		}
		List<ModelPart> rootParts = new ArrayList<>();
		for(ModelPart part : modelParts) {
			if(!nestedParts.contains(part)) rootParts.add(part);
		}

		if(maxBranchSizeRatio <= 0.0F || maxBranchesRatio <= 0.0F || nestedParts.isEmpty()) return generateGibsSimple(rootParts);
		else return generateGibsBranched(rootParts, maxBranchSizeRatio, maxBranchesRatio);
	}

	private static Set<ModelPart> collectModelParts(EntityModel<?> model) {
		Set<ModelPart> parts = new LinkedHashSet<>();
		if(model instanceof HierarchicalModel<?> hierarchicalModel) {
			parts.add(hierarchicalModel.root());
		}
		if(model instanceof ListModel<?> listModel) {
			listModel.parts().forEach(parts::add);
		}
		if(model instanceof AgeableListModel<?> ageableListModel) {
			((AgeableListModelInvoker)ageableListModel).gibbed$invokeHeadParts().forEach(parts::add);
			((AgeableListModelInvoker)ageableListModel).gibbed$invokeBodyParts().forEach(parts::add);
		}
		for(Class<?> clazz = model.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
			for(Field field : clazz.getDeclaredFields()) {
				if(Modifier.isStatic(field.getModifiers())) continue;
				Class<?> type = field.getType();
				boolean isPart = ModelPart.class.isAssignableFrom(type);
				boolean isPartArray = type.isArray() && ModelPart.class.isAssignableFrom(type.getComponentType());
				boolean isIterable = Iterable.class.isAssignableFrom(type);
				if(!isPart && !isPartArray && !isIterable) continue;
				try {
					field.setAccessible(true);
					Object value = field.get(model);
					if(value instanceof ModelPart part) parts.add(part);
					else if(value instanceof ModelPart[] partArray) parts.addAll(Arrays.asList(partArray));
					else if(value instanceof Iterable<?> iterable) {
						for(Object object : iterable) {
							if(object instanceof ModelPart part) parts.add(part);
						}
					}
				}
				catch(Exception ex) {
					Constants.LOG.debug("Failed to read model part field {} of {}", field.getName(), clazz.getName());
				}
			}
		}
		parts.remove(null);
		return parts;
	}

	private static Map<ModelPart,Matrix4f> computeParentTransforms(List<ModelPart> rootParts) {
		Map<ModelPart,Matrix4f> transforms = new HashMap<>();
		for(ModelPart root : rootParts) {
			diveTransforms(root, new Matrix4f(), transforms);
		}
		return transforms;
	}

	private static void diveTransforms(ModelPart part, Matrix4f parentTransform, Map<ModelPart,Matrix4f> transforms) {
		transforms.put(part, parentTransform);
		Matrix4f childTransform = new Matrix4f(parentTransform);
		BasicGib.applyRestTransform(childTransform, part);
		for(ModelPart childPart : ((ModelPartAccessor)(Object)part).gibbed$getChildren().values()) {
			diveTransforms(childPart, childTransform, transforms);
		}
	}

	private static List<AbstractGib> generateGibsSimple(List<ModelPart> rootParts) {
		Map<ModelPart,Matrix4f> parentTransforms = computeParentTransforms(rootParts);
		List<AbstractGib> generatedGibs = new ArrayList<>();
		for(ModelPart root : rootParts) {
			root.getAllParts().forEach(part -> {
				AbstractGib gib = new BasicGib(part, false, parentTransforms.get(part));
				if(gib.valid()) generatedGibs.add(gib);
			});
		}
		return generatedGibs;
	}

	private static List<AbstractGib> generateGibsBranched(List<ModelPart> rootParts, float maxBranchSizeRatio, float maxBranchesRatio) {
		Map<ModelPart,Matrix4f> parentTransforms = computeParentTransforms(rootParts);
		FullBody fullBody = new FullBody(rootParts);
		if(!fullBody.valid()) return Collections.emptyList();
		float fullArea = fullBody.dX() * fullBody.dY() * fullBody.dZ();
		float maxBranchSize = maxBranchSizeRatio * fullArea;
		float maxBranchFill = maxBranchesRatio * fullArea;

		List<AbstractGib> generatedGibs = new ArrayList<>();
		float branchFill = 0.0F;
		Queue<ModelPart> rootQueue = new LinkedList<>(rootParts);
		while(!rootQueue.isEmpty()) {
			ModelPart part = rootQueue.poll();
			Collection<ModelPart> children = ((ModelPartAccessor)(Object)part).gibbed$getChildren().values();
			if(!children.isEmpty() && branchFill < maxBranchFill) {
				AbstractGib gibBranch = new BasicGib(part, true, parentTransforms.get(part));
				if(gibBranch.valid()) {
					float branchSize = gibBranch.dX(1.0F) * gibBranch.dY(1.0F) * gibBranch.dZ(1.0F);
					if(branchSize <= maxBranchSize && branchFill + branchSize <= maxBranchFill) {
						generatedGibs.add(gibBranch);
						branchFill += branchSize;
						continue;
					}
				}
			}

			AbstractGib gibSingle = new BasicGib(part, false, parentTransforms.get(part));
			if(gibSingle.valid()) generatedGibs.add(gibSingle);

			rootQueue.addAll(children);
		}
		return generatedGibs;
	}

	//Lazy way to get full gib model bounds
	protected static class FullBody {
		private float minX = Float.MAX_VALUE;
		private float minY = Float.MAX_VALUE;
		private float minZ = Float.MAX_VALUE;
		private float maxX = -Float.MAX_VALUE;
		private float maxY = -Float.MAX_VALUE;
		private float maxZ = -Float.MAX_VALUE;

		protected FullBody(List<ModelPart> parts) {
			for(ModelPart part : parts) {
				PartPose pose = part.getInitialPose();
				this.diveCubes(part, pose.x, pose.y, pose.z);
			}
		}

		protected float dX() {
			return this.maxX - this.minX;
		}

		protected float dY() {
			return this.maxY - this.minY;
		}

		protected float dZ() {
			return this.maxZ - this.minZ;
		}

		protected boolean valid() {
			return this.minX != Float.MAX_VALUE && this.minY != Float.MAX_VALUE && this.minZ != Float.MAX_VALUE &&
					this.maxX != -Float.MAX_VALUE && this.maxY != -Float.MAX_VALUE && this.maxZ != -Float.MAX_VALUE;
		}

		protected void diveCubes(ModelPart part, float rotPointX, float rotPointY, float rotPointZ) {
			for(ModelPart.Cube cube : ((ModelPartAccessor)(Object)part).gibbed$getCubes()) {
				if(cube.minX + rotPointX < minX) minX = cube.minX + rotPointX;
				if(cube.maxX + rotPointX > maxX) maxX = cube.maxX + rotPointX;
				if(cube.minY + rotPointY < minY) minY = cube.minY + rotPointY;
				if(cube.maxY + rotPointY > maxY) maxY = cube.maxY + rotPointY;
				if(cube.minZ + rotPointZ < minZ) minZ = cube.minZ + rotPointZ;
				if(cube.maxZ + rotPointZ > maxZ) maxZ = cube.maxZ + rotPointZ;
			}
			for(ModelPart childPart : ((ModelPartAccessor)(Object)part).gibbed$getChildren().values()) {
				PartPose pose = childPart.getInitialPose();
				diveCubes(childPart, rotPointX + pose.x, rotPointY + pose.y, rotPointZ + pose.z);
			}
		}
	}
}