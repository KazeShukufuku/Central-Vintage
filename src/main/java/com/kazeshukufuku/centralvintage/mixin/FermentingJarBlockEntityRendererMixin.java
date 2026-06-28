package com.kazeshukufuku.centralvintage.mixin;

import com.kazeshukufuku.centralvintage.content.pickling.FermentingJarOutput;
import com.kazeshukufuku.centralvintage.content.pickling.FermentingJarOutputVisuals;
import com.kazeshukufuku.centralvintage.content.pickling.TimedOutputItem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ribs.vintagedelight.block.entity.FermentingJarBlockEntity;
import net.ribs.vintagedelight.block.entity.renderer.FermentingJarBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FermentingJarBlockEntityRenderer.class)
public abstract class FermentingJarBlockEntityRendererMixin {
    @Inject(method = "render(Lnet/ribs/vintagedelight/block/entity/FermentingJarBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("TAIL"))
    private void centralvintage$renderVisualizedOutputItems(
            FermentingJarBlockEntity fermentingJar,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay,
            CallbackInfo ci
    ) {
        if (!(fermentingJar instanceof FermentingJarOutputVisuals visuals)) {
            return;
        }

        visuals.centralvintage$tickVisualizedOutputItems((int) AnimationTickHolder.getTicks());
        if (visuals.centralvintage$getVisualizedOutputItems().isEmpty()) {
            return;
        }

        BlockState state = fermentingJar.getBlockState();
        if (!state.hasProperty(FermentingJarOutput.FACING)) {
            return;
        }

        Direction direction = state.getValue(FermentingJarOutput.FACING);
        if (direction == Direction.DOWN) {
            return;
        }

        Vec3 directionVector = Vec3.atLowerCornerOf(direction.getNormal());
        Vec3 outVector = Vec3.atLowerCornerOf(BlockPos.ZERO)
                .add(0.5D, 0.16D, 0.5D)
                .add(directionVector.scale(0.42D));

        for (TimedOutputItem visualizedItem : visuals.centralvintage$getVisualizedOutputItems()) {
            float progress = 1.0F - (visualizedItem.ticksRemaining() - partialTicks) / FermentingJarOutput.OUTPUT_ANIMATION_TIME;
            progress = Math.max(0.0F, Math.min(1.0F, progress));

            poseStack.pushPose();
            TransformStack.of(poseStack)
                    .translate(outVector)
                    .translate(new Vec3(0.0D, Math.max(-0.45D, -(progress * progress * 1.35D)), 0.0D))
                    .translate(directionVector.scale(progress * 0.55D))
                    .rotateYDegrees(AngleHelper.horizontalAngle(direction))
                    .rotateXDegrees(progress * 180.0F);
            centralvintage$renderItem(poseStack, buffer, light, overlay, visualizedItem.stack());
            poseStack.popPose();
        }
    }

    private void centralvintage$renderItem(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                light,
                overlay,
                poseStack,
                buffer,
                minecraft.level,
                0
        );
    }
}
