package com.xjeffrose.chicago.client;

import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoopFactory;
import io.netty.channel.EventLoopGroup;

import java.util.concurrent.TimeUnit;

/**
 * Created by root on 7/1/16.
 */
public class AsyncRetryLoop implements AsyncRetryLoopFactory {

  @Override
  public com.xjeffrose.xio.client.asyncretry.AsyncRetryLoop buildLoop(EventLoopGroup eventLoopGroup) {
    return new com.xjeffrose.xio.client.asyncretry.AsyncRetryLoop(4, eventLoopGroup,100, TimeUnit.MILLISECONDS);
  }
}
