package mod.traister101.test;

import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.constant.EmptyPart;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

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

	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
	private static final RegistryObject<Block> TEST_BLOCK = BLOCKS.register("test_block", () -> new TestBlock(BlockBehaviour.Properties.of()));

	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
	public static final RegistryObject<BlockEntityType<TestBE>> TEST_BE = BLOCK_ENTITIES.register("test_be",
			() -> BlockEntityType.Builder.of(TestBE::new, TEST_BLOCK.get()).build(new EmptyPart()));

	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
	public static final RegistryObject<MenuType<TestMenu>> ITEM_HANDLER_MENU = MENUS.register("item_test_menu",
			() -> IForgeMenuType.create(TestMenu::itemHandlerMenu));
	public static final RegistryObject<MenuType<TestMenu>> CONTAINER_MENU = MENUS.register("be_test_menu",
			() -> IForgeMenuType.create(TestMenu::containerMenu));

	public TestMod() {
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
		MENUS.register(modEventBus);

		if (FMLEnvironment.dist == Dist.CLIENT) {
			modEventBus.<FMLClientSetupEvent>addListener(event -> event.enqueueWork(() -> {
				MenuScreens.register(ITEM_HANDLER_MENU.get(), TestMenuScreen::new);
				MenuScreens.register(CONTAINER_MENU.get(), TestMenuScreen::new);
			}));
		}
	}
}