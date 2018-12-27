package com.eastwood.tools.plugins.repo

import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileUtil {

    static void compress(File dir, File zipFile) {
        FileOutputStream fileOutputStream = new FileOutputStream(zipFile)
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)
        WritableByteChannel writableByteChannel = Channels.newChannel(zipOutputStream)
        compressByType(dir, zipOutputStream, writableByteChannel, "")
    }

    private static void compressByType(File src, ZipOutputStream zipOutputStream, WritableByteChannel writableByteChannel, String baseDir) {
        if (!src.exists())
            return
        if (src.isFile()) {
            if (src.name.endsWith('.iml')) return

            compressFile(src, zipOutputStream, writableByteChannel, baseDir)
        } else if (src.isDirectory()) {
            if (src.name == 'build') return

            compressDir(src, zipOutputStream, writableByteChannel, baseDir)
        }
    }

    private static void compressDir(File dir, ZipOutputStream zipOutputStream, WritableByteChannel writableByteChannel, String baseDir) {
        if (!dir.exists())
            return

        File[] files = dir.listFiles();
        if (files.length == 0) {
            try {
                zipOutputStream.putNextEntry(new ZipEntry(baseDir + dir.getName() + File.separator))
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (File file : files) {
            compressByType(file, zipOutputStream, writableByteChannel, baseDir + dir.getName() + File.separator);
        }

    }

    private static void compressFile(File file, ZipOutputStream zipOutputStream, WritableByteChannel writableByteChannel, String baseDir) {
        if (!file.exists())
            return

        try {
            ZipEntry zipEntry = new ZipEntry(baseDir + file.getName())
            zipOutputStream.putNextEntry(zipEntry);
            FileInputStream fileInputStream = new FileInputStream(file)
            FileChannel inChannel = fileInputStream.getChannel()
            inChannel.transferTo(0, inChannel.size(), writableByteChannel)
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

}