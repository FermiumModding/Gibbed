package fonnymunkey.gibbed;

import fonnymunkey.gibbed.client.ClientEventHandler;
import fonnymunkey.gibbed.client.gib.handler.GibModelHandlers;
import fonnymunkey.gibbed.compat.citadel.CitadelGibModelHandler;
import fonnymunkey.gibbed.util.ModLoadedUtil;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;

@Mod(Constants.MOD_ID)
public class GibbedForge {

    public GibbedForge() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));

        if(FMLEnvironment.dist.isClient()) {
            GibbedCommon.init();
            ClientEventHandler.init();

            if(ModLoadedUtil.isCitadelLoaded()) GibModelHandlers.register(new CitadelGibModelHandler());
        }
    }
}