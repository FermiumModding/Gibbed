package fonnymunkey.gibbed;

import fonnymunkey.gibbed.client.EventHandlerClient;
import fonnymunkey.gibbed.client.gib.EntityGib;
import fonnymunkey.gibbed.client.gib.RenderGib;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Gibbed.MODID,
     name = Gibbed.NAME,
     version = Gibbed.VERSION,
     dependencies = "required-after:fermiumbooter",
     acceptableRemoteVersions = "*",
     clientSideOnly = true
)
public class Gibbed {
    public static final String MODID = "gibbed";
    public static final String VERSION = "1.0.2";
    public static final String NAME = "Gibbed";

    @Mod.Instance(MODID)
    public static Gibbed INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if(FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(EventHandlerClient.class);
            RenderingRegistry.registerEntityRenderingHandler(EntityGib.class, new RenderGib.RenderFactory());
        }
    }
}