package BIO.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Zhang Ruilong
 * @date 2021-09-12 0:52
 * @des 用来读取用户输入的消息并发送给服务器
 */
public class UserInputHandler implements Runnable {
    private ChatClient chatClient;
    private BufferedReader consoleReader;
    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
        consoleReader = new BufferedReader(
                new InputStreamReader(System.in)
        );
    }

    @Override
    public void run() {
        try {
            while (true) {
                String input = consoleReader.readLine();
                chatClient.send(input);
                if (chatClient.checkQuit(input)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            chatClient.close();
        }
    }
}
