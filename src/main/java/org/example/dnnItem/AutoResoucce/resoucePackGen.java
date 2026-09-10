package org.example.dnnItem.AutoResoucce;

import org.bukkit.configuration.ConfigurationSection;
import org.example.dnnItem.DnnItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bukkit.Bukkit.getLogger;


public class resoucePackGen {


    public void generateResourcePack() {


        File packSource = new File(DnnItem.getInstance().getDataFolder(), "pack");
        File packFolder = new File(DnnItem.getInstance().getDataFolder(), "generated_pack");

        File modelsSource = new File(packSource, "models");
        File texturesSource = new File(packSource, "textures");

        packSource.mkdirs();
        modelsSource.mkdirs();
        texturesSource.mkdirs();
        packFolder.mkdirs();

        try {
            // pack.mcmeta
            File metaFile = new File(packFolder, "pack.mcmeta");
            java.nio.file.Files.writeString(metaFile.toPath(), """
            {
              "pack": {
                "description": "dnnItemX Resource Pack",
                "min_format": 69,
                "max_format": 9999999
              }
            }
            """);

            ConfigurationSection items = DnnItem.getInstance().getItemsConfig().getConfigurationSection("items");
            if (items == null) {
                getLogger().warning("No items section found in items.yml");
                return;
            }

            // Purane item JSON clear
            File itemsFolder = new File(packFolder, "assets/minecraft/items");
            if (itemsFolder.exists()) {
                File[] oldFiles = itemsFolder.listFiles();
                if (oldFiles != null) {
                    for (File f : oldFiles) {
                        if (f.getName().endsWith(".json")) f.delete();
                    }
                }
            }

            // ★ Map yahan banao
            Map<String, List<String[]>> materialEntries = new HashMap<>();

            int success = 0;
            int failed = 0;

            // ========== LOOP 1: Collect + Copy models/textures ==========
            for (String id : items.getKeys(false)) {
                ConfigurationSection sec = items.getConfigurationSection(id);
                if (sec == null) continue;

                String material = sec.getString("material", "").toLowerCase();
                int cmd = sec.getInt("custom-model-data", 0);
                String modelPath = sec.getString("model");
                String texturePath = sec.getString("texture");

                if (cmd <= 0) {
                    getLogger().warning("Skipping " + id + " → custom-model-data missing");
                    failed++;
                    continue;
                }

                if (modelPath == null || modelPath.isEmpty()) {
                    getLogger().warning("Skipping " + id + " → model path missing");
                    failed++;
                    continue;
                }

                modelPath = modelPath.replace("\\", "/").trim();
                if (texturePath != null) texturePath = texturePath.replace("\\", "/").trim();

                String modelName = modelPath.contains("/")
                        ? modelPath.substring(modelPath.lastIndexOf("/") + 1)
                        : modelPath;

                boolean isArmor = material.contains("helmet") || material.contains("chestplate")
                        || material.contains("leggings") || material.contains("boots");

                String modelFolder = isArmor ? "armor" : "item";

                // Copy Model
                File sourceModel = new File(modelsSource, modelPath + ".json");
                if (!sourceModel.exists()) {
                    getLogger().warning("Model not found: " + sourceModel.getAbsolutePath());
                    failed++;
                    continue;
                }

                File destModel = new File(packFolder, "assets/minecraft/models/" + modelFolder + "/" + modelName + ".json");
                destModel.getParentFile().mkdirs();
                java.nio.file.Files.copy(sourceModel.toPath(), destModel.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Collect for JSON (duplicate check)
                materialEntries.putIfAbsent(material, new ArrayList<>());

                boolean exists = false;
                for (String[] arr : materialEntries.get(material)) {
                    if (Integer.parseInt(arr[0]) == cmd) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    materialEntries.get(material).add(new String[]{
                            String.valueOf(cmd),
                            modelFolder + "/" + modelName
                    });
                }

                success++;
                getLogger().info("§a✔ Prepared: " + id + " (CMD: " + cmd + ")");
            }

            // ========== Textures ek baar copy (loop ke bahar) ==========
            File texturesItemFolder = new File(texturesSource, "item");
            if (texturesItemFolder.exists() && texturesItemFolder.isDirectory()) {
                File destFolder = new File(packFolder, "assets/minecraft/textures/item");
                destFolder.mkdirs();

                File[] files = texturesItemFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            String name = file.getName().toLowerCase();
                            if (name.endsWith(".png") || name.endsWith(".mcmeta") || name.endsWith(".png.mcmeta")) {
                                File dest = new File(destFolder, file.getName());
                                java.nio.file.Files.copy(file.toPath(), dest.toPath(),
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                getLogger().info("§aCopied: " + file.getName());
                            }
                        }
                    }
                }
            }

            // ========== LOOP 2: Ab JSON files likho ==========
            for (Map.Entry<String, List<String[]>> entry : materialEntries.entrySet()) {
                String mat = entry.getKey();
                List<String[]> list = entry.getValue();

                StringBuilder entriesJson = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    String[] data = list.get(i);
                    entriesJson.append("""
                          {
                            "threshold": %s,
                            "model": {
                              "type": "minecraft:model",
                              "model": "minecraft:%s"
                            }
                          }""".formatted(data[0], data[1]));

                    if (i < list.size() - 1) {
                        entriesJson.append(",\n");
                    }
                }

                String itemJson = """
                {
                  "model": {
                    "type": "minecraft:range_dispatch",
                    "property": "minecraft:custom_model_data",
                    "index": 0,
                    "entries": [
                %s
                    ],
                    "fallback": {
                      "type": "minecraft:model",
                      "model": "minecraft:item/%s"
                    }
                  }
                }
                """.formatted(entriesJson.toString(), mat);

                File itemFile = new File(packFolder, "assets/minecraft/items/" + mat + ".json");
                itemFile.getParentFile().mkdirs();
                java.nio.file.Files.writeString(itemFile.toPath(), itemJson);

                getLogger().info("§aWritten: " + mat + ".json (" + list.size() + " models)");



            }

            getLogger().info("§a§lResource Pack Generated!");
            getLogger().info("§aSuccess: " + success + " | Failed: " + failed);
            getLogger().info("§eLocation: " + packFolder.getAbsolutePath());

        } catch (Exception e) {
            getLogger().severe("Failed to generate resource pack");
            e.printStackTrace();
        }
    }

    public void generateArmorPack() {
        File packSource = new File(DnnItem.getInstance().getDataFolder(), "pack");
        File packFolder = new File(DnnItem.getInstance().getDataFolder(), "generated_pack");

        File modelsSource = new File(packSource, "models");
        File texturesSource = new File(packSource, "textures");

        packSource.mkdirs();
        modelsSource.mkdirs();
        texturesSource.mkdirs();
        packFolder.mkdirs();

        try {
            // pack.mcmeta
            File metaFile = new File(packFolder, "pack.mcmeta");
            java.nio.file.Files.writeString(metaFile.toPath(), """
        {
          "pack": {
            "description": "dnnItemX Armor Pack",
            "min_format": 34,
            "max_format": 99
          }
        }
        """);

            ConfigurationSection items = DnnItem.getInstance().getItemsConfig().getConfigurationSection("items");
            if (items == null) {
                getLogger().warning("No items section found!");
                return;
            }

            // material → list of [cmd, modelPath]
            java.util.Map<String, java.util.List<String[]>> materialEntries = new java.util.HashMap<>();

            int success = 0;
            int failed = 0;

            for (String id : items.getKeys(false)) {
                ConfigurationSection sec = items.getConfigurationSection(id);
                if (sec == null) continue;

                String material = sec.getString("material", "").toLowerCase();
                int cmd = sec.getInt("custom-model-data", 0);
                String modelPath = sec.getString("model");
                String texturePath = sec.getString("texture");

                // Sirf armor
                boolean isArmor = material.contains("helmet") || material.contains("chestplate")
                        || material.contains("leggings") || material.contains("boots");

                if (!isArmor) continue;

                if (cmd <= 0 || modelPath == null || modelPath.isEmpty()) {
                    getLogger().warning("Skipping " + id + " → model/cmd missing");
                    failed++;
                    continue;
                }

                modelPath = modelPath.replace("\\", "/").trim();
                if (texturePath != null) texturePath = texturePath.replace("\\", "/").trim();

                String modelName = modelPath.contains("/")
                        ? modelPath.substring(modelPath.lastIndexOf("/") + 1)
                        : modelPath;

                // ========== Copy Model ==========
                File sourceModel = new File(modelsSource, modelPath + ".json");
                if (!sourceModel.exists()) {
                    getLogger().warning("Model not found: " + sourceModel.getAbsolutePath());
                    failed++;
                    continue;
                }

                File destModel = new File(packFolder, "assets/minecraft/models/armor/" + modelName + ".json");
                destModel.getParentFile().mkdirs();
                java.nio.file.Files.copy(sourceModel.toPath(), destModel.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // ========== Copy Texture ==========
                if (texturePath != null) {
                    File sourceTex = new File(texturesSource, texturePath + ".png");
                    if (sourceTex.exists()) {
                        String texName = texturePath.contains("/")
                                ? texturePath.substring(texturePath.lastIndexOf("/") + 1)
                                : texturePath;

                        File destTex = new File(packFolder, "assets/minecraft/textures/models/armor/" + texName + ".png");
                        destTex.getParentFile().mkdirs();
                        java.nio.file.Files.copy(sourceTex.toPath(), destTex.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        getLogger().info("§aCopied texture: " + texName + ".png");
                    }
                }

                // Collect for items JSON
                materialEntries.putIfAbsent(material, new java.util.ArrayList<>());
                boolean exists = false;
                for (String[] arr : materialEntries.get(material)) {
                    if (Integer.parseInt(arr[0]) == cmd) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    materialEntries.get(material).add(new String[]{String.valueOf(cmd), "armor/" + modelName});
                }

                success++;
                getLogger().info("§a✔ Armor prepared: " + id);
            }

            // ========== Write items/<material>.json ==========
            for (var entry : materialEntries.entrySet()) {
                String mat = entry.getKey();
                var list = entry.getValue();

                StringBuilder entriesJson = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    String[] data = list.get(i);
                    entriesJson.append("""
                      {
                        "threshold": %s,
                        "model": {
                          "type": "minecraft:model",
                          "model": "minecraft:%s"
                        }
                      }""".formatted(data[0], data[1]));
                    if (i < list.size() - 1) entriesJson.append(",\n");
                }

                String itemJson = """
            {
              "model": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:custom_model_data",
                "index": 0,
                "entries": [
            %s
                ],
                "fallback": {
                  "type": "minecraft:model",
                  "model": "minecraft:item/%s"
                }
              }
            }
            """.formatted(entriesJson.toString(), mat);

                File itemFile = new File(packFolder, "assets/minecraft/items/" + mat + ".json");
                itemFile.getParentFile().mkdirs();
                java.nio.file.Files.writeString(itemFile.toPath(), itemJson);
                getLogger().info("§aWritten: " + mat + ".json");
            }

            getLogger().info("§a§lArmor Pack Generated!");
            getLogger().info("§aSuccess: " + success + " | Failed: " + failed);
            getLogger().info("§eLocation: " + packFolder.getAbsolutePath());

        } catch (Exception e) {
            getLogger().severe("Armor pack generate failed!");
            e.printStackTrace();
        }
    }


    public void generateResourcePackX() {

        File packSource = new File(DnnItem.getInstance().getDataFolder(), "pack");
        File packFolder = new File(DnnItem.getInstance().getDataFolder(), "generated_pack");

        File modelsSource = new File(packSource, "models");
        File texturesSource = new File(packSource, "textures");

        packSource.mkdirs();
        modelsSource.mkdirs();
        texturesSource.mkdirs();
        packFolder.mkdirs();

        try {
            // pack.mcmeta
            File metaFile = new File(packFolder, "pack.mcmeta");
            java.nio.file.Files.writeString(metaFile.toPath(), """
        {
          "pack": {
            "description": "dnnItemX Resource Pack",
            "min_format": 69,
            "max_format": 9999999
          }
        }
        """);

            ConfigurationSection items = DnnItem.getInstance().getItemsConfig().getConfigurationSection("items");
            if (items == null) {
                getLogger().warning("No items section found in items.yml");
                return;
            }

            // Clear old item JSON files
            File itemsFolder = new File(packFolder, "assets/minecraft/items");
            if (itemsFolder.exists()) {
                File[] oldFiles = itemsFolder.listFiles();
                if (oldFiles != null) {
                    for (File f : oldFiles) {
                        if (f.getName().endsWith(".json")) f.delete();
                    }
                }
            }

            // Map for regular items
            Map<String, List<String[]>> materialEntries = new HashMap<>();
            // Map for crossbow items (special handling)
            Map<String, Map<Integer, String>> crossbowEntries = new HashMap<>();

            int success = 0;
            int failed = 0;

            // ========== LOOP 1: Collect + Copy models/textures ==========
            for (String id : items.getKeys(false)) {
                ConfigurationSection sec = items.getConfigurationSection(id);
                if (sec == null) continue;

                String material = sec.getString("material", "").toLowerCase();
                int cmd = sec.getInt("custom-model-data", 0);
                String modelPath = sec.getString("model");
                String texturePath = sec.getString("texture");

                // Check if this is a crossbow
                boolean isCrossbow = material.equals("crossbow");

                if (cmd <= 0) {
                    getLogger().warning("Skipping " + id + " → custom-model-data missing");
                    failed++;
                    continue;
                }

                if (modelPath == null || modelPath.isEmpty()) {
                    getLogger().warning("Skipping " + id + " → model path missing");
                    failed++;
                    continue;
                }

                modelPath = modelPath.replace("\\", "/").trim();
                if (texturePath != null) texturePath = texturePath.replace("\\", "/").trim();

                String modelName = modelPath.contains("/")
                        ? modelPath.substring(modelPath.lastIndexOf("/") + 1)
                        : modelPath;

                boolean isArmor = material.contains("helmet") || material.contains("chestplate")
                        || material.contains("leggings") || material.contains("boots");

                String modelFolder = isArmor ? "armor" : "item";

                // Copy Model
                File sourceModel = new File(modelsSource, modelPath + ".json");
                if (!sourceModel.exists()) {
                    getLogger().warning("Model not found: " + sourceModel.getAbsolutePath());
                    failed++;
                    continue;
                }

                File destModel = new File(packFolder, "assets/minecraft/models/" + modelFolder + "/" + modelName + ".json");
                destModel.getParentFile().mkdirs();
                java.nio.file.Files.copy(sourceModel.toPath(), destModel.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // ========== Handle Crossbow separately ==========
                if (isCrossbow) {
                    // For crossbow, we need to map each pull state to a different model
                    // Expected format in config: model: "crossbow/pull0", "crossbow/pull1", "crossbow/pull2"
                    // Or you can specify them separately

                    String pull0Model = sec.getString("pull0_model", modelPath + "_pull0");
                    String pull1Model = sec.getString("pull1_model", modelPath + "_pull1");
                    String pull2Model = sec.getString("pull2_model", modelPath + "_pull2");

                    // Copy pull state models
                    String[] pullStates = {pull0Model, pull1Model, pull2Model};
                    int[] pullIndices = {0, 1, 2};

                    for (int i = 0; i < pullStates.length; i++) {
                        String pullPath = pullStates[i];
                        pullPath = pullPath.replace("\\", "/").trim();

                        String pullName = pullPath.contains("/")
                                ? pullPath.substring(pullPath.lastIndexOf("/") + 1)
                                : pullPath;

                        File sourcePullModel = new File(modelsSource, pullPath + ".json");
                        if (!sourcePullModel.exists()) {
                            getLogger().warning("Pull model not found: " + sourcePullModel.getAbsolutePath());
                            continue;
                        }

                        File destPullModel = new File(packFolder, "assets/minecraft/models/" + modelFolder + "/" + pullName + ".json");
                        destPullModel.getParentFile().mkdirs();
                        java.nio.file.Files.copy(sourcePullModel.toPath(), destPullModel.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                        // Store for crossbow JSON generation
                        crossbowEntries.putIfAbsent(material, new HashMap<>());
                        crossbowEntries.get(material).put(pullIndices[i], modelFolder + "/" + pullName);
                    }

                    success++;
                    getLogger().info("§a✔ Prepared crossbow: " + id + " (CMD: " + cmd + ")");
                    continue; // Skip regular item processing
                }

                // ========== Regular item processing ==========
                materialEntries.putIfAbsent(material, new ArrayList<>());

                boolean exists = false;
                for (String[] arr : materialEntries.get(material)) {
                    if (Integer.parseInt(arr[0]) == cmd) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    materialEntries.get(material).add(new String[]{
                            String.valueOf(cmd),
                            modelFolder + "/" + modelName
                    });
                }

                success++;
                getLogger().info("§a✔ Prepared: " + id + " (CMD: " + cmd + ")");
            }

            // ========== Textures copy (outside loop) ==========
            File texturesItemFolder = new File(texturesSource, "item");
            if (texturesItemFolder.exists() && texturesItemFolder.isDirectory()) {
                File destFolder = new File(packFolder, "assets/minecraft/textures/item");
                destFolder.mkdirs();

                File[] files = texturesItemFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            String name = file.getName().toLowerCase();
                            if (name.endsWith(".png") || name.endsWith(".mcmeta") || name.endsWith(".png.mcmeta")) {
                                File dest = new File(destFolder, file.getName());
                                java.nio.file.Files.copy(file.toPath(), dest.toPath(),
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                getLogger().info("§aCopied: " + file.getName());
                            }
                        }
                    }
                }
            }

            // ========== LOOP 2: Write regular item JSON files ==========
            for (Map.Entry<String, List<String[]>> entry : materialEntries.entrySet()) {
                String mat = entry.getKey();
                List<String[]> list = entry.getValue();

                StringBuilder entriesJson = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    String[] data = list.get(i);
                    entriesJson.append("""
                      {
                        "threshold": %s,
                        "model": {
                          "type": "minecraft:model",
                          "model": "minecraft:%s"
                        }
                      }""".formatted(data[0], data[1]));

                    if (i < list.size() - 1) {
                        entriesJson.append(",\n");
                    }
                }

                String itemJson = """
            {
              "model": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:custom_model_data",
                "index": 0,
                "entries": [
            %s
                ],
                "fallback": {
                  "type": "minecraft:model",
                  "model": "minecraft:item/%s"
                }
              }
            }
            """.formatted(entriesJson.toString(), mat);

                File itemFile = new File(packFolder, "assets/minecraft/items/" + mat + ".json");
                itemFile.getParentFile().mkdirs();
                java.nio.file.Files.writeString(itemFile.toPath(), itemJson);

                getLogger().info("§aWritten: " + mat + ".json (" + list.size() + " models)");
            }

            // ========== LOOP 3: Write crossbow JSON files ==========
            for (Map.Entry<String, Map<Integer, String>> entry : crossbowEntries.entrySet()) {
                String mat = entry.getKey();
                Map<Integer, String> pullModels = entry.getValue();

                // Generate crossbow item JSON with pull states
                String crossbowJson = """
            {
              "model": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:custom_model_data",
                "index": 0,
                "entries": [
                  {
                    "threshold": 0,
                    "model": {
                      "type": "minecraft:model",
                      "model": "minecraft:%s"
                    }
                  }
                ],
                "fallback": {
                  "type": "minecraft:model",
                  "model": "minecraft:item/crossbow"
                }
              },
              "overrides": [
                {
                  "predicate": {
                    "minecraft:pulling": 1,
                    "minecraft:pull": 0.0
                  },
                  "model": "minecraft:%s"
                },
                {
                  "predicate": {
                    "minecraft:pulling": 1,
                    "minecraft:pull": 0.5
                  },
                  "model": "minecraft:%s"
                },
                {
                  "predicate": {
                    "minecraft:pulling": 1,
                    "minecraft:pull": 1.0
                  },
                  "model": "minecraft:%s"
                }
              ]
            }
            """.formatted(
                        pullModels.getOrDefault(0, "item/crossbow_standby"),
                        pullModels.getOrDefault(0, "item/crossbow_pull0"),
                        pullModels.getOrDefault(1, "item/crossbow_pull1"),
                        pullModels.getOrDefault(2, "item/crossbow_pull2")
                );

                File crossbowFile = new File(packFolder, "assets/minecraft/items/" + mat + ".json");
                crossbowFile.getParentFile().mkdirs();
                java.nio.file.Files.writeString(crossbowFile.toPath(), crossbowJson);

                getLogger().info("§aWritten crossbow: " + mat + ".json with pull states");
            }

            getLogger().info("§a§lResource Pack Generated!");
            getLogger().info("§aSuccess: " + success + " | Failed: " + failed);
            getLogger().info("§eLocation: " + packFolder.getAbsolutePath());

        } catch (Exception e) {
            getLogger().severe("Failed to generate resource pack");
            e.printStackTrace();
        }
    }


}

