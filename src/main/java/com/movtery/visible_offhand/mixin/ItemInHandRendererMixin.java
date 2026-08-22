package com.movtery.visible_offhand.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
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
    private static final boolean PUNCHY_LOADED = FabricLoader.getInstance().isModLoaded("punchy");

    @Shadow
    private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light,
                                 float equippedProgress, float swingProgress, HumanoidArm arm) {
    }

    @Shadow
    private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equippedProgress) {
    }

    @Shadow
    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
    }

    /**
     * Vanilla/normal Visible Offhand path. Punchy gets a separate path below
     * so that we never inject another render operation into its animation
     * method.
     */
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
        if (PUNCHY_LOADED || !getConfig().getOptions().doubleHands) {
            return;
        }

        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        Item mainHandItem = player.getMainHandItem().getItem();
        String mainHandItemId = BuiltInRegistries.ITEM.getKey(mainHandItem).toString();
        HumanoidArm offArm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();

        if (itemStack.isEmpty()
                && !getConfig().getOptions().handheldItems.contains(mainHandItemId)
                && !mainHand
                && !player.isInvisible()) {
            this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords,
                    inverseArmHeight, attack, offArm);
        }
    }

    /**
     * Punchy compatibility path.
     *
     * Punchy can replace/cancel the normal empty-hand render call, so the
     * old renderArmWithItem injection cannot reliably render Visible Offhand.
     * Instead, wait until the hand pass has completed and submit the empty
     * offhand ourselves.
     *
     * We recreate the vanilla arm transforms before calling renderPlayerArm.
     * The previous implementation called renderPlayerArm directly from the
     * end of renderHandsWithItems, which left the pose stack in the wrong
     * coordinate space and resulted in the arm being effectively invisible.
     */
    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void visibleOffhand$renderPunchyOffhand(
            float frameInterp,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LocalPlayer player,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (!PUNCHY_LOADED || !getConfig().getOptions().doubleHands || player.isInvisible()) {
            return;
        }

        // Punchy/vanilla should render a real offhand item itself. Visible
        // Offhand only supplies the missing empty arm.
        if (!player.getOffhandItem().isEmpty()) {
            return;
        }

        Item mainHandItem = player.getMainHandItem().getItem();
        String mainHandItemId = BuiltInRegistries.ITEM.getKey(mainHandItem).toString();
        if (getConfig().getOptions().handheldItems.contains(mainHandItemId)) {
            return;
        }

        HumanoidArm offArm = player.getMainArm().getOpposite();
        float attack = player.getAttackAnim(frameInterp);

        // Match the vanilla empty-arm rendering transform. Punchy's own
        // renderer has already finished, so this additive render cannot
        // recursively enter Punchy's animation pipeline.
        poseStack.pushPose();
        this.applyItemArmTransform(poseStack, offArm, 1.0F);
        this.applyItemArmAttackTransform(poseStack, offArm, attack);
        this.renderPlayerArm(
                poseStack,
                submitNodeCollector,
                lightCoords,
                1.0F,
                attack,
                offArm
        );
        poseStack.popPose();
    }
}
