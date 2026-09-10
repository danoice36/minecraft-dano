package org.example.dnnItem.Resource;

import org.example.dnnItem.DnnItem;
import org.example.dnnItem.Resource.genarator.*;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class ResourcePackGenerator {


    private final DnnItem plugin;

    private final File packFolder;
    public ResourcePackGenerator(DnnItem plugin) {
        this.plugin = plugin;
        this.packFolder = new File(plugin.getDataFolder(), "generated-pack");
    }



    public void generate() throws IOException, NoSuchAlgorithmException {

        deleteOldPack();

        createFolders();

        PackMetaGenerator.generate(plugin);

        TextureGenerator.generate(plugin);

        ModelGenerator.generate(plugin);

        List<ArmorGenerator.ArmorData> armors =
                ArmorGenerator.generate(plugin);

        EquipmentGenerator.generate(plugin, armors);

        ZipGenerator.generate(plugin);

        HashGenerator.generate(plugin);



    }



    private void deleteOldPack() {

        delete(packFolder);

        File zip = new File(plugin.getDataFolder(), "generated-pack.zip");

        if (zip.exists()) {
            zip.delete();
        }

        File sha1 = new File(plugin.getDataFolder(), "generated-pack.sha1");

        if (sha1.exists()) {
            sha1.delete();
        }

    }

    private void delete(File file) {

        if (file == null || !file.exists())
            return;

        if (file.isDirectory()) {

            File[] files = file.listFiles();

            if (files != null) {

                for (File child : files) {
                    delete(child);
                }

            }

        }

        file.delete();

    }

    private void createFolders() {

        new File(plugin.getDataFolder(),
                "generated-pack").mkdirs();

        new File(plugin.getDataFolder(),
                "generated-pack/assets").mkdirs();

        new File(plugin.getDataFolder(),
                "generated-pack/assets/minecraft/models/item").mkdirs();

        new File(plugin.getDataFolder(),
                "generated-pack/assets/dnnitem/models/item").mkdirs();

        new File(plugin.getDataFolder(),
                "generated-pack/assets/dnnitem/textures").mkdirs();

        new File(plugin.getDataFolder(),
                "generated-pack/assets/dnnitem/equipment").mkdirs();

    }
}
