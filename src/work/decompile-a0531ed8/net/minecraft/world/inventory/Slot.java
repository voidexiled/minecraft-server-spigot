package net.minecraft.world.inventory;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;

public class Slot {

    public final int slot;
    public final IInventory container;
    public int index;
    public final int x;
    public final int y;

    public Slot(IInventory iinventory, int i, int j, int k) {
        this.container = iinventory;
        this.slot = i;
        this.x = j;
        this.y = k;
    }

    public void onQuickCraft(ItemStack itemstack, ItemStack itemstack1) {
        int i = itemstack1.getCount() - itemstack.getCount();

        if (i > 0) {
            this.onQuickCraft(itemstack1, i);
        }

    }

    protected void onQuickCraft(ItemStack itemstack, int i) {}

    protected void onSwapCraft(int i) {}

    protected void checkTakeAchievements(ItemStack itemstack) {}

    public void onTake(EntityHuman entityhuman, ItemStack itemstack) {
        this.setChanged();
    }

    public boolean mayPlace(ItemStack itemstack) {
        return true;
    }

    public ItemStack getItem() {
        return this.container.getItem(this.slot);
    }

    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    public void setByPlayer(ItemStack itemstack) {
        this.setByPlayer(itemstack, this.getItem());
    }

    public void setByPlayer(ItemStack itemstack, ItemStack itemstack1) {
        this.set(itemstack);
    }

    public void set(ItemStack itemstack) {
        this.container.setItem(this.slot, itemstack);
        this.setChanged();
    }

    public void setChanged() {
        this.container.setChanged();
    }

    public int getMaxStackSize() {
        return this.container.getMaxStackSize();
    }

    public int getMaxStackSize(ItemStack itemstack) {
        return Math.min(this.getMaxStackSize(), itemstack.getMaxStackSize());
    }

    @Nullable
    public MinecraftKey getNoItemIcon() {
        return null;
    }

    public ItemStack remove(int i) {
        return this.container.removeItem(this.slot, i);
    }

    public boolean mayPickup(EntityHuman entityhuman) {
        return true;
    }

    public boolean isActive() {
        return true;
    }

    public Optional<ItemStack> tryRemove(int i, int j, EntityHuman entityhuman) {
        if (!this.mayPickup(entityhuman)) {
            return Optional.empty();
        } else if (!this.allowModification(entityhuman) && j < this.getItem().getCount()) {
            return Optional.empty();
        } else {
            i = Math.min(i, j);
            ItemStack itemstack = this.remove(i);

            if (itemstack.isEmpty()) {
                return Optional.empty();
            } else {
                if (this.getItem().isEmpty()) {
                    this.setByPlayer(ItemStack.EMPTY, itemstack);
                }

                return Optional.of(itemstack);
            }
        }
    }

    public ItemStack safeTake(int i, int j, EntityHuman entityhuman) {
        Optional<ItemStack> optional = this.tryRemove(i, j, entityhuman);

        optional.ifPresent((itemstack) -> {
            this.onTake(entityhuman, itemstack);
        });
        return (ItemStack) optional.orElse(ItemStack.EMPTY);
    }

    public ItemStack safeInsert(ItemStack itemstack) {
        return this.safeInsert(itemstack, itemstack.getCount());
    }

    public ItemStack safeInsert(ItemStack itemstack, int i) {
        if (!itemstack.isEmpty() && this.mayPlace(itemstack)) {
            ItemStack itemstack1 = this.getItem();
            int j = Math.min(Math.min(i, itemstack.getCount()), this.getMaxStackSize(itemstack) - itemstack1.getCount());

            if (j <= 0) {
                return itemstack;
            } else {
                if (itemstack1.isEmpty()) {
                    this.setByPlayer(itemstack.split(j));
                } else if (ItemStack.isSameItemSameComponents(itemstack1, itemstack)) {
                    itemstack.shrink(j);
                    itemstack1.grow(j);
                    this.setByPlayer(itemstack1);
                }

                return itemstack;
            }
        } else {
            return itemstack;
        }
    }

    public boolean allowModification(EntityHuman entityhuman) {
        return this.mayPickup(entityhuman) && this.mayPlace(this.getItem());
    }

    public int getContainerSlot() {
        return this.slot;
    }

    public boolean isHighlightable() {
        return true;
    }

    public boolean isFake() {
        return false;
    }
}
