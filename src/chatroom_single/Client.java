package chatroom_single;

import java.io.*;
import java.net.Socket;

/**
 * @author Zhang Ruilong
 * @date 2021-09-11 23:39
 * @des 客户端，实现一个服务端和一个客户端一直讲话
 */
public class Client {
    private Socket socket = null;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;
    private BufferedReader consoleReader = null;


    public static void main(String[] args) {
        Socket socket = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        BufferedReader consoleReader = null;
        try {
            //创建socket
            socket = new Socket("127.0.0.1", 8888);
            //创建IO流
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
            consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("请输入消息:");
                String msg = consoleReader.readLine();
                if (msg.equals("quit")) {
                    break;
                }
                //发送信息到server
                writer.write(msg+"\n");
                writer.flush();
                //接受server返回的信息
                System.out.println(reader.readLine());//阻塞
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                consoleReader.close();
                writer.close();
                reader.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
