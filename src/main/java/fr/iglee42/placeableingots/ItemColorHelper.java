package fr.iglee42.placeableingots;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.mojang.blaze3d.platform.NativeImage;
import fr.iglee42.placeableingots.config.ClientConfig;
import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@EventBusSubscriber(modid = PlaceableIngots.MODID,value = Dist.CLIENT)
public class ItemColorHelper {

    public static final LoadingCache<Item, Integer> ITEM_COLOR =
            CacheBuilder.newBuilder().build(new CacheLoader<>() {
                @Override
                public Integer load(Item item) {
                    return computeItemColor(item);
                }
            });

    @SubscribeEvent
    public static void cleanCache(TextureAtlasStitchedEvent event){
        if (event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
            ITEM_COLOR.invalidateAll();
        }
    }

    private static int computeItemColor(Item item) {

        AtomicInteger color = new AtomicInteger(-1);

        ClientConfig.colorOverrides.entrySet().stream().filter(e->e.getKey().equals(BuiltInRegistries.ITEM.getKey(item).toString())).findAny().ifPresent(e->{
            Color c = new Color(e.getValue());
            color.set(FastColor.ARGB32.color(c.getAlpha(),c.getRed(), c.getGreen(), c.getBlue()));
        });

        if (color.get() != -1)
            return color.get();

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null)
            return -1;

        BakedModel itemModel = Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(item),null,null,0);
        TextureAtlasSprite sprite = itemModel.getParticleIcon(ModelData.EMPTY);


        if (sprite == null)
            return -1;

        NativeImage img = sprite.contents().getOriginalImage();

        int w = img.getWidth();
        int h = img.getHeight();

        int[] xs = {w / 4, w / 2, (3 * w) / 4};
        int[] ys = {h / 4, h / 2, (3 * h) / 4};

        float r = 0;
        float g = 0;
        float b = 0;

        int samples = 0;

        for (int x : xs) {
            for (int y : ys) {

                int col = abgrToArgb(img.getPixelRGBA(x, y));

                int a = (col >> 24) & 255;

                if (a < 16)
                    continue;

                r += ((col >> 16) & 255) / 255f;
                g += ((col >> 8) & 255) / 255f;
                b += (col & 255) / 255f;

                samples++;
            }
        }

        if (samples == 0)
            return -1;

        r /= samples;
        g /= samples;
        b /= samples;

        return colorClamp(r, g, b, 1f);
    }

    public static int getColor(ItemStack stack) {

        if (stack == null)
            return -1;

        Item item = stack.getItem();

        int base;

        try {
            base = ITEM_COLOR.get(item);
        } catch (ExecutionException e) {
            base = -1;
        }

        int tint = Minecraft.getInstance().getItemColors().getColor(stack, 0);

        if (tint == -1)
            return base;

        float r = ((tint >> 16) & 255) / 255f * ((base >> 16) & 255) / 255f;
        float g = ((tint >> 8) & 255) / 255f * ((base >> 8) & 255) / 255f;
        float b = (tint & 255) / 255f * (base & 255) / 255f;

        return colorClamp(r, g, b, 1f);
    }

    private static int abgrToArgb(int c) {
        int a = (c >> 24) & 255;
        int b = (c >> 16) & 255;
        int g = (c >> 8) & 255;
        int r = c & 255;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int colorClamp(float r, float g, float b, float a) {

        int ri = (int) (Math.min(1, r) * 255);
        int gi = (int) (Math.min(1, g) * 255);
        int bi = (int) (Math.min(1, b) * 255);
        int ai = (int) (Math.min(1, a) * 255);

        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
}