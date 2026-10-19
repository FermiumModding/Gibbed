package fonnymunkey.gibbed.mixin.vanilla;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AgeableListModel.class)
public interface AgeableListModelInvoker {
	@Invoker("headParts")
	Iterable<ModelPart> gibbed$invokeHeadParts();
	@Invoker("bodyParts")
	Iterable<ModelPart> gibbed$invokeBodyParts();
}