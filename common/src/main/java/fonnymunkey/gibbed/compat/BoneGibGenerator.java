package fonnymunkey.gibbed.compat;

import fonnymunkey.gibbed.client.gib.AbstractGib;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class BoneGibGenerator {

	public static <B> List<AbstractGib> generateGibs(List<B> rootBones, Function<B,List<B>> getChildren, BiFunction<B,Boolean,AbstractGib> createGib, float maxBranchSizeRatio, float maxBranchesRatio) {
		if(rootBones.isEmpty()) return Collections.emptyList();

		boolean nested = false;
		for(B bone : rootBones) {
			if(!getChildren.apply(bone).isEmpty()) {
				nested = true;
				break;
			}
		}

		if(maxBranchSizeRatio <= 0.0F || maxBranchesRatio <= 0.0F || !nested) return generateGibsSimple(rootBones, getChildren, createGib);
		else return generateGibsBranched(rootBones, getChildren, createGib, maxBranchSizeRatio, maxBranchesRatio);
	}

	private static <B> List<AbstractGib> generateGibsSimple(List<B> rootBones, Function<B,List<B>> getChildren, BiFunction<B,Boolean,AbstractGib> createGib) {
		List<AbstractGib> generatedGibs = new ArrayList<>();
		Queue<B> boneQueue = new LinkedList<>(rootBones);
		while(!boneQueue.isEmpty()) {
			B bone = boneQueue.poll();
			AbstractGib gib = createGib.apply(bone, false);
			if(gib.valid()) generatedGibs.add(gib);
			boneQueue.addAll(getChildren.apply(bone));
		}
		return generatedGibs;
	}

	private static <B> List<AbstractGib> generateGibsBranched(List<B> rootBones, Function<B,List<B>> getChildren, BiFunction<B,Boolean,AbstractGib> createGib, float maxBranchSizeRatio, float maxBranchesRatio) {
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for(B bone : rootBones) {
			AbstractGib fullGib = createGib.apply(bone, true);
			if(!fullGib.valid()) continue;
			minX = Math.min(minX, fullGib.cX(1.0F) - fullGib.dX(1.0F) / 2.0F);
			minY = Math.min(minY, fullGib.cY(1.0F) - fullGib.dY(1.0F) / 2.0F);
			minZ = Math.min(minZ, fullGib.cZ(1.0F) - fullGib.dZ(1.0F) / 2.0F);
			maxX = Math.max(maxX, fullGib.cX(1.0F) + fullGib.dX(1.0F) / 2.0F);
			maxY = Math.max(maxY, fullGib.cY(1.0F) + fullGib.dY(1.0F) / 2.0F);
			maxZ = Math.max(maxZ, fullGib.cZ(1.0F) + fullGib.dZ(1.0F) / 2.0F);
		}
		if(minX == Float.MAX_VALUE) return Collections.emptyList();
		float fullArea = (maxX - minX) * (maxY - minY) * (maxZ - minZ);
		float maxBranchSize = maxBranchSizeRatio * fullArea;
		float maxBranchFill = maxBranchesRatio * fullArea;

		List<AbstractGib> generatedGibs = new ArrayList<>();
		float branchFill = 0.0F;
		Queue<B> rootQueue = new LinkedList<>(rootBones);
		while(!rootQueue.isEmpty()) {
			B bone = rootQueue.poll();
			List<B> children = getChildren.apply(bone);
			if(!children.isEmpty() && branchFill < maxBranchFill) {
				AbstractGib gibBranch = createGib.apply(bone, true);
				if(gibBranch.valid()) {
					float branchSize = gibBranch.dX(1.0F) * gibBranch.dY(1.0F) * gibBranch.dZ(1.0F);
					if(branchSize <= maxBranchSize && branchFill + branchSize <= maxBranchFill) {
						generatedGibs.add(gibBranch);
						branchFill += branchSize;
						continue;
					}
				}
			}

			AbstractGib gibSingle = createGib.apply(bone, false);
			if(gibSingle.valid()) generatedGibs.add(gibSingle);

			rootQueue.addAll(children);
		}
		return generatedGibs;
	}
}