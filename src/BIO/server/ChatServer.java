package BIO.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Zhang Ruilong
 * @date 2021-09-12 0:09
 * @des 基于BIO模型聊天室的SERVER
 */
public class ChatServer {
    private int DEFAULT_PORT=8888;
    private String QUIT="QUIT";
    private ServerSocket serverSocket;
    private Map<Integer, Writer> connectClients;

    public ChatServer() {
        this.connectClients = new ConcurrentHashMap<>();
    }
    /**
     * 新上线的用户加到map中
     */
    public void addClient(Socket socket) throws IOException {
        if (socket!=null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
            connectClients.put(port,writer);
            System.out.println("客户端["+port+"]已连接");
        }
    }
    /**
     * 移除下线的用户
     */
    public void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            if (connectClients.containsKey(port)) {
                socket.close();
                connectClients.remove(port);
                System.out.println("客户端["+port+"]已下线");
            }
        }
    }
    /**
     * 转发消息给除了发送者外的所有客户端
     */
    public void forwordMessage(Socket socket,String fwdMsg) throws IOException {
        for (Integer id : connectClients.keySet()) {
            if (id!=socket.getPort()) {
                Writer writer = connectClients.get(id);
                writer.write(fwdMsg);
                writer.flush();
            }
        }
    }
    /**
     * 检查发的消息是否是客户端退出
     */
    public boolean checkQuit(String msg) {
        return QUIT.equals(msg);
    }
    /**
     * 关闭资源
     */
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void start() {
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("服务器已经启动，正在监听"+DEFAULT_PORT+"端口...");
            while (true) {
                //等待客户端连接
                Socket socket = serverSocket.accept();
                //todo 有了客户端连接就创建一个线程
                new Thread(new ChatHandler(this,socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}
