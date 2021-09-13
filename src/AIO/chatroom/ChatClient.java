package AIO.chatroom;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
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

    AsynchronousSocketChannel clientChannel;
    ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);

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
            Future<Void> future = clientChannel.connect(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT));
            future.get();//会等待异步返回
            //读取用户输入
            new Thread(new UserInputHandler(this)).start();
            //todo 客户端接收消息
            while (true) {
                String receive = receive();
                System.out.println(receive);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(clientChannel);
        }
    }

    public void send(String input) {
        if (input.isEmpty()) {
            return;
        }
        wBuffer.clear();
        wBuffer.put(charset.encode(input));
        wBuffer.flip();
        Future<Integer> write = clientChannel.write(wBuffer);
        if (checkQuit(input)) {
            close(clientChannel);
        }
    }

    private String receive() {
        rBuffer.clear();
        Future<Integer> read = clientChannel.read(rBuffer);
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    public boolean checkQuit(String input) {
        return input.equals(QUIT);
    }

    public static void main(String[] args) {
        new ChatClient().start();
    }

}
