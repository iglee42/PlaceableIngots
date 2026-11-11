package fr.iglee42.placeableingots;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static fr.iglee42.placeableingots.IngotBlock.COUNT;

public class IngotBlockEntity extends BlockEntity {

    private List<ItemStack> ingots = new ArrayList<>();

    public IngotBlockEntity(BlockPos pos, BlockState state) {
        super(PlaceableIngots.INGOT_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        ingots.forEach(s->list.add(s.save(new CompoundTag())));
        tag.put("items",list);
    }

    @Override
    public void load(CompoundTag tag) {
        ingots.clear();
        tag.getList("items",ListTag.TAG_COMPOUND).forEach(t-> ingots.add(ItemStack.of((CompoundTag) t)));
        super.load(tag);
    }

    public List<ItemStack> getIngots() {
        return ingots;
    }

    public void addIngot(ItemStack ingot){
        ingots.add(ingot);
        setChanged();
    }


    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        ingots.forEach(s->list.add(s.save(new CompoundTag())));
        tag.put("items",list);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        if (level != null)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState().setValue(COUNT,ingots.size()), 3);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    public ItemStack removeLastIngot() {
        ItemStack stack = ingots.remove(ingots.size() - 1);
        setChanged();
        return stack.copyWithCount(1);
    }

    public void setChanged()
    {
        if (level != null && !level.isClientSide){
            level.setBlockAndUpdate(getBlockPos(),getBlockState().setValue(COUNT,ingots.size()));
            super.setChanged();
        }
    }

}
