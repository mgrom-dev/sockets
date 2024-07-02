package ru.mgrom;

import com.fasterxml.jackson.databind.DeserializationFeature;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.mgrom.JSONS.AbstractRequest;
import ru.mgrom.JSONS.BroadcastMessageRequest;
import ru.mgrom.JSONS.DisconnectRequest;
import ru.mgrom.JSONS.InfoResponse;
import ru.mgrom.JSONS.LoginRequest;
import ru.mgrom.JSONS.LoginResponse;
import ru.mgrom.JSONS.MessageRequest;
import ru.mgrom.JSONS.MessageResponse;
import ru.mgrom.JSONS.UserRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class ChatClient {
  private final static int PORT = 8888;
  private final static String IP = "localhost";
  private final static JSONMapper JSON = new JSONMapper();

  @AllArgsConstructor
  @Getter
  @Setter
  private static class ConnectionStatus {
    private volatile boolean connected;
  }

  public static void main(String[] args) {
    JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Scanner console = new Scanner(System.in);

    try (Socket server = new Socket(IP, PORT)) {
      System.out.println("Соединение с сервером установлено");
      ConnectionStatus connected = new ConnectionStatus(true);

      try (PrintWriter out = new PrintWriter(server.getOutputStream(), true)) {
        Scanner in = new Scanner(server.getInputStream());

        // авторизация
        LoginResponse loginResponse = null;
        String clientLogin = "";
        while (loginResponse == null && connected.isConnected()) {
          System.out.print("Введите логин: ");
          clientLogin = console.nextLine();
          if (clientLogin.isEmpty()) {
            System.err.println("Логин не может быть пустым");
            continue;
          }
          out.println(JSON.write(new LoginRequest(clientLogin)));
          loginResponse = JSON.read(in.nextLine(), LoginResponse.class);
          if (loginResponse == null) {
            System.err.println("Ошибка чтения ответа от сервера");
          } else if (!loginResponse.isConnected()) {
            System.err.println("Не удалось подключиться к серверу. " + loginResponse.getMessage());
            loginResponse = null;
          }
        }
        System.out.println("Вы успешно подключились к чату");
        help();

        // поток на чтение сообщение от сервера
        new Thread(() -> {
          while (connected.isConnected()) {
            String msgFromServer;
            if (in.hasNextLine()) {
              msgFromServer = in.nextLine();
            } else {
              continue;
            }

            // определяем тип сообщений
            AbstractRequest type = JSON.read(msgFromServer, AbstractRequest.class);
            if (type == null) {
              System.err.println("Ошибка при чтении формата сообщения: " + msgFromServer);
              continue;
            }

            switch (type.getType()) {
              case "userMessage":
                MessageResponse messageResponse = JSON.read(msgFromServer, MessageResponse.class);
                if (messageResponse == null) {
                  System.err.println("Ошибка при чтении сообщения от пользователя");
                  continue;
                }
                System.out.println("Сообщение от " + messageResponse.getFrom() + ": " + messageResponse.getMessage());
                break;

              case "infoMessage":
                InfoResponse infoResponse = JSON.read(msgFromServer, InfoResponse.class);
                if (infoResponse == null) {
                  System.err.println("Ошибка при чтении информационного сообщения");
                  continue;
                }
                System.out.println("Сообщение от сервера: " + infoResponse.getMessage());
                break;

              default:
                System.err.println("Неизвестный тип сообщения: " + msgFromServer);
                break;
            }
          }
        }).start();

        // Отправка сообщений
        while (connected.isConnected()) {
          System.out.println("Введите сообщение:");
          String raw = console.nextLine();
          if (raw.isEmpty()) {
            continue;
          } else if (raw.matches("@all:.+")) {
            // Отправка сообщения всем пользователям
            out.println(JSON.write(new BroadcastMessageRequest(raw.split(":")[1])));
          } else if (raw.matches("@.+:.+")) {
            // Отправка сообщения конкретному пользователю
            String text = raw.split(":")[1];
            String user = raw.substring(1, raw.length() - text.length() - 1);
            out.println(JSON.write(new MessageRequest(user, text)));
          } else if (raw.matches("!users")) {
            out.println(JSON.write(new UserRequest()));
          } else if (raw.matches("!exit")) {
            // выход
            connected.setConnected(false);
            out.println(JSON.write(new DisconnectRequest()));
          } else if (raw.matches("!help")) {
            // справка по коммандам
            help();
          } else {
            // отправка сообщения всем пользователям
            out.println(JSON.write(new BroadcastMessageRequest(raw)));
          }
        }

        in.close();
      }
    } catch (IOException e) {
      System.err.println("Ошибка во время подключения к серверу: " + e.getMessage());
    }

    System.out.println("Отключились от сервера");
    console.close();
  }

  private static void help() {
    System.out.println("Доступные команды");
    System.out.println("@user: message - послать сообщение другу");
    System.out.println("@all: message - послать сообщение всем");
    System.out.println("!users - получить список всех пользователей");
    System.out.println("!exit - выход");
    System.out.println("!help - вывести справку по коммандам");
  }

}
