package com.nao20010128nao.McServerPingPong;
import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.*;
import query.*;
import android.view.*;

public class ServerListActivity extends ListActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setListAdapter(new ServerList());
	}
	class ServerList extends ArrayAdapter<ServerStatus>{
		public ServerList(){
			super(ServerListActivity.this,0,new ArrayList<ServerStatus>());
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO: Implement this method
			View layout=getLayoutInflater().inflate(R.layout.quickstatus,null,false);
			layout.setTag(getItem(position));
			return layout;
		}
	}
	public static class Server{
		String ip;
		int port;
	}
	public static class ServerStatus extends Server{
		QueryResponseUniverse response;
	}
}
