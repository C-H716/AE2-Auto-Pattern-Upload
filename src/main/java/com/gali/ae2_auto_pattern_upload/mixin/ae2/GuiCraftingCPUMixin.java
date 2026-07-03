package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.provider.PacketOpenProviderGui;

import appeng.api.util.NamedDimensionalCoord;
import appeng.client.gui.implementations.GuiCraftingCPU;

/**
 * 为GuiCraftingCPU添加的Mixin，实现shift+左键点击打开供应器界面功能
 */
@Mixin(GuiCraftingCPU.class)
public abstract class GuiCraftingCPUMixin extends GuiContainer {

    @Shadow(remap = false)
    private List<NamedDimensionalCoord> hoveredInterfaceLocations;

    public GuiCraftingCPUMixin() {
        super(null);
    }

    @Shadow(remap = false)
    private void highlightHoveredInterfaces() {}

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int xCoord, int yCoord, int btn, CallbackInfo ci) {
        // 只在左键点击且shift按下且有悬停供应器位置时处理
        if (btn != 0 || !GuiScreen.isShiftKeyDown()
            || this.hoveredInterfaceLocations == null
            || this.hoveredInterfaceLocations.isEmpty()) {
            return;
        }

        int currentDimension = this.mc.thePlayer.worldObj.provider.dimensionId;
        NamedDimensionalCoord firstProvider = this.hoveredInterfaceLocations.stream()
            .filter(provider -> provider.getDimension() == currentDimension)
            .findFirst()
            .orElse(this.hoveredInterfaceLocations.get(0));

        ModNetwork.INSTANCE.sendToServer(new PacketOpenProviderGui(firstProvider, ForgeDirection.UNKNOWN));
        this.highlightHoveredInterfaces();
        ci.cancel();
    }
}
