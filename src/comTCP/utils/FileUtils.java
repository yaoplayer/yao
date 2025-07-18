package comTCP.utils;

/**
 * ProjectName: TCPFileTransfer
 * Package: com.utils
 * ClassName: FileUtils
 *
 * @Author 姚昭丞
 * @Create 2025/5/25 下午05:17
 * @Version 1.0
 * Description:
 */

import java.io.*;

public class FileUtils {

    // 将文件读取为字节数组
    public static byte[] readFileToBytes(File file) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    // 将字节数组写入到文件
    public static void writeBytesToFile(byte[] data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    // 创建目录（如果不存在）
    public static void ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // 检查文件是否存在
    public static boolean fileExists(String path) {
        return new File(path).exists();
    }

    // 获取文件大小（字节）
    public static long getFileSize(String path) {
        File file = new File(path);
        return file.exists() ? file.length() : -1;
    }
}

