package fonnymunkey.gibbed.client;

import fonnymunkey.gibbed.client.gib.GibManager;
import fonnymunkey.gibbed.config.GibbedConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public class ClientEventHandler {

	public static void init(ModContainer container) {
		container.registerExtensionPoint(
				IConfigScreenFactory.class,
				(modContainer, parent) -> AutoConfig.getConfigScreen(GibbedConfig.class, parent).get());

		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onClientTick);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onRenderLevelStage);
	}

	private static void onClientTick(ClientTickEvent.Post event) {
		EventHandlerClient.onClientTickEnd();
	}

	private static void onRenderLevelStage(RenderLevelStageEvent event) {
		if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			GibManager.render(event.getPoseStack(), event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
		}
	}
}