package com.xjeffrose.chicago;

import com.google.common.hash.Funnels;
import com.xjeffrose.chicago.RendezvousHash;
import io.netty.util.internal.PlatformDependent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

import static org.junit.Assert.*;

public class RendezvousHashTest {
  @Test
  public void get() throws Exception {
    List<String> nodeList = new ArrayList<>();
    Map<String, List<String>> mm = PlatformDependent.newConcurrentHashMap();
    for (int i = 0; i < 100; i++) {
      nodeList.add(("Host" + i));
      mm.put(("Host" + i), new ArrayList<>());
    }

    RendezvousHash rendezvousHash = new RendezvousHash(Funnels.stringFunnel(Charset.defaultCharset()), nodeList, 3);
    Random r = new Random();
    for (int i = 0; i < 100000; i++) {
      String thing = (Integer.toString(r.nextInt(123456789)));
      List<String> hosts = rendezvousHash.get(thing.getBytes());
      hosts.stream().forEach(xs -> {
        mm.get(xs).add(thing);
      });
    }

    List<Integer> xx = new ArrayList<>();
    mm.keySet().stream().forEach(xs -> {
      xx.add(mm.get(xs).size());
    });

    Double xd = xx.stream().mapToInt(x -> x).average().orElse(-1);
    assertEquals(3000, xd.intValue());

  }

  @Test
  public void HashTestNode0(){
    List<String> nodeList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      nodeList.add(("Host" + i));
    }
    RendezvousHash rendezvousHash = new RendezvousHash(Funnels.stringFunnel(Charset.defaultCharset()), nodeList, 3);

    String key ="someColFam+someKey";
    List<String> nodeBefore = rendezvousHash.get(key.getBytes());

    rendezvousHash.add("Host6");

    List<String> nodeAfter = rendezvousHash.get(key.getBytes());
    assertEquals(nodeBefore.get(0),nodeAfter.get(0));
  }

}
