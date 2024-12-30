package net.minecraft.world.entity.vehicle;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.World;

public class ChestBoat extends AbstractChestBoat {

    public ChestBoat(EntityTypes<? extends ChestBoat> entitytypes, World world, Supplier<Item> supplier) {
        super(entitytypes, world, supplier);
    }

    @Override
    protected double rideHeight(EntitySize entitysize) {
        return (double) (entitysize.height() / 3.0F);
    }
}