package fr.iglee42.placeableingots;

import com.mojang.logging.LogUtils;
import fr.iglee42.placeableingots.client.models.IngotBlockModel;
import fr.iglee42.placeableingots.config.ClientConfig;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.io.IOException;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PlaceableIngots.MODID)
public class PlaceableIngots {
    public static final String MODID = "placeableingots";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final RegistryObject<Block> INGOT_BLOCK = BLOCKS.register("ingot_block", IngotBlock::new);
    public static final RegistryObject<BlockEntityType<?>> INGOT_BLOCK_ENTITY = BLOCK_ENTITIES.register("ingot_block", ()->
            BlockEntityType.Builder.of(IngotBlockEntity::new,INGOT_BLOCK.get()).build(null));

    public PlaceableIngots() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void useItemOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.is(Tags.Items.INGOTS) || !player.mayBuild() || !player.isCrouching()) return;

        BlockPos clickedPos = event.getHitVec().getBlockPos();
        BlockPos targetPos = clickedPos;
        boolean stored = false;
        if (event.getLevel().getBlockState(clickedPos).is(INGOT_BLOCK.get())) {
            while (targetPos.getY() < event.getLevel().getMaxBuildHeight()) {
                if (event.getLevel().getBlockState(targetPos).is(INGOT_BLOCK.get())) {
                    if (!(event.getLevel().getBlockEntity(targetPos) instanceof IngotBlockEntity blockEntity)) return;
                    if (blockEntity.getIngots().size() < 64) {
                        blockEntity.addIngot(stack.copyWithCount(1));
                        stored = true;
                        break;
                    }
                    targetPos = targetPos.above();
                    continue;
                }

                if (!event.getLevel().getBlockState(targetPos).isAir()) return;
                stored = placeIngot(event, targetPos, stack);
                break;
            }
        } else {
            targetPos = clickedPos.relative(event.getFace());
            if (!event.getLevel().getBlockState(targetPos).isAir()) return;
            stored = placeIngot(event, targetPos, stack);
        }

        if (!stored) return;
        consumeItem(stack, player, event.getHand());
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    private boolean placeIngot(PlayerInteractEvent.RightClickBlock event, BlockPos pos, ItemStack stack) {
        if (!event.getLevel().setBlockAndUpdate(pos, INGOT_BLOCK.get().defaultBlockState())) return false;
        if (!(event.getLevel().getBlockEntity(pos) instanceof IngotBlockEntity blockEntity)) {
            event.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return false;
        }
        blockEntity.addIngot(stack.copyWithCount(1));
        return true;
    }

    private void consumeItem(ItemStack stack, Player player, InteractionHand hand) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            player.swing(hand);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) throws IOException, IllegalAccessException {
            ClientConfig.load();
            ItemBlockRenderTypes.setRenderLayer(INGOT_BLOCK.get(), RenderType.cutoutMipped());
        }


        @SubscribeEvent
        public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event)
        {
            event.register("ingot_block", IngotBlockModel.INSTANCE);
        }

    }
}
