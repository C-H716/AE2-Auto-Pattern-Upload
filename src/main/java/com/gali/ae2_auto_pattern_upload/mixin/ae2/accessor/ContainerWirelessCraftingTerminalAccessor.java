package com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor;

import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ContainerWirelessCraftingTerminal.class)
public interface ContainerWirelessCraftingTerminalAccessor {

    @Invoker(value = "isInRange", remap = false)
    boolean invokeIsInRange();
}
