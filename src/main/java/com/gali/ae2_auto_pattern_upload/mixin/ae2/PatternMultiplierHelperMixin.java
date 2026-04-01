package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.gali.ae2_auto_pattern_upload.util.CircuitUtils;

import appeng.util.PatternMultiplierHelper;

/**
 * Mixin to prevent programming circuits from being multiplied in interface pattern multiplication
 */
@Mixin(value = PatternMultiplierHelper.class, remap = false)
public abstract class PatternMultiplierHelperMixin {

    /**
     * @author AE2 Auto Pattern Upload
     * @reason Skip programming circuits when applying pattern modifications
     */
    @Overwrite
    public static void applyModification(ItemStack stack, int bitMultiplier) {
        if (bitMultiplier == 0) return;

        boolean isDividing = false;
        if (bitMultiplier < 0) {
            isDividing = true;
            bitMultiplier = -bitMultiplier;
        }

        NBTTagCompound encodedValue = stack.stackTagCompound;
        if (encodedValue == null) return;

        final NBTTagList inTag = encodedValue.getTagList("in", 10);
        final NBTTagList outTag = encodedValue.getTagList("out", 10);

        // 处理输入物品
        for (int x = 0; x < inTag.tagCount(); x++) {
            final NBTTagCompound tag = inTag.getCompoundTagAt(x);
            if (tag.hasNoTags()) continue;

            // 跳过编程器电路
            if (CircuitUtils.isProgrammingCircuitFromNBT(tag)) {
                continue;
            }

            // 处理 Count 字段
            if (tag.hasKey("Count")) {
                tag.setInteger(
                    "Count",
                    isDividing ? tag.getInteger("Count") >> bitMultiplier : tag.getInteger("Count") << bitMultiplier);
            }
            // 处理 Cnt 字段 (AE2FC)
            if (tag.hasKey("Cnt", 4)) {
                tag.setLong(
                    "Cnt",
                    isDividing ? tag.getLong("Cnt") >> bitMultiplier : tag.getLong("Cnt") << bitMultiplier);
            }
        }

        // 处理输出物品
        for (int x = 0; x < outTag.tagCount(); x++) {
            final NBTTagCompound tag = outTag.getCompoundTagAt(x);
            if (tag.hasNoTags()) continue;

            // 跳过编程器电路
            if (CircuitUtils.isProgrammingCircuitFromNBT(tag)) {
                continue;
            }

            // 处理 Count 字段
            if (tag.hasKey("Count")) {
                tag.setInteger(
                    "Count",
                    isDividing ? tag.getInteger("Count") >> bitMultiplier : tag.getInteger("Count") << bitMultiplier);
            }
            // 处理 Cnt 字段 (AE2FC)
            if (tag.hasKey("Cnt", 4)) {
                tag.setLong(
                    "Cnt",
                    isDividing ? tag.getLong("Cnt") >> bitMultiplier : tag.getLong("Cnt") << bitMultiplier);
            }
        }
    }
}
