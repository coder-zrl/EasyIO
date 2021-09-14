package NIO.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * @author Zhang Ruilong
 * @date 2021-09-12 23:37
 * @des 客户端，需要两个线程，一个是输入信息，一个是接收服务器的信息的线程
 */
public class ChatClient {
    private int DEFAULT_SERVER_PORT = 8888;
    private String DEFAULT_SERVER_HOST = "127.0.0.1";
    private String QUIT="quit";
    private int BUFFER = 1024;

    private SocketChannel client;
    private Selector selector;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Charset charset = Charset.forName("UTF-8");
    private String host;
    private int port;

    public ChatClient() {
        this.host = DEFAULT_SERVER_HOST;
        this.port = DEFAULT_SERVER_PORT;
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private void start() {
        try {
            client = SocketChannel.open();
            client.configureBlocking(false);
            selector = Selector.open();
            //连接可以被正式创建了
            client.register(selector, SelectionKey.OP_CONNECT);
            client.connect(new InetSocketAddress(host,port));
            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    handles(selectionKey);
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handles(SelectionKey selectionKey) throws IOException {
        // CONNECT - 连接就绪
        if (selectionKey.isConnectable()) {
            SocketChannel client = (SocketChannel) selectionKey.channel();
            //建立连接就绪了
            if (client.isConnectionPending()) {
                //建立连接
                client.finishConnect();
                //处理用户输入的信息
                new Thread(new UserInputHandler(this)).start();
            }
            //将read事件注册
            client.register(selector,SelectionKey.OP_READ);
        }
        //READ - 事件
        else if (selectionKey.isReadable()) {
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            String msg = receive(channel);
            if (msg.isEmpty()) {
                //服务端异常
                close(selector);
            } else {
                System.out.println(msg);
            }
        }
    }

    private String receive(SocketChannel channel) throws IOException {
        rBuffer.clear();
        try {
            while (channel.read(rBuffer)>0) {}
        } catch (IOException e) {
            throw new RuntimeException("服务器异常，程序自动关闭...");
        } finally {
            System.exit(1);
        }
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    public void send(String msg) throws IOException {
        if (msg.isEmpty()) {
            return;
        }
        wBuffer.clear();
        wBuffer.put(charset.encode(msg));
        wBuffer.flip();
        while (wBuffer.hasRemaining()) {
            client.write(wBuffer);
        }
        if (checkQuit(msg)) {
            close(selector);
        }
    }

    public boolean checkQuit(String msg) {
        return msg.equals(QUIT);
    }

    public void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatClient("127.0.0.1",7777).start();
    }
}
