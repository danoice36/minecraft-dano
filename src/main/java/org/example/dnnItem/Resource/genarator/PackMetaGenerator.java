package org.example.dnnItem.Resource.genarator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.example.dnnItem.DnnItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;


public class PackMetaGenerator {


    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void generate(DnnItem plugin) throws IOException {


        File file = new File(plugin.getDataFolder(), "generated-pack/pack.mcmeta");

        file.getParentFile().mkdirs();

        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 84); // Minecraft 1.21.8/1.21.11

        pack.addProperty("description", "DnnItem Resource Pack");

        JsonObject root = new JsonObject();
        root.add("pack", pack);

        try (FileWriter writer = new FileWriter(file)) {
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(root, writer);
        }

    }

}
