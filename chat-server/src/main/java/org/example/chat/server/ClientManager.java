package org.example.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable{
    private final Socket socket;
    public final static ArrayList<ClientManager> clients = new ArrayList<>();
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private String name;

    public ClientManager(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageForClient;

        while (socket.isConnected()) {
            try {
                messageForClient = bufferedReader.readLine();
                broadcastMessage(messageForClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }
    private void broadcastMessage(String message){
        String[] wordsOfMessage = message.split(" ");
        int counter = 0;
        if (wordsOfMessage[1].charAt(0) == '@'){
            String reciplient = wordsOfMessage[1].substring(1);
            StringBuilder messageForReciplient = new StringBuilder(wordsOfMessage[0] + "(Личное сообщение)");
            for (int i = 2; i < wordsOfMessage.length; i++) {
                messageForReciplient.append(" " + wordsOfMessage[i]);
            }
            for (ClientManager client : clients) {
                try {
                    if (client.name.equals(reciplient)) {
                        client.bufferedWriter.write(String.valueOf(messageForReciplient));
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                        counter++;
                        break;
                    }
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
            if (counter == 0) {
                for (ClientManager client : clients) {
                    try {
                        if (client.name.equals(name)) {
                            client.bufferedWriter.write("Пользователя " + reciplient + " в чате нет, введите корректное имя");
                            client.bufferedWriter.newLine();
                            client.bufferedWriter.flush();
                        }
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }else {
            for (ClientManager client : clients) {
                try {
                    if (!client.name.equals(name)) {
                        client.bufferedWriter.write(message);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        //удаление клиента из коллекции
        removeClient();
        try {
            //завершаем работу буффера на чтение данных
            if (bufferedReader != null) bufferedReader.close();
            //завершаем рабту буффера для записи данных
            if (bufferedWriter != null) bufferedWriter.close();
            //закрытие соединения с клиентским сокетом
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void removeClient(){
        clients.remove(this);
        System.out.println(name + " покинул чат");
        broadcastMessage("Server: " + name + " покинул чат.");
    }
}
