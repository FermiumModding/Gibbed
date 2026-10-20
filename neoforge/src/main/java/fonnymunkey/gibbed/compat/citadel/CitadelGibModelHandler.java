package fonnymunkey.gibbed.compat.citadel;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.handler.IGibModelHandler;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

import java.util.*;

public class CitadelGibModelHandler implements IGibModelHandler {

	@Override
	public boolean canHandle(EntityRenderer<?> renderer) {
		return renderer instanceof LivingEntityRenderer<?,?> livingRenderer && livingRenderer.getModel() instanceof AdvancedEntityModel<?>;
	}

	@Override
	public Object getGibCacheKey(LivingEntity entity, EntityRenderer<?> renderer) {
		return renderer instanceof LivingEntityRenderer<?,?> livingRenderer ? livingRenderer.getModel() : null;
	}

	@Override
	public List<AbstractGib> generateGibs(Object cacheKey, LivingEntity entity, EntityRenderer<?> renderer, float maxBranchSizeRatio, float maxBranchesRatio) {
		if(!(cacheKey instanceof AdvancedEntityModel<?> model)) return Collections.emptyList();

		Set<AdvancedModelBox> modelBoxes = new LinkedHashSet<>();
		for(BasicModelPart part : model.parts()) {
			if(part instanceof AdvancedModelBox box) modelBoxes.add(box);
		}
		if(modelBoxes.isEmpty()) return Collections.emptyList();

		Set<AdvancedModelBox> nestedBoxes = new HashSet<>();
		for(AdvancedModelBox box : modelBoxes) {
			collectChildren(box, nestedBoxes);
		}
		List<AdvancedModelBox> rootBoxes = new ArrayList<>();
		for(AdvancedModelBox box : modelBoxes) {
			if(!nestedBoxes.contains(box)) rootBoxes.add(box);
		}

		if(maxBranchSizeRatio <= 0.0F || maxBranchesRatio <= 0.0F || nestedBoxes.isEmpty()) return generateGibsSimple(rootBoxes);
		else return generateGibsBranched(rootBoxes, maxBranchSizeRatio, maxBranchesRatio);
	}

	private static List<AdvancedModelBox> getChildren(AdvancedModelBox box) {
		List<AdvancedModelBox> children = new ArrayList<>();
		for(BasicModelPart child : box.childModels) {
			if(child instanceof AdvancedModelBox childBox) children.add(childBox);
		}
		return children;
	}

	private static void collectChildren(AdvancedModelBox box, Set<AdvancedModelBox> children) {
		for(AdvancedModelBox child : getChildren(box)) {
			if(children.add(child)) collectChildren(child, children);
		}
	}

	private static Map<AdvancedModelBox,Matrix4f> computeParentTransforms(List<AdvancedModelBox> rootBoxes) {
		Map<AdvancedModelBox,Matrix4f> transforms = new HashMap<>();
		for(AdvancedModelBox root : rootBoxes) {
			diveTransforms(root, new Matrix4f(), transforms);
		}
		return transforms;
	}

	private static void diveTransforms(AdvancedModelBox box, Matrix4f parentTransform, Map<AdvancedModelBox,Matrix4f> transforms) {
		transforms.put(box, parentTransform);
		Matrix4f childTransform = new Matrix4f(parentTransform);
		CitadelGib.applyRestTransform(childTransform, box);
		for(AdvancedModelBox child : getChildren(box)) {
			diveTransforms(child, childTransform, transforms);
		}
	}

	private static List<AbstractGib> generateGibsSimple(List<AdvancedModelBox> rootBoxes) {
		Map<AdvancedModelBox,Matrix4f> parentTransforms = computeParentTransforms(rootBoxes);
		List<AbstractGib> generatedGibs = new ArrayList<>();
		for(Map.Entry<AdvancedModelBox,Matrix4f> entry : parentTransforms.entrySet()) {
			AbstractGib gib = new CitadelGib(entry.getKey(), false, entry.getValue());
			if(gib.valid()) generatedGibs.add(gib);
		}
		return generatedGibs;
	}

	private static List<AbstractGib> generateGibsBranched(List<AdvancedModelBox> rootBoxes, float maxBranchSizeRatio, float maxBranchesRatio) {
		Map<AdvancedModelBox,Matrix4f> parentTransforms = computeParentTransforms(rootBoxes);
		FullBody fullBody = new FullBody(rootBoxes);
		if(!fullBody.valid()) return Collections.emptyList();
		float fullArea = fullBody.dX() * fullBody.dY() * fullBody.dZ();
		float maxBranchSize = maxBranchSizeRatio * fullArea;
		float maxBranchFill = maxBranchesRatio * fullArea;

		List<AbstractGib> generatedGibs = new ArrayList<>();
		float branchFill = 0.0F;
		Queue<AdvancedModelBox> rootQueue = new LinkedList<>(rootBoxes);
		while(!rootQueue.isEmpty()) {
			AdvancedModelBox box = rootQueue.poll();
			List<AdvancedModelBox> children = getChildren(box);
			if(!children.isEmpty() && branchFill < maxBranchFill) {
				AbstractGib gibBranch = new CitadelGib(box, true, parentTransforms.get(box));
				if(gibBranch.valid()) {
					float branchSize = gibBranch.dX(1.0F) * gibBranch.dY(1.0F) * gibBranch.dZ(1.0F);
					if(branchSize <= maxBranchSize && branchFill + branchSize <= maxBranchFill) {
						generatedGibs.add(gibBranch);
						branchFill += branchSize;
						continue;
					}
				}
			}

			AbstractGib gibSingle = new CitadelGib(box, false, parentTransforms.get(box));
			if(gibSingle.valid()) generatedGibs.add(gibSingle);

			rootQueue.addAll(children);
		}
		return generatedGibs;
	}

	protected static class FullBody {
		private float minX = Float.MAX_VALUE;
		private float minY = Float.MAX_VALUE;
		private float minZ = Float.MAX_VALUE;
		private float maxX = -Float.MAX_VALUE;
		private float maxY = -Float.MAX_VALUE;
		private float maxZ = -Float.MAX_VALUE;

		protected FullBody(List<AdvancedModelBox> boxes) {
			for(AdvancedModelBox box : boxes) {
				this.diveCubes(box, box.defaultPositionX, box.defaultPositionY, box.defaultPositionZ);
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

		protected void diveCubes(AdvancedModelBox box, float rotPointX, float rotPointY, float rotPointZ) {
			for(TabulaModelRenderUtils.ModelBox cube : box.cubeList) {
				if(cube.posX1 + rotPointX < minX) minX = cube.posX1 + rotPointX;
				if(cube.posX2 + rotPointX > maxX) maxX = cube.posX2 + rotPointX;
				if(cube.posY1 + rotPointY < minY) minY = cube.posY1 + rotPointY;
				if(cube.posY2 + rotPointY > maxY) maxY = cube.posY2 + rotPointY;
				if(cube.posZ1 + rotPointZ < minZ) minZ = cube.posZ1 + rotPointZ;
				if(cube.posZ2 + rotPointZ > maxZ) maxZ = cube.posZ2 + rotPointZ;
			}
			for(AdvancedModelBox child : getChildren(box)) {
				diveCubes(child, rotPointX + child.defaultPositionX, rotPointY + child.defaultPositionY, rotPointZ + child.defaultPositionZ);
			}
		}
	}
}