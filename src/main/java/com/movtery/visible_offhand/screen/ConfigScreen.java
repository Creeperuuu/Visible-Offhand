package com.movtery.visible_offhand.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static com.movtery.visible_offhand.VisibleOffhandClient.getConfig;
import static com.movtery.visible_offhand.VisibleOffhandClient.reloadConfig;

public class ConfigScreen extends Screen {
    private final Screen parent;

    protected ConfigScreen(Screen parent) {
        super(Component.translatable("modmenu.nameTranslation.visible_offhand"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(CycleButton.booleanBuilder(
                        onOrOff(getConfig().getOptions().doubleHands),
                        onOrOff(!getConfig().getOptions().doubleHands)
                )
                .create(this.width / 2 - 112, this.height / 2, 110, 20,
                        Component.translatable("button.vo.double_hands"), (button, enabled) -> {
                            getConfig().getOptions().doubleHands = !getConfig().getOptions().doubleHands;
                            getConfig().save();
                        }));

        this.addRenderableWidget(Button.builder(Component.translatable("button.vo.reload_config"), button -> {
            reloadConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(this.width / 2 + 2, this.height / 2, 110, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(
                this.font,
                this.title,
                this.width / 2 - this.font.width(this.title) / 2,
                this.height / 2 - 30,
                0xFFFFFFFF,
                true
        );
    }

    private Component onOrOff(Boolean button) {
        return Component.translatable(button ? "button.vo.on" : "button.vo.off");
    }
}
