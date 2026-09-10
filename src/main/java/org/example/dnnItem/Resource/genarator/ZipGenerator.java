package org.example.dnnItem.Resource.genarator;

import org.example.dnnItem.DnnItem;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipGenerator {

    public static void generate(DnnItem plugin) throws IOException {

        File sourceFolder = new File(
                plugin.getDataFolder(),
                "generated-pack"
        );

        if (!sourceFolder.exists()) {
            throw new FileNotFoundException("generated-pack folder not found.");
        }

        File zipFile = new File(
                plugin.getDataFolder(),
                "generated-pack.zip"
        );

        if (zipFile.exists()) {
            zipFile.delete();
        }

        try (ZipOutputStream zos =
                     new ZipOutputStream(new FileOutputStream(zipFile))) {

            zipFolder(sourceFolder, sourceFolder, zos);

        }

        plugin.getLogger().info("Resource pack zipped successfully.");

    }

    private static void zipFolder(
            File root,
            File current,
            ZipOutputStream zos
    ) throws IOException {

        File[] files = current.listFiles();

        if (files == null)
            return;

        for (File file : files) {

            if (file.isDirectory()) {

                zipFolder(root, file, zos);

                continue;
            }

            String entryName = root.toPath()
                    .relativize(file.toPath())
                    .toString()
                    .replace("\\", "/");

            zos.putNextEntry(new ZipEntry(entryName));

            Files.copy(file.toPath(), zos);

            zos.closeEntry();

        }

    }

}
