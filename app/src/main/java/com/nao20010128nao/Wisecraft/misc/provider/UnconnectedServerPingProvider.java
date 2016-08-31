package com.nao20010128nao.Wisecraft.misc.provider;
import android.util.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.pinger.pe.*;
import java.io.*;
import java.util.*;
public class UnconnectedServerPingProvider implements ServerPingProvider
{
	Queue<Map.Entry<Server,PingHandler>> queue=new LinkedList<>();
	Thread pingThread=new PingThread();
    boolean offline=false;
	
	public void putInQueue(Server server, PingHandler handler) {
		Utils.requireNonNull(server);
		Utils.requireNonNull(handler);
		queue.add(new KVP<Server,PingHandler>(server, handler));
		if (!pingThread.isAlive()) {
			pingThread = new PingThread();
			pingThread.start();
		}
	}
	@Override
	public int getQueueRemain() {
		// TODO: Implement this method
		return queue.size();
	}

	@Override
	public void stop() {
		// TODO: Implement this method
		pingThread.interrupt();
	}

	@Override
	public void clearQueue() {
		// TODO: Implement this method
		queue.clear();
	}

    @Override
    public void offline() {
        // TODO: Implement this method
        offline=true;
    }

    @Override
    public void online() {
        // TODO: Implement this method
        offline=false;
    }

	private class PingThread extends Thread implements Runnable {
		@Override
		public void run() {
			// TODO: Implement this method
			Map.Entry<Server,PingHandler> now=null;
			while (!(queue.isEmpty()|isInterrupted())) {
				try {
					Log.d("UPP", "Starting ping");
					now = queue.poll();
                    if(offline){
                        Log.d("UPP", "Offline");
                        try {
                            now.getValue().onPingFailed(now.getKey());
                        } catch (Throwable ex_) {

                        }
                        continue;
                    }
					ServerStatus stat=new ServerStatus();
					stat.ip = now.getKey().ip;
					stat.port = now.getKey().port;
					stat.mode = now.getKey().mode;
					Log.d("UPP", stat.ip + ":" + stat.port + " " + stat.mode);
					switch(now.getKey().mode){
						case 0:
							try {
								UnconnectedPing.UnconnectedPingResult res=UnconnectedPing.doPing(stat.ip, stat.port);
								stat.response = res;
								stat.ping=res.getLatestPingElapsed();
								Log.d("UPP", "Success: Unconnected Ping");
							} catch (IOException e) {
								Log.d("UPP", "Failed");
								now.getValue().onPingFailed(now.getKey());
								continue;
							}
							break;
						case 1:
							try{
								now.getValue().onPingFailed(now.getKey());
							}catch(Throwable h){

							}
							continue;
					}
					try {
                        now.getValue().onPingArrives(stat);
                    } catch (Throwable f) {

                    }
				} catch (Throwable e) {
					e.printStackTrace();
				}
				Log.d("UPP", "Next");
			}
		}
	}
}