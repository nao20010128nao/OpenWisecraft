package com.nao20010128nao.Wisecraft.services;
import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.support.v4.app.*;
import com.nao20010128nao.Wisecraft.*;
import com.nao20010128nao.Wisecraft.activity.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.provider.*;
import java.util.*;
import android.graphics.*;

public class ServerFinderService extends Service
{
	public static final String EXTRA_IP="ip";
	public static final String EXTRA_MODE="mode";
	public static final String EXTRA_START_PORT="sport";
	public static final String EXTRA_END_PORT="eport";
	
	static Map<String,State> sessions=new SuppliedHashMap<String,State>(new Supplier<String,State>(){
			public State supply(String tag){
				State stt=new State();
				stt.tag=tag;
				return stt;
			}
		},true);
	
	@Override
	public IBinder onBind(Intent p1) {
		return new InternalBinder();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String ip=intent.getStringExtra(EXTRA_IP);
		int mode=intent.getIntExtra(EXTRA_MODE,0);
		int start=intent.getIntExtra(EXTRA_START_PORT,0);
		int end=intent.getIntExtra(EXTRA_END_PORT,0);
		explore(ip,start,end,mode);
		return START_NOT_STICKY;
	}
	
	private void updateNotification(String tag,int now,int max){
		int id=tag.hashCode();
		Notification ntf=createProgressNotification(this,now,max,tag,sessions.get(tag).detected);
		NotificationManagerCompat.from(this).notify(id,ntf);
	}
	
	private static Notification createProgressNotification(Context c,int now,int max,String tag,Map<Integer,ServerStatus> servers){
		NotificationCompat.Builder ntf=new NotificationCompat.Builder(c);
		// Add title like "Server Finder - ** servers found"
		ntf.setContentTitle("Server Finder - [COUNT] servers found".replace("[COUNT]",servers.size()+""));
		ntf.setContentText("Checked [PORTS] port(s)".replace("[PORTS]",now+""));
		ntf.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		ntf.setColor(ThemePatcher.getMainColor(c));
		ntf.setProgress(max,now,false);
		if(servers.size()!=0){
			List<Integer> l=Factories.arrayList(servers.keySet());
			Collections.sort(l);
			NotificationCompat.InboxStyle bts=new NotificationCompat.InboxStyle();
			for(int port:l){
				bts.addLine(servers.get(port).toString());
			}
			ntf.setStyle(bts);
		}
		ntf.setSmallIcon(R.drawable.ic_search_black_48dp);
		ntf.setLargeIcon(BitmapFactory.decodeResource(c.getResources(),R.drawable.ic_search_black_48dp));
		ntf.setContentIntent(PendingIntent.getActivity(c,tag.hashCode()^800,new Intent(c,ServerFinderActivity.class).putExtra("tag",tag),PendingIntent.FLAG_UPDATE_CURRENT));
		ntf.setOngoing(true);
		return ntf.build();
	}
	
	private void updateNotificationFinished(String tag){
		int id=tag.hashCode();
		Notification ntf=createFinishedNotification(this,tag,sessions.get(tag).detected);
		NotificationManagerCompat.from(this).notify(id,ntf);
	}

	private static Notification createFinishedNotification(Context c,String tag,Map<Integer,ServerStatus> servers){
		NotificationCompat.Builder ntf=new NotificationCompat.Builder(c);
		// Add title like "Server Finder - ** servers found"
		ntf.setContentTitle("Server Finder - [COUNT] servers found".replace("[COUNT]",servers.size()+""));
		ntf.setContentText("Finished");
		ntf.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		ntf.setColor(ThemePatcher.getMainColor(c));
		if(servers.size()!=0){
			List<Integer> l=Factories.arrayList(servers.keySet());
			Collections.sort(l);
			NotificationCompat.InboxStyle bts=new NotificationCompat.InboxStyle();
			for(int port:l){
				bts.addLine(servers.get(port).toString());
			}
			bts.setSummaryText("Finished");
			ntf.setStyle(bts);
		}
		ntf.setSmallIcon(R.drawable.ic_search_black_48dp);
		ntf.setLargeIcon(BitmapFactory.decodeResource(c.getResources(),R.drawable.ic_search_black_48dp));
		ntf.setContentIntent(PendingIntent.getActivity(c,tag.hashCode()^800,new Intent(c,ServerFinderActivity.class).putExtra("tag",tag),PendingIntent.FLAG_UPDATE_CURRENT));
		return ntf.build();
	}
	
	private String explore(final String ip, final int startPort, final int endPort, final int mode) {
		final String tag=Utils.randomText();
		sessions.get(tag).ip=ip;
		sessions.get(tag).start=startPort;
		sessions.get(tag).end=endPort;
		sessions.get(tag).mode=mode;
		
		AsyncTask<Void,ServerStatus,Void> at=new AsyncTask<Void,ServerStatus,Void>(){
			public Void doInBackground(Void... l) {
				final int max=endPort - startPort;

				int threads=Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(ServerFinderService.this).getString("parallels", "6"));
				ServerPingProvider spp;
				if (mode==1) {
					spp = new PCMultiServerPingProvider(threads);
				} else {
					spp = new UnconnectedMultiServerPingProvider(threads);
				}
				sessions.get(tag).pinger=spp;

				for (int p=startPort;p <= endPort;p++) {
					Server s=new Server();
					s.ip = ip;
					s.port = p;
					s.mode = mode;
					spp.putInQueue(s, new ServerPingProvider.PingHandler(){
							public void onPingArrives(ServerStatus s) {
								publishProgress(s);
								update(endPort-s.port,max);
							}
							public void onPingFailed(Server s) {
								update(endPort-s.port,max);
							}
						});
				}
				return null;
			}
			public void onProgressUpdate(ServerStatus... s) {
				ServerStatus ss=s[0];
				sessions.get(tag).detected.put(ss.port,ss);
			}
			private void update(final int now,final int max) {
				int remain=sessions.get(tag).pinger.getQueueRemain();
				updateNotification(tag,max-remain,max);
				if(remain==0){
					sessions.get(tag).finished=true;
					updateNotificationFinished(tag);
				}
			}
		};
		at.execute();
		sessions.get(tag).worker=at;
		return tag;
	}
	
	public class InternalBinder extends Binder{
		public String startExploration(String ip,int mode,int start,int end){
			return explore(ip,start,end,mode);
		}
		public State getState(String tag){
			return sessions.get(tag);
		}
		public void cancel(String tag){
			getState(tag).worker.cancel(getState(tag).cancelled=true);
			getState(tag).pinger.clearAndStop();
		}
	}
	
	public static class State{
		public final Map<Integer,ServerStatus> detected=Collections.<Integer,ServerStatus>synchronizedMap(new HashMap<Integer,ServerStatus>());
		public volatile String tag,ip;
		public volatile AsyncTask<Void,ServerStatus,Void> worker;
		public volatile boolean finished=false,closed=false,cancelled=false;
		public volatile int start,end,mode;
		ServerPingProvider pinger;
	}
}
