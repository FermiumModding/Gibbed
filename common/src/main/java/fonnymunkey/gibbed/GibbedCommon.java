package fonnymunkey.gibbed;

import fonnymunkey.gibbed.client.gib.handler.GibModelHandlers;
import fonnymunkey.gibbed.client.gib.handler.VanillaGibModelHandler;
import fonnymunkey.gibbed.client.particle.ParticleHandler;
import fonnymunkey.gibbed.client.particle.VanillaBloodParticleHandler;
import fonnymunkey.gibbed.config.ConfigHandler;

public class GibbedCommon {

    public static void init() {
        ConfigHandler.init();

        GibModelHandlers.register(new VanillaGibModelHandler());
        ParticleHandler.register(new VanillaBloodParticleHandler());
    }
}