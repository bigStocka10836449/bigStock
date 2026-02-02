package com.bigstock.biz.utils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractUtils {

    private ZipExtractUtils() {}

    public static byte[] extractFirstFileBytes(byte[] zipBytes, String endsWith) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName() != null
                        && entry.getName().toLowerCase().endsWith(endsWith.toLowerCase())) {
                    return readAllBytes(zis);
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("extract zip failed: " + e.getMessage(), e);
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
