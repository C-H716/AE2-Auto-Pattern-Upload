package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminal;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.Platform;

@Mixin(ContainerFluidPatternTerminal.class)
public abstract class ContainerFluidPatternTerminalMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void onInit(InventoryPlayer ip, ITerminalHost monitorable, CallbackInfo ci) {
        ContainerFluidPatternTerminal self = (ContainerFluidPatternTerminal) (Object) this;
        if (!Platform.isServer()) {
            return;
        }

        // 获取空白样板槽位 (patternSlotIN)
        Slot patternSlotIN = self.getSlotFromInventory(
            self.getPatternTerminal()
                .getInventoryByName("pattern"),
            0);
        if (patternSlotIN == null) {
            return;
        }

        // 获取当前空白样板
        ItemStack blanks = patternSlotIN.getStack();
        int blanksToRefill = 64;
        if (blanks != null) {
            blanksToRefill -= blanks.stackSize;
        }
        if (blanksToRefill <= 0) {
            return;
        }

        // 从ME网络提取空白样板
        final IDefinitions definitions = AEApi.instance()
            .definitions();
        for (ItemStack blankPattern : definitions.materials()
            .blankPattern()
            .maybeStack(blanksToRefill)
            .asSet()) {
            IAEItemStack request = AEApi.instance()
                .storage()
                .createItemStack(blankPattern);
            if (blanks != null && !request.isSameType(blanks)) {
                continue;
            }

            IAEItemStack extracted = null;
            if (monitorable.getItemInventory() != null) {
                // 使用poweredExtraction来提取物品
                IEnergySource powerSource = self.getPowerSource();
                PlayerSource actionSource = new PlayerSource(ip.player, (IActionHost) monitorable);
                extracted = Platform
                    .poweredExtraction(powerSource, monitorable.getItemInventory(), request, actionSource);
            }

            if (extracted != null) {
                if (blanks != null) {
                    blanks.stackSize += (int) extracted.getStackSize();
                } else {
                    blanks = extracted.getItemStack();
                }
                patternSlotIN.putStack(blanks);
            }
            break;
        }
    }
}
