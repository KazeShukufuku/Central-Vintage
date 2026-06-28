package com.kazeshukufuku.centralvintage.content.pickling;

import com.kazeshukufuku.centralvintage.CentralVintage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Collections;
import java.util.List;

public class PicklingGuideScreen extends AbstractSimiContainerScreen<PicklingGuideMenu> {
    private static final ResourceLocation TEXTURE =
            CentralVintage.id("textures/gui/pickling_guide.png");
    private static final float TITLE_SCALE = 0.75F;

    private List<Rect2i> extraAreas = Collections.emptyList();

    public PicklingGuideScreen(PicklingGuideMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        setWindowSize(184, 84 + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        setWindowOffset(-40, 0);
        super.init();

        int guideLeft = getLeftOfCentered(176);
        extraAreas = List.of(
                new Rect2i(guideLeft + 176 + 16, topPos + 16, 48, 48),
                new Rect2i(guideLeft, topPos, imageWidth, imageHeight)
        );
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int inventoryLeft = getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.getWidth());
        renderPlayerInventory(graphics, inventoryLeft, topPos + 84);

        int guideLeft = getLeftOfCentered(176);
        renderGuide(graphics, guideLeft, topPos);
        renderTitle(graphics, guideLeft, topPos);
        GuiGameElement.GuiRenderBuilder guideIcon = GuiGameElement.of(menu.getGuideStack());
        guideIcon.at(guideLeft + 176 + 16, topPos + 16, -200);
        guideIcon.scale(3).render(graphics);
    }

    private void renderTitle(GuiGraphics graphics, int x, int y) {
        float titleX = x + (176 - font.width(title) * TITLE_SCALE) / 2.0F;
        float titleY = y + 4.0F;

        graphics.pose().pushPose();
        graphics.pose().translate(titleX, titleY, 0);
        graphics.pose().scale(TITLE_SCALE, TITLE_SCALE, 1);
        graphics.drawString(font, title, 0, 0, 0xFFFFFF, false);
        graphics.pose().popPose();
    }

    private void renderGuide(GuiGraphics graphics, int x, int y) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, x, y, 0, 0, 184, 80);

        ClientLevel level = Minecraft.getInstance().level;
        int blazeStatus = menu.getBlazeStatus();
        if (level == null || blazeStatus <= 0) {
            graphics.blit(TEXTURE, x + 88, y + 24, 0, 80, 24, 24);
        } else {
            int frame = (int) (level.getGameTime() / 5L % 3L) + 1;
            graphics.blit(TEXTURE, x + 88, y + 24, 24 * frame, 80, 24, 24);
        }

        graphics.blit(TEXTURE, x + 80, y + 66, 96, 80 + blazeStatus * 6, 80, 6);
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }
}
