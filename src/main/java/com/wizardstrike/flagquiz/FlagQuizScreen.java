package com.wizardstrike.flagquiz;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FlagQuizScreen extends Screen {
    private final CountryFlag countryFlag;
    private EditBox answerField;

    public FlagQuizScreen(CountryFlag countryFlag) {
        super(Component.literal("Flag Quiz"));
        this.countryFlag = countryFlag;
    }

    @Override
    protected void init() {
        int flagWidth = 240;
        int flagHeight = 150;
        int flagX = (this.width - flagWidth) / 2;
        int flagY = this.height / 2 - 92;

        StringWidget title = new StringWidget((this.width / 2) - 80, 20, 160, 20, Component.literal("Guess the country"), this.font);
        this.addRenderableWidget(title);

        ImageWidget flag = ImageWidget.texture(flagWidth, flagHeight, this.countryFlag.texture(), flagWidth, flagHeight);
        flag.setX(flagX);
        flag.setY(flagY);
        this.addRenderableWidget(flag);

        int inputWidth = 240;
        int inputHeight = 20;
        int x = (this.width - inputWidth) / 2;
        int y = this.height / 2 + 70;

        this.answerField = new EditBox(this.font, x, y, inputWidth, inputHeight, Component.literal("Country"));
        this.answerField.setMaxLength(64);
        this.answerField.setFocused(true);
        this.answerField.setHint(Component.literal("Type the country name..."));
        this.addRenderableWidget(this.answerField);

        this.addRenderableWidget(Button.builder(Component.literal("Submit"), button -> this.submitAnswer())
            .bounds(x, y + 28, inputWidth, 20)
            .build());

        this.setInitialFocus(this.answerField);
    }

    private void submitAnswer() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        String guess = this.answerField.getValue().trim();
        FlagQuizMod.handleAnswer(this.countryFlag, guess, this.minecraft);
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (this.answerField != null && this.answerField.isFocused()) {
            if (event.key() == 257 || event.key() == 335) {
                this.submitAnswer();
                return true;
            }
        }
        return super.keyPressed(event);
    }
}
