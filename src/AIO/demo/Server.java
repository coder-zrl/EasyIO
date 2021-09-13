package AIO.demo;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Zhang Ruilong
 * @date 2021-09-13 22:44
 * @des 一应一答的服务端，异步调用选择通过CompletionHandler，执行成功和失败后的逻辑
 */
public class Server {

    private final String DEFAULT_HOST="localhost";
    private final int DEFAULT_PORT = 8888;
    AsynchronousServerSocketChannel serverSocketChannel;

    private void close(Closeable closeable) {
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
            //绑定监听端口
            //使用默认的AsynchronousChannelGroup，同组可以共享资源
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(DEFAULT_HOST,DEFAULT_PORT));
            System.out.println("服务器启动，正在监听"+DEFAULT_PORT+"端口");
            while (true) {//这里要循环是因为异步操作，accept不阻塞，执行完服务器就宕掉了
                //异步操作，attachment是辅助信息，我们这里不需要 todo
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

    //AsynchronousSocketChannel是异步调用返回的结果的对象，Object是进行accept的attachment的类型
    private class AcceptHandler implements java.nio.channels.CompletionHandler<AsynchronousSocketChannel,Object> {
        /**
         * 成功执行完了
         */
        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            //继续监听下一个连接
            if (serverSocketChannel.isOpen()){
                //在回调函数中调用回调函数，底层实现会限制递归层级，不用太担心
                serverSocketChannel.accept(null,this);
            }
            //在这里又要进行异步调用read，来接收信息
            AsynchronousSocketChannel clientChannel = result;
            if (clientChannel.isOpen() && clientChannel!=null) {
                //读操作
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                HashMap<String, Object> info = new HashMap<>();
                info.put("type","read");
                info.put("buffer",buffer);
                //attachment告诉handler刚刚完成的是读操作还是写操作，将这些参数传给clientHandler
                clientChannel.read(buffer,info,new ClientHandler(clientChannel));
            }
        }
        /**
         * 执行失败了
         */
        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }
    private class ClientHandler implements CompletionHandler<Integer,Object> {
        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            Map<String,Object> info = (Map<String, Object>) attachment;
            String type = (String) info.get("type");
            if ("read".equals(type)) {//刚刚完成的是读操作
                //写回clientChannel
                ByteBuffer buffer = (ByteBuffer) info.get("buffer");
                buffer.flip();//进入读模式，从buffer读
                info.put("type","write");
                clientChannel.write(buffer,info,this);
                buffer.clear();
            } else if ("write".equals(type)) {//刚刚完成的是写操作
                //写回clientChannel
                ByteBuffer buffer = (ByteBuffer) info.get("buffer");
                buffer.flip();//进入写模式，数据写到buffer
                info.put("type","write");
                clientChannel.read(buffer,info,this);
                buffer.clear();
            }
        }
        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }

    public static void main(String[] args) {
        new Server().start();
    }
}
