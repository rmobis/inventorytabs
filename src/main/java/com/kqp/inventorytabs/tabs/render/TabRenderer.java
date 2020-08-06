package com.kqp.inventorytabs.tabs.render;

import com.kqp.inventorytabs.init.InventoryTabs;
import com.kqp.inventorytabs.mixin.client.accessor.HandledScreenAccessor;
import com.kqp.inventorytabs.mixin.client.accessor.ScreenAccessor;
import com.kqp.inventorytabs.tabs.TabManager;
import com.kqp.inventorytabs.tabs.tab.Tab;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.awt.*;

/**
 * Handles the rendering of tabs.
 */
@Environment(EnvType.CLIENT)
public class TabRenderer {
    private static final Identifier TABS_TEXTURE = new Identifier("textures/gui/container/creative_inventory/tabs.png");
    private static final Identifier BUTTONS_TEXTURE = InventoryTabs.id("textures/gui/buttons.png");

    public static final int TAB_WIDTH = 28;
    public static final int TAB_HEIGHT = 32;
    public static final int BUTTON_WIDTH = 15;
    public static final int BUTTON_HEIGHT = 13;

    public final TabManager tabManager;

    public int bottomRowYOffset;
    private TabRenderInfo[] tabRenderInfos;

    private long pageTextRefreshTime;

    public TabRenderer(TabManager tabManager) {
        this.tabManager = tabManager;
    }

    public void renderBackground(MatrixStack matrices) {
        matrices.push();

        tabRenderInfos = getTabRenderInfos();

        for (int i = 0; i < tabRenderInfos.length; i++) {
            TabRenderInfo tabRenderInfo = tabRenderInfos[i];

            if (tabRenderInfo != null) {
                if (tabRenderInfo.tabReference != tabManager.currentTab) {
                    renderTab(matrices, tabRenderInfo);
                }
            }
        }
        matrices.pop();
    }

    public void renderForeground(MatrixStack matrices, double mouseX, double mouseY) {
        MinecraftClient.getInstance().getTextureManager().bindTexture(TABS_TEXTURE);

        for (int i = 0; i < tabRenderInfos.length; i++) {
            TabRenderInfo tabRenderInfo = tabRenderInfos[i];

            if (tabRenderInfo != null) {
                if (tabRenderInfo.tabReference == tabManager.currentTab) {
                    renderTab(matrices, tabRenderInfo);
                }
            }
        }

        drawButtons(matrices, mouseX, mouseY);

        drawPageText(matrices);
    }

    private void drawButtons(MatrixStack matrices, double mouseX, double mouseY) {
        HandledScreen currentScreen = tabManager.getCurrentScreen();

        MinecraftClient.getInstance().getTextureManager().bindTexture(BUTTONS_TEXTURE);

        int width = ((HandledScreenAccessor) currentScreen).getBackgroundWidth();
        int height = ((HandledScreenAccessor) currentScreen).getBackgroundHeight();
        int oX = (currentScreen.width - width) / 2;
        int oY = (currentScreen.height - height) / 2;

        // Drawing back button
        int x = oX - BUTTON_WIDTH - 4;
        int y = oY - 16;
        boolean hovered = new Rectangle(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).contains(mouseX, mouseY);
        int u = 0;
        u += tabManager.canGoBackAPage() && hovered ? BUTTON_WIDTH * 2 : 0;
        int v = tabManager.canGoBackAPage() ? 0 : 13;
        currentScreen.drawTexture(
                matrices,
                x,
                y,
                u,
                v,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        );

        // Drawing forward button
        x = oX + width + 4;
        y = oY - 16;
        hovered = new Rectangle(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).contains(mouseX, mouseY);
        u = 15;
        u += tabManager.canGoForwardAPage() && hovered ? BUTTON_WIDTH * 2 : 0;
        v = tabManager.canGoForwardAPage() ? 0 : 13;
        currentScreen.drawTexture(
                matrices,
                x,
                y,
                u,
                v,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        );
    }

    private void drawPageText(MatrixStack matrices) {
        if (pageTextRefreshTime > 0) {
            RenderSystem.pushMatrix();

            int color = 0xFFFFFFFF;

            if (pageTextRefreshTime <= 20) {
                RenderSystem.disableTexture();
                RenderSystem.enableBlend();
                RenderSystem.disableAlphaTest();
                RenderSystem.defaultBlendFunc();
                RenderSystem.colorMask(true, true, true, true);
                float transparency = pageTextRefreshTime / 20F;

                color &= 0x00FFFFFF;
                color = ((int) (0xFF * transparency) << 24) | color;
            }

            HandledScreen currentScreen = tabManager.getCurrentScreen();
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            int height = ((HandledScreenAccessor) currentScreen).getBackgroundHeight();
            int oX = currentScreen.width;
            int oY = (currentScreen.height - height) / 2;

            String text = (tabManager.currentPage + 1) + " / " + (tabManager.getMaxPages() + 1);
            int x = (oX - textRenderer.getWidth(text)) / 2;
            int y = oY - 34;

            MinecraftClient.getInstance().textRenderer.draw(
                    matrices,
                    text,
                    x,
                    y,
                    color
            );

            RenderSystem.popMatrix();
        }
    }

    private void renderTab(MatrixStack matrices, TabRenderInfo tabRenderInfo) {
        HandledScreen currentScreen = tabManager.getCurrentScreen();

        MinecraftClient.getInstance().getTextureManager().bindTexture(TABS_TEXTURE);
        currentScreen.drawTexture(
                matrices,
                tabRenderInfo.x,
                tabRenderInfo.y,
                tabRenderInfo.texU,
                tabRenderInfo.texV,
                tabRenderInfo.texW,
                tabRenderInfo.texH
        );

        ItemRenderer itemRenderer = ((ScreenAccessor) currentScreen).getItemRenderer();
        TextRenderer textRenderer = ((ScreenAccessor) currentScreen).getTextRenderer();
        itemRenderer.zOffset = 100.0F;
        RenderSystem.enableRescaleNormal();
        ItemStack itemStack = tabRenderInfo.tabReference.getItemStack();
        itemRenderer.renderInGuiWithOverrides(itemStack, tabRenderInfo.itemX, tabRenderInfo.itemY);
        itemRenderer.renderGuiItemOverlay(textRenderer, itemStack, tabRenderInfo.itemX, tabRenderInfo.itemY);
        itemRenderer.zOffset = 0.0F;
    }

    public void renderHoverTooltips(MatrixStack matrices, double mouseX, double mouseY) {
        for (int i = 0; i < tabRenderInfos.length; i++) {
            TabRenderInfo tabRenderInfo = tabRenderInfos[i];

            if (tabRenderInfo != null) {
                Rectangle itemRec = new Rectangle(tabRenderInfo.itemX, tabRenderInfo.itemY, 16, 16);

                if (itemRec.contains(mouseX, mouseY)) {
                    tabManager.getCurrentScreen().renderTooltip(matrices, tabRenderInfo.tabReference.getHoverText(), (int) mouseX, (int) mouseY);
                }
            }
        }
    }

    public TabRenderInfo[] getTabRenderInfos() {
        HandledScreen currentScreen = tabManager.getCurrentScreen();

        int maxRowLength = tabManager.getMaxRowLength();
        int numVisibleTabs = maxRowLength * 2;
        int startingIndex = tabManager.currentPage * numVisibleTabs;

        TabRenderInfo[] tabRenderInfo = new TabRenderInfo[numVisibleTabs];

        int x = (currentScreen.width - ((HandledScreenAccessor) currentScreen).getBackgroundWidth()) / 2;
        int y = (currentScreen.height - ((HandledScreenAccessor) currentScreen).getBackgroundHeight()) / 2;

        for (int i = 0; i < numVisibleTabs; i++) {
            if (startingIndex + i < tabManager.tabs.size()) {
                // Setup basic info
                Tab tab = tabManager.tabs.get(startingIndex + i);
                boolean topRow = i < maxRowLength;
                boolean selected = tab == tabManager.currentTab;

                // Create tab info object
                TabRenderInfo tabInfo = new TabRenderInfo();
                tabInfo.tabReference = tab;
                tabInfo.index = startingIndex + i;

                // Calc x value
                tabInfo.x = x + i * (TAB_WIDTH + 1);
                if (!topRow) {
                    tabInfo.x -= maxRowLength * (TAB_WIDTH + 1);
                }

                // Calc y value
                if (topRow) {
                    tabInfo.y = y - 26;

                    if (selected) {
                        tabInfo.y = y - 28;
                    }
                } else {
                    tabInfo.y = y + ((HandledScreenAccessor) currentScreen).getBackgroundHeight() - 4 + bottomRowYOffset;
                }

                // Calc texture dimensions
                tabInfo.texW = 28;
                tabInfo.texH = 32;

                // Calc texture U
                if (i == 0 || i == maxRowLength) {
                    tabInfo.texU = 0;
                } else {
                    tabInfo.texU = 28;
                }

                // Calc texture V
                if (topRow) {
                    if (selected) {
                        tabInfo.texV = 32;
                    } else {
                        tabInfo.texV = 0;
                    }
                } else {
                    if (selected) {
                        tabInfo.texV = 96;
                    } else {
                        tabInfo.texV = 64;
                    }
                }

                // Calc item position
                if (topRow) {
                    tabInfo.itemX = tabInfo.x + 6;
                    tabInfo.itemY = tabInfo.y + 8;
                } else {
                    tabInfo.itemX = tabInfo.x + 6;
                    tabInfo.itemY = tabInfo.y + 6;
                }

                tabRenderInfo[i] = tabInfo;
            }
        }

        return tabRenderInfo;
    }

    public void update() {
        pageTextRefreshTime = Math.max(pageTextRefreshTime - 1, 0);
    }

    public void resetPageTextRefreshTime() {
        pageTextRefreshTime = 60;
    }
}
