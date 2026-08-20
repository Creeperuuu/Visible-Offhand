package com.movtery.visible_offhand;

import com.mojang.blaze3d.platform.InputConstants;
import com.movtery.visible_offhand.config.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Path;

public class VisibleOffhandClient implements ClientModInitializer {
    private static Config config = null;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(VisibleOffhand.MOD_ID, "visible_offhand")
    );

    private final KeyMapping doubleHands = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.visible_offhand.double_hands",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    ));

    public static Config getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    private static void loadConfig() {
        Path configPath = FabricLoader.getInstance().getConfigDir();
        File configFile = new File(configPath.toFile(), "visible_offhand.json");
        config = new Config(configFile);
        config.load();
    }

    public static void reloadConfig() {
        if (config == null) {
            loadConfig();
        }
        config.load();
        VisibleOffhand.LOGGER.info("The configuration file has been reloaded!");
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (doubleHands.consumeClick()) {
                getConfig().getOptions().doubleHands = !getConfig().getOptions().doubleHands;
                getConfig().save();

                if (client.player != null) {
                    Component component = Component.translatable("button.vo.double_hands")
                            .append(" : ")
                            .append(Component.translatable(
                                    getConfig().getOptions().doubleHands
                                            ? "button.vo.on"
                                            : "button.vo.off"
                            ));
                    client.player.sendOverlayMessage(component);
                }
            }
        });
    }
}
