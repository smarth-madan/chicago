package com.xjeffrose.chicago.client;

import com.xjeffrose.chicago.ZkClient;
import com.xjeffrose.xio.SSL.XioSecurityHandlerImpl;
import com.xjeffrose.xio.client.XioConnectionPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by root on 7/1/16.
 */
public class ConnectionPoolManager2 {
  private static final Logger log = LoggerFactory.getLogger(ConnectionPoolManager2.class);
  private final static String NODE_LIST_PATH = "/chicago/node-list";
  private static final long TIMEOUT = 1000;
  private static boolean TIMEOUT_ENABLED = false;
  private final Map<String, Listener> listenerMap = new ConcurrentHashMap<>();
  private final Map<String, XioConnectionPool> connectionPoolMap = new ConcurrentHashMap<>();
  private final NioEventLoopGroup workerLoop = new NioEventLoopGroup(20);
  private final ZkClient zkClient;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public ConnectionPoolManager2(ZkClient zkClient) {
    this.zkClient = zkClient;
  }

  public ConnectionPoolManager2(String hostname) {
    this.zkClient = null;
    listenerMap.put(hostname, new ChicagoListener());
    connect(hostname, listenerMap.get(hostname));
  }

  public void start() {
    running.set(true);
    refreshPool();
  }

  public void stop() {
    log.info("ConnectionPoolManager stopping");
    running.set(false);
    log.info("Stopping workerLoop");
    workerLoop.shutdownGracefully();
  }

  private List<String> buildNodeList() {
    return zkClient.list(NODE_LIST_PATH);
  }

  private InetSocketAddress address(String node) {
    String chunks[] = node.split(":");
    return new InetSocketAddress(chunks[0], Integer.parseInt(chunks[1]));
  }

  private void refreshPool() {
    buildNodeList().stream().forEach(xs -> {
      listenerMap.put(xs, new ChicagoListener());
      connect(xs, listenerMap.get(xs));
    });
  }

  public Channel getNode(String node) throws ChicagoClientTimeoutException {
    log.debug("Trying to get node:"+node);
    return _getNode(node);
  }

  private Channel _getNode(String node) throws ChicagoClientTimeoutException {
    Channel ch = null;
    long start = System.currentTimeMillis();
    try {
      Future<Channel> fch = connectionPoolMap.get(node).acquire();
      ch = fch.syncUninterruptibly().getNow();
    }catch (Exception e){
      e.printStackTrace();
      throw new ChicagoClientTimeoutException();
    }
    System.out.println("Time to get channel " + ch.toString() + " " + (System.currentTimeMillis() - start)+ "ms");
    return ch;
  }

  public void releaseChannel(String address,Channel ch){
      connectionPoolMap.get(address).release(ch);
  }

  public Listener getListener(String node) {
    return listenerMap.get(node);
  }

  private void connect(String address, Listener listener) {
    // Start the connection attempt.
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
      .option(ChannelOption.SO_REUSEADDR, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
      .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
      .option(ChannelOption.TCP_NODELAY, true);
    bootstrap.group(workerLoop)
      .channel(NioSocketChannel.class);


    bootstrap.remoteAddress(address(address));
    XioConnectionPool sp = new XioConnectionPool(bootstrap, new AsyncRetryLoop(), new ChicagoChannelPoolHandler(listener));
    connectionPoolMap.put(address, sp);
  }

}
