package com.kazeshukufuku.centralvintage.client;

import com.kazeshukufuku.centralvintage.CentralVintage;
import com.simibubi.create.foundation.item.ItemDescription;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CentralVintage.MOD_ID, value = Dist.CLIENT)
public final class CVForgeClientEvents {
    private static final ResourceLocation FERMENTING_JAR_ID =
            new ResourceLocation("vintagedelight", "fermenting_jar");
    private static final FontHelper.Palette TOOLTIP_PALETTE = FontHelper.Palette.STANDARD_CREATE;

    private CVForgeClientEvents() {
    }

    @SubscribeEvent
    public static void addFermentingJarTooltip(ItemTooltipEvent event) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (FERMENTING_JAR_ID.equals(itemId)) {
            event.getToolTip().addAll(ItemDescription.create(event.getItemStack().getItem(), TOOLTIP_PALETTE).getCurrentLines());
        }
    }
}
