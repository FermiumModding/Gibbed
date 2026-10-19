package fonnymunkey.gibbed.client;

import fonnymunkey.gibbed.client.gib.GibManager;
import fonnymunkey.gibbed.config.GibbedConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;

public class ClientEventHandler {

	public static void init() {
		ModLoadingContext.get().registerExtensionPoint(
				ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> AutoConfig.getConfigScreen(GibbedConfig.class, parent).get()));

		MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::onClientTick);
		MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::onRenderLevelStage);
	}

	private static void onClientTick(TickEvent.ClientTickEvent event) {
		if(event.phase == TickEvent.Phase.END) EventHandlerClient.onClientTickEnd();
	}

	private static void onRenderLevelStage(RenderLevelStageEvent event) {
		if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			GibManager.render(event.getPoseStack(), event.getCamera(), event.getPartialTick());
		}
	}
}