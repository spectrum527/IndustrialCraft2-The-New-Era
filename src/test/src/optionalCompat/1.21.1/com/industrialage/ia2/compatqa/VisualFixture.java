package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.compat.CuriosBack;
import com.industrialage.ia2.gameplay.PortableGear;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.network.client.CPacketOpenCurios;

/** Drives the normal TOP HUD and Curios inventory screen on an isolated Minecraft client. */
@Mod(value = "ic2_compat_qa", dist = Dist.CLIENT)
public final class VisualFixture {
    private static final BlockPos TANK = new BlockPos(12, 70, 8);
    private final Minecraft mc = Minecraft.getInstance();
    private int ticks;
    private int reliableAim;
    private int phase;

    /** Enables the fixture only on the selected acceptance client. */
    public VisualFixture() {
        if (Boolean.getBoolean("ic2.compat.visual")) {
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::tick);
        }
    }

    private void tick(ClientTickEvent.Post event) {
        if (mc.player == null || mc.level == null || phase == 5) return;
        try {
            if (++ticks > 800)
                throw new IllegalStateException("Visual fixture timed out at " + phase);
            switch (phase) {
                case 0 -> aimAtLiveTank();
                case 1, 2 -> equipFromInventory();
                case 3 -> inspectBackSlot();
                case 4 -> waitForCharge();
                default -> throw new IllegalStateException("Unknown visual fixture phase");
            }
        } catch (Throwable failure) {
            try {
                Files.writeString(Path.of("visual-failure.txt"), failure.toString());
            } catch (IOException error) {
                failure.addSuppressed(error);
            }
            phase = 5;
            throw new IllegalStateException(failure);
        }
    }

    private void aimAtLiveTank() {
        if (mc.screen != null
                || !(mc.hitResult instanceof BlockHitResult hit)
                || !hit.getBlockPos().equals(TANK)
                || !id(mc.player.getOffhandItem()).equals("theoneprobe:probe")) {
            reliableAim = 0;
            return;
        }
        if (++reliableAim >= 40) {
            capture("top-live-water-tank.png");
            PacketDistributor.sendToServer(new CPacketOpenCurios(ItemStack.EMPTY));
            phase = 1;
        }
    }

    private void equipFromInventory() {
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;
        var back =
                screen.getMenu().slots.stream()
                        .filter(
                                s ->
                                        s instanceof CurioSlot curio
                                                && curio.isActive()
                                                && !curio.getSlotContext().cosmetic()
                                                && curio.getIdentifier().equals("back"))
                        .findFirst()
                        .orElse(null);
        if (back == null) return;
        if (phase == 1) {
            var item =
                    screen.getMenu().slots.stream()
                            .filter(s -> id(s.getItem()).equals("ic2:batpack"))
                            .findFirst()
                            .orElseThrow();
            click(screen, item);
            if (id(screen.getMenu().getCarried()).equals("ic2:batpack")) phase = 2;
        } else if (id(screen.getMenu().getCarried()).equals("ic2:batpack")) {
            click(screen, back);
            phase = 3;
        }
    }

    private void inspectBackSlot() throws IOException {
        var slots = CuriosBack.slots(mc.player);
        if (slots.size() != 1
                || !id(slots.getFirst()).equals("ic2:batpack")
                || !mc.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) return;
        capture("curios-live-back-slot.png");
        Files.writeString(
                Path.of("visual-equipped.txt"),
                "Curios UI placed batpack into functional back slot; chest empty\n");
        mc.player.closeContainer();
        phase = 4;
    }

    private void waitForCharge() throws IOException {
        var slots = CuriosBack.slots(mc.player);
        if (slots.size() != 1 || PortableGear.stored(slots.getFirst()) < 500) return;
        Files.writeString(
                Path.of("visual-passed.txt"),
                "Actual Curios accessory synced with "
                        + PortableGear.stored(slots.getFirst())
                        + " EU after standing on the server pad\n");
        phase = 5;
    }

    private void capture(String file) {
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), message -> {});
    }

    private void click(AbstractContainerScreen<?> screen, Slot slot) {
        mc.gameMode.handleInventoryMouseClick(
                screen.getMenu().containerId, slot.index, 0, ClickType.PICKUP, mc.player);
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
