package org.example.dnnItem.AutoResoucce;

import org.bukkit.configuration.ConfigurationSection;
import org.example.dnnItem.DnnItem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;


import static org.bukkit.Bukkit.getLogger;

public class betterResouceGenerator {



    public void generateResourcePack() {

        File packSource = new File(DnnItem.getInstance().getDataFolder(), "pack");
        File packFolder = new File(DnnItem.getInstance().getDataFolder(), "generated_pack_bedrock");

        File modelsSource = new File(packSource, "models");
        File texturesSource = new File(packSource, "textures");

        packSource.mkdirs();
        modelsSource.mkdirs();
        texturesSource.mkdirs();
        packFolder.mkdirs();

        try {
            // ========== 1. MANIFEST.JSON (Bedrock Required) ==========
            File manifestFile = new File(packFolder, "manifest.json");
            String uuid1 = UUID.randomUUID().toString();
            String uuid2 = UUID.randomUUID().toString();

            java.nio.file.Files.writeString(manifestFile.toPath(), """
            {
              "format_version": 2,
              "header": {
                "description": "dnnItemX Bedrock Resource Pack",
                "name": "dnnItemX RP",
                "uuid": "%s",
                "version": [1, 0, 0],
                "min_engine_version": [1, 20, 0]
              },
              "modules": [
                {
                  "type": "resources",
                  "uuid": "%s",
                  "version": [1, 0, 0]
                }
              ]
            }
            """.formatted(uuid1, uuid2));

            getLogger().info("§amanifest.json created!");

            // ========== 2. PACK_ICON (Optional) ==========
            File iconSource = new File(packSource, "pack_icon.png");
            if (iconSource.exists()) {
                File iconDest = new File(packFolder, "pack_icon.png");
                java.nio.file.Files.copy(iconSource.toPath(), iconDest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("§apack_icon.png copied!");
            }

            ConfigurationSection items = DnnItem.getInstance().getItemsConfig().getConfigurationSection("items");
            if (items == null) {
                getLogger().warning("No items section found in items.yml");
                return;
            }

            // ========== 3. ATTACHMENT.JSON (for item textures) ==========
            // Bedrock uses attachable for armor/items
            File attachableFolder = new File(packFolder, "attachables");
            attachableFolder.mkdirs();

            // ========== 4. TEXTURES - item_texture.json ==========
            Map<String, String> textureMap = new HashMap<>();

            // ========== 5. MODELS - geometry & item models ==========
            File modelsFolder = new File(packFolder, "models");
            File modelsItemFolder = new File(modelsFolder, "item");
            modelsItemFolder.mkdirs();

            File modelsEntityFolder = new File(modelsFolder, "entity");
            modelsEntityFolder.mkdirs();

            // ========== 6. TEXTURES FOLDER ==========
            File texturesFolder = new File(packFolder, "textures");
            File texturesItemFolder = new File(texturesFolder, "items");
            texturesItemFolder.mkdirs();

            File texturesArmorFolder = new File(texturesFolder, "armor");
            texturesArmorFolder.mkdirs();

            int success = 0;
            int failed = 0;

            // ========== LOOP: Process each item ==========
            for (String id : items.getKeys(false)) {
                ConfigurationSection sec = items.getConfigurationSection(id);
                if (sec == null) continue;

                String material = sec.getString("material", "").toLowerCase();
                int cmd = sec.getInt("custom-model-data", 0);
                String modelPath = sec.getString("model");
                String texturePath = sec.getString("texture");
                boolean isArmor = material.contains("helmet") || material.contains("chestplate")
                        || material.contains("leggings") || material.contains("boots");

                if (cmd <= 0) {
                    getLogger().warning("Skipping " + id + " → custom-model-data missing");
                    failed++;
                    continue;
                }

                // Clean paths
                if (modelPath != null) modelPath = modelPath.replace("\\", "/").trim();
                if (texturePath != null) texturePath = texturePath.replace("\\", "/").trim();

                if (modelPath == null || modelPath.isEmpty()) {
                    getLogger().warning("Skipping " + id + " → model path missing");
                    failed++;
                    continue;
                }

                String modelName = modelPath.contains("/")
                        ? modelPath.substring(modelPath.lastIndexOf("/") + 1)
                        : modelPath;

                String textureName = texturePath != null && texturePath.contains("/")
                        ? texturePath.substring(texturePath.lastIndexOf("/") + 1)
                        : (modelName + ".png");

                // ========== COPY TEXTURES ==========
                if (texturePath != null && !texturePath.isEmpty()) {
                    File sourceTexture = new File(texturesSource, texturePath + ".png");

                    // Try alternative paths
                    if (!sourceTexture.exists()) {
                        sourceTexture = new File(texturesSource, texturePath);
                    }

                    if (sourceTexture.exists()) {
                        File destTexture;
                        if (isArmor) {
                            destTexture = new File(texturesArmorFolder, textureName);
                        } else {
                            destTexture = new File(texturesItemFolder, textureName);
                        }
                        destTexture.getParentFile().mkdirs();
                        java.nio.file.Files.copy(sourceTexture.toPath(), destTexture.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        getLogger().info("§aCopied texture: " + textureName);
                    } else {
                        getLogger().warning("Texture not found: " + sourceTexture.getAbsolutePath());
                    }
                }

                // ========== CONVERT JAVA MODEL TO BEDROCK ==========
                File sourceModel = new File(modelsSource, modelPath + ".json");
                if (!sourceModel.exists()) {
                    sourceModel = new File(modelsSource, modelPath);
                }

                if (sourceModel.exists()) {
                    // Read Java model
                    String javaModel = new String(java.nio.file.Files.readAllBytes(sourceModel.toPath()));

                    // Convert to Bedrock format
                    String bedrockModel = convertJavaToBedrock(javaModel, textureName, isArmor);

                    // Save Bedrock model
                    File destModel;
                    if (isArmor) {
                        // Armor uses entity models
                        destModel = new File(modelsEntityFolder, modelName + ".geo.json");
                    } else {
                        destModel = new File(modelsItemFolder, modelName + ".geo.json");
                    }

                    java.nio.file.Files.writeString(destModel.toPath(), bedrockModel);
                    getLogger().info("§aConverted model: " + modelName);

                    success++;
                } else {
                    getLogger().warning("Model not found: " + sourceModel.getAbsolutePath());
                    failed++;
                    continue;
                }

                // ========== Add to texture map ==========
                String textureKey = isArmor ? "armor." + id : "item." + id;
                textureMap.put(textureKey, textureName.replace(".png", ""));
            }

            // ========== GENERATE item_texture.json ==========
            generateItemTextureJson(textureMap, packFolder);

            // ========== GENERATE attachable.json for armor ==========
            generateAttachableJson(items, packFolder);

            // ========== GENERATE render_controllers (optional) ==========
            generateRenderControllers(packFolder);

            // ========== GENERATE sounds.json (optional) ==========
            generateSoundsJson(packFolder);

            getLogger().info("§a§l✅ Bedrock Resource Pack Generated!");
            getLogger().info("§aSuccess: " + success + " | Failed: " + failed);
            getLogger().info("§eLocation: " + packFolder.getAbsolutePath());
            getLogger().info("§6📦 Zip this folder and use in Minecraft Bedrock!");

        } catch (Exception e) {
            getLogger().severe("❌ Failed to generate resource pack");
            e.printStackTrace();
        }
    }

    // ========== CONVERT JAVA MODEL TO BEDROCK ==========
    private String convertJavaToBedrock(String javaModel, String textureName, boolean isArmor) {
        try {
            // Simple conversion - you can make it more sophisticated
            if (isArmor) {
                return """
                {
                  "format_version": "1.12.0",
                  "minecraft:geometry": [
                    {
                      "description": {
                        "identifier": "geometry.%s",
                        "texture_width": 64,
                        "texture_height": 64,
                        "visible_bounds_width": 2,
                        "visible_bounds_height": 2,
                        "visible_bounds_offset": [0, 1, 0]
                      },
                      "bones": [
                        {
                          "name": "head",
                          "pivot": [0, 0, 0],
                          "cubes": [
                            {
                              "origin": [-4, -8, -4],
                              "size": [8, 8, 8],
                              "uv": [0, 0]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.formatted(textureName.replace(".png", ""));
            } else {
                // Item model (simple cube)
                return """
                {
                  "format_version": "1.12.0",
                  "minecraft:geometry": [
                    {
                      "description": {
                        "identifier": "geometry.%s",
                        "texture_width": 16,
                        "texture_height": 16,
                        "visible_bounds_width": 1,
                        "visible_bounds_height": 1,
                        "visible_bounds_offset": [0, 0, 0]
                      },
                      "bones": [
                        {
                          "name": "item",
                          "pivot": [0, 0, 0],
                          "cubes": [
                            {
                              "origin": [-4, -8, -4],
                              "size": [8, 8, 8],
                              "uv": [0, 0]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.formatted(textureName.replace(".png", ""));
            }
        } catch (Exception e) {
            getLogger().warning("Failed to convert model: " + e.getMessage());
            return "{}";
        }
    }

    // ========== GENERATE item_texture.json ==========
    private void generateItemTextureJson(Map<String, String> textureMap, File packFolder) throws Exception {
        StringBuilder textures = new StringBuilder();
        textures.append("{\n  \"resource_pack_name\": \"dnnItemX\",\n");
        textures.append("  \"texture_name\": \"atlas.items\",\n");
        textures.append("  \"texture_data\": {\n");

        int i = 0;
        for (Map.Entry<String, String> entry : textureMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            textures.append("    \"").append(key).append("\": {\n");
            textures.append("      \"textures\": \"textures/items/").append(value).append("\"\n");
            textures.append("    }");

            if (i < textureMap.size() - 1) {
                textures.append(",");
            }
            textures.append("\n");
            i++;
        }

        textures.append("  }\n}");

        File textureJson = new File(packFolder, "textures/item_texture.json");
        textureJson.getParentFile().mkdirs();
        java.nio.file.Files.writeString(textureJson.toPath(), textures.toString());
        getLogger().info("§aitem_texture.json generated!");
    }

    // ========== GENERATE attachable.json for armor ==========
    private void generateAttachableJson(ConfigurationSection items, File packFolder) throws Exception {
        // Check if any armor exists
        boolean hasArmor = false;
        for (String id : items.getKeys(false)) {
            ConfigurationSection sec = items.getConfigurationSection(id);
            if (sec == null) continue;
            String material = sec.getString("material", "").toLowerCase();
            if (material.contains("helmet") || material.contains("chestplate") ||
                    material.contains("leggings") || material.contains("boots")) {
                hasArmor = true;
                break;
            }
        }

        if (!hasArmor) return;

        String attachableJson = """
        {
          "format_version": "1.10.0",
          "minecraft:attachable": {
            "description": {
              "identifier": "dnnitem:custom_armor",
              "materials": {
                "default": "armor",
                "enchanted": "armor_enchanted"
              },
              "textures": {
                "default": "textures/armor/custom_armor"
              },
              "geometry": {
                "default": "geometry.custom_armor"
              },
              "scripts": {
                "parent_setup": "variable.helmet_layer_visible = 0.0;"
              },
              "render_controllers": ["controller.render.armor"]
            }
          }
        }
        """;

        File attachableFile = new File(packFolder, "attachables/custom_armor.attachable.json");
        attachableFile.getParentFile().mkdirs();
        java.nio.file.Files.writeString(attachableFile.toPath(), attachableJson);
        getLogger().info("§aattachable.json generated!");
    }

    // ========== GENERATE render_controllers ==========
    private void generateRenderControllers(File packFolder) throws Exception {
        File rcFolder = new File(packFolder, "render_controllers");
        rcFolder.mkdirs();

        String rcJson = """
        {
          "format_version": "1.10.0",
          "render_controllers": {
            "controller.render.custom_item": {
              "geometry": "geometry.default",
              "materials": [
                {
                  "*": "Material.default"
                }
              ],
              "textures": [
                "texture.default"
              ]
            }
          }
        }
        """;

        File rcFile = new File(rcFolder, "custom_item.render_controllers.json");
        java.nio.file.Files.writeString(rcFile.toPath(), rcJson);
        getLogger().info("§arender_controllers generated!");
    }

    // ========== GENERATE sounds.json ==========
    private void generateSoundsJson(File packFolder) throws Exception {
        String soundsJson = """
        {
          "entity_sounds": {
            "entities": {}
          }
        }
        """;

        File soundsFile = new File(packFolder, "sounds.json");
        java.nio.file.Files.writeString(soundsFile.toPath(), soundsJson);
        getLogger().info("§asounds.json generated!");
    }
}
