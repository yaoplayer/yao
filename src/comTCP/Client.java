package comTCP;

/**
 * 项目名称: TCPFileTransfer
 * 包名: comTCP
 * 类名: Client
 *
 * 功能描述:
 * 客户端程序，通过 TCP 协议连接到服务器，实现文件的上传和下载功能。
 * 提供菜单操作，用户可以选择上传本地文件到服务器，或从服务器下载文件。
 * 使用 DataInputStream 和 DataOutputStream 实现文件和命令的双向通信。
 *
 * @Author 姚昭丞
 * @Create 2025/5/28 下午10:16
 * @Version 1.0
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    // 服务器主机名（本地）
    private static final String SERVER_HOST = "localhost";
    // 服务器监听端口号
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        // 使用 try-with-resources 自动关闭资源
        try (
                // 创建客户端 Socket 连接服务器
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                // 创建输入流：用于接收来自服务器的数据
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                // 创建输出流：用于向服务器发送数据
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                // 控制台输入工具
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("客户端已连接服务器。");

            // 无限循环直到用户选择退出
            while (true) {
                // 显示操作菜单
                System.out.println("\n请选择操作：");
                System.out.println("1 - 上传文件");
                System.out.println("2 - 下载文件");
                System.out.println("0 - 退出");
                System.out.print("输入选项：");
                String choice = scanner.nextLine().trim();

                // 处理退出命令
                if (choice.equals("0")) {
                    System.out.println("已退出客户端。");
                    break;
                }

                // 根据用户选择执行相应操作
                switch (choice) {
                    case "1":
                        uploadFile(scanner, dos, dis); // 上传文件
                        break;
                    case "2":
                        downloadFile(scanner, dos, dis); // 下载文件
                        break;
                    default:
                        System.out.println("无效选项，请重新输入！");
                }
            }
        } catch (IOException e) {
            System.err.println("客户端异常：" + e.getMessage());
        }
    }

    /**
     * 上传文件到服务器
     */
    private static void uploadFile(Scanner scanner, DataOutputStream dos, DataInputStream dis) throws IOException {
        System.out.print("请输入要上传的文件路径：");
        String path = scanner.nextLine().trim();
        File file = new File(path);

        // 验证文件是否存在且为普通文件
        if (!file.exists() || !file.isFile()) {
            System.out.println("文件不存在或无效！");
            return;
        }

        // 向服务器发送上传请求
        dos.writeUTF("UPLOAD");
        dos.writeUTF(file.getName());       // 发送文件名
        dos.writeLong(file.length());       // 发送文件大小

        // 读取本地文件并发送内容
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(file.length(), 64 * 1024)]; // 64KB 缓冲区
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read); // 分批发送文件内容
            }
        }

        // 等待并输出服务器返回的上传结果
        String response = dis.readUTF();
        System.out.println("上传结果：" + response);
    }

    /**
     * 从服务器下载文件
     */
    private static void downloadFile(Scanner scanner, DataOutputStream dos, DataInputStream dis) throws IOException {
        // 向服务器发送文件列表请求
        dos.writeUTF("LIST");
        int fileCount = dis.readInt(); // 接收文件数量

        if (fileCount == 0) {
            System.out.println("服务器上暂无可下载文件。");
            return;
        }

        // 读取服务器返回的文件名列表
        List<String> fileList = new ArrayList<>();
        System.out.println("服务器可下载文件列表：");
        for (int i = 0; i < fileCount; i++) {
            String fileName = dis.readUTF();
            fileList.add(fileName);
            System.out.println((i + 1) + ". " + fileName); // 显示序号
        }

        // 用户选择下载的文件
        System.out.print("请输入要下载的文件序号：");
        int index;
        try {
            index = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("输入无效！");
            return;
        }

        // 检查输入是否越界
        if (index < 1 || index > fileList.size()) {
            System.out.println("序号超出范围！");
            return;
        }

        String selectedFile = fileList.get(index - 1); // 获取选择的文件名

        // 向服务器请求下载文件
        dos.writeUTF("DOWNLOAD");
        dos.writeUTF(selectedFile);

        String response = dis.readUTF();
        if (!response.equals("FOUND")) {
            System.out.println("文件未找到！");
            return;
        }

        // 读取服务器发回的文件大小
        long fileSize = dis.readLong();

        // 创建本地保存目录（如果不存在）
        File saveDir = new File("client_downloads");
        if (!saveDir.exists()) saveDir.mkdirs();
        File saveFile = new File(saveDir, selectedFile);

        // 开始接收文件并写入本地磁盘
        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            byte[] buffer = new byte[4096];
            int read;
            long totalRead = 0;
            while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                totalRead += read;
            }
        }

        System.out.println("下载完成：" + saveFile.getAbsolutePath());
    }
}
