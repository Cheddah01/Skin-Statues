package com.cozycrafters.skinstatues.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/** Small vanilla-style form that dispatches the existing server commands. */
public final class SkinStatuesScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.skinstatues.title");
    private static final Component SUBTITLE = Component.translatable("screen.skinstatues.subtitle");
    private static final Component PLAYER_LABEL = Component.translatable("screen.skinstatues.player");
    private static final Component SCALE_LABEL = Component.translatable("screen.skinstatues.scale");
    private static final Component SCALE_HINT = Component.translatable("screen.skinstatues.scale_hint");
    private static final Component CREATE_LABEL = Component.translatable("screen.skinstatues.create");
    private static final Component UNDO_LABEL = Component.translatable("screen.skinstatues.undo");
    private static final Component UNAVAILABLE = Component.translatable("screen.skinstatues.unavailable");
    private static final Component PLAYER_REQUIRED = Component.translatable(
            "screen.skinstatues.validation.player_required");
    private static final Component PLAYER_INVALID = Component.translatable(
            "screen.skinstatues.validation.player_invalid");
    private static final Component SCALE_INVALID = Component.translatable(
            "screen.skinstatues.validation.scale_invalid");

    private final String initialPlayerName;
    private boolean commandAvailable;
    private EditBox playerField;
    private EditBox scaleField;
    private Button createButton;
    private Button undoButton;
    private StatueMenuLayout layout;
    private Component status = SCALE_HINT;
    private boolean statusIsError;

    public SkinStatuesScreen(String initialPlayerName, boolean commandAvailable) {
        super(TITLE);
        this.initialPlayerName = initialPlayerName;
        this.commandAvailable = commandAvailable;
    }

    @Override
    protected void init() {
        playerField = addRenderableWidget(new EditBox(
                font, 0, 0, 200, StatueMenuLayout.WIDGET_HEIGHT, PLAYER_LABEL));
        playerField.setMaxLength(16);
        playerField.setValue(initialPlayerName);
        playerField.setResponder(ignored -> updateFormState());

        scaleField = addRenderableWidget(new EditBox(
                font, 0, 0, 200, StatueMenuLayout.WIDGET_HEIGHT, SCALE_LABEL));
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
        layout = StatueMenuLayout.calculate(width, height);
        applyBounds(playerField, layout.playerField());
        applyBounds(scaleField, layout.scaleField());
        applyBounds(createButton, layout.createButton());
        applyBounds(undoButton, layout.undoButton());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        StatueMenuLayout.Bounds panel = layout.panel();
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xCC101010);
        graphics.outline(panel.x(), panel.y(), panel.width(), panel.height(), 0xFF707070);

        graphics.centeredText(font, TITLE, panel.x() + panel.width() / 2, layout.titleY(), 0xFFFFFFFF);
        if (layout.showSubtitle()) {
            graphics.centeredText(
                    font, SUBTITLE, panel.x() + panel.width() / 2, layout.subtitleY(), 0xFFAAAAAA);
        }
        graphics.text(font, PLAYER_LABEL, layout.playerField().x(), layout.playerLabelY(), 0xFFE0E0E0);
        graphics.text(font, SCALE_LABEL, layout.scaleField().x(), layout.scaleLabelY(), 0xFFE0E0E0);
        graphics.textWithWordWrap(
                font,
                status,
                layout.scaleField().x(),
                layout.statusY(),
                layout.scaleField().width(),
                statusIsError ? 0xFFFF5555 : 0xFF909090);

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
        statusIsError = !commandAvailable || state.validation() != StatueMenuLogic.Validation.VALID;
    }

    private Component statusFor(StatueMenuLogic.FormState state) {
        if (!commandAvailable) {
            return UNAVAILABLE;
        }
        return switch (state.validation()) {
            case VALID -> SCALE_HINT;
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

    private static void applyBounds(AbstractWidget widget, StatueMenuLayout.Bounds bounds) {
        widget.setSize(bounds.width(), bounds.height());
        widget.setPosition(bounds.x(), bounds.y());
    }
}
