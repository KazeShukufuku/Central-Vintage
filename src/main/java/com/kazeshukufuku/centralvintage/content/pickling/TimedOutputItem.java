package com.kazeshukufuku.centralvintage.content.pickling;

import net.minecraft.world.item.ItemStack;

public class TimedOutputItem {
    private int ticksRemaining;
    private final ItemStack stack;

    public TimedOutputItem(int ticksRemaining, ItemStack stack) {
        this.ticksRemaining = ticksRemaining;
        this.stack = stack;
    }

    public int ticksRemaining() {
        return ticksRemaining;
    }

    public ItemStack stack() {
        return stack;
    }

    public void decrement(int ticks) {
        ticksRemaining -= ticks;
    }

    public boolean isFinished() {
        return ticksRemaining <= 0;
    }
}
