package com.xjeffrose.chicago.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.xjeffrose.chicago.DefaultChicagoMessage;
import com.xjeffrose.chicago.Op;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChicagoTSClient extends BaseChicagoClient {
  private static final Logger log = LoggerFactory.getLogger(ChicagoTSClient.class);

  public ChicagoTSClient(String zkConnectionString, int quorum) throws InterruptedException {
    super(zkConnectionString, quorum);
  }

  public ChicagoTSClient(String address) throws InterruptedException{
    super(address);
  }

  public ListenableFuture<ChicagoStream> stream(byte[] key) throws ChicagoClientTimeoutException {
    return stream(key, null);
  }

  public ListenableFuture<ChicagoStream> stream(byte[] key, byte[] offset) throws ChicagoClientTimeoutException {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(exe);
    return executor.submit(() -> {
      final ChicagoStream[] cs = new ChicagoStream[1];
        final long startTime = System.currentTimeMillis();
        try {
          List<String> hashList = getEffectiveNodes(key);
          for (String node : hashList) {
            if (node == null) {
            } else {
              Channel cf = connectionPoolMgr.getNode(node);
              if (cf.isWritable()) {
                exe.execute(() -> {
//                try {
                    UUID id = UUID.randomUUID();
                    Listener listener = connectionPoolMgr.getListener(node); //Blocking
                    if(cs[0] == null) {
                      cs[0] = new ChicagoStream(listener);
                    }
                    cf.writeAndFlush(new DefaultChicagoMessage(id, Op.STREAM, key, null, offset));
                    listener.addID(id);
                    cs[0].addID(id);
                    connectionPoolMgr.releaseChannel(node,cf);
                });
              }else{
                log.info("Channel is not writeable !!!!!"+cf.toString());
              }
            }
          }

        } catch (ChicagoClientTimeoutException e) {
          Thread.currentThread().interrupt();
          log.error("Client Timeout During Read Operation:", e);
          return null;
        }

        while (cs[0] == null) {
          if (TIMEOUT_ENABLED && (System.currentTimeMillis() - startTime) > TIMEOUT) {
            Thread.currentThread().interrupt();
            throw new ChicagoClientTimeoutException();
          }
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        return cs[0];
//      }

    });

  }

  public ListenableFuture<byte[]> read(byte[] key) throws ChicagoClientTimeoutException {
    return read("chicago".getBytes(), key);
  }

  public ListenableFuture<byte[]> read(byte[] key, byte[] offset) throws ChicagoClientTimeoutException {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(exe);
    return executor.submit(() -> {
      final long startTime = System.currentTimeMillis();
      ConcurrentLinkedDeque<byte[]> responseList = new ConcurrentLinkedDeque<>();
      try {
        List<String> hashList = getEffectiveNodes(key);
        for (String node : hashList) {
          if (node == null) {
          } else {
            Channel cf = connectionPoolMgr.getNode(node);
            if (cf.isWritable()) {
              exe.execute(() -> {
                UUID id = UUID.randomUUID();
                Listener listener = connectionPoolMgr.getListener(node); //Blocking
                cf.writeAndFlush(new DefaultChicagoMessage(id, Op.STREAM, key, null, offset));
                listener.addID(id);
                exe.execute(() -> {
                  try {
                    responseList.add((byte[]) listener.getResponse(id)); //Blocking
                  } catch (ChicagoClientTimeoutException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                  }
                });
              });
            }
          }
        }

      } catch (ChicagoClientTimeoutException e) {
        Thread.currentThread().interrupt();
        log.error("Client Timeout During Read Operation:", e);
        return null;
      }


      while (responseList.isEmpty()) {
        if (TIMEOUT_ENABLED && (System.currentTimeMillis() - startTime) > TIMEOUT) {
          Thread.currentThread().interrupt();
          throw new ChicagoClientTimeoutException();
        }
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      return responseList.stream().findFirst().orElse(null);
    });
  }

  public byte[] write(byte[] key, byte[] value) throws ChicagoClientTimeoutException, ChicagoClientException {
    try {
      if (TIMEOUT_ENABLED) {
        return _write(key, value, 0).get(TIMEOUT, TimeUnit.MILLISECONDS);
      } else {
        return _write(key, value, 0).get();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (TimeoutException e) {
      throw new ChicagoClientTimeoutException();
    }

    return null;
  }

  public  ListenableFuture<byte[]> _write(byte[] key, byte[] value, int _retries) throws ChicagoClientTimeoutException, ChicagoClientException {
    return _write(null,key, value, 0);
  }

  public ListenableFuture<byte[]> _write(byte[] colFam, byte[] key, byte[] value) throws ChicagoClientTimeoutException, ChicagoClientException {
    return _write(colFam ,key, value, 0);
  }


  private ListenableFuture<byte[]> _write(byte[] colFam, byte[] key, byte[] value, int _retries) throws ChicagoClientTimeoutException, ChicagoClientException {
    final int retries = _retries;

    final ConcurrentLinkedDeque<byte[]> responseList = new ConcurrentLinkedDeque<>();
    final ConcurrentLinkedDeque<UUID> idList = new ConcurrentLinkedDeque<>();
    final ConcurrentLinkedDeque<Listener> listenerList = new ConcurrentLinkedDeque<>();

    ListeningExecutorService executor = MoreExecutors.listeningDecorator(exe);
    return executor.submit(() -> {
        int dquorum = quorum;
        final long startTime = System.currentTimeMillis();
        try {

          List<String> hashList = getEffectiveNodes(key);
          dquorum = hashList.size();
          for (String node : hashList) {
            if (node == null) {

            } else {
              Channel cf = connectionPoolMgr.getNode(node);
              if (cf.isWritable()) {
                exe.execute(() -> {
                    UUID id = UUID.randomUUID();
                    Listener listener = connectionPoolMgr.getListener(node); // Blocking
                    if(colFam != null){
                      cf.writeAndFlush(new DefaultChicagoMessage(id, Op.TS_WRITE, colFam, key, value));
                    }else {
                      cf.writeAndFlush(new DefaultChicagoMessage(id, Op.TS_WRITE, key, null, value));
                    }
                    listener.addID(id);
                    idList.add(id);
                    listenerList.add(listener);
                    exe.execute(() -> {
                        try {
                          responseList.add((byte[]) listener.getStatus(idList)); //Blocking
                        } catch (ChicagoClientTimeoutException e) {
//                          Thread.currentThread().interrupt();
                          throw new RuntimeException(e);
                        }
                    });
                  connectionPoolMgr.releaseChannel(node,cf);
                });
              }
            }
          }

        } catch (ChicagoClientTimeoutException e) {
          log.error("Client Timeout During Write Operation: ", e);
          return null;
        }


        while (responseList.size() < dquorum) {
          if (TIMEOUT_ENABLED && (System.currentTimeMillis() - startTime) > TIMEOUT) {
            Thread.currentThread().interrupt();
            throw new ChicagoClientTimeoutException();
          }
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        if (responseList.stream().allMatch(b -> b != null)) {
//          return (byte[]) listenerList.removeFirst().getResponse(idList.getFirst());
          return responseList.getFirst();
        } else {
          if (MAX_RETRY < retries) {
            if (TIMEOUT_ENABLED) {
              return _write(key, value, retries + 1).get(TIMEOUT, TimeUnit.MILLISECONDS);
            } else {
              return _write(key, value, retries + 1).get();
            }
          } else {
            throw new ChicagoClientException("Could not successfully complete a replicated write. Please retry the operation");
          }
        }
    });
  }
}
