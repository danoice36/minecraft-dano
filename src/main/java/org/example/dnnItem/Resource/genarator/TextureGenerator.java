package org.example.dnnItem.Resource.genarator;

import org.example.dnnItem.DnnItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
public class TextureGenerator {

    public static void generate(DnnItem plugin) throws IOException {

        File source = new File(plugin.getDataFolder(), "textures");

        if (!source.exists()) {
            plugin.getLogger().warning("textures folder not found!");
            return;
        }

        File destination = new File(
                plugin.getDataFolder(),
                "generated-pack/assets/dnnitem/textures"
        );

        copyFolder(source.toPath(), destination.toPath());

        plugin.getLogger().info("Textures copied.");

    }

    static void copyFolder(Path source, Path target)
            throws IOException {

        Files.walk(source).forEach(path -> {

            try {

                Path relative = source.relativize(path);

                Path output = target.resolve(relative);

                if (Files.isDirectory(path)) {

                    Files.createDirectories(output);

                } else {

                    Files.copy(
                            path,
                            output,
                            StandardCopyOption.REPLACE_EXISTING
                    );

                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

    }

}
