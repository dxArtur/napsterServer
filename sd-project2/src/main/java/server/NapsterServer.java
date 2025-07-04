package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class NapsterServer {
    private static final int PORT = 1234;
    private static final Map<String, List<FileInfo>> allFiles = new HashMap<String, List<FileInfo>>();
    private static final Logger logger = Logger.getLogger(NapsterServer.class.getName());
    private static volatile boolean running = true;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            logger.info("Servidor Napster escutando na porta " + PORT);

            while (running) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            logger.severe("Erro ao iniciar o servidor: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    logger.info("Servidor encerrado.");
                }
            } catch (IOException e) {
                logger.warning("Erro ao fechar o servidor: " + e.getMessage());
            }
        }
    }

    static class FileInfo {
        String filename;
        long size;

        FileInfo(String filename, long size) {
            this.filename = filename;
            this.size = size;
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String clientID;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            BufferedReader in = null;
            PrintWriter out = null;

            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split(" ");
                    String command = parts[0];

                    if ("JOIN".equals(command)) {
                        clientID = parts[1];
                        if (!allFiles.containsKey(clientID)) {
                            allFiles.put(clientID, new ArrayList<FileInfo>());
                        }
                        out.println("CONFIRMJOIN");
                        logger.info("Cliente conectado: " + clientID);

                    } else if ("CREATEFILE".equals(command)) {
                        if (clientID == null) {
                            out.println("ERRO: JOIN não enviado.");
                            continue;
                        }
                        String filename = parts[1];
                        long size = Long.parseLong(parts[2]);
                        List<FileInfo> fileList = allFiles.get(clientID);
                        fileList.add(new FileInfo(filename, size));
                        out.println("CONFIRMCREATEFILE " + filename);
                        logger.info("Arquivo registrado: " + filename + " (" + size + " bytes) de " + clientID);

                    } else if ("DELETEFILE".equals(command)) {
                        if (clientID == null) continue;
                        String toDelete = parts[1];
                        List<FileInfo> fileList = allFiles.get(clientID);
                        if (fileList != null) {
                            for (Iterator<FileInfo> it = fileList.iterator(); it.hasNext(); ) {
                                FileInfo f = it.next();
                                if (f.filename.equals(toDelete)) {
                                    it.remove();
                                    break;
                                }
                            }
                        }
                        out.println("CONFIRMDELETEFILE " + toDelete);
                        logger.info("Arquivo removido: " + toDelete + " de " + clientID);

                    } else if ("LEAVE".equals(command)) {
                        allFiles.remove(clientID);
                        out.println("CONFIRMLEAVE");
                        logger.info("Cliente saiu: " + clientID);
                        break;

                    } else if ("SEARCH".equals(command)) {
                        String pattern = parts[1];
                        boolean found = false;
                        for (Map.Entry<String, List<FileInfo>> entry : allFiles.entrySet()) {
                            String id = entry.getKey();
                            List<FileInfo> fileList = entry.getValue();
                            for (FileInfo f : fileList) {
                                if (f.filename.contains(pattern)) {
                                    out.println("FILE " + f.filename + " " + id + " " + f.size);
                                    found = true;
                                }
                            }
                        }
                        if (!found) {
                            out.println("NORESULTS");
                        }
                        out.println("ENDSEARCH");

                    } else {
                        out.println("ERRO: Comando desconhecido.");
                        logger.warning("Comando desconhecido: " + command);
                    }
                }

            } catch (IOException e) {
                logger.warning("Erro com cliente " + clientID + ": " + e.getMessage());
            } finally {
                if (clientID != null) {
                    allFiles.remove(clientID);
                    logger.info("Removido cliente desconectado: " + clientID);
                }
                try {
                    if (in != null) in.close();
                } catch (IOException ignored) {}
                if (out != null) out.close();
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    // Permite parar o servidor externamente
    public static void stopServer() {
        running = false;
        logger.info("Flag de execução alterada para false.");
    }
}
