package com.movtery.visible_offhand.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.registries.BuiltInRegistries;
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

    /**
     * Normal Visible Offhand path. This stays inside vanilla's arm rendering
     * method so the existing behavior is unchanged when Punchy is absent.
     */
    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void visibleOffhand$renderArmWithItem(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            net.minecraft.world.InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        // Punchy has its own first-person renderer. Its renderArmWithItem path
        // must not be modified by Visible Offhand because that can conflict
        // with Punchy's animation/render state.
        if (PUNCHY_LOADED || !getConfig().getOptions().doubleHands) {
            return;
        }

        boolean mainHand = hand == net.minecraft.world.InteractionHand.MAIN_HAND;
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
     * We wait until the entire vanilla/Punchy hand pass has finished and then
     * submit one additional empty offhand arm. This avoids injecting into
     * Punchy's renderArmWithItem animation pipeline while still showing the
     * offhand arm that Visible Offhand is intended to provide.
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

        Item mainHandItem = player.getMainHandItem().getItem();
        String mainHandItemId = BuiltInRegistries.ITEM.getKey(mainHandItem).toString();

        // Only add the arm when the offhand is empty. If Punchy/vanilla is
        // already rendering an actual offhand item, do not duplicate it.
        if (!player.getOffhandItem().isEmpty()
                || mainHandItem == null
                || getConfig().getOptions().handheldItems.contains(mainHandItemId)) {
            return;
        }

        HumanoidArm offArm = player.getMainArm().getOpposite();
        float attack = player.getAttackAnim(frameInterp);

        // Render after Punchy has completed its own pass, using the same
        // collector. The arm is intentionally rendered additively so Punchy's
        // animations remain untouched.
        this.renderPlayerArm(
                poseStack,
                submitNodeCollector,
                lightCoords,
                1.0F,
                attack,
                offArm
        );
    }
}
