package comTCP;

/**
 * ��Ŀ����: TCPFileTransfer
 * ����: comTCP
 * ����: Client
 *
 * ��������:
 * �ͻ��˳���ͨ�� TCP Э�����ӵ���������ʵ���ļ����ϴ������ع��ܡ�
 * �ṩ�˵��������û�����ѡ���ϴ������ļ�������������ӷ����������ļ���
 * ʹ�� DataInputStream �� DataOutputStream ʵ���ļ��������˫��ͨ�š�
 *
 * @Author Ҧ��ة
 * @Create 2025/5/28 ����10:16
 * @Version 1.0
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    // �����������������أ�
    private static final String SERVER_HOST = "localhost";
    // �����������˿ں�
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        // ʹ�� try-with-resources �Զ��ر���Դ
        try (
                // �����ͻ��� Socket ���ӷ�����
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                // ���������������ڽ������Է�����������
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                // ������������������������������
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                // ����̨���빤��
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("�ͻ��������ӷ�������");

            // ����ѭ��ֱ���û�ѡ���˳�
            while (true) {
                // ��ʾ�����˵�
                System.out.println("\n��ѡ�������");
                System.out.println("1 - �ϴ��ļ�");
                System.out.println("2 - �����ļ�");
                System.out.println("0 - �˳�");
                System.out.print("����ѡ�");
                String choice = scanner.nextLine().trim();

                // �����˳�����
                if (choice.equals("0")) {
                    System.out.println("���˳��ͻ��ˡ�");
                    break;
                }

                // �����û�ѡ��ִ����Ӧ����
                switch (choice) {
                    case "1":
                        uploadFile(scanner, dos, dis); // �ϴ��ļ�
                        break;
                    case "2":
                        downloadFile(scanner, dos, dis); // �����ļ�
                        break;
                    default:
                        System.out.println("��Чѡ����������룡");
                }
            }
        } catch (IOException e) {
            System.err.println("�ͻ����쳣��" + e.getMessage());
        }
    }

    /**
     * �ϴ��ļ���������
     */
    private static void uploadFile(Scanner scanner, DataOutputStream dos, DataInputStream dis) throws IOException {
        System.out.print("������Ҫ�ϴ����ļ�·����");
        String path = scanner.nextLine().trim();
        File file = new File(path);

        // ��֤�ļ��Ƿ������Ϊ��ͨ�ļ�
        if (!file.exists() || !file.isFile()) {
            System.out.println("�ļ������ڻ���Ч��");
            return;
        }

        // ������������ϴ�����
        dos.writeUTF("UPLOAD");
        dos.writeUTF(file.getName());       // �����ļ���
        dos.writeLong(file.length());       // �����ļ���С

        // ��ȡ�����ļ�����������
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(file.length(), 64 * 1024)]; // 64KB ������
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read); // ���������ļ�����
            }
        }

        // �ȴ���������������ص��ϴ����
        String response = dis.readUTF();
        System.out.println("�ϴ������" + response);
    }

    /**
     * �ӷ����������ļ�
     */
    private static void downloadFile(Scanner scanner, DataOutputStream dos, DataInputStream dis) throws IOException {
        // ������������ļ��б�����
        dos.writeUTF("LIST");
        int fileCount = dis.readInt(); // �����ļ�����

        if (fileCount == 0) {
            System.out.println("�����������޿������ļ���");
            return;
        }

        // ��ȡ���������ص��ļ����б�
        List<String> fileList = new ArrayList<>();
        System.out.println("�������������ļ��б�");
        for (int i = 0; i < fileCount; i++) {
            String fileName = dis.readUTF();
            fileList.add(fileName);
            System.out.println((i + 1) + ". " + fileName); // ��ʾ���
        }

        // �û�ѡ�����ص��ļ�
        System.out.print("������Ҫ���ص��ļ���ţ�");
        int index;
        try {
            index = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("������Ч��");
            return;
        }

        // ��������Ƿ�Խ��
        if (index < 1 || index > fileList.size()) {
            System.out.println("��ų�����Χ��");
            return;
        }

        String selectedFile = fileList.get(index - 1); // ��ȡѡ����ļ���

        // ����������������ļ�
        dos.writeUTF("DOWNLOAD");
        dos.writeUTF(selectedFile);

        String response = dis.readUTF();
        if (!response.equals("FOUND")) {
            System.out.println("�ļ�δ�ҵ���");
            return;
        }

        // ��ȡ���������ص��ļ���С
        long fileSize = dis.readLong();

        // �������ر���Ŀ¼����������ڣ�
        File saveDir = new File("client_downloads");
        if (!saveDir.exists()) saveDir.mkdirs();
        File saveFile = new File(saveDir, selectedFile);

        // ��ʼ�����ļ���д�뱾�ش���
        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            byte[] buffer = new byte[4096];
            int read;
            long totalRead = 0;
            while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                totalRead += read;
            }
        }

        System.out.println("������ɣ�" + saveFile.getAbsolutePath());
    }
}
