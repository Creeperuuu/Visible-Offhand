package com.movtery.visible_offhand.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.movtery.visible_offhand.VisibleOffhandClient.getConfig;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    /**
     * Punchy has its own first-person hand renderer and animation pipeline.
     * Calling vanilla renderPlayerArm from inside Punchy's render path can cause
     * the two renderers to repeatedly invoke each other, resulting in severe
     * frame-time spikes, including in Punchy's animated main menu.
     *
     * Visible Offhand therefore lets Punchy take over hand rendering whenever
     * it is installed. Punchy already provides visible/dual-hand rendering.
     */
    private static final boolean PUNCHY_LOADED = FabricLoader.getInstance().isModLoaded("punchy");

    @Shadow
    private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light,
                                 float equippedProgress, float swingProgress, HumanoidArm arm) {
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void visibleOffhand$renderArmWithItem(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        // Punchy owns the first-person hand renderer when it is installed.
        // Do not add our extra renderPlayerArm call to its render pipeline.
        if (PUNCHY_LOADED || !getConfig().getOptions().doubleHands) {
            return;
        }

        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        Item mainHandItem = player.getMainHandItem().getItem();
        String mainHandItemId = BuiltInRegistries.ITEM.getKey(mainHandItem).toString();
        HumanoidArm offArm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();

        if (itemStack.isEmpty()
                && !getConfig().getOptions().handheldItems.contains(mainHandItemId)
                && (!mainHand && !player.isInvisible())) {
            this.renderPlayerArm(
                    poseStack,
                    submitNodeCollector,
                    lightCoords,
                    inverseArmHeight,
                    attack,
                    offArm
            );
        }
    }
}
