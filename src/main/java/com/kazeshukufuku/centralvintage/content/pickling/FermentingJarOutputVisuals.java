package com.kazeshukufuku.centralvintage.content.pickling;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface FermentingJarOutputVisuals {
    List<TimedOutputItem> centralvintage$getVisualizedOutputItems();

    void centralvintage$visualizeOutput(ItemStack stack);

    void centralvintage$tickVisualizedOutputItems(int currentTick);
}
