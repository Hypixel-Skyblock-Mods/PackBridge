package org.hypixelskyblockmods.packbridge;

import java.util.LinkedHashMap;
import java.util.Map;

/** Names and paths used by vanilla texture assets, independent of pack contents. */
public final class TextureNames {
    private static final Map<String, String> RENAMES = new LinkedHashMap<>();
    static {
        String[][] pairs = {
            {"apple_golden", "golden_apple"}, {"carrot_golden", "golden_carrot"},
            {"melon", "melon_slice"}, {"speckled_melon", "glistering_melon_slice"},
            {"reeds", "sugar_cane"}, {"slimeball", "slime_ball"}, {"fireball", "fire_charge"},
            {"boat", "oak_boat"}, {"door_wood", "oak_door"}, {"door_iron", "iron_door"},
            {"sign", "oak_sign"}, {"bed", "red_bed"}, {"fish_raw", "cod"},
            {"fish_cooked", "cooked_cod"}, {"fish_salmon_raw", "salmon"},
            {"fish_salmon_cooked", "cooked_salmon"}, {"fish_clownfish_raw", "tropical_fish"},
            {"fish_pufferfish_raw", "pufferfish"}, {"beef_raw", "beef"},
            {"beef_cooked", "cooked_beef"}, {"porkchop_raw", "porkchop"},
            {"porkchop_cooked", "cooked_porkchop"}, {"chicken_raw", "chicken"},
            {"chicken_cooked", "cooked_chicken"}, {"mutton_raw", "mutton"},
            {"mutton_cooked", "cooked_mutton"}, {"rabbit_raw", "rabbit"},
            {"rabbit_cooked", "cooked_rabbit"}, {"potato_baked", "baked_potato"},
            {"potato_poisonous", "poisonous_potato"}, {"netherbrick", "nether_brick"},
            {"ender_eye", "ender_eye"}, {"nether_star", "nether_star"},
            {"potion_bottle_drinkable", "potion"}, {"potion_bottle_splash", "splash_potion"},
            {"potion_bottle_empty", "glass_bottle"}, {"book_normal", "book"},
            {"book_writable", "writable_book"}, {"book_written", "written_book"},
            {"book_enchanted", "enchanted_book"}, {"map_empty", "map"},
            {"map_filled", "filled_map"}, {"minecart_normal", "minecart"},
            {"minecart_chest", "chest_minecart"}, {"minecart_furnace", "furnace_minecart"},
            {"minecart_tnt", "tnt_minecart"}, {"minecart_hopper", "hopper_minecart"},
            {"minecart_command_block", "command_block_minecart"},
            {"bucket_empty", "bucket"}, {"bucket_water", "water_bucket"},
            {"bucket_lava", "lava_bucket"}, {"bucket_milk", "milk_bucket"},
            {"fishing_rod_uncast", "fishing_rod"}, {"fishing_rod_cast", "fishing_rod_cast"},
            {"carrot_on_a_stick", "carrot_on_a_stick"}, {"totem", "totem_of_undying"},
            {"horsearmor_iron", "iron_horse_armor"}, {"horsearmor_gold", "golden_horse_armor"},
            {"horsearmor_diamond", "diamond_horse_armor"}, {"wooden_armorstand", "armor_stand"},
            {"stonebrick", "stone_bricks"}, {"stonebricksmooth", "stone_bricks"},
            {"stone_slab_top", "smooth_stone"}, {"stone_slab_side", "smooth_stone_slab_side"},
            {"grass_top", "grass_block_top"}, {"grass_side", "grass_block_side"},
            {"grass_side_overlay", "grass_block_side_overlay"},
            {"grass_side_snowed", "grass_block_snow"}, {"dirt_podzol_top", "podzol_top"},
            {"dirt_podzol_side", "podzol_side"}, {"dirt_coarse", "coarse_dirt"},
            {"grass", "short_grass"}, {"tallgrass", "short_grass"},
            {"deadbush", "dead_bush"}, {"waterlily", "lily_pad"},
            {"flower_rose", "poppy"}, {"flower_dandelion", "dandelion"},
            {"flower_blue_orchid", "blue_orchid"}, {"flower_allium", "allium"},
            {"flower_houstonia", "azure_bluet"}, {"flower_oxeye_daisy", "oxeye_daisy"},
            {"flower_tulip_red", "red_tulip"}, {"flower_tulip_orange", "orange_tulip"},
            {"flower_tulip_white", "white_tulip"}, {"flower_tulip_pink", "pink_tulip"},
            {"double_plant_grass_bottom", "tall_grass_bottom"},
            {"double_plant_grass_top", "tall_grass_top"},
            {"double_plant_fern_bottom", "large_fern_bottom"},
            {"double_plant_fern_top", "large_fern_top"},
            {"stone_granite", "granite"}, {"stone_granite_smooth", "polished_granite"},
            {"stone_diorite", "diorite"}, {"stone_diorite_smooth", "polished_diorite"},
            {"stone_andesite", "andesite"}, {"stone_andesite_smooth", "polished_andesite"},
            {"stonebrick_mossy", "mossy_stone_bricks"},
            {"stonebrick_cracked", "cracked_stone_bricks"},
            {"stonebrick_carved", "chiseled_stone_bricks"},
            {"brick", "bricks"}, {"nether_brick", "nether_bricks"},
            {"red_nether_brick", "red_nether_bricks"}, {"end_bricks", "end_stone_bricks"},
            {"lightgem", "glowstone"}, {"noteblock", "note_block"},
            {"soul_sand", "soul_sand"}, {"redstone_dust_line", "redstone_dust_line0"},
            {"redstone_dust_cross", "redstone_dust_dot"},
            {"rail_normal", "rail"}, {"rail_normal_turned", "rail_corner"},
            {"rail_golden", "powered_rail"}, {"rail_golden_powered", "powered_rail_on"},
            {"rail_detector", "detector_rail"}, {"rail_detector_powered", "detector_rail_on"},
            {"rail_activator", "activator_rail"}, {"rail_activator_powered", "activator_rail_on"},
            {"piston_top_normal", "piston_top"}, {"piston_top_sticky", "piston_top_sticky"},
            {"wheat_stage_0", "wheat_stage0"}, {"snow", "snow"},
            {"ice_packed", "packed_ice"}, {"slime", "slime_block"},
            {"mob_spawner", "spawner"}, {"web", "cobweb"},
            {"portal", "nether_portal"}, {"fire_layer_0", "fire_0"},
            {"fire_layer_1", "fire_1"}, {"redstone_lamp_off", "redstone_lamp"},
            {"redstone_lamp_on", "redstone_lamp_on"},
            {"redstone_torch_on", "redstone_torch"}, {"redstone_torch_off", "redstone_torch_off"},
            {"repeater_off", "repeater"}, {"repeater_on", "repeater_on"},
            {"comparator_off", "comparator"}, {"comparator_on", "comparator_on"},
            {"enchanting_table_top", "enchanting_table_top"},
            {"enchanting_table_side", "enchanting_table_side"},
            {"quartz_block_chiseled", "chiseled_quartz_block"},
            {"quartz_block_chiseled_top", "chiseled_quartz_block_top"},
            {"quartz_block_lines", "quartz_pillar"}, {"quartz_block_lines_top", "quartz_pillar_top"},
            {"sandstone_normal", "sandstone"}, {"sandstone_carved", "chiseled_sandstone"},
            {"sandstone_smooth", "cut_sandstone"}, {"red_sandstone_normal", "red_sandstone"},
            {"red_sandstone_carved", "chiseled_red_sandstone"},
            {"red_sandstone_smooth", "cut_red_sandstone"}
        };
        for (String[] pair : pairs) RENAMES.put(pair[0], pair[1]);
        for (String tool : new String[]{"sword", "pickaxe", "axe", "shovel", "hoe"}) {
            RENAMES.put("gold_" + tool, "golden_" + tool);
            RENAMES.put("wood_" + tool, "wooden_" + tool);
        }
        for (String armor : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            RENAMES.put("gold_" + armor, "golden_" + armor);
            RENAMES.put("chainmail_" + armor, "chainmail_" + armor);
        }
        for (String wood : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"}) {
            RENAMES.put("planks_" + wood, wood + "_planks");
            RENAMES.put("log_" + wood, wood + "_log");
            RENAMES.put("log_" + wood + "_top", wood + "_log_top");
            RENAMES.put("leaves_" + wood, wood + "_leaves");
            RENAMES.put("sapling_" + wood, wood + "_sapling");
            RENAMES.put("door_" + wood, wood + "_door");
            RENAMES.put("door_" + wood + "_lower", wood + "_door_bottom");
            RENAMES.put("door_" + wood + "_upper", wood + "_door_top");
            RENAMES.put("boat_" + wood, wood + "_boat");
        }
        for (String color : new String[]{"white", "orange", "magenta", "light_blue", "yellow",
                "lime", "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"}) {
            String modern = color.equals("silver") ? "light_gray" : color;
            RENAMES.put("wool_colored_" + color, modern + "_wool");
            RENAMES.put("hardened_clay_stained_" + color, modern + "_terracotta");
            RENAMES.put("glass_" + color, modern + "_stained_glass");
            RENAMES.put("glass_pane_top_" + color, modern + "_stained_glass_pane_top");
            RENAMES.put("concrete_" + color, modern + "_concrete");
            RENAMES.put("concrete_powder_" + color, modern + "_concrete_powder");
        }
        RENAMES.put("hardened_clay", "terracotta");
        for (String crop : new String[]{"wheat", "carrots", "potatoes", "nether_wart"}) {
            for (int stage = 0; stage < 8; stage++) RENAMES.put(crop + "_stage_" + stage, crop + "_stage" + stage);
        }
    }

    private TextureNames() {}

    public static String modernPath(String path) {
        String normalized = path.replace("textures/items/", "textures/item/")
                .replace("textures/blocks/", "textures/block/");
        var armor = java.util.regex.Pattern.compile(
                "textures/models/armor/(leather|chainmail|iron|gold|diamond)_layer_([12])(_overlay)?(\\.png(?:\\.mcmeta)?)")
                .matcher(normalized);
        if (armor.matches()) {
            return "textures/entity/equipment/" + (armor.group(2).equals("2") ? "humanoid_leggings/" : "humanoid/")
                    + armor.group(1) + (armor.group(3) == null ? "" : armor.group(3)) + armor.group(4);
        }
        if (!normalized.startsWith("textures/item/") && !normalized.startsWith("textures/block/")) {
            return switch (normalized) {
                case "textures/misc/enchanted_item_glint.png" -> "textures/misc/enchanted_glint_item.png";
                case "textures/entity/iron_golem.png" -> "textures/entity/iron_golem/iron_golem.png";
                default -> normalized;
            };
        }
        int slash = normalized.lastIndexOf('/');
        int dot = normalized.indexOf('.', slash);
        if (dot < 0) dot = normalized.length();
        String name = normalized.substring(slash + 1, dot);
        // Item nether_brick and block nether_bricks were named alike in older packs.
        String renamed = normalized.startsWith("textures/item/") && (name.equals("netherbrick") || name.equals("nether_brick"))
                ? "nether_brick" : RENAMES.getOrDefault(name, name);
        return normalized.substring(0, slash + 1) + renamed + normalized.substring(dot);
    }

    public static String modernReference(String reference) {
        if (reference.startsWith("#")) return reference;
        String namespace = "";
        int colon = reference.indexOf(':');
        if (colon >= 0) {
            namespace = reference.substring(0, colon + 1);
            reference = reference.substring(colon + 1);
        }
        if (reference.equals("builtin/generated")) return namespace + "item/generated";
        return namespace + modernPath("textures/" + reference).substring("textures/".length());
    }
}
