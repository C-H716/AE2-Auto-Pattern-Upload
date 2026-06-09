package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import appeng.client.render.highlighter.BlockPosHighlighter;
import appeng.core.localization.Localization;
import appeng.core.localization.PlayerMessages;

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

    /**
     * 在mouseClicked方法开头注入，处理带有供应器信息的物品的shift+左键点击
     * 这将在高亮显示的同时打开第一个供应器的GUI
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int xCoord, int yCoord, int btn, CallbackInfo ci) {
        // 只在左键点击且shift按下且有悬停供应器位置时处理
        if (btn != 0 || !GuiScreen.isShiftKeyDown()
            || this.hoveredInterfaceLocations == null
            || this.hoveredInterfaceLocations.isEmpty()) {
            return;
        }

        NamedDimensionalCoord firstProvider = this.hoveredInterfaceLocations.get(0);

        // 发送数据包到服务器打开GUI
        ModNetwork.INSTANCE.sendToServer(new PacketOpenProviderGui(firstProvider, ForgeDirection.UNKNOWN));

        // 仍然执行高亮显示
        Map<NamedDimensionalCoord, String[]> messages = new HashMap<>();
        for (NamedDimensionalCoord provider : this.hoveredInterfaceLocations) {
            messages.put(
                provider,
                new String[] { PlayerMessages.MachineHighlightedNamed.getUnlocalized(),
                    PlayerMessages.MachineInOtherDimNamed.getUnlocalized() });
        }
        BlockPosHighlighter.highlightNamedBlocks(
            mc.thePlayer,
            messages,
            ((Localization) () -> "tile.appliedenergistics2.BlockInterface.name").getLocal());

        // 关闭当前界面
        mc.thePlayer.closeScreen();

        // 取消原始方法的执行（因为我们已经处理了所有逻辑）
        ci.cancel();
    }
}
