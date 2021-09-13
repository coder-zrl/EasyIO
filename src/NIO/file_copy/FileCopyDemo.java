package NIO.file_copy;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Zhang Ruilong
 * @date 2021-09-12 2:01
 * @des 几张文件复制方式的对比
 */
interface FileCopyRunner {
    void copyFile(File source,File target);
}
public class FileCopyDemo {

    private static final int ROUNDS=5;

    private static void bencgmark(FileCopyRunner fileCopyRunner,File source,File target) {
        long elapsed=0L;
        for (int i=0;i<ROUNDS;i++) {
            long startTime = System.currentTimeMillis();
            fileCopyRunner.copyFile(source, target);
            elapsed += System.currentTimeMillis()-startTime;
            target.delete();
        }
        System.out.println(fileCopyRunner+":"+elapsed/ROUNDS);
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        //不使用任何缓冲区，一个个字节来写
        FileCopyRunner noBufferStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                InputStream fin = null;
                OutputStream fout = null;
                try {
                    fin = new FileInputStream(source);
                    fout = new FileOutputStream(target);
                    int read;
                    while ((read = fin.read())!=-1) {
                        fout.write(read);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }
        };
        //使用缓冲区，一次读一个缓冲区
        FileCopyRunner bufferedStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                BufferedInputStream fin = null;
                BufferedOutputStream fout = null;
                try {
                    fin = new BufferedInputStream(
                            new FileInputStream(source)
                    );
                    fout = new BufferedOutputStream(
                            new FileOutputStream(target)
                    );
                    byte[] buffer = new byte[1024];
                    int result;
                    while ((result = fin.read(buffer))!=-1) {
                        fout.write(buffer,0,result);
                        fout.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }
        };
        //Channel用它的buffer操作
        FileCopyRunner nioBufferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileChannel fin = null;
                FileChannel fout = null;
                try {
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    while ((fin.read(buffer)!=-1)) {
                        buffer.flip();
                        //全部读完
                        while (buffer.hasRemaining()) {
                            fout.write(buffer);
                        }
                        buffer.clear();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }
        };
        //两个Channel直接传输数据
        FileCopyRunner nioTransferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileChannel fin = null;
                FileChannel fout = null;
                try {
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();
                    long transferred=0L;//记录一共拷贝了多少字节
                    long size = fin.size();
                    while (transferred != size) {
                        //从哪开始、传输多少、传到哪里
                        transferred += fin.transferTo(0, size, fout);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }
        };
        File smallFile = new File("E:\\IdeaProjects\\EasyIO\\resources\\bigFile.mp4");
        File smallFileCopy = new File("E:\\IdeaProjects\\EasyIO\\resources\\bigFileCopy.mp4");
        bencgmark(noBufferStreamCopy,smallFile,smallFileCopy);
        bencgmark(bufferedStreamCopy,smallFile,smallFileCopy);
        bencgmark(nioBufferCopy,smallFile,smallFileCopy);
        bencgmark(nioTransferCopy,smallFile,smallFileCopy);
    }
}
