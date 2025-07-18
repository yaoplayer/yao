package comTCP;

/**
 * 项目名称: TCPFileTransfer
 * 包名: comTCP
 * 类名: Server
 *
 * 功能描述:
 * TCP 文件传输服务器端，支持客户端上传文件、下载文件、列出文件列表功能。
 * 多线程处理多个客户端请求，使用线程池提升性能。
 *
 * @Author 姚昭丞
 * @Create 2025/5/25 下午07:16
 * @Version 1.0
 */

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    // 服务器监听端口
    private static final int PORT = 8888;

    // 文件存储目录名（服务器存放上传文件的路径）
    private static final String STORAGE_DIR = "server_storage";

    public static void main(String[] args) {
        // 创建存储目录（如果不存在）
        File storage = new File(STORAGE_DIR);
        if (!storage.exists()) {
            storage.mkdirs();
        }

        // 启动服务器 socket 并监听端口
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("文件传输服务器已启动，端口：" + PORT);

            // 创建线程池以支持多个客户端并发连接
            ExecutorService threadPool = Executors.newCachedThreadPool();

            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接：" + clientSocket.getInetAddress());

                // 为每个客户端连接分配一个线程处理
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("服务器异常：" + e.getMessage());
        }
    }

    /**
     * 内部类：用于处理每一个客户端连接
     */
    static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // 建立数据输入输出流用于通信
            try (
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream())
            ) {
                // 循环读取客户端发送的指令
                while (true) {
                    String command;
                    try {
                        command = dis.readUTF(); // 接收客户端命令
                    } catch (EOFException eof) {
                        break;  // 客户端关闭连接
                    }

                    // 根据命令类型进行处理
                    switch (command) {
                        case "UPLOAD":
                            handleUpload(dis, dos);     // 处理上传文件
                            break;
                        case "DOWNLOAD":
                            handleDownload(dis, dos);   // 处理文件下载
                            break;
                        case "LIST":
                            handleList(dos);            // 处理文件列表请求
                            break;
                        default:
                            System.out.println("未知命令：" + command);
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("处理客户端时出错：" + e.getMessage());
            } finally {
                // 关闭 socket
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        /**
         * 处理上传文件请求
         * 客户端先发送文件名和文件大小，然后发送文件内容
         */
        private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();     // 读取文件名
            long fileSize = dis.readLong();      // 读取文件大小
            File file = new File(STORAGE_DIR, fileName);  // 构造保存路径

            // 开始接收并写入文件
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];  // 缓冲区
                int read;
                long totalRead = 0;
                while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                }
            }

            // 通知客户端上传成功
            dos.writeUTF("上传成功！");
            dos.flush();
            System.out.println("文件已上传：" + fileName);
        }

        /**
         * 处理下载文件请求
         * 先接收要下载的文件名，如果存在则发送文件大小和内容，否则返回 NOT_FOUND
         */
        private void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF(); // 获取客户端请求的文件名
            File file = new File(STORAGE_DIR, fileName);

            if (!file.exists()) {
                dos.writeUTF("NOT_FOUND");   // 文件不存在
                return;
            }

            // 文件存在，发送确认和文件大小
            dos.writeUTF("FOUND");
            dos.writeLong(file.length());

            // 读取文件内容并发送给客户端
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, read);
                }
            }

            dos.flush();
            System.out.println("文件已发送：" + fileName);
        }

        /**
         * 处理文件列表请求
         * 向客户端发送服务器存储目录下的所有文件名
         */
        private void handleList(DataOutputStream dos) throws IOException {
            File dir = new File(STORAGE_DIR);
            File[] files = dir.listFiles(); // 获取文件数组

            if (files == null || files.length == 0) {
                dos.writeInt(0); // 没有文件
                return;
            }

            dos.writeInt(files.length); // 发送文件个数
            for (File file : files) {
                dos.writeUTF(file.getName()); // 发送每个文件名
            }

            dos.flush();
        }
    }
}
