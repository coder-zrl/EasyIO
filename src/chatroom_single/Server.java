package chatroom_single;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Zhang Ruilong
 * @date 2021-09-11 23:32
 * @des 服务端，实现一个服务端和一个客户端一直讲话
 */
public class Server {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            serverSocket = new ServerSocket(8888);
            //创建socket
            System.out.println("等待客户端连接...");
            Socket socket = serverSocket.accept();
            System.out.println("客户端"+socket.getPort()+"连接成功");//socket.getPort()是本地的端口号
            //创建IO流
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
            //一行一行的显示，以\n为分隔符
            String msg = null;
            while ((msg=reader.readLine()) != null) {//阻塞
                System.out.println("客户端["+socket.getPort()+"]:"+msg);
                writer.write("服务器："+msg+"\n");
                writer.flush();
                if (msg.equals("quit")) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
                writer.close();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
