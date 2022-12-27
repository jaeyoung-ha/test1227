package com.aws.demo.utils;

import java.util.UUID;

public class MultipartUtil {
    private static final String BASE_DIR = "images";

    public static String getFileExtension(String fileName) {
        try {
            return fileName.substring(fileName.lastIndexOf("."));

        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(String.format("Invalid file %s", e.getMessage()));
        }
    }

    public static String createFileName(String originFileName) {
        return UUID.randomUUID().toString().concat(getFileExtension(originFileName));
    }


}
