package org.example.dnnItem.Resource.genarator;

import org.example.dnnItem.DnnItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashGenerator {

    public static byte[] generate(DnnItem plugin)
            throws IOException, NoSuchAlgorithmException {

        File zip = new File(
                plugin.getDataFolder(),
                "generated-pack.zip"
        );

        if (!zip.exists()) {
            throw new IOException("generated-pack.zip not found.");
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        try (FileInputStream fis = new FileInputStream(zip)) {

            byte[] buffer = new byte[8192];
            int read;

            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

        }

        byte[] hash = digest.digest();

        plugin.getLogger().info(
                "SHA1: " + toHex(hash)
        );

        return hash;
    }

    private static String toHex(byte[] hash) {

        StringBuilder sb = new StringBuilder();

        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();

    }
}
