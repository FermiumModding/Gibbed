package fonnymunkey.gibbed.mixin.citadel;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AdvancedModelBox.class, remap = false)
public interface AdvancedModelBoxInvoker {
	@Invoker("doRender")
	void gibbed$invokeDoRender(PoseStack.Pose pose, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha);
}