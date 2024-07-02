package ru.mgrom;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;

import ru.mgrom.JSONS.AbstractRequest;
import ru.mgrom.JSONS.BroadcastMessageRequest;
import ru.mgrom.JSONS.DisconnectRequest;
import ru.mgrom.JSONS.InfoResponse;
import ru.mgrom.JSONS.LoginRequest;
import ru.mgrom.JSONS.LoginResponse;
import ru.mgrom.JSONS.MessageRequest;
import ru.mgrom.JSONS.MessageResponse;
import ru.mgrom.JSONS.UserRequest;

public class ChatServer {
    private final static int PORT = 8888;
    private final static JSONMapper JSON = new JSONMapper();
    private final static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен на порту " + PORT);

            while (true) {
                Socket client = server.accept();
                ClientHandler clientHandler = new ClientHandler(client, clients);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Ошибка во время работы сервера: ");
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {

        private final Socket client;
        private final Scanner in;
        private final PrintWriter out;
        private final Map<String, ClientHandler> clients;
        private String clientLogin;

        public ClientHandler(Socket client, Map<String, ClientHandler> clients) throws IOException {
            this.client = client;
            this.clients = clients;

            this.in = new Scanner(client.getInputStream());
            this.out = new PrintWriter(client.getOutputStream(), true);
        }

        @Override
        public void run() {
            System.out.println("Подключен новый клиент");
            boolean connected = true;

            // Авторизация клиента
            LoginRequest loginRequest = null;
            while (loginRequest == null && connected) {
                loginRequest = JSON.read(in.nextLine(), LoginRequest.class);
                if (loginRequest == null) {
                    out.println(JSON.write(new LoginResponse(false, "Ошибка при чтении логина, попробуйте еще раз.")));
                } else if (clients.containsKey(loginRequest.getLogin())) {
                    out.println(JSON.write(new LoginResponse(false, "Логин: " + loginRequest.getLogin() + " занят.")));
                    loginRequest = null;
                }
            }
            clientLogin = loginRequest.getLogin();
            clients.put(clientLogin, this);
            out.println(JSON.write(new LoginResponse(true, "Авторизация прошла успешно")));
            System.out.println("Клиент: " + clientLogin + " успешно авторизован");

            // Чтение сообщений от клиента
            while (connected) {
                String msgFromClient = in.nextLine();

                // определяем тип сообщений
                AbstractRequest type = JSON.read(msgFromClient, AbstractRequest.class);
                if (type == null) {
                    System.err.println("Ошибка при чтении формата сообщения: " + msgFromClient);
                    infoMessage("Ошибка формата сообщения");
                    continue;
                }

                // Определяем действие в зависимости от типа сообщения
                switch (type.getType()) {
                    // 1. отправка сообщения
                    case "sendMessage":
                        MessageRequest messageRequest = JSON.read(msgFromClient, MessageRequest.class);
                        if (messageRequest == null) {
                            infoMessage("Ошибка при чтении сообщения");
                            continue;
                        }
                        ClientHandler clientTo = clients.get(messageRequest.getRecipient());
                        if (clientTo == null) {
                            infoMessage("Клиент с логином [" + messageRequest.getRecipient() + "] не найден");
                        } else {
                            clientTo.sendMessage(messageRequest.getMessage(), clientLogin);
                        }
                        break;

                    // 1.1 BroadcastMessageRequest - послать сообщение ВСЕМ пользователям
                    case "broadcastSendMessage":
                        BroadcastMessageRequest mess = JSON.read(msgFromClient, BroadcastMessageRequest.class);
                        if (mess == null) {
                            infoMessage("Ошибка при чтении сообщения пользователям");
                            continue;
                        }
                        clients.values().stream()
                                .filter(v -> !v.clientLogin.equals(clientLogin))
                                .forEach(c -> c.sendMessage(mess.getMessage(), clientLogin));
                        break;

                    // 1.2 UsersRequest - получить список всех логинов, которые есть в чате
                    case "userRequest":
                        UserRequest userRequest = JSON.read(msgFromClient, UserRequest.class);
                        if (userRequest == null) {
                            infoMessage("Ошибка при чтении команды userRequest");
                            continue;
                        }
                        String message = clients.keySet().stream()
                                .map(key -> "'" + key + "'")
                                .collect(Collectors.joining(", ", "[", "]"));
                        infoMessage(message);
                        break;

                    // 1.3 DisconnectRequest - клиент оповещает сервер о том, что он отключился
                    // 1.3.1 * Доп. задание: при отключении юзера, делать рассылку на остальных
                    case "disconnect":
                        DisconnectRequest disconnectRequest = JSON.read(msgFromClient, DisconnectRequest.class);
                        if (disconnectRequest == null) {
                            infoMessage("Ошибка при чтении завершения соединения");
                            continue;
                        }
                        clients.remove(clientLogin);
                        String text = "Клиент: " + clientLogin + " отключился";
                        System.out.println(text);
                        clients.values().forEach(c -> c.infoMessage(text));
                        connected = false;
                        break;

                    default:
                        System.err.println("Неизвестный тип сообщения: " + msgFromClient);
                        infoMessage("Неизвестный тип сообщения");
                        break;
                }
            }

            doClose();
        }

        private void doClose() {
            try {
                in.close();
                out.close();
                client.close();
            } catch (IOException e) {
                System.err.println("Ошибка во время отключения клиента: " + e.getMessage());
            }
        }

        public void sendMessage(String message, String from) {
            out.println(JSON.write(new MessageResponse(from, message)));
        }

        public void infoMessage(String message) {
            out.println(JSON.write(new InfoResponse(message)));
        }
    }

}