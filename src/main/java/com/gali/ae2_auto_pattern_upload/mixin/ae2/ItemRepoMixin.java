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
import appeng.api.storage.data.IAEStack;
import appeng.client.me.ItemRepo;

/**
 * Mixin to modify item sorting in AE2 terminals
 * Items being crafted by CPUs will be sorted to the top
 */
@Mixin(value = ItemRepo.class, remap = false)
public abstract class ItemRepoMixin {

    @Final
    @Shadow
    private ArrayList<IAEStack<?>> view;

    /**
     * Inject after the normal sorting to re-sort with crafting items at the top
     * 在dsp.clear()之前执行，即在所有排序完成后
     */
    @Inject(method = "updateView", at = @At("RETURN"))
    private void onUpdateView(CallbackInfo ci) {
        if (this.view != null && !this.view.isEmpty()) {
            this.view.sort((firstStack, secondStack) -> {
                boolean firstCrafting = firstStack instanceof IAEItemStack
                    && CraftingItemsCache.isCrafting((IAEItemStack) firstStack);
                boolean secondCrafting = secondStack instanceof IAEItemStack
                    && CraftingItemsCache.isCrafting((IAEItemStack) secondStack);

                if (firstCrafting && secondCrafting) {
                    return 0;
                }
                if (firstCrafting) {
                    return -1;
                }
                if (secondCrafting) {
                    return 1;
                }
                return 0;
            });
        }
    }
}
