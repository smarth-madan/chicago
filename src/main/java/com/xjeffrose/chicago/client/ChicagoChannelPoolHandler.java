package com.xjeffrose.chicago.client;

import com.xjeffrose.xio.SSL.XioSecurityHandlerImpl;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;

/**
 * Created by root on 7/5/16.
 */
public class ChicagoChannelPoolHandler implements ChannelPoolHandler{
  private final Listener listener;

  ChicagoChannelPoolHandler(Listener listener){
    this.listener = listener;
  }

  @Override
  public void channelReleased(Channel channel) throws Exception {

  }

  @Override
  public void channelAcquired(Channel channel) throws Exception {

  }

  @Override
  public void channelCreated(Channel channel) throws Exception {
    channel.pipeline().addLast(new XioSecurityHandlerImpl(true).getEncryptionHandler());
    channel.pipeline().addLast(new ChicagoClientCodec());
    channel.pipeline().addLast(new ChicagoClientHandler(this.listener));
  }
}
