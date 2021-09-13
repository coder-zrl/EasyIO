package BIO_UseThreadPool.client;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Zhang Ruilong
 * @date 2021-09-12 0:39
 * @des 客户端，需要两个线程，一个是输入信息，一个是接收服务器的信息的线程
 */
public class ChatClient {
    private final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final int DEFAULT_SERVER_PORT = 8888;
    private final String QUIT = "quit";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    /**
     * 发送给服务器
     */
    public void send(String msg) throws IOException {
        if (!socket.isOutputShutdown()) {
            writer.write(msg+"\n");//这里要加上\n，因为不加\n的话readLine读不到
            writer.flush();
        }
    }
    /**
     * 接收服务器的信息
     */
    public String receive() throws IOException {
        String msg = null;
        if (!socket.isInputShutdown()) {
            msg = reader.readLine();
        }
        return msg;
    }
    /**
     * 检查用户是否要退出
     */
    public boolean checkQuit(String msg) {
        return QUIT.equals(msg);
    }
    public void close() {
        try {
            if (writer!=null) {
                writer.close();
            }
            if (reader!=null) {
                reader.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void start() {
        try {
            //创建socket
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
            //创建IO流
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
        } catch (IOException e) {
            throw new RuntimeException("服务端配置信息错误...");
        } try {
            //创建一条线程读取用户输入
            new Thread(new UserInputHandler(this)).start();
            //读取服务器信息
            String msg = null;
            while ((msg=reader.readLine())!=null) {
                System.out.println(msg);
            }
        } catch (IOException e) {
            throw new RuntimeException("服务端发生异常，请关闭程序...");
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        new ChatClient().start();
    }
}
