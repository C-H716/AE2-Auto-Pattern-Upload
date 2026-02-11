package com.gali.ae2_auto_pattern_upload.mixin;

import net.minecraft.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.container.implementations.ContainerMEMonitorable;

@Mixin(ContainerMEMonitorable.class)
public interface ContainerMEMonitorableAccessor {

    @Invoker(value = "refillBlankPatterns", remap = false)
    void invokeRefillBlankPatterns(Slot slot);
}
