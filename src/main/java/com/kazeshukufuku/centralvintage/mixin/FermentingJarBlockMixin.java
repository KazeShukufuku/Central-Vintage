package com.kazeshukufuku.centralvintage.mixin;

import com.kazeshukufuku.centralvintage.content.pickling.FermentingJarOutputController;
import com.kazeshukufuku.centralvintage.content.pickling.FermentingJarOutput;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.ribs.vintagedelight.block.custom.FermentingJarBlock;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FermentingJarBlock.class)
public abstract class FermentingJarBlockMixin extends BaseEntityBlock implements IWrenchable {
    protected FermentingJarBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void centralvintage$registerDefaultDirectionalState(Properties properties, CallbackInfo ci) {
        registerDefaultState(defaultBlockState().setValue(FermentingJarOutput.FACING, Direction.DOWN));
    }

    @Inject(method = "createBlockStateDefinition", at = @At("RETURN"))
    private void centralvintage$addDirectionalState(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(FermentingJarOutput.FACING);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void centralvintage$setDirectionalStateOnPlacement(
            BlockPlaceContext context,
            CallbackInfoReturnable<@Nullable BlockState> cir
    ) {
        BlockState state = cir.getReturnValue();
        if (state != null) {
            cir.setReturnValue(state.setValue(FermentingJarOutput.FACING, Direction.DOWN));
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void centralvintage$useWrenchInsteadOfOpeningGui(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (!AllItems.WRENCH.isIn(heldItem)) {
            return;
        }

        if (player.mayBuild()) {
            onWrenched(state, new UseOnContext(player, hand, hit));
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
            if (blockEntity instanceof FermentingJarOutputController outputController) {
                outputController.centralvintage$onWrenched(context.getClickedFace());
            }
        }
        return InteractionResult.SUCCESS;
    }
}
