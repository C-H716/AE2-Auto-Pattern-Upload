package com.gali.ae2_auto_pattern_upload.mixin.ae2;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.PacketOpenProviderGui;

import appeng.api.util.DimensionalCoord;
import appeng.client.gui.implementations.GuiCraftingCPU;
import appeng.client.render.highlighter.BlockPosHighlighter;
import appeng.core.localization.PlayerMessages;
import appeng.util.Platform;

/**
 * Mixin for GuiCraftingCPU to add shift+left-click to open provider interface GUI
 */
@Mixin(GuiCraftingCPU.class)
public abstract class GuiCraftingCPUMixin extends GuiContainer {

    @Shadow(remap = false)
    private ItemStack hoveredNbtStack;

    public GuiCraftingCPUMixin() {
        super(null);
    }

    /**
     * Inject at the beginning of mouseClicked to handle shift+left-click on items with provider info
     * This will open the first provider's GUI in addition to highlighting it
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int xCoord, int yCoord, int btn, CallbackInfo ci) {
        // 只在左键点击且shift按下且有hoveredNbtStack时处理
        if (btn != 0 || !GuiScreen.isShiftKeyDown() || this.hoveredNbtStack == null) {
            return;
        }

        NBTTagCompound data = Platform.openNbtData(this.hoveredNbtStack);
        List<DimensionalCoord> providers = DimensionalCoord.readAsListFromNBT(data);

        if (!providers.isEmpty()) {
            // 获取第一个供应器的位置
            DimensionalCoord firstProvider = providers.get(0);

            // 发送数据包到服务器打开GUI
            ModNetwork.INSTANCE.sendToServer(new PacketOpenProviderGui(firstProvider, ForgeDirection.UNKNOWN));

            // 仍然执行高亮显示
            BlockPosHighlighter.highlightBlocks(
                mc.thePlayer,
                providers,
                PlayerMessages.InterfaceHighlighted.getUnlocalized(),
                PlayerMessages.InterfaceInOtherDim.getUnlocalized());

            // 关闭当前界面
            mc.thePlayer.closeScreen();

            // 取消原始方法的执行（因为我们已经处理了所有逻辑）
            ci.cancel();
        }
    }
}
