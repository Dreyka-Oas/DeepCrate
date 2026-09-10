package oas.dreyka.deepcrate.client.screen;

import oas.dreyka.deepcrate.config.domain.ScreenConfig;
import oas.dreyka.deepcrate.inventory.slot.DeepCrateSlot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A crate slot holding more than the base game ever draws in one: past the configured threshold the
 * count is shortened, because five digits do not fit in sixteen pixels.
 */
final class CrateSlotCount {
    private CrateSlotCount() {}

    static boolean shortens(Slot slot) {
        return slot instanceof DeepCrateSlot && slot.getItem().getCount() > ScreenConfig.abbreviateAbove;
    }

    static void render(GuiGraphics guiGraphics, Font font, Slot slot, int imageWidth) {
        ItemStack itemStack = slot.getItem();
        guiGraphics.renderItem(itemStack, slot.x, slot.y, slot.x + slot.y * imageWidth);
        guiGraphics.renderItemDecorations(font, itemStack, slot.x, slot.y, abbreviate(itemStack.getCount()));
    }

    private static String abbreviate(int count) {
        if (count >= 1_000_000) {
            return count / 1_000_000 + "M";
        }

        return count / 1000 + "k";
    }
}
