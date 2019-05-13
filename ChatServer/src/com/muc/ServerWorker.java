package com.muc;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;

public class ServerWorker extends Thread {

    private final Socket clientSocket;
    private final Server server;
    private String login = null;
    private OutputStream outputStream;
    private HashSet<String> topicSet = new HashSet<>();

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ( (line = reader.readLine()) != null) {
            String[] tokens = line.split(" ");
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                } else if ("logoff".equalsIgnoreCase(cmd) || "quit".equalsIgnoreCase(cmd)) {
                    handleLogoff();
                    break;
                } else if ("msg".equalsIgnoreCase(cmd)) {
                    String[] tokensMsg = line.split(line, 3);
                    handleMessage(tokensMsg);
                } else if ("#".equalsIgnoreCase(cmd)) {
                    handleJoin(tokens);
                } else {
                    String msg = "unknown " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }

        clientSocket.close();
    }

    public boolean isMemberOfTopic(String topic) {
        return topicSet.contains(topic);
    }

    private void handleJoin(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        boolean isTopic = sendTo.charAt(0) == '#';
        
        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList) {
            if (isTopic) {
                if (worker.isMemberOfTopic(sendTo)) {
                    String outMsg = "msg " + sendTo + ":" + login + " " + body + "\n";
                    worker.send(outMsg);
                }
            } else {
                if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                    String outMsg = "msg " + login + " " + body + "\n";
                    worker.send(outMsg);
                }
            }

        }
    }

    private void handleLogoff() throws IOException {
        server.removeWorker(this);
        List<ServerWorker> workerList = server.getWorkerList();

        String onlineMsg = "offline " + login + "\n";
        for(ServerWorker worker : workerList) {
            if (!login.equals(worker.getLogin())) {
                worker.send(onlineMsg);
            }
        }
        clientSocket.close();
    }

    private void handleLeave(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    public String getLogin() {

        return login;
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];

            if ((login.equals("guest") && password.equals("guest")) || (login.equals("Luke") && password.equals("1234")) ) {
                String msg = "ok login" + "\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User logged in successfully: " + login);
                String onlineMsg = "online " + login + "\n";
                outputStream.write(onlineMsg.getBytes());
                List<ServerWorker> workerList = server. getWorkerList();
                for(ServerWorker worker : workerList) {
                    worker.send(onlineMsg);
                }
            } else {
                String msg = "error login\n";
                outputStream.write(msg.getBytes());
                System.err.println("Login failed for " + login);
            }
        }
    }

    private void send(String msg) throws IOException {
        outputStream.write(msg.getBytes());
    }
}
