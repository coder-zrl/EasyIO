package AIO.chatroom;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Zhang Ruilong
 * @date 2021-09-14 00:23
 */
public class ChatClient {
    private final String DEFAULT_HOST="localhost";
    private final int DEFAULT_PORT = 8888;
    private final static String QUIT="quit";
    private Charset charset = Charset.forName("UTF-8");
    private final static int BUFFER=1024;
    private String host;
    private int port;

    AsynchronousSocketChannel clientChannel;
    ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);

    public ChatClient() {
        this.host = DEFAULT_HOST;
        this.port = DEFAULT_PORT;
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            //创建channel
            clientChannel = AsynchronousSocketChannel.open();
            //异步连接
            Future<Void> future = clientChannel.connect(new InetSocketAddress(host, port));
            future.get();//会等待异步返回
            //读取用户输入
            new Thread(new UserInputHandler(this)).start();
            //todo 客户端接收消息
            ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER);
            while (true) {
                //启动异步读操作，以从该通道读取到给定的缓冲器字节序列
                Future<Integer> readResult = clientChannel.read(byteBuffer);
                //Future的get方法返回读取的字节数或-1如果没有字节可以读取，因为通道已经到达流终止。
                int result = readResult.get();
                if(result <= 0){
                    // 服务器异常
                    System.out.println("服务器异常，程序自动关闭...");
                    close(clientChannel);
                    // 0是正常退出，非0是不正常退出
                    System.exit(1);
                }else {
                    //转化为写模式
                    byteBuffer.flip();
                    String msg = String.valueOf(charset.decode(byteBuffer));
                    //每次将缓冲区的内容写出来后都将缓冲区数据清空
                    byteBuffer.clear();
                    System.out.println(msg);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("服务器异常，程序自动关闭...");
        } finally {
            close(clientChannel);
            //0是正常退出,非0是不正常退出
            System.exit(1);
        }
    }

    public void send(String input) {
        if (input.isEmpty()) {
            return;
        }
        wBuffer.clear();
        wBuffer.put(charset.encode(input));
        //别人可以读rBuffer了
        wBuffer.flip();
        Future<Integer> writeResult = clientChannel.write(wBuffer);
        try {
            writeResult.get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("消息发送失败");
            e.printStackTrace();
        }
        if (checkQuit(input)) {
            close(clientChannel);
        }
    }

    private String receive() {
        rBuffer.clear();
        rBuffer.flip();
        Future<Integer> read = clientChannel.read(rBuffer);
        return String.valueOf(charset.decode(rBuffer));
    }

    public boolean checkQuit(String input) {
        return input.equals(QUIT);
    }

    public static void main(String[] args) {
        new ChatClient("127.0.0.1",7777).start();
    }
}
