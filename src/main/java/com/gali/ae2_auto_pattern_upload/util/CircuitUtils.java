package com.gali.ae2_auto_pattern_upload.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 工具类，用于识别编程器电路
 */
public class CircuitUtils {

    /**
     * 检查物品是否是编程器电路（PH虚拟电路或GT配置电路）
     */
    public static boolean isProgrammingCircuit(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }

        // 检查 PH 的编程电路
        String itemId = stack.getItem()
            .getClass()
            .getName();
        if (itemId.contains("prog_circuit") || itemId.contains("ItemProgrammingCircuit")) {
            return true;
        }

        // 检查 GT 配置电路 (ID 10407)
        String unlocalizedName = stack.getItem()
            .getUnlocalizedName();
        if (unlocalizedName != null && unlocalizedName.contains("integrated_circuit")) {
            return true;
        }

        // 检查物品ID
        String registryName = getRegistryName(stack);
        if (registryName != null) {
            if (registryName.contains("integrated_circuit") || registryName.equals("10407")
                || registryName.equals("10407s")
                || registryName.equals("gregtech:gt.integrated_circuit")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 从 NBT 标签检查是否是编程器电路
     */
    public static boolean isProgrammingCircuitFromNBT(NBTTagCompound tag) {
        if (tag == null) {
            return false;
        }

        String itemId = tag.getString("id");
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }

        // PH 的编程电路
        if (itemId.contains("proghatches") && itemId.contains("prog_circuit")) {
            return true;
        }

        // GT 配置电路
        if (itemId.contains("integrated_circuit") || itemId.equals("10407")
            || itemId.equals("10407s")
            || itemId.equals("gregtech:gt.integrated_circuit")) {
            return true;
        }

        return false;
    }

    private static String getRegistryName(ItemStack stack) {
        try {
            return stack.getItem().delegate.name();
        } catch (Exception e) {
            return null;
        }
    }
}
