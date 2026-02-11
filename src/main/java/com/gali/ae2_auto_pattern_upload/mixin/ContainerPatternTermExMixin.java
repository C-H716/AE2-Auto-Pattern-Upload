package com.gali.ae2_auto_pattern_upload.mixin;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.storage.ITerminalHost;
import appeng.container.implementations.ContainerPatternTermEx;

@Mixin(ContainerPatternTermEx.class)
public abstract class ContainerPatternTermExMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void onInit(InventoryPlayer ip, ITerminalHost monitorable, CallbackInfo ci) {
        ContainerPatternTermEx self = (ContainerPatternTermEx) (Object) this;
        // 获取空白样板槽位
        Slot patternSlotIN = self.getSlotFromInventory(
            self.getPatternTerminal()
                .getInventoryByName("pattern"),
            0);
        if (patternSlotIN != null) {
            // 调用refillBlankPatterns方法填充空白样板
            ((ContainerMEMonitorableAccessor) self).invokeRefillBlankPatterns(patternSlotIN);
        }
    }
}
