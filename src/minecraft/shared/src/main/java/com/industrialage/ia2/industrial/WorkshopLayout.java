package com.industrialage.ia2.industrial;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;

/** Screen coordinates shared by the native container and the four clients. */
final class WorkshopLayout {
    private static final String RESOURCE = "/assets/ic2/gui/workshop-layouts.json";
    private static final Map<WorkshopKind, WorkshopLayout> LAYOUTS = load();

    final String atlas;
    final int width;
    final int height;
    final InventoryArea player;
    final boolean inventoryTitle;
    final Map<Integer, Point> slots;
    final List<Integer> frames;
    final List<Picture> pictures;
    final List<Meter> meters;
    final List<Control> controls;
    final List<Label> labels;

    private WorkshopLayout(JsonObject layout) {
        atlas = layout.get("atlas").getAsString();
        var panel = layout.getAsJsonArray("panel");
        width = number(panel, 0);
        height = number(panel, 1);
        var inventory = layout.getAsJsonArray("inventory");
        player =
                inventory.size() == 0
                        ? null
                        : new InventoryArea(
                                number(inventory, 0), number(inventory, 1), number(inventory, 2));
        inventoryTitle = layout.get("inventoryTitle").getAsBoolean();
        slots =
                layout.getAsJsonObject("slots").entrySet().stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        row -> Integer.parseInt(row.getKey()),
                                        row -> Point.read(row.getValue().getAsJsonArray())));
        frames = rows(layout, "frames", JsonElement::getAsInt);
        pictures = rows(layout, "draw", row -> Picture.read(row.getAsJsonObject()));
        meters = rows(layout, "meters", row -> Meter.read(row.getAsJsonObject()));
        controls = rows(layout, "controls", row -> Control.read(row.getAsJsonObject()));
        labels = rows(layout, "labels", row -> Label.read(row.getAsJsonObject()));
    }

    static WorkshopLayout of(WorkshopKind device) {
        return LAYOUTS.get(device);
    }

    boolean shows(int slot) {
        return slots.containsKey(slot);
    }

    Point slot(int index) {
        return slots.getOrDefault(index, Point.HIDDEN);
    }

    private static Map<WorkshopKind, WorkshopLayout> load() {
        try (var input =
                new InputStreamReader(
                        Objects.requireNonNull(
                                WorkshopLayout.class.getResourceAsStream(RESOURCE), RESOURCE),
                        StandardCharsets.UTF_8)) {
            var all = JsonParser.parseReader(input).getAsJsonObject().getAsJsonObject("machines");
            return Arrays.stream(WorkshopKind.values())
                    .filter(kind -> !kind.kilnPort())
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    kind -> kind,
                                    kind ->
                                            new WorkshopLayout(
                                                    Objects.requireNonNull(
                                                            all.getAsJsonObject(kind.path),
                                                            "Missing Workshop layout for "
                                                                    + kind.path))));
        } catch (IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    private static <T> List<T> rows(
            JsonObject object, String key, Function<JsonElement, T> converter) {
        if (!object.has(key)) return List.of();
        var values = object.getAsJsonArray(key);
        return java.util.stream.IntStream.range(0, values.size())
                .mapToObj(index -> converter.apply(values.get(index)))
                .toList();
    }

    private static int number(JsonArray array, int index) {
        return array.get(index).getAsInt();
    }

    record Point(int x, int y) {
        static final Point HIDDEN = new Point(0, 0);

        static Point read(JsonArray row) {
            return new Point(number(row, 0), number(row, 1));
        }
    }

    record InventoryArea(int x, int rows, int hotbar) {}

    record Area(int x, int y, int width, int height) {
        static Area read(JsonObject row) {
            var area = row.getAsJsonArray("rect");
            return new Area(number(area, 0), number(area, 1), number(area, 2), number(area, 3));
        }

        boolean contains(int px, int py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    record Picture(String atlas, Area area, Point uv, int textureWidth, int textureHeight) {
        static Picture read(JsonObject element) {
            String atlas = element.get("atlas").getAsString();
            var area = Area.read(element);
            var uv = Point.read(element.getAsJsonArray("uv"));
            var size = element.getAsJsonArray("textureSize");
            return new Picture(
                    atlas,
                    area,
                    uv,
                    size == null ? 256 : number(size, 0),
                    size == null ? 256 : number(size, 1));
        }
    }

    enum Readout {
        PROGRESS,
        ENERGY,
        HEAT,
        INLET,
        OUTLET,
        BYPRODUCT,
        ACTIVE,
        KINETIC,
        KINETIC_AVAILABLE,
        KINETIC_INPUT,
        MODE,
        PATTERN,
        RECIPE;

        static Readout read(String id) {
            return valueOf(id.toUpperCase(java.util.Locale.ROOT));
        }
    }

    record Meter(
            Readout value,
            Area area,
            boolean rises,
            int maximum,
            String atlas,
            Point uv,
            int color) {
        static Meter read(JsonObject element) {
            var sprite = element.getAsJsonArray("sprite");
            return new Meter(
                    Readout.read(element.get("value").getAsString()),
                    Area.read(element),
                    element.get("direction").getAsString().equals("up"),
                    element.has("maximum") ? element.get("maximum").getAsInt() : 0,
                    sprite == null ? "" : sprite.get(0).getAsString(),
                    sprite == null ? Point.HIDDEN : new Point(number(sprite, 1), number(sprite, 2)),
                    element.has("color")
                            ? Integer.parseUnsignedInt(element.get("color").getAsString(), 16)
                            : 0);
        }
    }

    record Label(Readout value, Area area, boolean light) {
        static Label read(JsonObject row) {
            return new Label(
                    Readout.read(row.get("text").getAsString()),
                    Area.read(row),
                    !row.has("light") || row.get("light").getAsBoolean());
        }
    }

    record Control(int action, String label, Area area, String icon) {
        static Control read(JsonObject row) {
            return new Control(
                    row.get("action").getAsInt(),
                    row.get("label").getAsString(),
                    Area.read(row),
                    row.has("icon") ? row.get("icon").getAsString() : "");
        }

        Component caption(WorkshopMenu menu) {
            return switch (label) {
                case "mode" -> menu.controlLabel();
                case "record", "next_pattern" -> workshop(label);
                default -> workshop("control." + label);
            };
        }

        private static Component workshop(String key) {
            return Component.translatable("gui.ic2.workshop." + key);
        }
    }
}
