package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor.ContainerMEMonitorableAccessor;
import com.gali.ae2_auto_pattern_upload.util.CircuitUtils;

import appeng.api.storage.ITerminalHost;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.SlotFake;

@Mixin(value = ContainerPatternTerm.class, remap = false)
public abstract class ContainerPatternTermMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(InventoryPlayer ip, ITerminalHost monitorable, CallbackInfo ci) {
        ContainerPatternTerm self = (ContainerPatternTerm) (Object) this;
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

    /**
     * @author AE2 Auto Pattern Upload
     * @reason Skip programming circuits when checking if multiply/divide is possible
     */
    @Overwrite
    static boolean canMultiplyOrDivide(SlotFake[] slots, int mult) {
        if (mult > 0) {
            for (Slot s : slots) {
                ItemStack stack = s.getStack();
                if (stack != null) {
                    // 跳过编程器电路的检查
                    if (CircuitUtils.isProgrammingCircuit(stack)) {
                        continue;
                    }
                    long val = (long) stack.stackSize * mult;
                    if (val > Integer.MAX_VALUE) return false;
                }
            }
            return true;
        } else if (mult < 0) {
            mult = -mult;
            for (Slot s : slots) {
                ItemStack stack = s.getStack();
                if (stack != null) {
                    // 跳过编程器电路的检查
                    if (CircuitUtils.isProgrammingCircuit(stack)) {
                        continue;
                    }
                    if (stack.stackSize % mult != 0) return false;
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
                    st.stackSize *= mult;
                    s.putStack(st);
                }
            }
        } else if (mult < 0) {
            mult = -mult;
            for (final Slot s : enabledSlots) {
                ItemStack st = s.getStack();
                if (st != null) {
                    // 跳过编程器电路
                    if (CircuitUtils.isProgrammingCircuit(st)) {
                        continue;
                    }
                    st.stackSize /= mult;
                    s.putStack(st);
                }
            }
        }
    }
}
