package AIO.chatroom;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Zhang Ruilong
 * @date 2021-09-13 23:46
 * @des
 */
public class ChatServer {
    private final static String DEFAULT_HOST="localhost";
    private final static int DEFAULT_PORT=8888;
    private final static String QUIT="quit";
    private final static int BUFFER=1024;
    private final static int THREADPOOL_SIZE=8;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverSocketChannel;
    private Charset charset = Charset.forName("UTF-8");
    private int port;
    private ArrayList<ClientHandler> connectedClients = new ArrayList<>();

    public ChatServer() {
        this.port = DEFAULT_PORT;
    }

    public ChatServer(int port) {
        this.port = port;
    }
    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 获取客户端信息
     */
    private String getClientName(AsynchronousSocketChannel clientChannel) {
        int clientPort = -1;
        try {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
            clientPort = inetSocketAddress.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "客户端[" + clientPort + "]";
    }
    /**
     * 添加用户
     */
    private synchronized void addClient(ClientHandler clientHandler) {
        connectedClients.add(clientHandler);
        System.out.println(getClientName(clientHandler.clientChannel) + "已经连接到服务器");
    }

    /**
     * 移除用户
     */
    private synchronized void removeClient(ClientHandler clientHandler) {
        connectedClients.remove(clientHandler);
        System.out.println(getClientName(clientHandler.clientChannel)+"已被移除");
        close(clientHandler.clientChannel);
    }
    public void start() {
        ExecutorService executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
        try {
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverSocketChannel.bind(new InetSocketAddress(DEFAULT_HOST,port));
            System.out.println("服务器启动，正在监听"+port+"端口");
            while (true) {//这里要循环是因为异步操作，accept不阻塞，执行完服务器就宕掉了
                //异步操作，attachment是辅助信息，我们这里不需要
                serverSocketChannel.accept(null, new AcceptHandler());
                //等待输入，会阻塞一下，不至于一直循环或者等待几秒再循环
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverSocketChannel);
        }
    }
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel,Object> {
        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            //继续监听下一个连接
            if (serverSocketChannel.isOpen()){
                //在回调函数中调用回调函数，底层实现会限制递归层级，不用太担心
                serverSocketChannel.accept(null,this);
            }
            //在这里又要进行异步调用read，来接收信息
            if (clientChannel.isOpen() && clientChannel!=null) {
                //读操作
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
                //新用户添加到用户列表
                ClientHandler handler = new ClientHandler(clientChannel);
                addClient(handler);
                clientChannel.read(buffer,buffer, handler);
            }
        }
        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }
    private class ClientHandler implements CompletionHandler<Integer,Object> {
        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        public AsynchronousSocketChannel getClientChannel() {
            return clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            //写回clientChannel
            ByteBuffer buffer = (ByteBuffer) attachment;
            if (buffer!=null) {
                if (result<=0) {
                    //用户异常下线，移除用户
                    connectedClients.remove(this);
                } else {
                    buffer.flip();
                    //读取信息
                    String fwdMsg = receive(buffer);
                    System.out.println(getClientName(clientChannel) + ":" +fwdMsg);
                    //转发信息
                    forwardMessage(clientChannel,fwdMsg);
                    buffer.clear();
                    // 检查用户是否退出
                    if (checkQuit(fwdMsg)){
                        // 将客户从在线客户列表中去除
                        removeClient(this);
                    }else {
                        // 接着读
                        clientChannel.read(buffer,buffer,this);
                    }
                }
            }
        }
        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }

    private boolean checkQuit(String msg) {
        return msg.equals(QUIT);
    }

    private String receive(ByteBuffer buffer) {
        return String.valueOf(charset.decode(buffer));
    }

    private synchronized void forwardMessage(AsynchronousSocketChannel clientChannel, String fwdMsg) {
        for (ClientHandler connectedClient : connectedClients) {
            if (!connectedClient.clientChannel.equals(clientChannel)) {
                try {
                    // 防止发生意想不到的错误或异常
                    ByteBuffer byteBuffer = charset.encode(getClientName(connectedClient.clientChannel) + ":" + fwdMsg);
                    connectedClient.clientChannel.write(byteBuffer, null, connectedClient);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        new ChatServer(7777).start();
    }
}
