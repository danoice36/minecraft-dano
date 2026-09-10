package org.example.dnnItem.Resource.genarator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.dnnItem.DnnItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EquipmentGenerator {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void generate(DnnItem plugin, List<ArmorGenerator.ArmorData> armors)
            throws IOException {

        File folder = new File(
                plugin.getDataFolder(),
                "generated-pack/assets/dnnitem/equipment"
        );

        folder.mkdirs();

        for (ArmorGenerator.ArmorData armor : armors) {

            Map<String, Object> root = new LinkedHashMap<>();

            Map<String, Object> layers = new LinkedHashMap<>();

            layers.put(
                    "humanoid",
                    "dnnitem:" + armor.getLayer1()
            );

            layers.put(
                    "humanoid_leggings",
                    "dnnitem:" + armor.getLayer2()
            );

            root.put("layers", layers);

            File out = new File(folder, armor.getId() + ".json");

            try (FileWriter writer = new FileWriter(out)) {
                GSON.toJson(root, writer);
            }

        }

    }

}
