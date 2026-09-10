package org.example.dnnItem.Resource.genarator;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.example.dnnItem.DnnItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


import static org.example.dnnItem.Resource.genarator.TextureGenerator.copyFolder;

public class ModelGenerator {

    private static String getParent(Material material) {

        String name = material.name();

        if (name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")) {

            return "minecraft:item/handheld";
        }

        if (material == Material.BOW) {
            return "minecraft:item/bow";
        }

        if (material == Material.CROSSBOW) {
            return "minecraft:item/crossbow";
        }

        return "minecraft:item/generated";
    }



    private static void generateModel(
            DnnItem plugin,
            String id,
            ConfigurationSection item
    ) throws IOException {

        // Agar custom model diya hai
        if (item.contains("model")) {
            return;
        }

        String texture = item.getString("texture");

        if (texture == null)
            return;

        Material material = Material.valueOf(item.getString("material"));

        getParent(material);

        JsonObject root = new JsonObject();

        root.addProperty("parent", getParent(material));

        JsonObject textures = new JsonObject();

        textures.addProperty("layer0", "dnnitem:" + texture);

        root.add("textures", textures);

        File output = new File(
                plugin.getDataFolder(),
                "generated-pack/assets/dnnitem/models/item/" + id + ".json"
        );

        output.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(output)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        }


    }

    public static void generate(DnnItem plugin) throws IOException {

        // 1. Custom models ek baar copy karo
        copyModels(plugin);

        // 2. items.yml padho
        ConfigurationSection items =
                plugin.getItemsConfig().getConfigurationSection("items");

        if (items == null)
            return;

        // 3. Har item ka model generate karo
        for (String id : items.getKeys(false)) {

            ConfigurationSection item = items.getConfigurationSection(id);

            generateModel(plugin, id, item);
        }
    }

    private static void copyModels(DnnItem plugin) throws IOException {

        File source = new File(plugin.getDataFolder(), "models");

        if (!source.exists())
            return;

        File destination = new File(
                plugin.getDataFolder(),
                "generated-pack/assets/dnnitem/models"
        );

        copyFolder(source.toPath(), destination.toPath());
    }
}
