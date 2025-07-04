package client;

import java.io.*;
import java.net.*;
import java.util.*;

public class NapsterClient {
    private static final int SERVER_PORT = 1234;
    private static int CLIENT_PORT;
    private static final String PUBLIC_FOLDER = "public";
    private static volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: java client.NapsterClient <porta_do_upload>");
            return;
        }

        CLIENT_PORT = Integer.parseInt(args[0]);

        // Inicia servidor de upload
        Thread uploadServerThread = new Thread(new Runnable() {
            public void run() {
                startUploadServer();
            }
        });
        uploadServerThread.start();

        Scanner scanner = new Scanner(System.in);
        String myIP = InetAddress.getLocalHost().getHostAddress();
        final String clientID = myIP + ":" + CLIENT_PORT;

        Socket socket = new Socket("localhost", SERVER_PORT);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // JOIN
        out.println("JOIN " + clientID);
        System.out.println(in.readLine());

        // Envia arquivos da pasta public
        File publicDir = new File(PUBLIC_FOLDER);
        File[] files = publicDir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    String filename = files[i].getName();
                    long size = files[i].length();
                    out.println("CREATEFILE " + filename + " " + size);
                    System.out.println(in.readLine());
                }
            }
        }

        while (true) {
            System.out.print("Comando (SEARCH <nome> | GET <ip:porta> <arquivo> | LEAVE): ");
            String input = scanner.nextLine();
            String[] parts = input.trim().split(" ");

            if (parts.length == 0) continue;

            if ("SEARCH".equalsIgnoreCase(parts[0]) && parts.length >= 2) {
                out.println("SEARCH " + parts[1]);
                String line;
                while ((line = in.readLine()) != null && !line.equals("ENDSEARCH")) {
                    System.out.println(line);
                }
            } else if ("GET".equalsIgnoreCase(parts[0]) && parts.length >= 3) {
                String[] ipPort = parts[1].split(":");
                if (ipPort.length == 2) {
                    downloadFile(ipPort[0], Integer.parseInt(ipPort[1]), parts[2]);
                } else {
                    System.out.println("Formato inválido de IP:porta.");
                }
            } else if ("LEAVE".equalsIgnoreCase(parts[0])) {
                out.println("LEAVE");
                System.out.println(in.readLine());
                running = false;
                break;
            } else {
                System.out.println("Comando inválido.");
            }
        }

        in.close();
        out.close();
        socket.close();
        scanner.close();
    }

    private static void downloadFile(String ip, int port, String filename) {
        Socket peer = null;
        PrintWriter out = null;
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            peer = new Socket(ip, port);
            out = new PrintWriter(peer.getOutputStream(), true);
            in = peer.getInputStream();

            File downloadsDir = new File("downloads");
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File file = new File(downloadsDir, filename);
            fos = new FileOutputStream(file);

            out.println("GET " + filename + " 0");

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("Download completo: downloads/" + filename);
        } catch (IOException e) {
            System.out.println("Erro no download: " + e.getMessage());
        } finally {
            try {
                if (fos != null) fos.close();
                if (in != null) in.close();
                if (out != null) out.close();
                if (peer != null) peer.close();
            } catch (IOException ignored) {}
        }
    }

    private static void startUploadServer() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(CLIENT_PORT);
            System.out.println("Upload server escutando na porta " + CLIENT_PORT);
            while (running) {
                final Socket socket = serverSocket.accept();
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        handleUpload(socket);
                    }
                });
                t.start();
            }
        } catch (IOException e) {
            System.out.println("Erro no servidor de upload: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private static void handleUpload(Socket socket) {
        BufferedReader in = null;
        OutputStream out = null;
        FileInputStream fileIn = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = socket.getOutputStream();

            String request = in.readLine();
            if (request == null) return;

            String[] parts = request.split(" ");
            if (parts.length < 3 || !"GET".equals(parts[0])) return;

            String filename = parts[1];
            int offset = Integer.parseInt(parts[2]);

            File file = new File(PUBLIC_FOLDER, filename);
            if (!file.exists()) return;

            fileIn = new FileInputStream(file);
            long skipped = 0;
            while (skipped < offset) {
                long s = fileIn.skip(offset - skipped);
                if (s == 0) break;
                skipped += s;
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            out.flush();
            System.out.println("Arquivo enviado com sucesso: " + filename);

        } catch (IOException e) {
            System.out.println("Erro ao enviar arquivo: " + e.getMessage());
        } finally {
            try {
                if (fileIn != null) fileIn.close();
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}
