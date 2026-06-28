package com.kazeshukufuku.centralvintage.client;

import com.kazeshukufuku.centralvintage.CentralVintage;
import com.kazeshukufuku.centralvintage.content.pickling.PicklingGuideScreen;
import com.kazeshukufuku.centralvintage.registry.CVMenus;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.mojang.math.Transformation;
import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber(modid = CentralVintage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CVClientEvents {
    private static final ResourceLocation FERMENTING_JAR_ID =
            new ResourceLocation("vintagedelight", "fermenting_jar");
    private static final ResourceLocation DIRECTIONAL_FERMENTING_JAR_MODEL =
            new ResourceLocation("vintagedelight", "block/fermenting_jar_directional");

    private CVClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new CVPonderPlugin());
        event.enqueueWork(() -> MenuScreens.register(CVMenus.PICKLING_GUIDE.get(), PicklingGuideScreen::new));
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(DIRECTIONAL_FERMENTING_JAR_MODEL);
    }

    @SubscribeEvent
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        BakedModel directionalJar = event.getModels().get(DIRECTIONAL_FERMENTING_JAR_MODEL);
        if (directionalJar == null) {
            return;
        }

        replaceDirectionalJarModel(event, Direction.SOUTH, directionalJar);
        replaceDirectionalJarModel(event, Direction.WEST, new RotatedBakedModel(directionalJar, 90));
        replaceDirectionalJarModel(event, Direction.NORTH, new RotatedBakedModel(directionalJar, 180));
        replaceDirectionalJarModel(event, Direction.EAST, new RotatedBakedModel(directionalJar, 270));
    }

    private static void replaceDirectionalJarModel(ModelEvent.ModifyBakingResult event, Direction facing, BakedModel model) {
        event.getModels().put(modelLocation(facing, false), model);
        event.getModels().put(modelLocation(facing, true), model);
    }

    private static ModelResourceLocation modelLocation(Direction facing, boolean waterlogged) {
        String variant = "facing=" + facing.getName() + ",waterlogged=" + waterlogged;
        return new ModelResourceLocation(FERMENTING_JAR_ID, variant);
    }

    private static final class RotatedBakedModel extends BakedModelWrapper<BakedModel> {
        private final IQuadTransformer transformer;

        private RotatedBakedModel(BakedModel originalModel, int yDegrees) {
            super(originalModel);
            this.transformer = QuadTransformers.applying(centeredYRotation(yDegrees));
        }

        private static Transformation centeredYRotation(int degrees) {
            Matrix4f matrix = new Matrix4f()
                    .translation(0.5F, 0.5F, 0.5F)
                    .rotateY((float) Math.toRadians(-degrees))
                    .translate(-0.5F, -0.5F, -0.5F);
            return new Transformation(matrix);
        }

        @Override
        public List<BakedQuad> getQuads(
                @Nullable BlockState state,
                @Nullable Direction side,
                RandomSource rand
        ) {
            return transformer.process(super.getQuads(state, side, rand));
        }

        @NotNull
        @Override
        public List<BakedQuad> getQuads(
                @Nullable BlockState state,
                @Nullable Direction side,
                @NotNull RandomSource rand,
                @NotNull ModelData extraData,
                @Nullable RenderType renderType
        ) {
            return transformer.process(super.getQuads(state, side, rand, extraData, renderType));
        }
    }
}
