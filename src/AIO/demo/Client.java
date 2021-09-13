package AIO.demo;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Zhang Ruilong
 * @date 2021-09-13 22:44
 * @des 一应一答的服务端，异步调用选择返回future对象，然后操作
 */
public class Client {
    private final String DEFAULT_HOST="localhost";
    private final int DEFAULT_PORT = 8888;
    AsynchronousSocketChannel clientChannel;

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
            //创建channel
            clientChannel = AsynchronousSocketChannel.open();
            //异步连接
            Future<Void> future = clientChannel.connect(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT));
            future.get();//会等待异步返回
            //可以发送消息了
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String input = consoleReader.readLine();
                //发送给服务器
                ByteBuffer buffer = ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8));
                Future<Integer> writeRestlt = clientChannel.write(buffer);
                writeRestlt.get();//阻塞成功完成
                buffer.flip();//进入写模式，数据写到buffer
                //读取服务器的返回
                Future<Integer> read = clientChannel.read(buffer);
                read.get();
                String result = new String(buffer.array());
                System.out.println(result);
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            close(clientChannel);
        }
    }

    public static void main(String[] args) {
        new Client().start();
    }
}
