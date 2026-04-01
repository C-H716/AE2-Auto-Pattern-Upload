package com.gali.ae2_auto_pattern_upload.mixin.ae2fc;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.gali.ae2_auto_pattern_upload.util.CircuitUtils;
import com.glodblock.github.client.gui.container.base.FCContainerEncodeTerminal;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.container.slot.SlotFake;

/**
 * Mixin to prevent programming circuits from being multiplied in AE2FC pattern terminals
 */
@Mixin(value = FCContainerEncodeTerminal.class, remap = false)
public abstract class FCContainerEncodeTerminalMixin {

    /**
     * @author AE2 Auto Pattern Upload
     * @reason Skip programming circuits when checking if multiply/divide is possible
     */
    @Overwrite
    static boolean canMultiplyOrDivide(SlotFake[] slots, int mult) {
        if (mult > 0) {
            for (Slot s : slots) {
                ItemStack st = s.getStack();
                if (st == null) continue;

                // 跳过编程器电路的检查
                if (CircuitUtils.isProgrammingCircuit(st)) {
                    continue;
                }

                final long count;
                if (st.getItem() instanceof ItemFluidPacket) {
                    count = ItemFluidPacket.getFluidAmount(st);
                } else {
                    count = st.stackSize;
                }
                long result = count * mult;
                if (result > Integer.MAX_VALUE) {
                    return false;
                }
            }
            return true;
        } else if (mult < 0) {
            mult = Math.abs(mult);
            for (Slot s : slots) {
                ItemStack st = s.getStack();
                if (st == null) continue;

                // 跳过编程器电路的检查
                if (CircuitUtils.isProgrammingCircuit(st)) {
                    continue;
                }

                final int count;
                if (st.getItem() instanceof ItemFluidPacket) {
                    count = ItemFluidPacket.getFluidAmount(st);
                } else {
                    count = st.stackSize;
                }
                if (count % mult != 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @author AE2 Auto Pattern Upload
     * @reason Skip programming circuits when multiplying/dividing stacks
     */
    @Overwrite
    static void multiplyOrDivideStacksInternal(SlotFake[] slots, int mult) {
        List<SlotFake> enabledSlots = Arrays.stream(slots)
            .filter(SlotFake::isEnabled)
            .collect(Collectors.toList());
        if (mult > 0) {
            for (final Slot s : enabledSlots) {
                ItemStack st = s.getStack();
                if (st != null) {
                    // 跳过编程器电路
                    if (CircuitUtils.isProgrammingCircuit(st)) {
                        continue;
                    }

                    if (st.getItem() instanceof ItemFluidPacket) {
                        ItemFluidPacket.setFluidAmount(st, ItemFluidPacket.getFluidAmount(st) * mult);
                    } else {
                        st.stackSize *= mult;
                        s.putStack(st);
                    }
                }
            }
        } else if (mult < 0) {
            mult = Math.abs(mult);
            for (final Slot s : enabledSlots) {
                ItemStack st = s.getStack();
                if (st != null) {
                    // 跳过编程器电路
                    if (CircuitUtils.isProgrammingCircuit(st)) {
                        continue;
                    }

                    if (st.getItem() instanceof ItemFluidPacket) {
                        ItemFluidPacket.setFluidAmount(st, ItemFluidPacket.getFluidAmount(st) / mult);
                    } else {
                        st.stackSize /= mult;
                        s.putStack(st);
                    }
                }
            }
        }
    }
}
