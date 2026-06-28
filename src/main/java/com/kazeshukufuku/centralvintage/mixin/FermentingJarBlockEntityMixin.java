package com.kazeshukufuku.centralvintage.mixin;

import com.kazeshukufuku.centralvintage.content.pickling.FermentingJarOutputController;
import com.kazeshukufuku.centralvintage.content.pickling.FermentingJarOutputVisuals;
import com.kazeshukufuku.centralvintage.content.pickling.PicklingAcceleration;
import com.kazeshukufuku.centralvintage.content.pickling.FermentingJarOutput;
import com.kazeshukufuku.centralvintage.content.pickling.TimedOutputItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ribs.vintagedelight.block.entity.FermentingJarBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(FermentingJarBlockEntity.class)
public abstract class FermentingJarBlockEntityMixin implements FermentingJarOutputController, FermentingJarOutputVisuals {
    @Unique
    private static final String CENTRALVINTAGE_PREFERRED_OUTPUT_KEY = "CentralVintagePreferredOutput";

    @Unique
    private static final String CENTRALVINTAGE_DISABLED_OUTPUTS_KEY = "CentralVintageDisabledOutputs";

    @Unique
    private static final String CENTRALVINTAGE_VISUALIZED_ITEMS_KEY = "CentralVintageVisualizedItems";

    @Shadow
    private int progress;

    @Shadow
    private int maxProgress;

    @Unique
    private double centralvintage$fermentingAccelerationRemainder;

    @Unique
    private final List<Direction> centralvintage$disabledOutputs = new ArrayList<>();

    @Unique
    private Direction centralvintage$preferredOutput;

    @Unique
    private final List<TimedOutputItem> centralvintage$visualizedOutputItems = new ArrayList<>();

    @Unique
    private int centralvintage$lastVisualizedOutputTick = -1;

    @Shadow
    private void increaseCraftingProgress() {
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/ribs/vintagedelight/block/entity/FermentingJarBlockEntity;increaseCraftingProgress()V"
            )
    )
    private void centralvintage$increaseProgressWithBlazeStoveAcceleration(FermentingJarBlockEntity instance) {
        increaseCraftingProgress();

        BlockEntity blockEntity = (BlockEntity) (Object) this;
        double multiplier = PicklingAcceleration.getFermentingSpeedMultiplier(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (multiplier <= 1.0D || progress >= maxProgress) {
            centralvintage$fermentingAccelerationRemainder = 0.0D;
            return;
        }

        double extraProgress = multiplier - 1.0D + centralvintage$fermentingAccelerationRemainder;
        int extraSteps = (int) Math.floor(extraProgress);
        centralvintage$fermentingAccelerationRemainder = extraProgress - extraSteps;

        for (int i = 0; i < extraSteps && progress < maxProgress; i++) {
            increaseCraftingProgress();
        }
    }

    @Override
    public List<Direction> centralvintage$getDisabledOutputs() {
        return centralvintage$disabledOutputs;
    }

    @Override
    public Direction centralvintage$getPreferredOutput() {
        return centralvintage$preferredOutput;
    }

    @Override
    public void centralvintage$onWrenched(Direction face) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        BlockState blockState = blockEntity.getBlockState();
        if (!blockState.hasProperty(FermentingJarOutput.FACING)) {
            return;
        }

        Direction currentFacing = blockState.getValue(FermentingJarOutput.FACING);
        centralvintage$disabledOutputs.remove(face);
        if (currentFacing == face) {
            if (centralvintage$preferredOutput == face) {
                centralvintage$preferredOutput = null;
            }
            centralvintage$disabledOutputs.add(face);
        } else {
            centralvintage$preferredOutput = face;
        }

        Level level = blockEntity.getLevel();
        if (level != null) {
            FermentingJarOutput.updateOutputDirection(
                    level,
                    blockEntity.getBlockPos(),
                    blockState,
                    centralvintage$disabledOutputs,
                    centralvintage$preferredOutput
            );
        }
        blockEntity.setChanged();
    }

    @Override
    public List<TimedOutputItem> centralvintage$getVisualizedOutputItems() {
        return centralvintage$visualizedOutputItems;
    }

    @Override
    public void centralvintage$visualizeOutput(ItemStack stack) {
        if (centralvintage$visualizedOutputItems.size() >= 3) {
            return;
        }

        BlockEntity blockEntity = (BlockEntity) (Object) this;
        ItemStack visualizedStack = stack.copy();
        visualizedStack.setCount(Math.min(visualizedStack.getCount(), visualizedStack.getMaxStackSize()));
        centralvintage$visualizedOutputItems.add(new TimedOutputItem(FermentingJarOutput.OUTPUT_ANIMATION_TIME, visualizedStack));
        Level level = blockEntity.getLevel();
        if (level != null) {
            BlockState state = blockEntity.getBlockState();
            level.sendBlockUpdated(blockEntity.getBlockPos(), state, state, 2);
        }
    }

    @Override
    public void centralvintage$tickVisualizedOutputItems(int currentTick) {
        if (centralvintage$lastVisualizedOutputTick == -1) {
            centralvintage$lastVisualizedOutputTick = currentTick;
            return;
        }

        int elapsedTicks = currentTick - centralvintage$lastVisualizedOutputTick;
        if (elapsedTicks <= 0) {
            return;
        }

        centralvintage$lastVisualizedOutputTick = currentTick;
        centralvintage$visualizedOutputItems.forEach(item -> item.decrement(elapsedTicks));
        centralvintage$visualizedOutputItems.removeIf(TimedOutputItem::isFinished);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void centralvintage$saveDirectionalOutputSettings(CompoundTag tag, CallbackInfo ci) {
        if (centralvintage$preferredOutput != null) {
            tag.putString(CENTRALVINTAGE_PREFERRED_OUTPUT_KEY, centralvintage$preferredOutput.getName());
        }

        ListTag disabledOutputs = new ListTag();
        for (Direction direction : centralvintage$disabledOutputs) {
            disabledOutputs.add(StringTag.valueOf(direction.getName()));
        }
        tag.put(CENTRALVINTAGE_DISABLED_OUTPUTS_KEY, disabledOutputs);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void centralvintage$loadDirectionalOutputSettings(CompoundTag tag, CallbackInfo ci) {
        centralvintage$preferredOutput = null;
        if (tag.contains(CENTRALVINTAGE_PREFERRED_OUTPUT_KEY, Tag.TAG_STRING)) {
            centralvintage$preferredOutput = Direction.byName(tag.getString(CENTRALVINTAGE_PREFERRED_OUTPUT_KEY));
        }

        centralvintage$disabledOutputs.clear();
        ListTag disabledOutputs = tag.getList(CENTRALVINTAGE_DISABLED_OUTPUTS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < disabledOutputs.size(); i++) {
            Direction direction = Direction.byName(disabledOutputs.getString(i));
            if (direction != null) {
                centralvintage$disabledOutputs.add(direction);
            }
        }

        if (tag.contains(CENTRALVINTAGE_VISUALIZED_ITEMS_KEY, Tag.TAG_LIST)) {
            centralvintage$visualizedOutputItems.clear();
            ListTag visualizedItems = tag.getList(CENTRALVINTAGE_VISUALIZED_ITEMS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < visualizedItems.size(); i++) {
                centralvintage$visualizedOutputItems.add(new TimedOutputItem(
                        FermentingJarOutput.OUTPUT_ANIMATION_TIME,
                        ItemStack.of(visualizedItems.getCompound(i))
                ));
            }
            centralvintage$lastVisualizedOutputTick = -1;
        }
    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"), cancellable = true)
    private void centralvintage$addVisualizedOutputsToUpdateTag(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        if (centralvintage$visualizedOutputItems.isEmpty()) {
            return;
        }

        ListTag visualizedItems = new ListTag();
        for (TimedOutputItem item : centralvintage$visualizedOutputItems) {
            visualizedItems.add(item.stack().serializeNBT());
        }
        tag.put(CENTRALVINTAGE_VISUALIZED_ITEMS_KEY, visualizedItems);
        centralvintage$visualizedOutputItems.clear();
    }

    @Inject(method = "resetProgress", at = @At("HEAD"))
    private void centralvintage$resetAccelerationRemainder(CallbackInfo ci) {
        centralvintage$fermentingAccelerationRemainder = 0.0D;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void centralvintage$exportOutputsToDirectionalTarget(
            Level level,
            BlockPos pos,
            BlockState state,
            CallbackInfo ci
    ) {
        if (!level.isClientSide) {
            FermentingJarOutput.exportOutputs((FermentingJarBlockEntity) (Object) this, level, pos, state);
        }
    }
}
