package com.xjeffrose.chicago.client;

import com.xjeffrose.xio.SSL.XioSecurityHandlerImpl;
import com.xjeffrose.xio.client.XioConnectionPool;
import com.xjeffrose.xio.client.asyncretry.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by root on 7/5/16.
 */
public class XioConnectionPoolManager {
  private final NioEventLoopGroup workerLoop = new NioEventLoopGroup(20);
  private final ChicagoListener listener = new ChicagoListener();
  private InetSocketAddress address(String node) {
    String chunks[] = node.split(":");
    return new InetSocketAddress(chunks[0], Integer.parseInt(chunks[1]));
  }

  public Channel getChannelFromPool() throws ExecutionException, InterruptedException{

      ChannelPoolHandler channelPoolHandler = new ChannelPoolHandler() {
      private final AtomicInteger channelCount = new AtomicInteger(0);
      private final AtomicInteger acquiredCount = new AtomicInteger(0);
      private final AtomicInteger releasedCount = new AtomicInteger(0);

      @Override
      public void channelCreated(Channel ch) {
        ch.pipeline().addLast(new XioSecurityHandlerImpl(true).getEncryptionHandler());
        ch.pipeline().addLast(new ChicagoClientCodec());
        ch.pipeline().addLast(new ChicagoClientHandler(listener));
        channelCount.incrementAndGet();
      }

      @Override
      public void channelReleased(Channel ch) {
        releasedCount.incrementAndGet();
      }

      @Override
      public void channelAcquired(Channel ch) {
        acquiredCount.incrementAndGet();
      }
    };
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
      .option(ChannelOption.SO_REUSEADDR, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
      .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
      .option(ChannelOption.TCP_NODELAY, true);
    bootstrap.group(workerLoop)
      .channel(NioSocketChannel.class);

    bootstrap.handler(new ChannelInitializer<SocketChannel>() {

      @Override
      protected void initChannel(SocketChannel channel) throws Exception {
        System.out.println("Handler added");
        ChannelPipeline cp = channel.pipeline();
        cp.addLast(new XioSecurityHandlerImpl(true).getEncryptionHandler());
//            cp.addLast(new XioSecurityHandlerImpl(true).getAuthenticationHandler());
        //cp.addLast(new XioIdleDisconnectHandler(60, 60, 60));
        cp.addLast(new ChicagoClientCodec());
        cp.addLast(new ChicagoClientHandler(listener));
      }
    });
    bootstrap.remoteAddress(address("10.25.180.247:12000"));
    XioConnectionPool xcp = new XioConnectionPool(bootstrap, new AsyncRetryLoop(),channelPoolHandler);
    Channel ch = xcp.acquire().get();
    return ch;
  }


  public Channel getChannelFromBootstrap() {
    Channel[] ch = new Channel[1];
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
      .option(ChannelOption.SO_REUSEADDR, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
      .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
      .option(ChannelOption.TCP_NODELAY, true);
    bootstrap.group(workerLoop)
      .channel(NioSocketChannel.class)
      .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
          System.out.println("Handler added 2222");
          ChannelPipeline cp = channel.pipeline();
          cp.addLast(new XioSecurityHandlerImpl(true).getEncryptionHandler());
//            cp.addLast(new XioSecurityHandlerImpl(true).getAuthenticationHandler());
          //cp.addLast(new XioIdleDisconnectHandler(60, 60, 60));
          cp.addLast(new ChicagoClientCodec());
          cp.addLast(new ChicagoClientHandler(listener));
        }
      });

    ChannelFutureListener listener = new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) {
        if (!future.isSuccess()) {

          }else{
            System.out.println("Connected");
            ch[0] = future.channel();
          }
        }
      };

    bootstrap.connect(address("10.25.180.247:12000")).addListener(listener);
    while(ch[0] == null){
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return ch[0];

  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    XioConnectionPoolManager xpmgr = new XioConnectionPoolManager();
    Channel ch = xpmgr.getChannelFromPool();
    System.out.println(ch.pipeline().toString());

    Channel ch2 = xpmgr.getChannelFromBootstrap();
    System.out.println(ch2.pipeline().toString());

  }
}
