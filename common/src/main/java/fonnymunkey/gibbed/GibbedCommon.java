package fonnymunkey.gibbed;

import fonnymunkey.gibbed.client.gib.handler.GibModelHandlers;
import fonnymunkey.gibbed.client.gib.handler.VanillaGibModelHandler;
import fonnymunkey.gibbed.client.particle.ParticleHandler;
import fonnymunkey.gibbed.client.particle.VanillaBloodParticleHandler;
import fonnymunkey.gibbed.compat.azurelib.AzureLibGibModelHandler;
import fonnymunkey.gibbed.compat.geckolib.GeckoLibGibModelHandler;
import fonnymunkey.gibbed.config.ConfigHandler;
import fonnymunkey.gibbed.util.ModLoadedUtil;

public class GibbedCommon {

    public static void init() {
        ConfigHandler.init();

        GibModelHandlers.register(new VanillaGibModelHandler());
        ParticleHandler.register(new VanillaBloodParticleHandler());

        if(ModLoadedUtil.isGeckoLibLoaded()) GibModelHandlers.register(new GeckoLibGibModelHandler());
        if(ModLoadedUtil.isAzureLibLoaded()) GibModelHandlers.register(new AzureLibGibModelHandler());
    }
}