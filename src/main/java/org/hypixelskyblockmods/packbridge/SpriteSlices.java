package org.hypixelskyblockmods.packbridge;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SpriteSlices {
    public record Slice(String sheet, int sheetWidth, int sheetHeight, int x, int y, int width, int height) {}
    public static final Map<String, Slice> ALL;
    static {
        Map<String, Slice> slices = new LinkedHashMap<>();
        String icons = "textures/gui/icons.png", widgets = "textures/gui/widgets.png";
        add(slices, "hud/crosshair", icons, 0, 0, 15, 15);
        for (int i = 0; i < 3; i++) {
            String state = new String[]{"empty", "half", "full"}[i];
            add(slices, "hud/armor_" + state, icons, 16 + i * 9, 9, 9, 9);
        }
        for (String state : new String[]{"empty", "full", "half"}) {
            int x = switch (state) { case "full" -> 52; case "half" -> 61; default -> 16; };
            add(slices, "hud/food_" + state, icons, x, 27, 9, 9);
            add(slices, "hud/food_" + state + "_hunger", icons,
                    state.equals("empty") ? 133 : x + 36, 27, 9, 9);
        }
        add(slices, "hud/air", icons, 16, 18, 9, 9);
        add(slices, "hud/air_bursting", icons, 25, 18, 9, 9);
        add(slices, "hud/experience_bar_background", icons, 0, 64, 182, 5);
        add(slices, "hud/experience_bar_progress", icons, 0, 69, 182, 5);
        add(slices, "hud/jump_bar_background", icons, 0, 84, 182, 5);
        add(slices, "hud/jump_bar_progress", icons, 0, 89, 182, 5);
        for (boolean hardcore : new boolean[]{false, true}) {
            int y = hardcore ? 45 : 0;
            String suffix = hardcore ? "_hardcore" : "";
            add(slices, "hud/heart/container" + suffix, icons, 16, y, 9, 9);
            add(slices, "hud/heart/container" + suffix + "_blinking", icons, 25, y, 9, 9);
            for (String type : new String[]{"", "poisoned_", "withered_", "absorbing_"}) {
                int base = switch (type) { case "poisoned_" -> 88; case "withered_" -> 124;
                    case "absorbing_" -> 160; default -> 52; };
                for (boolean half : new boolean[]{false, true}) {
                    String name = type + (hardcore ? "hardcore_" : "") + (half ? "half" : "full");
                    add(slices, "hud/heart/" + name, icons, base + (half ? 9 : 0), y, 9, 9);
                    add(slices, "hud/heart/" + name + "_blinking", icons,
                            base + (type.equals("absorbing_") ? 0 : 18) + (half ? 9 : 0), y, 9, 9);
                }
            }
        }
        add(slices, "hud/heart/vehicle_container", icons, 52, 9, 9, 9);
        add(slices, "hud/heart/vehicle_full", icons, 88, 9, 9, 9);
        add(slices, "hud/heart/vehicle_half", icons, 97, 9, 9, 9);
        add(slices, "hud/hotbar", widgets, 0, 0, 182, 22);
        add(slices, "hud/hotbar_selection", widgets, 0, 22, 24, 24);
        add(slices, "widget/button_disabled", widgets, 0, 46, 200, 20);
        add(slices, "widget/button", widgets, 0, 66, 200, 20);
        add(slices, "widget/button_highlighted", widgets, 0, 86, 200, 20);
        ALL = Map.copyOf(slices);
    }
    private static void add(Map<String, Slice> map, String name, String sheet, int x, int y, int w, int h) {
        map.put("textures/gui/sprites/" + name + ".png", new Slice(sheet, 256, 256, x, y, w, h));
    }
    private SpriteSlices() {}
}
