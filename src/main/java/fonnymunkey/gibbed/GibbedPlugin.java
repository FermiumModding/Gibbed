package fonnymunkey.gibbed;

import fermiumbooter.FermiumRegistryAPI;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class GibbedPlugin implements IFMLLoadingPlugin {
	
	public GibbedPlugin() {
		FermiumRegistryAPI.enqueueMixin(false, "mixins.gibbed.vanilla.json");
		FermiumRegistryAPI.enqueueMixin(true, "mixins.gibbed.llibrary.json", () -> FermiumRegistryAPI.isModPresent("llibrary"));
		FermiumRegistryAPI.enqueueMixin(true, "mixins.gibbed.mobends.json", () -> FermiumRegistryAPI.isModPresent("mobends"));
		FermiumRegistryAPI.enqueueMixin(true, "mixins.gibbed.lycanites.json", () -> FermiumRegistryAPI.isModPresent("lycanitesmobs"));
		FermiumRegistryAPI.enqueueMixin(true, "mixins.gibbed.geckolib.json", () -> FermiumRegistryAPI.isModPresent("geckolib3"));
		FermiumRegistryAPI.enqueueMixin(false, "mixins.gibbed.geckolib.vanilla.json", () -> FermiumRegistryAPI.isModPresent("geckolib3"));
	}
	
	@Override
	public String[] getASMTransformerClass() {
		return new String[0];
	}
	
	@Override
	public String getModContainerClass() {
		return null;
	}
	
	@Override
	public String getSetupClass() {
		return null;
	}
	
	@Override
	public void injectData(Map<String, Object> data) {}
	
	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}