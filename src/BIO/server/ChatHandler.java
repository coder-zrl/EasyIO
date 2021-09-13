package BIO.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * @author Zhang Ruilong
 * @date 2021-09-12 0:23
 * @des 接收到一个socket客户端就创建一个线程
 */
public class ChatHandler implements Runnable {
    private ChatServer chatServer;//主要是操作chatServer的connectClient
    private Socket socket;//需要知道建立的socket对象

    public ChatHandler(ChatServer chatServer, Socket socket) {
        this.chatServer = chatServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            //存储新用户
            chatServer.addClient(socket);
            //等待新用户发送消息
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            String msg = null;
            while ((msg=reader.readLine())!= null) {
                if (chatServer.checkQuit(msg)) {
                    //用户主动退出，如果在这里调用removeClient方法的话会有问题
                    //我们就不能确定catch的时候的Exception是removeClient产生的，还是socket客户端断开导致readLine报错产生的
                    throw new RuntimeException();
                }
                //转发消息，这里需要多加一个\n，因为在客户端发的消息的\n在readLine的时候已经被消耗掉了
                String fwdMsg = "客户端["+socket.getPort()+"]:"+msg+"\n";
                chatServer.forwordMessage(socket,fwdMsg);
            }
        } catch (IOException e) {
            //两种情况：客户端突然关闭导致readLine出错；客户端正常退出
            try {
                chatServer.removeClient(socket);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
