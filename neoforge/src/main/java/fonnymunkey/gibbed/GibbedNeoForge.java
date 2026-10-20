package fonnymunkey.gibbed;

import fonnymunkey.gibbed.client.ClientEventHandler;
import fonnymunkey.gibbed.client.gib.handler.GibModelHandlers;
import fonnymunkey.gibbed.compat.citadel.CitadelGibModelHandler;
import fonnymunkey.gibbed.util.ModLoadedUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class GibbedNeoForge {

    public GibbedNeoForge(ModContainer container) {
        GibbedCommon.init();
        ClientEventHandler.init(container);

        if(ModLoadedUtil.isCitadelLoaded()) GibModelHandlers.register(new CitadelGibModelHandler());
    }
}