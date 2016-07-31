package com.nao20010128nao.Wisecraft.misc;
import android.content.*;
import android.os.*;
import com.google.gson.*;
import com.nao20010128nao.Wisecraft.*;
import java.util.*;

//Server Sort Part
public abstract class ServerListActivityBase2 extends ServerListActivityBase3
{
	public void doSort(final List<Server> sl,final SortKind sk){
		new Thread(){
			public void run(){
				final List<Server> sortingServer=sk.doSort(sl);
				runOnUiThread(new Runnable(){
						public void run() {
							finish();
							ServerListActivity.Content.deleteRef();
							new Handler().postDelayed(new Runnable(){
									public void run() {
										pref.edit().putString("servers", new Gson().toJson(sortingServer.toArray(new Server[sortingServer.size()]), Server[].class)).commit();
										startActivity(new Intent(ServerListActivityBase2.this, ServerListActivity.class));
									}
								}, 10);
						}
					});
			}
		}.start();
	}
	
	public static enum SortKind{
		BRING_ONLINE_SERVERS_TO_TOP{
			public List<Server> doSort(List<Server> list){
				List<Server> backup,online,offline;
				backup=new ArrayList<>(list);
				online=new ArrayList<>();
				offline=new ArrayList<>(backup);
				for(Server s:list)if(s instanceof ServerStatus)online.add(s);
				offline.removeAll(online);
				backup.clear();
				backup.addAll(online);
				backup.addAll(offline);
				return backup;
			}
		},
		IP_AND_PORT{
			public List<Server> doSort(List<Server> list){
				List<Server> l=new ArrayList<>(list);
				Collections.sort(l,ServerSorter.INSTANCE);
				return l;
			}
		},
		ONLINE_AND_OFFLINE{
			public List<Server> doSort(List<Server> list){
				List<Server> backup,online,offline;
				backup=new ArrayList<>(list);
				online=new ArrayList<>();
				offline=new ArrayList<>(backup);
				for(Server s:list)if(s instanceof ServerStatus)online.add(s);
				offline.removeAll(online);
				online=IP_AND_PORT.doSort(online);
				offline=IP_AND_PORT.doSort(offline);
				backup.clear();
				backup.addAll(online);
				backup.addAll(offline);
				return backup;
			}
		};
		public abstract List<Server> doSort(List<Server> list);
	}
}
