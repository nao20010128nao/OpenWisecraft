package com.nao20010128nao.Wisecraft;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.preference.*;
import android.support.design.widget.*;
import android.support.v4.view.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.compat.*;
import com.nao20010128nao.Wisecraft.misc.pinger.pc.*;
import com.nao20010128nao.Wisecraft.pingEngine.*;
import com.nao20010128nao.Wisecraft.provider.*;
import java.util.*;
import uk.co.chrisjenx.calligraphy.*;
import java.lang.ref.WeakReference;

import static com.nao20010128nao.Wisecraft.misc.Utils.*;
class ServerFinderActivityImpl extends AppCompatListActivity implements ServerListActivityInterface{
	ServerList sl;
	List<ServerStatus> list;
	String ip;
	int mode;
	View dialog,dialog2;
	ServerPingProvider spp;
	SharedPreferences pref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("useBright",false)){
			setTheme(R.style.AppTheme_Bright);
			getTheme().applyStyle(R.style.AppTheme_Bright,true);
		}
		super.onCreate(savedInstanceState);
		setListAdapter(sl = new ServerList());
		getListView().setOnItemClickListener(sl);
		ip = getIntent().getStringExtra("ip");
		mode = getIntent().getIntExtra("mode", 0);
		new AppCompatAlertDialog.Builder(this,R.style.AppAlertDialog)
			.setTitle(R.string.serverFinder)
			.setView(dialog = getLayoutInflater().inflate(R.layout.server_finder_start, null, false))
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int w) {
					String ip=((EditText)dialog.findViewById(R.id.ip)).getText().toString();
					int startPort=new Integer(((EditText)dialog.findViewById(R.id.startPort)).getText().toString());
					int endPort=new Integer(((EditText)dialog.findViewById(R.id.endPort)).getText().toString());
					boolean isPC=((CheckBox)dialog.findViewById(R.id.pc)).isChecked();
					startFinding(ip, startPort, endPort, isPC);
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int w) {
					di.dismiss();
					finish();
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener(){
				public void onCancel(DialogInterface di) {
					finish();
				}
			})
			.show();
		if (ip != null)((EditText)dialog.findViewById(R.id.ip)).setText(ip);
		((CheckBox)dialog.findViewById(R.id.pc)).setChecked(mode==0?false:true);
		
		if (pref.getBoolean("colorFormattedText", false) & pref.getBoolean("darkBackgroundForServerName", false)) {
			BitmapDrawable bd=(BitmapDrawable)getResources().getDrawable(R.drawable.soil);
			bd.setTargetDensity(getResources().getDisplayMetrics());
			bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
			getListView().setBackground(bd);
		}
	}
	private void startFinding(final String ip, final int startPort, final int endPort, final boolean isPC) {
		DisplayMetrics dm=getResources().getDisplayMetrics();
		final PopupWindow pw=new PopupWindow(this);
		pw.setTouchable(false);
		pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		pw.setContentView(dialog2 = getLayoutInflater().inflate(R.layout.server_finder_finding, null, false));
		pw.setWidth(Math.min(dm.widthPixels, dm.heightPixels));
		pw.setHeight(getResources().getDimensionPixelSize(R.dimen.server_finder_finding_height));
		
		ViewCompat.setAlpha(dialog2,0.7f);
		
		pw.showAtLocation(getWindow().getDecorView().findViewById(android.R.id.content),Gravity.CENTER,0,0);
		;
		new AsyncTask<Void,ServerStatus,Void>(){
			public Void doInBackground(Void... l) {
				final int max=endPort - startPort;
				
				int threads=new Integer(PreferenceManager.getDefaultSharedPreferences(ServerFinderActivityImpl.this).getString("parallels", "6"));
				if (isPC) {
					spp = new PCMultiServerPingProvider(threads);
				} else {
					spp = new UnconnectedMultiServerPingProvider(threads);
				}

				for (int p=startPort;p < endPort;p++) {
					Server s=new Server();
					s.ip = ip;
					s.port = p;
					s.mode = isPC?1:0;
					spp.putInQueue(s, new ServerPingProvider.PingHandler(){
							public void onPingArrives(ServerStatus s) {
								publishProgress(s);
								update(max);
							}
							public void onPingFailed(Server s) {
								update(max);
							}
						});
				}
				return null;
			}
			public void onProgressUpdate(ServerStatus... s) {
				sl.addAll(s);
			}
			private void update(final int max) {
				runOnUiThread(new Runnable(){
						public void run() {
							((ProgressBar)dialog2.findViewById(R.id.perc)).setMax(max);
							((ProgressBar)dialog2.findViewById(R.id.perc)).setProgress(((ProgressBar)dialog2.findViewById(R.id.perc)).getProgress() + 1);
							((TextView)dialog2.findViewById(R.id.status)).setText(((ProgressBar)dialog2.findViewById(R.id.perc)).getProgress() + "/" + max);
							if (((ProgressBar)dialog2.findViewById(R.id.perc)).getProgress() == max) {
								pw.dismiss();
								Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content),getResources().getString(R.string.foundServersCount).replace("[NUMBER]",""+sl.getCount()),Snackbar.LENGTH_SHORT).show();
							}
						}
					});
			}
		}.execute();
	}
	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
	}

	@Override
	protected void onDestroy() {
		// TODO: Implement this method
		super.onDestroy();
		if(spp!=null){
			spp.clearQueue();
			spp.stop();
		}
	}

	@Override
	public void addIntoList(Server s) {
		// TODO: Implement this method
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO: Implement this method
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	class ServerList extends AppBaseArrayAdapter<ServerStatus> implements AdapterView.OnItemClickListener {
		List<View> cached=new ArrayList<>();
		public ServerList() {
			super(ServerFinderActivityImpl.this, 0, list = new ArrayList<ServerStatus>());
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			// TODO: Implement this method
			if (cached.size() > position) {
				View v=cached.get(position);
				if (v != null) {
					return v;
				}
			}
			//if(convertView!=null)return convertView;
			final View layout;
			if (pref.getBoolean("colorFormattedText", false)) {
				if (pref.getBoolean("darkBackgroundForServerName", false)) {
					layout = getLayoutInflater().inflate(R.layout.quickstatus_dark, null, false);
				} else {
					layout = getLayoutInflater().inflate(R.layout.quickstatus, null, false);
				}
			} else {
				layout = getLayoutInflater().inflate(R.layout.quickstatus, null, false);
			}
			layout.findViewById(R.id.serverPlayers).setVisibility(View.GONE);
			ServerStatus s=getItem(position);
			layout.setTag(s);
			((ImageView)layout.findViewById(R.id.statColor)).setImageDrawable(new ColorDrawable(getResources().getColor(R.color.stat_ok)));

			final String title;
			if (s.response instanceof Reply19) {//PC 1.9~
				Reply19 rep=(Reply19)s.response;
				if (rep.description == null) {
					title = s.ip + ":" + s.port;
				} else {
					title = rep.description.text;
				}
			} else if (s.response instanceof Reply) {//PC
				Reply rep=(Reply)s.response;
				if (rep.description == null) {
					title = s.ip + ":" + s.port;
				} else {
					title = rep.description;
				}
			} else if (s.response instanceof UnconnectedPing.UnconnectedPingResult) {
				title = ((UnconnectedPing.UnconnectedPingResult)s.response).getServerName();
			} else {//Unreachable
				title = s.ip + ":" + s.port;
			}
			if (pref.getBoolean("colorFormattedText", false)) {
				if (pref.getBoolean("darkBackgroundForServerName", false)) {
					((TextView)layout.findViewById(R.id.serverName)).setText(parseMinecraftFormattingCodeForDark(title));
				} else {
					((TextView)layout.findViewById(R.id.serverName)).setText(parseMinecraftFormattingCode(title));
				}
			} else {
				((TextView)layout.findViewById(R.id.serverName)).setText(deleteDecorations(title));
			}
			((TextView)layout.findViewById(R.id.pingMillis)).setText(s.ping + " ms");
			((TextView)layout.findViewById(R.id.serverAddress)).setText(s.port + "");

			list.set(position, s);

			if (cached.size() <= position) {
				cached.addAll(Constant.ONE_HUNDRED_LENGTH_NULL_LIST);
			}
			cached.set(position, layout);
			return layout;
		}
		public View getCachedView(int position) {
			return cached.get(position);
		}
		public View getViewQuick(int pos) {
			return getView(pos, null, null);
		}
		@Override
		public void onItemClick(AdapterView<?> p1, View p2, final int p3, long p4) {
			// TODO: Implement this method
			final Server s=getItem(p3);
			if (s instanceof ServerStatus) {
				new AppCompatAlertDialog.Builder(ServerFinderActivityImpl.this)
					.setTitle(s.toString())
					.setItems(R.array.serverFinderMenu,new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface di,int w){
							switch(w){
								case 0:
									ServerListActivityImpl.instance.get().sl.add(s);
									break;
							}
						}
					})
					.show();
			}
		}

		@Override
		public void remove(ServerStatus object) {
			// TODO: Implement this method
			cached.remove(list.indexOf(object));
			super.remove(object);
		}
	}
}
public class ServerFinderActivity extends CompatActivityGroup {
	public static WeakReference<ServerFinderActivity> instance=new WeakReference(null);

	boolean nonLoop=false;
	SharedPreferences pref;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		instance = new WeakReference(this);
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		if(pref.getBoolean("useBright",false)){
			setTheme(R.style.AppTheme_Bright);
			getTheme().applyStyle(R.style.AppTheme_Bright,true);
		}
		super.onCreate(savedInstanceState);
		getSupportActionBar().hide();
		if(pref.getBoolean("useOldActivity",false))
			setContentView(getLocalActivityManager().startActivity("main", new Intent(this, Content$Old.class).putExtras(getIntent())).getDecorView());
		else
			setContentView(getLocalActivityManager().startActivity("main", new Intent(this, Content.class).putExtras(getIntent())).getDecorView());
	}
	public static class Content extends ServerFinderActivityImpl {public static void deleteRef(){instance=new WeakReference<>(null);}}
	public static class Content$Old extends com.nao20010128nao.Wisecraft.old.ServerFinderActivity {public static void deleteRef(){instance=new WeakReference<>(null);}}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO: Implement this method
		if (nonLoop)
			return true;
		nonLoop = true;
		boolean val= getLocalActivityManager().getActivity("main").onCreateOptionsMenu(menu);
		nonLoop = false;
		return val;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO: Implement this method
		if (nonLoop)
			return true;
		nonLoop = true;
		boolean val= getLocalActivityManager().getActivity("main").onOptionsItemSelected(item);
		nonLoop = false;
		return val;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO: Implement this method
		((ActivityResultInterface)getLocalActivityManager().getActivity("main")).onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
	}
}
