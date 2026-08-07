package com.cozycrafters.skinstatues.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/** Small vanilla-style form that dispatches the existing server commands. */
public final class SkinStatuesScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.skinstatues.title");
    private static final Component PLAYER_LABEL = Component.translatable("screen.skinstatues.player");
    private static final Component SCALE_LABEL = Component.translatable("screen.skinstatues.scale");
    private static final Component CREATE_LABEL = Component.translatable("screen.skinstatues.create");
    private static final Component UNDO_LABEL = Component.translatable("screen.skinstatues.undo");
    private static final Component UNAVAILABLE = Component.translatable("screen.skinstatues.unavailable");
    private static final Component PLAYER_REQUIRED = Component.translatable(
            "screen.skinstatues.validation.player_required");
    private static final Component PLAYER_INVALID = Component.translatable(
            "screen.skinstatues.validation.player_invalid");
    private static final Component SCALE_INVALID = Component.translatable(
            "screen.skinstatues.validation.scale_invalid");

    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CONTENT_HEIGHT = 170;

    private final String initialPlayerName;
    private boolean commandAvailable;
    private EditBox playerField;
    private EditBox scaleField;
    private Button createButton;
    private Button undoButton;
    private int contentLeft;
    private int contentTop;
    private int contentWidth;
    private Component status = Component.empty();

    public SkinStatuesScreen(String initialPlayerName, boolean commandAvailable) {
        super(TITLE);
        this.initialPlayerName = initialPlayerName;
        this.commandAvailable = commandAvailable;
    }

    @Override
    protected void init() {
        playerField = addRenderableWidget(new EditBox(font, 0, 0, 200, FIELD_HEIGHT, PLAYER_LABEL));
        playerField.setMaxLength(16);
        playerField.setValue(initialPlayerName);
        playerField.setResponder(ignored -> updateFormState());

        scaleField = addRenderableWidget(new EditBox(font, 0, 0, 200, FIELD_HEIGHT, SCALE_LABEL));
        scaleField.setMaxLength(10);
        scaleField.setValue("1");
        scaleField.setResponder(ignored -> updateFormState());

        createButton = addRenderableWidget(Button.builder(CREATE_LABEL, ignored -> createStatue()).build());
        undoButton = addRenderableWidget(Button.builder(UNDO_LABEL, ignored -> sendCommand(
                StatueMenuLogic.undoCommand())).build());

        repositionElements();
        updateFormState();
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(playerField);
    }

    @Override
    protected void repositionElements() {
        contentWidth = Math.min(260, Math.max(140, width - 40));
        contentLeft = (width - contentWidth) / 2;
        contentTop = Math.max(8, (height - CONTENT_HEIGHT) / 2);

        playerField.setRectangle(contentLeft, contentTop + 34, contentWidth, FIELD_HEIGHT);
        scaleField.setRectangle(contentLeft, contentTop + 74, contentWidth, FIELD_HEIGHT);
        createButton.setRectangle(contentLeft, contentTop + 126, contentWidth, BUTTON_HEIGHT);
        undoButton.setRectangle(contentLeft, contentTop + 150, contentWidth, BUTTON_HEIGHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int panelLeft = Math.max(2, contentLeft - 10);
        int panelRight = Math.min(width - 2, contentLeft + contentWidth + 10);
        int panelTop = Math.max(2, contentTop - 7);
        int panelBottom = Math.min(height - 2, contentTop + CONTENT_HEIGHT + 7);
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xA0000000);
        graphics.outline(panelLeft, panelTop, panelRight - panelLeft, panelBottom - panelTop, 0xFF555555);

        graphics.centeredText(font, TITLE, width / 2, contentTop, 0xFFFFFFFF);
        graphics.text(font, PLAYER_LABEL, contentLeft, contentTop + 22, 0xFFA0A0A0);
        graphics.text(font, SCALE_LABEL, contentLeft, contentTop + 62, 0xFFA0A0A0);
        if (!status.getString().isEmpty()) {
            graphics.textWithWordWrap(font, status, contentLeft, contentTop + 102, contentWidth, 0xFFFF5555);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)
                && createButton.active) {
            createStatue();
            return true;
        }
        return super.keyPressed(event);
    }

    private void updateFormState() {
        StatueMenuLogic.FormState state = StatueMenuLogic.state(
                playerField.getValue(), scaleField.getValue(), commandAvailable);
        createButton.active = state.canCreate();
        undoButton.active = state.canUndo();
        status = statusFor(state);
    }

    private Component statusFor(StatueMenuLogic.FormState state) {
        if (!commandAvailable) {
            return UNAVAILABLE;
        }
        return switch (state.validation()) {
            case VALID -> Component.empty();
            case PLAYER_REQUIRED -> PLAYER_REQUIRED;
            case PLAYER_INVALID -> PLAYER_INVALID;
            case SCALE_INVALID -> SCALE_INVALID;
        };
    }

    private void createStatue() {
        StatueMenuLogic.createCommand(playerField.getValue(), scaleField.getValue()).ifPresent(this::sendCommand);
    }

    private void sendCommand(String command) {
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || !commandAvailable) {
            commandAvailable = false;
            updateFormState();
            return;
        }
        connection.sendCommand(command);
        onClose();
    }
}
