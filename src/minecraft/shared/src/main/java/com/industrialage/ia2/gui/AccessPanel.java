package com.industrialage.ia2.gui;

import com.industrialage.ia2.access.MachineAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Client layout for a modal, authoritative editor for the six faces of a fixed device. */
final class AccessPanel {
    static final int ENTRY_WIDTH = 26;
    static final int ENTRY_GUTTER = ENTRY_WIDTH + 8;
    private static final int ROW_HEIGHT = 22;
    private static final int FIRST_ROW = 38;
    private static final int TEXT = 0xff263340;
    private static final String KEY = "gui.ic2.access.";
    private static final String[] FACES = {"up", "down", "left", "right", "front", "back"};
    private static final String[] MODES = {"full", "blocked", "output", "input"};

    private final DeviceAccessMenu menu;
    private final IntConsumer publish;
    private final Button entry;
    private final List<Button> choices = new ArrayList<>();
    private boolean open;
    private int page;
    private int focus = -1;
    private int rows;
    private int screenWidth;
    private int screenHeight;
    private int left;
    private int top;
    private int width;
    private int height;
    private int lastState = -1;

    AccessPanel(DeviceAccessMenu menu, IntConsumer publish) {
        this.menu = menu;
        this.publish = publish;
        entry = DeviceScreen.accessWidget(0, 0, ENTRY_WIDTH, 20, message("button"), this::open);
    }

    private static Component message(String path) {
        return Component.translatable(KEY + path);
    }

    Button entry() {
        return entry;
    }

    static Component title() {
        return message("title");
    }

    boolean isOpen() {
        return open;
    }

    void move(int screenWidth, int screenHeight, int panelRight, int panelTop, int panelHeight) {
        DeviceScreen.moveAccessWidget(
                entry,
                panelRight + 4,
                Math.max(
                        2, Math.min(screenHeight - 22, panelTop + Math.min(26, panelHeight - 20))));
        if (this.screenWidth != screenWidth || this.screenHeight != screenHeight) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            lastState = -1;
        }
        int state = menu.accessData();
        if (open && lastState != state) rebuild(state);
    }

    private void open() {
        open = true;
        page = 0;
        focus = -1;
        lastState = -1;
    }

    void close() {
        open = false;
        choices.clear();
    }

    private void page(int direction) {
        int next = Math.max(0, Math.min((FACES.length - 1) / rows, page + direction));
        if (next == page) return;
        page = next;
        focus = -1;
        lastState = -1;
    }

    private void rebuild(int state) {
        choices.clear();
        lastState = state;
        width = Math.min(246, screenWidth - 8);
        int maxRows = Math.max(1, (screenHeight - 72) / ROW_HEIGHT);
        rows = Math.min(screenWidth < 360 || screenHeight < 260 ? 3 : 6, maxRows);
        height = FIRST_ROW + rows * ROW_HEIGHT + 30;
        left = (screenWidth - width) / 2;
        top = (screenHeight - height) / 2;
        page = Math.min(page, (FACES.length - 1) / rows);
        boolean editable = DeviceAccessMenu.accessEditable(state);
        int optionWidth = Math.max(19, Math.min(38, (width - 66) / MODES.length));
        int optionStart = left + width - 8 - MODES.length * (optionWidth + 2);
        for (int row = 0; row < rows && row + page * rows < FACES.length; row++) {
            int face = row + page * rows;
            int y = top + FIRST_ROW + row * ROW_HEIGHT;
            for (int mode = 0; mode < MODES.length; mode++) {
                Component caption = message("mode." + MODES[mode]);
                if (DeviceAccessMenu.accessMode(state, face) == mode) {
                    caption =
                            Component.literal("[")
                                    .append(caption)
                                    .append("]")
                                    .withStyle(ChatFormatting.YELLOW);
                }
                int assignment = DeviceAccessMenu.accessButton(face, mode);
                button(
                        optionStart + mode * (optionWidth + 2),
                        y,
                        optionWidth,
                        caption,
                        () -> publish.accept(assignment),
                        editable);
            }
        }
        int bottom = top + height - 26;
        if (rows < FACES.length) {
            button(left + 8, bottom, 22, message("previous"), () -> page(-1), page > 0);
            button(
                    left + 67,
                    bottom,
                    22,
                    message("next"),
                    () -> page(1),
                    (page + 1) * rows < FACES.length);
        }
        button(left + width - 58, bottom, 50, message("done"), this::close, true);
        if (focus >= choices.size()) focus = -1;
        if (focus >= 0 && choices.get(focus).active)
            DeviceScreen.focusAccessWidget(choices.get(focus), true);
    }

    private void button(
            int x, int y, int width, Component label, Runnable action, boolean enabled) {
        var button = DeviceScreen.accessWidget(x, y, width, 20, label, action);
        button.active = enabled;
        choices.add(button);
    }

    List<Button> buttons() {
        return open ? choices : List.of(entry);
    }

    Button at(double x, double y) {
        var buttons = buttons();
        for (int i = 0; i < buttons.size(); i++) {
            var button = buttons.get(i);
            if (button.isMouseOver(x, y)) {
                if (open) focus(i);
                return button;
            }
        }
        return null;
    }

    void draw(PanelCanvas canvas) {
        if (!open) return;
        canvas.fill(0, 0, screenWidth, screenHeight, 0x800a111b);
        canvas.frame(left, top, width, height);
        canvas.fittedText(title(), left + width / 2, top + 8, width - 18, TEXT);
        canvas.fittedText(
                message(DeviceAccessMenu.accessEditable(lastState) ? "automation" : "readonly"),
                left + width / 2,
                top + 23,
                width - 16,
                TEXT);
        int labelWidth =
                width
                        - 18
                        - MODES.length
                                * (Math.max(19, Math.min(38, (width - 66) / MODES.length)) + 2);
        for (int row = 0; row < rows && row + page * rows < FACES.length; row++) {
            int face = row + page * rows;
            var world = MachineAccess.worldSide(DeviceAccessMenu.accessFront(lastState), face);
            var caption =
                    Component.translatable(
                            KEY + "face",
                            message("relative." + FACES[face]),
                            message("world." + world.getName()));
            canvas.fittedText(
                    caption,
                    left + 8 + labelWidth / 2,
                    top + FIRST_ROW + row * ROW_HEIGHT + 6,
                    labelWidth,
                    TEXT);
        }
        if (rows < FACES.length) {
            canvas.text(
                    Component.translatable(
                            KEY + "page", page + 1, (FACES.length + rows - 1) / rows),
                    left + 48,
                    top + height - 20,
                    TEXT,
                    true);
        }
    }

    boolean key(int key, boolean reverse, java.util.function.Consumer<Button> activate) {
        if (!open) return false;
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> close();
            case GLFW.GLFW_KEY_TAB -> seek(reverse ? -1 : 1);
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> seek(-1);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_DOWN -> seek(1);
            case GLFW.GLFW_KEY_PAGE_UP -> page(-1);
            case GLFW.GLFW_KEY_PAGE_DOWN -> page(1);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (focus >= 0 && choices.get(focus).active) activate.accept(choices.get(focus));
            }
            default -> {}
        }
        return true;
    }

    private void seek(int direction) {
        if (choices.isEmpty()) return;
        int start = focus < 0 && direction < 0 ? 0 : focus;
        for (int step = 1; step <= choices.size(); step++) {
            int index = Math.floorMod(start + direction * step, choices.size());
            if (choices.get(index).active) {
                focus(index);
                return;
            }
        }
    }

    private void focus(int index) {
        if (focus >= 0) DeviceScreen.focusAccessWidget(choices.get(focus), false);
        focus = index;
        DeviceScreen.focusAccessWidget(choices.get(focus), true);
    }
}
