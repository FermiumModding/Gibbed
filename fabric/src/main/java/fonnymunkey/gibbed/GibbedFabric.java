package fonnymunkey.gibbed;

import fonnymunkey.gibbed.client.EventHandlerClient;
import fonnymunkey.gibbed.client.gib.GibManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

@Environment(EnvType.CLIENT)
public class GibbedFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        GibbedCommon.init();

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> EventHandlerClient.onClientTickEnd());
        WorldRenderEvents.AFTER_ENTITIES.register(context -> GibManager.render(context.matrixStack(), context.camera(), context.tickCounter().getGameTimeDeltaPartialTick(false)));
    }
}