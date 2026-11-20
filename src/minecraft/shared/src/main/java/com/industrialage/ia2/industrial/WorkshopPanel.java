package com.industrialage.ia2.industrial;

import com.industrialage.ia2.gui.PanelCanvas;
import com.industrialage.ia2.industrial.WorkshopLayout.Area;
import com.industrialage.ia2.industrial.WorkshopLayout.Meter;
import com.industrialage.ia2.industrial.WorkshopLayout.Readout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class WorkshopPanel {
    private static final int DARK = 0xff323b48;
    private static final int LCD = 0xff8de6ee;

    private WorkshopPanel() {}

    static void background(WorkshopMenu menu, PanelCanvas canvas) {
        var layout = menu.layout();
        if (layout.atlas.equals("common")) {
            canvas.frame(0, 0, layout.width, layout.height);
            if (layout.player != null) {
                canvas.inventory(layout.player.x(), layout.player.rows(), layout.player.hotbar());
            }
            if (layout.frames.isEmpty()) {
                for (var slot : layout.slots.keySet()) {
                    if (menu.slots.get(slot).isActive()) {
                        var at = layout.slot(slot);
                        canvas.slot(at.x(), at.y());
                    }
                }
            }
        } else {
            canvas.background(layout.atlas, layout.width, layout.height);
        }
        for (int index : layout.frames) {
            var at = layout.slot(index);
            canvas.slot(at.x(), at.y());
        }
        for (var picture : layout.pictures) {
            var area = picture.area();
            canvas.blit(
                    picture.atlas(),
                    area.x(),
                    area.y(),
                    picture.uv().x(),
                    picture.uv().y(),
                    area.width(),
                    area.height(),
                    picture.textureWidth(),
                    picture.textureHeight());
        }
        for (var meter : layout.meters) {
            meter(menu, canvas, meter);
        }
        var info = info(layout);
        canvas.blit("infobutton", info.x(), info.y(), 0, 0, 10, 10);
    }

    static void labels(
            WorkshopMenu menu,
            Component title,
            Component inventoryTitle,
            PanelCanvas canvas,
            BiFunction<Component, Integer, Component> fit) {
        var layout = menu.layout();
        canvas.text(fit.apply(title, layout.width - 32), layout.width / 2, 6, DARK, true);
        if (layout.inventoryTitle && layout.player != null) {
            canvas.text(
                    inventoryTitle, layout.player.x() - 1, layout.player.rows() - 11, DARK, false);
        }
        for (var label : layout.labels) {
            var area = label.area();
            var text = readout(menu, label.value());
            canvas.text(fit.apply(text, area.width() - 2), area.x() + 1, area.y(), LCD, false);
        }
        for (var control : layout.controls) {
            var area = control.area();
            if (control.icon().equals("mode")) {
                canvas.text(
                        fit.apply(control.caption(menu), area.width() - 2),
                        area.x() + area.width() / 2,
                        area.y() + 2,
                        DARK,
                        true);
            }
        }
    }

    static List<Component> tooltip(WorkshopMenu menu, int x, int y, int slot, boolean hasItem) {
        var layout = menu.layout();
        if (info(layout).contains(x, y)) {
            var lines = new ArrayList<Component>();
            lines.add(menu.job());
            if (menu.duration() > 0) lines.add(readout(menu, Readout.PROGRESS));
            if (menu.capacity() > 0) lines.add(readout(menu, Readout.ENERGY));
            if (menu.heatMeter()) lines.add(readout(menu, Readout.HEAT));
            for (int i = 0; i < 3; i++) {
                if (menu.tank(i).maximum() > 0) lines.add(tankReadout(menu.tank(i)));
            }
            return lines;
        }
        for (var control : layout.controls) {
            if (control.area().contains(x, y)) return List.of(control.caption(menu));
        }
        if (!hasItem && slot >= 0 && slot < WorkshopInventory.SIZE) {
            return List.of(menu.slotLabel(slot));
        }
        for (var meter : layout.meters) {
            if (meter.area().contains(x, y)) {
                if (meter.value() == Readout.PROGRESS) {
                    return List.of(menu.job(), readout(menu, Readout.PROGRESS));
                }
                return List.of(readout(menu, meter.value()));
            }
        }
        for (var display : layout.labels) {
            if (display.area().contains(x, y)) return List.of(readout(menu, display.value()));
        }
        return List.of();
    }

    static ModePicture modePicture(WorkshopMenu menu) {
        if (!menu.device().equals("metal_former")) return null;
        var area = menu.layout().controls.get(0).area();
        var item =
                WorkshopPlatform.stack(
                        switch (menu.mode()) {
                            case 1 -> "ic2:copper_plate";
                            case 2 -> "ic2:cutter";
                            default -> "ic2:copper_cable";
                        },
                        1);
        return new ModePicture(item, area.x() + 3, area.y() + 3);
    }

    record ModePicture(ItemStack item, int x, int y) {}

    private static Area info(WorkshopLayout layout) {
        return new Area(layout.width + 4, 4, 10, 10);
    }

    private static Component readout(WorkshopMenu menu, Readout readout) {
        return switch (readout) {
            case ACTIVE -> label(menu.active() ? "running" : "waiting");
            case PROGRESS -> label("progress", menu.progress(), menu.duration());
            case ENERGY -> label("energy", menu.energy(), menu.capacity());
            case HEAT -> label("heat", menu.heat());
            case KINETIC -> label("kinetic", menu.motion());
            case MODE -> menu.controlLabel();
            case RECIPE -> menu.job();
            case PATTERN -> menu.patternName();
            case INLET -> tankReadout(menu.tank(0));
            case OUTLET -> tankReadout(menu.tank(1));
            case BYPRODUCT -> tankReadout(menu.tank(2));
        };
    }

    private static Component tankReadout(WorkshopMenu.TankReadout tank) {
        return label(
                "detail_tank", label(tank.role()), tank.liquid(), tank.amount(), tank.maximum());
    }

    private static void meter(WorkshopMenu menu, PanelCanvas canvas, Meter meter) {
        var area = meter.area();
        int maximum =
                switch (meter.value()) {
                    case ENERGY -> menu.capacity();
                    case PROGRESS -> menu.duration();
                    case HEAT -> meter.maximum();
                    case ACTIVE -> 1;
                    case INLET -> menu.tank(0).maximum();
                    case OUTLET -> menu.tank(1).maximum();
                    case BYPRODUCT -> menu.tank(2).maximum();
                    default -> 0;
                };
        int value =
                switch (meter.value()) {
                    case ENERGY -> menu.energy();
                    case PROGRESS -> menu.progress();
                    case HEAT -> menu.heat();
                    case ACTIVE -> menu.active() ? 1 : 0;
                    case INLET -> menu.tank(0).amount();
                    case OUTLET -> menu.tank(1).amount();
                    case BYPRODUCT -> menu.tank(2).amount();
                    default -> 0;
                };
        if (menu.atlas().equals("common")
                && meter.value() == Readout.ENERGY
                && !meter.atlas().isEmpty()) {
            canvas.blit("common", area.x(), area.y(), 100, 65, area.width(), area.height());
        }
        if (maximum <= 0 || value <= 0) return;
        int length = meter.rises() ? area.height() : area.width();
        int visible = Math.max(1, (int) (length * (long) Math.min(value, maximum) / maximum));
        int width = meter.rises() ? area.width() : visible;
        int height = meter.rises() ? visible : area.height();
        int dy = area.height() - height;
        if (!meter.atlas().isEmpty()) {
            canvas.blit(
                    meter.atlas(),
                    area.x(),
                    area.y() + dy,
                    meter.uv().x(),
                    meter.uv().y() + dy,
                    width,
                    height);
        } else {
            int paint = meter.color() == 0 ? color(menu, meter.value()) : meter.color();
            canvas.fill(area.x(), area.y() + dy, area.x() + width, area.y() + area.height(), paint);
        }
    }

    private static int color(WorkshopMenu menu, Readout value) {
        if (value == Readout.INLET || value == Readout.OUTLET || value == Readout.BYPRODUCT) {
            return switch (menu.device()) {
                case "matter_generator", "replicator" -> 0xaab354d8;
                case "blast_furnace" -> 0x9991d4e6;
                case "semifluid_generator", "fluid_heat_generator" -> 0xaae3a759;
                case "fermenter" -> value == Readout.INLET ? 0x995a9e35 : 0x999ba060;
                default -> value == Readout.BYPRODUCT ? 0x8892cbb1 : 0x99549ddd;
            };
        }
        return value == Readout.HEAT ? 0xbbb45126 : 0xb065d6e4;
    }

    private static Component label(String key, Object... args) {
        return Component.translatable("gui.ic2.workshop." + key, args);
    }
}
