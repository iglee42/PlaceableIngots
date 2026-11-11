package fr.iglee42.placeableingots.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.iglee42.placeableingots.IngotBlock;
import fr.iglee42.placeableingots.IngotBlockEntity;
import fr.iglee42.placeableingots.ItemColorHelper;
import fr.iglee42.placeableingots.PlaceableIngots;
import fr.iglee42.placeableingots.client.RenderHelpers;
import fr.iglee42.placeableingots.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class IngotBlockModel implements SimpleStaticBlockModel<IngotBlockModel> {

    public static final IngotBlockModel INSTANCE = new IngotBlockModel();


    @Override
    public TextureAtlasSprite render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, BlockState state, BlockEntity blockEntity) {
        IngotBlockEntity ibe = (IngotBlockEntity) blockEntity;
        poseStack.pushPose();

        float pixelSize = 1/32f;
        float xSize = 14*pixelSize;
        float ySize = 4*pixelSize;
        float zSize = 6*pixelSize;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(BLOCKS_ATLAS).apply(new ResourceLocation(PlaceableIngots.MODID,"block/ingot"));

        for (int i = 0; i < state.getValue(IngotBlock.COUNT); i++){
            poseStack.pushPose();
            if (i >= ibe.getIngots().size())continue;
            ItemStack stack = ibe.getIngots().get(i);
            int color = ItemColorHelper.getColor(stack);

            int layer = Math.floorDiv(i , 8);
            int rowIndex = (i - layer*8)  % 4;
            int index = (i - layer*8) % 8;
            float paddingX = pixelSize + index > 4 ? xSize + 2*pixelSize : 0;
            float paddingY = layer * ySize;
            float paddingZ = pixelSize * (rowIndex+1) + rowIndex*(pixelSize+zSize);


            if (layer % 2 == 1){
                poseStack.translate(0.5f,0,0.5f);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.translate(-0.5f,0,-0.5f);
            }

            poseStack.translate(pixelSize,0,0);

            RenderHelpers.renderTexturedCuboid(poseStack,buffer,sprite,packedLight,packedOverlay,paddingX,paddingY,paddingZ,paddingX + xSize,paddingY + ySize,paddingZ + zSize,true, color);

            poseStack.popPose();
        }

        poseStack.popPose();

        return sprite;
    }

    @Override
    public int faces(BlockState state) {
        return state.getValue(IngotBlock.COUNT)*6;
    }
}
