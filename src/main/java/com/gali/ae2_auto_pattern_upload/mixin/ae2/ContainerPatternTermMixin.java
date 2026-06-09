package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.gali.ae2_auto_pattern_upload.util.CircuitUtils;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.tile.inventory.IAEStackInventory;

@Mixin(value = ContainerPatternTerm.class, remap = false)
public abstract class ContainerPatternTermMixin {

    /**
     * @author AE2 Auto Pattern Upload
     * @reason Skip programming circuits when checking if multiply/divide is possible
     */
    @Overwrite
    static boolean canMultiplyOrDivide(IAEStackInventory inventory, int mult) {
        if (mult > 0) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                IAEStack<?> stack = inventory.getAEStackInSlot(i);
                if (stack != null) {
                    // 跳过编程器电路的检查
                    if (stack instanceof IAEItemStack
                        && CircuitUtils.isProgrammingCircuit(((IAEItemStack) stack).getItemStack())) {
                        continue;
                    }
                    double val = (double) stack.getStackSize() * mult;
                    if (val > Long.MAX_VALUE) return false;
                }
            }
            return true;
        } else if (mult < 0) {
            mult = -mult;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                IAEStack<?> stack = inventory.getAEStackInSlot(i);
                if (stack != null) {
                    // 跳过编程器电路的检查
                    if (stack instanceof IAEItemStack
                        && CircuitUtils.isProgrammingCircuit(((IAEItemStack) stack).getItemStack())) {
                        continue;
                    }
                    if (stack.getStackSize() % mult != 0) return false;
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
    static void multiplyOrDivideStacksInternal(IAEStackInventory inventory, int mult) {
        if (mult > 0) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                IAEStack<?> stack = inventory.getAEStackInSlot(i);
                if (stack != null) {
                    // 跳过编程器电路
                    if (stack instanceof IAEItemStack
                        && CircuitUtils.isProgrammingCircuit(((IAEItemStack) stack).getItemStack())) {
                        continue;
                    }
                    stack.setStackSize(stack.getStackSize() * mult);
                    inventory.putAEStackInSlot(i, stack);
                }
            }
        } else if (mult < 0) {
            mult = -mult;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                IAEStack<?> stack = inventory.getAEStackInSlot(i);
                if (stack != null) {
                    // 跳过编程器电路
                    if (stack instanceof IAEItemStack
                        && CircuitUtils.isProgrammingCircuit(((IAEItemStack) stack).getItemStack())) {
                        continue;
                    }
                    stack.setStackSize(stack.getStackSize() / mult);
                    inventory.putAEStackInSlot(i, stack);
                }
            }
        }
    }
}
