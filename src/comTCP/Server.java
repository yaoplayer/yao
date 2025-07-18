package comTCP;

/**
 * ��Ŀ����: TCPFileTransfer
 * ����: comTCP
 * ����: Server
 *
 * ��������:
 * TCP �ļ�����������ˣ�֧�ֿͻ����ϴ��ļ��������ļ����г��ļ��б��ܡ�
 * ���̴߳������ͻ�������ʹ���̳߳��������ܡ�
 *
 * @Author Ҧ��ة
 * @Create 2025/5/25 ����07:16
 * @Version 1.0
 */

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    // �����������˿�
    private static final int PORT = 8888;

    // �ļ��洢Ŀ¼��������������ϴ��ļ���·����
    private static final String STORAGE_DIR = "server_storage";

    public static void main(String[] args) {
        // �����洢Ŀ¼����������ڣ�
        File storage = new File(STORAGE_DIR);
        if (!storage.exists()) {
            storage.mkdirs();
        }

        // ���������� socket �������˿�
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("�ļ�������������������˿ڣ�" + PORT);

            // �����̳߳���֧�ֶ���ͻ��˲�������
            ExecutorService threadPool = Executors.newCachedThreadPool();

            while (true) {
                // �ȴ��ͻ�������
                Socket clientSocket = serverSocket.accept();
                System.out.println("�ͻ������ӣ�" + clientSocket.getInetAddress());

                // Ϊÿ���ͻ������ӷ���һ���̴߳���
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("�������쳣��" + e.getMessage());
        }
    }

    /**
     * �ڲ��ࣺ���ڴ���ÿһ���ͻ�������
     */
    static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // ���������������������ͨ��
            try (
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream())
            ) {
                // ѭ����ȡ�ͻ��˷��͵�ָ��
                while (true) {
                    String command;
                    try {
                        command = dis.readUTF(); // ���տͻ�������
                    } catch (EOFException eof) {
                        break;  // �ͻ��˹ر�����
                    }

                    // �����������ͽ��д���
                    switch (command) {
                        case "UPLOAD":
                            handleUpload(dis, dos);     // �����ϴ��ļ�
                            break;
                        case "DOWNLOAD":
                            handleDownload(dis, dos);   // �����ļ�����
                            break;
                        case "LIST":
                            handleList(dos);            // �����ļ��б�����
                            break;
                        default:
                            System.out.println("δ֪���" + command);
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("����ͻ���ʱ����" + e.getMessage());
            } finally {
                // �ر� socket
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        /**
         * �����ϴ��ļ�����
         * �ͻ����ȷ����ļ������ļ���С��Ȼ�����ļ�����
         */
        private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();     // ��ȡ�ļ���
            long fileSize = dis.readLong();      // ��ȡ�ļ���С
            File file = new File(STORAGE_DIR, fileName);  // ���챣��·��

            // ��ʼ���ղ�д���ļ�
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];  // ������
                int read;
                long totalRead = 0;
                while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                }
            }

            // ֪ͨ�ͻ����ϴ��ɹ�
            dos.writeUTF("�ϴ��ɹ���");
            dos.flush();
            System.out.println("�ļ����ϴ���" + fileName);
        }

        /**
         * ���������ļ�����
         * �Ƚ���Ҫ���ص��ļ�����������������ļ���С�����ݣ����򷵻� NOT_FOUND
         */
        private void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF(); // ��ȡ�ͻ���������ļ���
            File file = new File(STORAGE_DIR, fileName);

            if (!file.exists()) {
                dos.writeUTF("NOT_FOUND");   // �ļ�������
                return;
            }

            // �ļ����ڣ�����ȷ�Ϻ��ļ���С
            dos.writeUTF("FOUND");
            dos.writeLong(file.length());

            // ��ȡ�ļ����ݲ����͸��ͻ���
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, read);
                }
            }

            dos.flush();
            System.out.println("�ļ��ѷ��ͣ�" + fileName);
        }

        /**
         * �����ļ��б�����
         * ��ͻ��˷��ͷ������洢Ŀ¼�µ������ļ���
         */
        private void handleList(DataOutputStream dos) throws IOException {
            File dir = new File(STORAGE_DIR);
            File[] files = dir.listFiles(); // ��ȡ�ļ�����

            if (files == null || files.length == 0) {
                dos.writeInt(0); // û���ļ�
                return;
            }

            dos.writeInt(files.length); // �����ļ�����
            for (File file : files) {
                dos.writeUTF(file.getName()); // ����ÿ���ļ���
            }

            dos.flush();
        }
    }
}
