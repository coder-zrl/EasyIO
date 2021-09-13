package NIO.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * @author Zhang Ruilong
 * @date 2021-09-12 22:35
 * @des 基于NIO模型聊天室的SERVER
 */
public class ChatServer {
    private int DEFAULT_PORT=8888;
    private String QUIT="quit";
    private int BUFFER = 1024;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer(int port) {
        this.port = port;
    }

    public ChatServer() {
        this.port = DEFAULT_PORT;
    }

    public void start() {
        try {
            //默认处于阻塞式调用
            serverSocketChannel = ServerSocketChannel.open();
            //修改成非阻塞式调用
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            selector = Selector.open();
            //注册accept事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口:"+port);
            while (true) {
                //如果没有就会一直阻塞，等待有内容返回
                selector.select();
                //获取被触发的事件
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    //todo 处理被触发的事件
                    handles(selectionKey);
                }
                //需要手动清空事件
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //会自动解除注册并关闭
            close(selector);
        }
    }

    /**
     * 处理被触发的事件
     */
    private void handles(SelectionKey selectionKey) throws IOException {
        //ACCEPT - 和客户端建立了连接
        if (selectionKey.isAcceptable()) {
            ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel client = serverSocketChannel.accept();
            client.configureBlocking(false);
            client.register(selector,SelectionKey.OP_READ);
            System.out.println("客户端:["+client.socket().getPort()+"]已连接");
            System.out.println("现有客户端:"+(selector.keys().size()-1));
        }
        //READ - 客户端发送了消息
        else if(selectionKey.isReadable()) {
            SocketChannel client = (SocketChannel) selectionKey.channel();
            String fwdMsg = receive(selectionKey,selector);
            if (checkQuit(fwdMsg) || fwdMsg.isEmpty()) {
                remove(selectionKey,selector);
            } else {
                forwordMessage(client,fwdMsg);
            }
        }
    }

    /**
     * 读取用户通道发来的消息
     */
    private String receive(SelectionKey selectionKey,Selector selector) throws IOException {
        SocketChannel client = (SocketChannel) selectionKey.channel();
        rBuffer.clear();
        try {
            //如果还有数据就一直读
            while (client.read(rBuffer)>0) {}
        } catch (IOException e) {
            //这里不用写东西，wdMsg.isEmpty()会处理
        }
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    /**
     * 移除一个客户端
     */
    private void remove(SelectionKey selectionKey, Selector selector) {
        SocketChannel client = (SocketChannel) selectionKey.channel();
        System.out.println("客户端:["+client.socket().getPort()+"]已退出");
        //客户端异常或自动关闭，取消selector监听
        selectionKey.cancel();
        //更新selector，单一线程其实没啥作用，当多线程是很有必要的
        selector.wakeup();
        System.out.println("现有客户端:"+(selector.keys().size()-1));
    }

    /**
     * 转发消息给除了发送者外的所有客户端
     */
    private void forwordMessage(SocketChannel client, String fwdMsg) throws IOException {
        //所有注册在selector的
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            Channel channel = key.channel();
            if (channel instanceof ServerSocketChannel) {
                continue;
            }
            //处于正常的状态，channel没有被关闭，监视的selector也是好的
            //保证不给自己发送
            if (key.isValid() && !client.equals(channel)) {
                //wBuffer写入channel
                wBuffer.clear();
                String msg = client.socket().getPort()+":"+fwdMsg;
                wBuffer.put(charset.encode(msg));
                //写转为读
                wBuffer.flip();
                while (wBuffer.hasRemaining()) {
                    ((SocketChannel)channel).write(wBuffer);
                }
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
    public void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new ChatServer(7777).start();
    }
}
