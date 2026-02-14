package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gali.ae2_auto_pattern_upload.crafting.CraftingItemsCache;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.me.ItemRepo;

/**
 * Mixin to modify item sorting in AE2 terminals
 * Items being crafted by CPUs will be sorted to the top
 */
@Mixin(value = ItemRepo.class, remap = false)
public abstract class ItemRepoMixin {

    @Final
    @Shadow
    private ArrayList<IAEItemStack> view;

    /**
     * Inject after the normal sorting to re-sort with crafting items at the top
     * 在dsp.clear()之前执行，即在所有排序完成后
     */
    @Inject(method = "updateView", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;clear()V", ordinal = 1))
    private void onUpdateView(CallbackInfo ci) {
        // 在排序后，将正在合成的物品排到最前面
        if (this.view != null && !this.view.isEmpty()) {
            this.view.sort((o1, o2) -> {
                boolean isCrafting1 = CraftingItemsCache.isCrafting(o1);
                boolean isCrafting2 = CraftingItemsCache.isCrafting(o2);

                // 如果两个物品都是正在合成的，保持原有顺序
                if (isCrafting1 && isCrafting2) {
                    return 0;
                }
                // 如果只有o1正在合成，o1排在前面
                if (isCrafting1) {
                    return -1;
                }
                // 如果只有o2正在合成，o2排在前面
                if (isCrafting2) {
                    return 1;
                }
                // 都不是正在合成的，保持原有顺序
                return 0;
            });
        }
    }
}
