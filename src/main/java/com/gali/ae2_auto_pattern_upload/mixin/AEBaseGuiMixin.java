package com.gali.ae2_auto_pattern_upload.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.client.gui.AEBaseGui;

/**
 * Mixin to intercept AE's mouse wheel event handling
 * This prevents AE from handling shift+scroll when our mod wants to handle it
 */
@Mixin(AEBaseGui.class)
public abstract class AEBaseGuiMixin extends GuiContainer {

    public AEBaseGuiMixin() {
        super(null);
    }

    /**
     * Inject at the beginning of mouseWheelEvent to check if we should cancel AE's handling
     * If the mouse is over the ME item area, cancel AE's default behavior
     */
    @Inject(method = "mouseWheelEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void onMouseWheelEvent(int x, int y, int wheel, CallbackInfoReturnable<Boolean> cir) {
        // Check if shift is down (AE only handles shift+scroll)
        if (!isShiftKeyDown()) {
            return;
        }

        // Check if mouse is in the ME item display area
        // The item area is typically in the upper portion of the GUI
        // We'll check if y is above the player inventory area
        int inventoryStartY = this.guiTop + this.ySize - 90; // Player inventory starts here

        if (y < inventoryStartY) {
            // Mouse is in the item area, cancel AE's handling
            // Return true to indicate the event was "handled" (consumed)
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
