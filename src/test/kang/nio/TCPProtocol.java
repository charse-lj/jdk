package test.kang.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface TCPProtocol{
   /**
    * 接收一个SocketChannel的处理
    * @param key
    * @throws IOException
    */
   void handleAccept(SelectionKey key) throws IOException;
   
   /**
    * 从一个SocketChannel读取信息的处理
    * @param key
    * @throws IOException
    */
   void handleRead(SelectionKey key) throws IOException;
   
   /**
    * 向一个SocketChannel写入信息的处理
    * @param key
    * @throws IOException
    */
   void handleWrite(SelectionKey key) throws IOException;
 }