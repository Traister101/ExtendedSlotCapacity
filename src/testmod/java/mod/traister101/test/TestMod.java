package mod.traister101.test;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.*;

@Mod(TestMod.MODID)
public final class TestMod {

	public static final String MODID = "testmod";
	public static final Logger LOGGER = LogUtils.getLogger();

	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
	private static final RegistryObject<Item> TEST_ITEM = ITEMS.register("test_item", () -> new TestItem(new Properties()));

	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
	public static final RegistryObject<MenuType<TestMenu>> TEST_MENU = MENUS.register("test_menu",
			() -> IForgeMenuType.create(TestMenu::fromNetwork));

	public TestMod() {
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		ITEMS.register(modEventBus);
		MENUS.register(modEventBus);

		if (FMLEnvironment.dist == Dist.CLIENT) {
			modEventBus.<FMLClientSetupEvent>addListener(event -> MenuScreens.register(TEST_MENU.get(), TestMenuScreen::new));
		}
	}
}