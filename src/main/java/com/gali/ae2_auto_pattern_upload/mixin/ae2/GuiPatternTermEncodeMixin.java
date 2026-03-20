package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import net.minecraft.client.gui.GuiButton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gali.ae2_auto_pattern_upload.client.handler.AutoUploadHandler;

import appeng.api.config.ActionItems;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.widgets.GuiImgButton;

@Mixin(GuiPatternTerm.class)
public class GuiPatternTermEncodeMixin {

    @Inject(method = "actionPerformed", at = @At("RETURN"))
    private void onEncodeButtonClicked(GuiButton btn, CallbackInfo ci) {
        // 检查是否是编码按钮被点击
        if (btn instanceof GuiImgButton imgBtn) {
            if (imgBtn.getCurrentValue() == ActionItems.ENCODE) {
                // 编码按钮被点击，触发自动上传
                AutoUploadHandler.onEncodeButtonClicked();
            }
        }
    }
}
