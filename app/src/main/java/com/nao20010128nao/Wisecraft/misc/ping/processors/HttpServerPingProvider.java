package com.nao20010128nao.Wisecraft.misc.ping.processors;

import android.text.*;
import android.util.*;
import com.nao20010128nao.Wisecraft.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.ping.methods.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class HttpServerPingProvider implements ServerPingProvider {
    String head;
    boolean offline;
    Queue<Map.Entry<Server, PingHandler>> queue = Factories.newDefaultQueue();
    Thread pingThread = new PingThread();

    public HttpServerPingProvider(String host) {
        if (TextUtils.isEmpty(host)) throw new IllegalArgumentException("host");
        if (host.startsWith("http://") | host.startsWith("https://")) {
            head = host;
        } else {
            head = "http://" + host;
        }
        if (!head.endsWith("/")) head += "/";
    }

    public void putInQueue(Server server, PingHandler handler) {
        Utils.requireNonNull(server);
        Utils.requireNonNull(handler);
        Utils.prepareLooper();
        queue.add(new KVP<>(server, handler));
        if (!pingThread.isAlive()) {
            pingThread = new PingThread();
            pingThread.start();
        }
    }

    @Override
    public int getQueueRemain() {
        return queue.size();
    }

    @Override
    public void stop() {
        pingThread.interrupt();
    }

    @Override
    public void clearQueue() {
        queue.clear();
    }

    @Override
    public void clearAndStop() {
        clearQueue();
        stop();
    }

    @Override
    public void offline() {
        offline = true;
    }

    @Override
    public void online() {
        offline = false;
    }

    @Override
    public String getClassName() {
        return "HttpServerPingProvider";
    }

    private class PingThread extends Thread implements Runnable {
        @Override
        public void run() {
            final String TAG = ProcessorUtils.getLogTag(HttpServerPingProvider.this);

            Map.Entry<Server, PingHandler> now = null;
            while (!(queue.isEmpty() | isInterrupted())) {
                Log.d(TAG, "Starting ping");
                try {
                    now = queue.poll();
                    if (offline) {
                        Log.d(TAG, "Offline");
                        try {
                            now.getValue().onPingFailed(now.getKey());
                        } catch (Throwable ex_) {

                        }
                        continue;
                    }
                    try {
                        ServerStatus stat = null;
                        Server s = now.getKey();
                        InputStream is = null;
                        try {
                            is = new URL(head + "ping?ip=" + s.ip + "&port=" + s.port + "&mode=" + s.mode).openConnection().getInputStream();
                            stat = PingSerializeProvider.loadFromServerDumpFile(is);
                        } finally {
                            if (is != null) is.close();
                        }
                        try {
                            now.getValue().onPingArrives(stat);
                        } catch (Throwable f) {

                        }
                    } catch (Throwable e) {
                        WisecraftError.report(TAG, e);
                        try {
                            now.getValue().onPingFailed(now.getKey());
                        } catch (Throwable ex_) {

                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Next");
            }
        }
    }
}
