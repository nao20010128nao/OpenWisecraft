package com.nao20010128nao.Wisecraft.old;
import android.app.ActivityGroup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.nao20010128nao.ToolBox.HandledPreference;
import com.nao20010128nao.Wisecraft.R;
import com.nao20010128nao.Wisecraft.ServerFinderActivity;
import com.nao20010128nao.Wisecraft.ServerGetActivity;
import com.nao20010128nao.Wisecraft.ServerInfoActivity;
import com.nao20010128nao.Wisecraft.ServerTestActivity;
import com.nao20010128nao.Wisecraft.TheApplication;
import com.nao20010128nao.Wisecraft.collector.CollectorMain;
import com.nao20010128nao.Wisecraft.misc.AppBaseArrayAdapter;
import com.nao20010128nao.Wisecraft.misc.Constant;
import com.nao20010128nao.Wisecraft.misc.DebugWriter;
import com.nao20010128nao.Wisecraft.misc.Factories;
import com.nao20010128nao.Wisecraft.misc.NonNullableMap;
import com.nao20010128nao.Wisecraft.misc.OldServer19;
import com.nao20010128nao.Wisecraft.misc.Server;
import com.nao20010128nao.Wisecraft.misc.ServerListActivityBase2.SortKind;
import com.nao20010128nao.Wisecraft.misc.ServerListActivityInterface;
import com.nao20010128nao.Wisecraft.misc.ServerListArrayList;
import com.nao20010128nao.Wisecraft.misc.ServerStatus;
import com.nao20010128nao.Wisecraft.misc.SlsUpdater;
import com.nao20010128nao.Wisecraft.misc.SprPair;
import com.nao20010128nao.Wisecraft.misc.Utils;
import com.nao20010128nao.Wisecraft.misc.WorkingDialog;
import com.nao20010128nao.Wisecraft.misc.compat.AppCompatAlertDialog;
import com.nao20010128nao.Wisecraft.misc.compat.AppCompatListActivity;
import com.nao20010128nao.Wisecraft.misc.contextwrappers.extender.ContextWrappingExtender;
import com.nao20010128nao.Wisecraft.misc.pinger.pc.Reply;
import com.nao20010128nao.Wisecraft.misc.pinger.pc.Reply19;
import com.nao20010128nao.Wisecraft.misc.pinger.pe.FullStat;
import com.nao20010128nao.Wisecraft.misc.pref.StartPref;
import com.nao20010128nao.Wisecraft.misc.server.GhostPingServer;
import com.nao20010128nao.Wisecraft.misc.view.ExtendedImageView;
import com.nao20010128nao.Wisecraft.misc.view.StatusesLayout;
import com.nao20010128nao.Wisecraft.pingEngine.UnconnectedPing;
import com.nao20010128nao.Wisecraft.provider.MultiServerPingProvider;
import com.nao20010128nao.Wisecraft.provider.NormalServerPingProvider;
import com.nao20010128nao.Wisecraft.provider.ServerPingProvider;
import com.nao20010128nao.Wisecraft.proxy.ProxyActivity;
import com.nao20010128nao.Wisecraft.rcon.RCONActivity;
import com.nao20010128nao.Wisecraft.services.SlsUpdaterService;
import com.nao20010128nao.Wisecraft.settings.SettingsDelegate;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.nao20010128nao.Wisecraft.misc.Utils.*;


abstract class ServerListActivityImpl extends ServerListActivityBase1 implements ServerListActivityInterface {
	public static WeakReference<ServerListActivityImpl> instance=new WeakReference(null);

	static final File mcpeServerList=new File(Environment.getExternalStorageDirectory(), "/games/com.mojang/minecraftpe/external_servers.txt");

	final List<String> appMenu=new ArrayList<>();
	ServerPingProvider spp,updater;
	Gson gson=new Gson();
	SharedPreferences pref;
	ServerList sl;
	List<Server> list;
	int clicked=-1;
	WorkingDialog wd;
	SwipeRefreshLayout srl;
	List<MenuItem> items=new ArrayList<>();
	DrawerLayout dl;
	boolean drawerOpened;
	Snackbar networkState;
	NetworkStateBroadcastReceiver nsbr;
	boolean skipSave=false;
	StatusesLayout statLayout;
	Map<Server,Boolean> pinging=new NonNullableMap<Server>();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		getLayoutInflater().inflate(R.layout.hacks, null);//空インフレート
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		appMenu.add(getResources().getString(R.string.add));//0
		appMenu.add(getResources().getString(R.string.addFromMCPE));//1
		appMenu.add(getResources().getString(R.string.update_all));//2
		appMenu.add(getResources().getString(R.string.export));//3
		appMenu.add(getResources().getString(R.string.imporT));//4
		appMenu.add(getResources().getString(R.string.sort));//5
		appMenu.add(getResources().getString(R.string.serverFinder));//6
		appMenu.add(getResources().getString(R.string.addServerFromServerListSite));//7
		appMenu.add(getResources().getString(R.string.settings));//8
		appMenu.add(getResources().getString(R.string.exit));//9

		switch (pref.getInt("main_style", 0)) {
			case 0:
				setContentView(R.layout.server_list_content_nodrawer_old);
				break;
			case 1:
				setContentView(R.layout.server_list_content_old);
				LinearLayout ll=(LinearLayout)findViewById(R.id.app_menu);
				for (String s:appMenu) {
					if (appMenu.indexOf(s) == 5 & !pref.getBoolean("feature_bott", true)) {
						continue;
					}
					if (appMenu.indexOf(s) == 6 & !pref.getBoolean("feature_serverFinder", false)) {
						continue;
					}
					if (appMenu.indexOf(s) == 7 & !pref.getBoolean("feature_asfsls", false)) {
						continue;
					}
					Button btn=(Button)getLayoutInflater().inflate(R.layout.server_list_bar_button, null).findViewById(R.id.menu_btn);
					//((ViewGroup)btn.getParent()).removeView(btn);
					btn.setText(s);
					btn.setOnClickListener(new MenuExecClickListener(appMenu.indexOf(s)));
					ll.addView(btn);
				}
				
				setupDrawer();
				break;
			case 2:
				setContentView(R.layout.server_list_content_listview_old);
				LinearLayout lv=(LinearLayout)findViewById(R.id.app_menu);
				ArrayList<String> editing=new ArrayList<>(appMenu);
				if (!pref.getBoolean("feature_bott", true))
					editing.remove(appMenu.get(5));
				if (!pref.getBoolean("feature_serverFinder", false))
					editing.remove(appMenu.get(6));
				if (!pref.getBoolean("feature_asfsls", false))
					editing.remove(appMenu.get(7));
				lv.addView(((ActivityGroup)getParent()).getLocalActivityManager().startActivity("menu", new Intent(this, MenuPreferenceActivity.class).putExtra("values", editing)).getDecorView());

				setupDrawer();
				break;
		}
		srl = (SwipeRefreshLayout)findViewById(R.id.swipelayout);
		srl.setColorSchemeResources(R.color.upd_1, R.color.upd_2, R.color.upd_3, R.color.upd_4);
		srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
				public void onRefresh() {
					execOption(2);
				}
			});
		statLayout=(StatusesLayout)findViewById(R.id.serverStatuses);
		statLayout.setColorRes(R.color.stat_error,R.color.stat_pending,R.color.stat_ok);
		statLayout.initStatuses(0,0);
		if(!pref.getBoolean("showStatusesBar",false))statLayout.setVisibility(View.GONE);
		boolean usesOldInstance=false;
		if (instance.get() != null) {
			list = instance.get().list;
			sl = instance.get().sl;
			pinging = instance.get().pinging;
			spp = instance.get().spp;
			updater = instance.get().updater;
			clicked = instance.get().clicked;
			statLayout.setStatuses(instance.get().statLayout.getStatuses());
			instance.get().statLayout=statLayout;
			usesOldInstance = true;

			sl.attachNewActivity(this);
		}
		instance = new WeakReference(this);
		if (usesOldInstance) {
			setListAdapter(sl);
		} else {
			spp = updater = new MultiServerPingProvider(Integer.parseInt(pref.getString("parallels", "6")));
			if (pref.getBoolean("updAnotherThread", false))
				updater = new NormalServerPingProvider();
			setListAdapter(sl = new ServerList(this));
		}
		getListView().setOnItemClickListener(sl);
		getListView().setOnItemLongClickListener(sl);
		getListView().setLongClickable(true);
		wd = new WorkingDialog(this);
		if (!usesOldInstance)loadServers();
		for (int i=0;i < list.size();i++)
			sl.getViewQuick(i);
		if (pref.getBoolean("colorFormattedText", false) & pref.getBoolean("darkBackgroundForServerName", false)) {
			BitmapDrawable bd=(BitmapDrawable)getResources().getDrawable(R.drawable.soil);
			bd.setTargetDensity(getResources().getDisplayMetrics());
			bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
			getListView().setBackground(bd);
		}
	}

	private void setupDrawer() {
		dl = (DrawerLayout)findViewById(R.id.drawer);
		dl.setDrawerListener(new OpenCloseListener());

		if (pref.getBoolean("specialDrawer1", false)) {
			ViewGroup decor=(ViewGroup)getWindow().getDecorView();
			View decorChild=decor.getChildAt(0);
			View dChild=dl.getChildAt(0);
			ViewGroup content=(ViewGroup)dl.getParent();

			dl.removeView(dChild);
			decor.removeView(decorChild);
			content.removeView(dl);

			content.addView(dChild);
			decor.addView(dl);
			dl.addView(decorChild, 0);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		super.onPostCreate(savedInstanceState);

		networkState = Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content), "", Snackbar.LENGTH_INDEFINITE);
		ViewCompat.setAlpha(networkState.getView(), 0.7f);
		networkState.getView().setClickable(false);
		new NetworkStatusCheckWorker().execute();
		IntentFilter inFil=new IntentFilter();
		inFil.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(nsbr = new NetworkStateBroadcastReceiver(), inFil);
		////////////
		new Thread(){
			String replyAction;
			ServerSocket ss=null;
			public void run() {
				TheApplication.instance.stolenInfos=getSharedPreferences("majeste",MODE_PRIVATE);
				try {
					ss = new ServerSocket(35590);//bind to this port to start a critical session
					replyAction = Utils.randomText();
					IntentFilter infi=new IntentFilter();
					infi.addAction(replyAction);
					registerReceiver(new BroadcastReceiver(){
							@Override
							public void onReceive(Context p1, Intent p2) {
								// TODO: Implement this method
								Log.d("slsupd", "received");
								SlsUpdater.loadCurrentCode(p1);
								Log.d("slsupd", "loaded");
								try {
									if (ss != null)ss.close();
								} catch (IOException e) {}
							}
						}, infi);
					startService(new Intent(ServerListActivityImpl.this, SlsUpdaterService.class).putExtra("action", replyAction));
				} catch (IOException se) {

				}
			}
		}.start();
		new GhostPingServer().start();
		pref.edit().putString("previousVersion", Utils.getVersionName(this)).putInt("previousVersionInt", Utils.getVersionCode(this)).commit();
		new Thread(){
			public void run() {
				int launched;
				pref.edit().putInt("launched", (launched = pref.getInt("launched", 0)) + 1).commit();
				if (launched > 30)
					pref.edit().putBoolean("sendInfos_force", true).commit();
			}
		}.start();
	}
	
	@Override
	protected void attachBaseContext(Context newBase) {
		TheApplication.instance.initForActivities();
		super.attachBaseContext(ContextWrappingExtender.wrap(CalligraphyContextWrapper.wrap(newBase)));
	}
	@Override
	protected void onDestroy() {
		// TODO: Implement this method
		super.onDestroy();
		if (!skipSave)saveServers();
		unregisterReceiver(nsbr);
	}

	@Override
	public void onBackPressed() {
		// TODO: Implement this method
		if (dl == null)
			super.onBackPressed();
		else
			if (drawerOpened)
				dl.closeDrawers();
			else
				super.onBackPressed();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO: Implement this method
		if(dispatchActivityResult(requestCode,resultCode,data))return;
		switch (requestCode) {
			case 0:
				switch (resultCode) {
					case Constant.ACTIVITY_RESULT_UPDATE:
						Bundle obj=data.getBundleExtra("object");
						updater.putInQueue(list.get(clicked), new PingHandlerImpl(true, data.getIntExtra("offset", 0), true));
						((TextView)sl.getViewQuick(clicked).findViewById(R.id.pingMillis)).setText(R.string.working);
						((ExtendedImageView)sl.getViewQuick(clicked).findViewById(R.id.statColor)).setColor(getResources().getColor(R.color.stat_pending));
						((TextView)sl.getViewQuick(clicked).findViewById(R.id.serverName)).setText(R.string.working);
						((TextView)sl.getViewQuick(clicked).findViewById(R.id.serverPlayers)).setText("-/-");
						wd.showWorkingDialog();
						pinging.put(list.get(clicked), true);
						statLayout.setStatusAt(clicked, 1);
						break;
				}
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO: Implement this method
		if (pref.getInt("main_style", 0) == 0) {
			for (String s:appMenu) {
				if (appMenu.indexOf(s) == 5 & !pref.getBoolean("feature_bott", true))
					continue;
				if (appMenu.indexOf(s) == 6 & !pref.getBoolean("feature_serverFinder", false))
					continue;
				if (appMenu.indexOf(s) == 7 & !pref.getBoolean("feature_asfsls", false))
					continue;
				menu.add(Menu.NONE, appMenu.indexOf(s), appMenu.indexOf(s), s);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return execOption(item.getItemId());
	}

	public boolean execOption(int item) {
		// TODO: Implement this method
		if (dl != null)dl.closeDrawers();
		switch (item) {
			case 0:
				View dialog=getLayoutInflater().inflate(R.layout.server_add_dialog_new, null);
				final LinearLayout peFrame=(LinearLayout)dialog.findViewById(R.id.pe);
				final LinearLayout pcFrame=(LinearLayout)dialog.findViewById(R.id.pc);
				final EditText pe_ip=(EditText)dialog.findViewById(R.id.pe).findViewById(R.id.serverIp);
				final EditText pe_port=(EditText)dialog.findViewById(R.id.pe).findViewById(R.id.serverPort);
				final EditText pc_ip=(EditText)dialog.findViewById(R.id.pc).findViewById(R.id.serverIp);
				final CheckBox split=(CheckBox)dialog.findViewById(R.id.switchFirm);

				pe_ip.setText("localhost");
				pe_port.setText("19132");
				split.setChecked(false);

				split.setOnClickListener(new View.OnClickListener(){
						public void onClick(View v) {
							if (split.isChecked()) {
								//PE->PC
								peFrame.setVisibility(View.GONE);
								pcFrame.setVisibility(View.VISIBLE);
								split.setText(R.string.pc);
								StringBuilder result=new StringBuilder();
								result.append(pe_ip.getText());
								int port=new Integer(pe_port.getText().toString()).intValue();
								if (!(port == 25565 | port == 19132))
									result.append(':').append(pe_port.getText());
								pc_ip.setText(result);
							} else {
								//PC->PE
								pcFrame.setVisibility(View.GONE);
								peFrame.setVisibility(View.VISIBLE);
								split.setText(R.string.pe);
								Server s=Utils.convertServerObject(Arrays.asList(com.nao20010128nao.McServerList.Server.makeServerFromString(pc_ip.getText().toString(), false))).get(0);
								pe_ip.setText(s.ip);
								pe_port.setText(s.port + "");
							}
						}
					});

				new AppCompatAlertDialog.Builder(this, R.style.AppAlertDialog).
					setView(dialog).
					setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface d, int sel) {
							Server s;
							if (split.isChecked()) {
								s = Utils.convertServerObject(Arrays.asList(com.nao20010128nao.McServerList.Server.makeServerFromString(pc_ip.getText().toString(), false))).get(0);
							} else {
								s = new Server();
								s.ip = pe_ip.getText().toString();
								s.port = new Integer(pe_port.getText().toString());
								s.mode = split.isChecked() ?1: 0;
							}

							if (list.contains(s)) {
								Toast.makeText(ServerListActivityImpl.this, R.string.alreadyExists, Toast.LENGTH_LONG).show();
							} else {
								sl.add(s);
							}
							saveServers();
						}
					}).
					setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface d, int sel) {

						}
					}).
					show();
				break;
			case 1:
				Toast.makeText(ServerListActivityImpl.this, R.string.importing, Toast.LENGTH_LONG).show();
				new Thread(){
					public void run() {
						ArrayList<String[]> al=new ArrayList<String[]>();
						try {
							String[] lines=Utils.lines(Utils.readWholeFile(new File(Environment.getExternalStorageDirectory(), "/games/com.mojang/minecraftpe/external_servers.txt")));
							for (String s:lines) {
								Log.d("readLine", s);
								al.add(s.split("\\:"));
							}
						} catch (Throwable ex) {
							DebugWriter.writeToE("ServerListActivity", ex);
						}
						final ArrayList<Server> sv=new ArrayList<>();
						for (String[] s:al) {
							if (s.length != 4)continue;
							try {
								Server svr=new Server();
								svr.ip = s[2];
								svr.port = new Integer(s[3]);
								svr.mode = 0;
								sv.add(svr);
							} catch (NumberFormatException e) {}
						}
						runOnUiThread(new Runnable(){
								public void run() {
									sl.addAll(sv);
									saveServers();
								}
							});
					}
				}.start();
				break;
			case 2:
				for (int i=0;i < list.size();i++) {
					if (pinging.get(list.get(i)))
						continue;
					((TextView)sl.getViewQuick(i).findViewById(R.id.pingMillis)).setText(R.string.working);
					((ExtendedImageView)sl.getViewQuick(i).findViewById(R.id.statColor)).setColor(getResources().getColor(R.color.stat_pending));
					((TextView)sl.getViewQuick(i).findViewById(R.id.serverName)).setText(R.string.working);
					((TextView)sl.getViewQuick(i).findViewById(R.id.serverPlayers)).setText("-/-");
					statLayout.setStatusAt(i, 1);
					if (!srl.isRefreshing())
						srl.setRefreshing(true);
				}
				new Thread(){
					public void run() {
						for (int i=0;i < list.size();i++) {
							if (pinging.get(list.get(i)))
								continue;
							spp.putInQueue(list.get(i), new PingHandlerImpl(false, -1, false){
									public void onPingFailed(final Server s) {
										super.onPingFailed(s);
										runOnUiThread(new Runnable(){
												public void run() {			
													wd.hideWorkingDialog();
												}
											});
									}
									public void onPingArrives(final ServerStatus s) {
										super.onPingArrives(s);
										runOnUiThread(new Runnable(){
												public void run() {
													wd.hideWorkingDialog();
												}
											});
									}
								});
							pinging.put(list.get(i), true);
						}
					}
				}.start();
				break;
			case 3:
				final AppCompatEditText et_=new AppCompatEditText(ServerListActivityImpl.this);
				et_.setTypeface(TheApplication.instance.getLocalizedFont());
				et_.setText(new File(Environment.getExternalStorageDirectory(), "/Wisecraft/servers.json").toString());
				new AppCompatAlertDialog.Builder(ServerListActivityImpl.this, R.style.AppAlertDialog)
					.setTitle(R.string.export_typepath)
					.setView(et_)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface di, int w) {
							Toast.makeText(ServerListActivityImpl.this, R.string.exporting, Toast.LENGTH_LONG).show();
							new AsyncTask<Void,Void,File>(){
								public File doInBackground(Void... a) {
									Server[] servs=new Server[list.size()];
									for (int i=0;i < servs.length;i++)
										servs[i] = list.get(i).cloneAsServer();
									File f=new File(Environment.getExternalStorageDirectory(), "/Wisecraft");
									f.mkdirs();
									if (writeToFile(f = new File(et_.getText().toString()), gson.toJson(servs, Server[].class)))
										return f;
									else
										return null;
								}
								public void onPostExecute(File f) {
									if (f != null) {
										Toast.makeText(ServerListActivityImpl.this, getResources().getString(R.string.export_complete).replace("[PATH]", f + ""), Toast.LENGTH_LONG).show();
									} else {
										Toast.makeText(ServerListActivityImpl.this, getResources().getString(R.string.export_failed), Toast.LENGTH_LONG).show();
									}
								}
							}.execute();
						}
					})
					.show();
				break;
			case 4:
				final AppCompatEditText et=new AppCompatEditText(ServerListActivityImpl.this);
				et.setTypeface(TheApplication.instance.getLocalizedFont());
				et.setText(new File(Environment.getExternalStorageDirectory(), "/Wisecraft/servers.json").toString());
				new AppCompatAlertDialog.Builder(ServerListActivityImpl.this, R.style.AppAlertDialog)
					.setTitle(R.string.import_typepath)
					.setView(et)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface di, int w) {
							Toast.makeText(ServerListActivityImpl.this, R.string.importing, Toast.LENGTH_LONG).show();
							new Thread(){
								public void run() {
									final Server[] sv;
									String json=readWholeFile(new File(et.getText().toString()));
									if (json.contains("\"isPC\"") & (json.contains("true") | json.contains("false"))) {
										//old version json file
										OldServer19[] sa=gson.fromJson(json, OldServer19[].class);
										List<Server> ns=new ArrayList<>();
										for (OldServer19 s:sa) {
											Server nso=new Server();
											nso.ip = s.ip;
											nso.port = s.port;
											nso.mode = s.isPC ?1: 0;
											ns.add(nso);
										}
										sv = ns.toArray(new Server[ns.size()]);
									} else {
										sv = gson.fromJson(json, Server[].class);
									}
									runOnUiThread(new Runnable(){
											public void run() {
												sl.addAll(sv);
												saveServers();
												Toast.makeText(ServerListActivityImpl.this, getResources().getString(R.string.imported).replace("[PATH]", et.getText().toString()), Toast.LENGTH_LONG).show();
											}
										});
								}
							}.start();
						}
					})
					.show();
				break;
			case 5:
				new AppCompatAlertDialog.Builder(this,R.style.AppAlertDialog)
					.setTitle(R.string.sort)
					.setSingleChoiceItems(R.array.serverSortMenu_old,-1,new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface di,int w){
							SortKind sk=new SortKind[]{SortKind.BRING_ONLINE_SERVERS_TO_TOP,SortKind.IP_AND_PORT,SortKind.ONLINE_AND_OFFLINE}[w];
							skipSave=true;
							doSort(list,sk);
						}
					})
					.show();
				break;
			case 6:
				startActivity(new Intent(this, ServerFinderActivity.class));
				break;
			case 7:
				startActivity(new Intent(this, ServerGetActivity.class));
				break;
			case 8:
				SettingsDelegate.openAppSettings(this);
				break;
			case 9:
				finish();
				saveServers();
				instance = new WeakReference(null);
				if (pref.getBoolean("exitCompletely", false))
					if (ProxyActivity.cont != null)
						ProxyActivity.cont.stopService();
				new Handler().postDelayed(new Runnable(){
						public void run() {
							System.exit(0);
						}
					}, 150 * 2);
				break;
		}
		return true;
	}

	@Override
	protected void onStart() {
		// TODO: Implement this method
		super.onStart();
		Log.d("ServerListActivity", "onStart");
		TheApplication.instance.collect();
	}

	public void loadServers() {
		int version=pref.getInt("serversJsonVersion", 0);
		version = version == 0 ?pref.getString("servers", "[]").equals("[]") ?version: 0: version;
		switch (version) {
			case 0:
				wd.showWorkingDialog(getResources().getString(R.string.upgrading));
				new AsyncTask<Void,Void,Void>(){
					public Void doInBackground(Void...args) {
						OldServer19[] sa=gson.fromJson(pref.getString("servers", "[]"), OldServer19[].class);
						List<Server> ns=new ArrayList<>();
						for (OldServer19 s:sa) {
							Server nso=new Server();
							nso.ip = s.ip;
							nso.port = s.port;
							nso.mode = s.isPC ?1: 0;
							ns.add(nso);
						}
						pref.edit().putInt("serversJsonVersion", 1).putString("servers", gson.toJson(ns)).commit();
						return null;
					}
					public void onPostExecute(Void v) {
						wd.hideWorkingDialog();
						loadServers();
					}
				}.execute();
				break;
			case 1:
				Server[] sa=gson.fromJson(pref.getString("servers", "[]"), Server[].class);
				sl.clear();
				sl.addAll(sa);
				break;
		}
	}
	public void saveServers() {
		new Thread(){
			public void run() {
				List<Server> toSave=new ArrayList<>();
				for (Server s:list)toSave.add(s.cloneAsServer());
				String json;
				pref.edit().putString("servers", json = gson.toJson(toSave)).commit();
				Log.d("json", json);
			}
		}.start();
	}

	public void dryUpdate(Server s) {
		if (pinging.get(s))return;
		updater.putInQueue(s, new PingHandlerImpl(true, -1));
		((TextView)sl.getViewQuick(list.indexOf(s)).findViewById(R.id.pingMillis)).setText(R.string.working);
		((ExtendedImageView)sl.getViewQuick(list.indexOf(s)).findViewById(R.id.statColor)).setColor(getResources().getColor(R.color.stat_pending));
		pinging.put(s, true);
	}

	public List<Server> getServers() {
		return new ArrayList<Server>(list);
	}

	@Override
	public void addIntoList(Server s) {
		// TODO: Implement this method
		sl.add(s);
	}
	
	static class ServerList extends AppBaseArrayAdapter<Server> implements AdapterView.OnItemClickListener,AdapterView.OnItemLongClickListener {
		List<View> cached=new ArrayList();
		ServerListActivityImpl sla;
		public ServerList(ServerListActivityImpl sla) {
			super(sla, 0, sla.list = new ServerListArrayList());
			this.sla = sla;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (cached.size() > position) {
				View v=cached.get(position);
				if (v != null)
					return v;
			}
			//if(convertView!=null)return convertView;
			while (cached.size() <= position)
				cached.addAll(Constant.TEN_LENGTH_NULL_LIST);
			final View layout;
			if (sla.pref.getBoolean("colorFormattedText", false)) {
				if (sla.pref.getBoolean("darkBackgroundForServerName", false)) {
					layout = sla.getLayoutInflater().inflate(R.layout.quickstatus_dark, null, false);
				} else {
					layout = sla.getLayoutInflater().inflate(R.layout.quickstatus, null, false);
				}
			} else {
				layout = sla.getLayoutInflater().inflate(R.layout.quickstatus, null, false);
			}
			Server s=getItem(position);
			layout.setTag(s);
			((TextView)layout.findViewById(R.id.serverName)).setText(R.string.working);
			((TextView)layout.findViewById(R.id.pingMillis)).setText(R.string.working);
			((TextView)layout.findViewById(R.id.serverAddress)).setText(s.ip + ":" + s.port);
			((ExtendedImageView)layout.findViewById(R.id.statColor)).setColor(sla.getResources().getColor(R.color.stat_pending));
			if (s instanceof ServerStatus) {
				new PingHandlerImpl().onPingArrives((ServerStatus)s);
				sla.statLayout.setStatusAt(position, 2);
			} else {
				sla.spp.putInQueue(s, new PingHandlerImpl(false, -1, true));
				sla.statLayout.setStatusAt(position, 1);
			}
			cached.set(position, layout);
			sla.pinging.put(s, true);
			return layout;
		}
		public View getCachedView(int position) {
			return cached.get(position);
		}
		public View getViewQuick(int pos) {
			return getView(pos, null, null);
		}
		@Override
		public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
			// TODO: Implement this method
			Server s=getItem(p3);
			sla.clicked = p3;
			if (sla.pinging.get(s))return;
			if (s instanceof ServerStatus) {
				ServerInfoActivity.stat.clear();
				ServerInfoActivity.stat.add((ServerStatus)s);
				int ofs=ServerInfoActivity.stat.indexOf(s);
				Bundle bnd=new Bundle();
				bnd.putInt("statListOffset", ofs);
				sla.startActivityForResult(new Intent(sla, ServerInfoActivity.class).putExtra("statListOffset", ofs).putExtra("object", bnd), 0);
			} else {
				sla.updater.putInQueue(s, new PingHandlerImpl(true, 0, true));
				((TextView)getViewQuick(sla.clicked).findViewById(R.id.pingMillis)).setText(R.string.working);
				((ExtendedImageView)getViewQuick(sla.clicked).findViewById(R.id.statColor)).setColor(sla.getResources().getColor(R.color.stat_pending));
				((TextView)getViewQuick(sla.clicked).findViewById(R.id.serverName)).setText(R.string.working);
				((TextView)getViewQuick(sla.clicked).findViewById(R.id.serverPlayers)).setText("-/-");
				sla.wd.showWorkingDialog();
				sla.pinging.put(sla.list.get(sla.clicked), true);
				sla.statLayout.setStatusAt(sla.clicked, 1);
			}
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> p1, View p2, final int p3, long p4) {
			// TODO: Implement this method
			sla.clicked = p3;
			new AppCompatAlertDialog.Builder(sla)
				.setItems(generateSubMenu(getItem(p3).mode == 1), new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface di, int which) {
						List<Runnable> executes=new ArrayList<>();
						executes.add(0, new Runnable(){
								public void run() {
									new AppCompatAlertDialog.Builder(sla,R.style.AppAlertDialog)
										.setMessage(R.string.auSure)
										.setNegativeButton(android.R.string.yes, new DialogInterface.OnClickListener(){
											public void onClick(DialogInterface di, int i) {
												sla.sl.remove(sla.list.get(sla.clicked));
												sla.saveServers();
												sla.statLayout.removeStatus(sla.clicked);
											}
										})
										.setPositiveButton(android.R.string.no, new DialogInterface.OnClickListener(){
											public void onClick(DialogInterface di, int i) {
											}
										})
										.show();
								}
							});
						executes.add(1, new Runnable(){
								public void run() {
									if (sla.pinging.get(getItem(p3)))return;
									sla.updater.putInQueue(getItem(p3), new PingHandlerImpl(true, -1));
									((TextView)getViewQuick(p3).findViewById(R.id.pingMillis)).setText(R.string.working);
									((ExtendedImageView)getViewQuick(p3).findViewById(R.id.statColor)).setColor(sla.getResources().getColor(R.color.stat_pending));
									((TextView)getViewQuick(p3).findViewById(R.id.serverName)).setText(R.string.working);
									((TextView)getViewQuick(p3).findViewById(R.id.serverPlayers)).setText("-/-");
									sla.wd.showWorkingDialog();
									sla.pinging.put(sla.list.get(p3), true);
									sla.statLayout.setStatusAt(p3,1);
								}
							});
						executes.add(2, new Runnable(){
								public void run() {
									final Server data=getItem(p3);
									View dialog=sla.getLayoutInflater().inflate(R.layout.server_add_dialog_new, null);
									final LinearLayout peFrame=(LinearLayout)dialog.findViewById(R.id.pe);
									final LinearLayout pcFrame=(LinearLayout)dialog.findViewById(R.id.pc);
									final EditText pe_ip=(EditText)dialog.findViewById(R.id.pe).findViewById(R.id.serverIp);
									final EditText pe_port=(EditText)dialog.findViewById(R.id.pe).findViewById(R.id.serverPort);
									final EditText pc_ip=(EditText)dialog.findViewById(R.id.pc).findViewById(R.id.serverIp);
									final CheckBox split=(CheckBox)dialog.findViewById(R.id.switchFirm);

									if (data.mode == 1) {
										if (data.port == 25565) {
											pc_ip.setText(data.ip);
										} else {
											pc_ip.setText(data.toString());
										}
									} else {
										pe_ip.setText(data.ip);
										pe_port.setText(data.port + "");
									}
									split.setChecked(data.mode == 1);
									if (data.mode == 1) {
										peFrame.setVisibility(View.GONE);
										pcFrame.setVisibility(View.VISIBLE);
										split.setText(R.string.pc);
									} else {
										pcFrame.setVisibility(View.GONE);
										peFrame.setVisibility(View.VISIBLE);
										split.setText(R.string.pe);
									}

									split.setOnClickListener(new View.OnClickListener(){
											public void onClick(View v) {
												if (split.isChecked()) {
													//PE->PC
													peFrame.setVisibility(View.GONE);
													pcFrame.setVisibility(View.VISIBLE);
													split.setText(R.string.pc);
													StringBuilder result=new StringBuilder();
													result.append(pe_ip.getText());
													int port=new Integer(pe_port.getText().toString()).intValue();
													if (!(port == 25565 | port == 19132)) {
														result.append(':').append(pe_port.getText());
													}
													pc_ip.setText(result);
												} else {
													//PC->PE
													pcFrame.setVisibility(View.GONE);
													peFrame.setVisibility(View.VISIBLE);
													split.setText(R.string.pe);
													Server s=Utils.convertServerObject(Arrays.<com.nao20010128nao.McServerList.Server>asList(com.nao20010128nao.McServerList.Server.makeServerFromString(pc_ip.getText().toString(), false))).get(0);
													pe_ip.setText(s.ip);
													pe_port.setText(s.port + "");
												}
											}
										});

									new AppCompatAlertDialog.Builder(sla, R.style.AppAlertDialog).
										setView(dialog).
										setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
											public void onClick(DialogInterface d, int sel) {
												Server s;
												if (split.isChecked()) {
													s = Utils.convertServerObject(Arrays.<com.nao20010128nao.McServerList.Server>asList(com.nao20010128nao.McServerList.Server.makeServerFromString(pc_ip.getText().toString(), false))).get(0);
												} else {
													s = new Server();
													s.ip = pe_ip.getText().toString();
													s.port = new Integer(pe_port.getText().toString());
													s.mode = split.isChecked() ?1: 0;
												}

												List<Server> localServers=new ArrayList<>(sla.list);
												int ofs=localServers.indexOf(data);
												localServers.set(ofs, s);
												if (localServers.contains(data)) {
													Toast.makeText(sla, R.string.alreadyExists, Toast.LENGTH_LONG).show();
												} else {
													sla.list.set(ofs, s);
													sla.sl.notifyDataSetChanged();
													sla.dryUpdate(s);
												}
												sla.saveServers();
											}
										}).
										setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener(){
											public void onClick(DialogInterface d, int sel) {

											}
										}).
										show();
								}
							});
						executes.add(3, new Runnable(){
								public void run() {
									sla.startActivity(new Intent(sla, ServerTestActivity.class).putExtra("ip", getItem(p3).ip).putExtra("port", getItem(p3).port).putExtra("ispc", getItem(p3).mode));
								}
							});
						executes.add(4, new Runnable(){
								public void run() {
									sla.startActivity(new Intent(sla, RCONActivity.class).putExtra("ip", getItem(p3).ip).putExtra("port", getItem(p3).port));
								}
							});
						executes.add(5, new Runnable(){
								public void run() {
									new Thread(){
										public void run() {
											File servLst=new File(Environment.getExternalStorageDirectory(), "/games/com.mojang/minecraftpe/external_servers.txt");
											Server s=getItem(p3);
											String sls=Utils.readWholeFile(servLst);
											if (sls == null)
												sls = "";
											for (String l:Utils.lines(sls))
												if (l.endsWith(s.toString()))return;
											sls += "\n900:" + randomText() + ":" + s + "\n";
											StringBuilder sb=new StringBuilder(100);
											for (String line:Utils.lines(sls))
												if (line.split("\\:").length == 4)
													sb.append(line).append('\n');
											Utils.writeToFile(servLst, sb.toString());
										}
									}.start();
								}
							});
						executes.add(6, new Runnable(){
								public void run() {
									sla.startActivity(new Intent(sla, ProxyActivity.class).putExtra("ip", getItem(p3).ip).putExtra("port", getItem(p3).port).setAction("start"));
								}
							});
						executes.add(7, new Runnable(){
								public void run() {
									sla.startActivity(new Intent(sla, ServerFinderActivity.class).putExtra("ip", getItem(p3).ip).putExtra("port", getItem(p3).port).putExtra("mode", getItem(p3).mode));
								}
							});

						List<Runnable> all=new ArrayList(executes);

						if (getItem(p3).mode == 1) {
							executes.remove(all.get(5));
							executes.remove(all.get(6));
						}
						if (!sla.pref.getBoolean("feature_proxy", true)) {
							executes.remove(all.get(6));
						}
						if (!sla.pref.getBoolean("feature_serverFinder", false)) {
							executes.remove(all.get(7));
						}

						executes.get(which).run();
					}
				})
				.setCancelable(true)
				.show();
			return true;
		}

		@Override
		public void add(Server object) {
			// TODO: Implement this method
			if (!sla.list.contains(object)){
				sla.statLayout.addStatuses(1);
				super.add(object);
			}
		}

		@Override
		public void addAll(Server[] items) {
			// TODO: Implement this method
			for (Server s:items)add(s);
		}

		@Override
		public void addAll(Collection<? extends Server> collection) {
			// TODO: Implement this method
			for (Server s:collection)add(s);
		}

		@Override
		public void remove(Server object) {
			// TODO: Implement this method
			cached.remove(sla.list.indexOf(object));
			super.remove(object);
		}

		private String[] generateSubMenu(boolean isPC) {
			List<String> result=new ArrayList<String>(Arrays.<String>asList(sla.getResources().getStringArray(R.array.serverSubMenu_old)));
			List<String> all=new ArrayList<String>(result);
			if (isPC) {
				result.remove(all.get(5));
				result.remove(all.get(6));
			}
			if (!sla.pref.getBoolean("feature_proxy", true)) {
				result.remove(all.get(6));
			}
			if (!sla.pref.getBoolean("feature_serverFinder", false)) {
				result.remove(all.get(7));
			}
			return result.toArray(new String[result.size()]);
		}

		public void attachNewActivity(ServerListActivityImpl sla) {
			this.sla = sla;
		}
	}

	static class PingHandlerImpl implements ServerPingProvider.PingHandler {
		boolean closeDialog;
		int statTabOfs;
		Bundle obj;
		public PingHandlerImpl() {
			this(false, -1);
		}
		public PingHandlerImpl(boolean cd, int os) {
			this(cd, os, true);
		}
		public PingHandlerImpl(boolean cd, int os, boolean updSrl) {
			this(cd, os, updSrl, null);
		}
		public PingHandlerImpl(boolean cd, int os, boolean updSrl, Bundle receive) {
			closeDialog = cd;
			statTabOfs = os;
			if (updSrl)act().srl.setRefreshing(true);
			obj = receive;
		}
		public void onPingFailed(final Server s) {
			act().runOnUiThread(new Runnable(){
					public void run() {
						try {
							int i_=act().list.indexOf(s);
							if (i_ == -1) {
								return;
							}
							((ExtendedImageView)act().sl.getViewQuick(i_).findViewById(R.id.statColor)).setColor(act().getResources().getColor(R.color.stat_error));
							((TextView)act().sl.getViewQuick(i_).findViewById(R.id.serverName)).setText(s.ip + ":" + s.port);
							((TextView)act().sl.getViewQuick(i_).findViewById(R.id.pingMillis)).setText(R.string.notResponding);
							((TextView)act().sl.getViewQuick(i_).findViewById(R.id.serverPlayers)).setText("-/-");
							((TextView)act().sl.getViewQuick(i_).findViewById(R.id.serverAddress)).setText(s.ip + ":" + s.port);
							Server sn=s.cloneAsServer();
							act().list.set(i_, sn);
							act().pinging.put(act().list.get(i_), false);
							if (closeDialog)
								act().wd.hideWorkingDialog();
							if (statTabOfs != -1)
								Toast.makeText(act(), R.string.serverOffline, Toast.LENGTH_SHORT).show();
							if (!act().pinging.containsValue(true))
								act().srl.setRefreshing(false);
							act().statLayout.setStatusAt(i_,0);
						} catch (final Throwable e) {
							CollectorMain.reportError("ServerListActivity#onPingFailed",e);
						}
					}
				});
		}
		public void onPingArrives(final ServerStatus s) {
			act().runOnUiThread(new Runnable() {
				public void run() {
					try {
						int i_ = act().list.indexOf(s);
						if (i_ == -1) {
							return;
						}
						((ExtendedImageView) act().sl.getViewQuick(i_).findViewById(R.id.statColor)).setColor(act().getResources().getColor(R.color.stat_ok));
						final String title;
						if (s.response instanceof FullStat) {//PE
							FullStat fs = (FullStat) s.response;
							Map<String, String> m = fs.getData();
							if (m.containsKey("hostname")) {
								title = m.get("hostname");
							} else if (m.containsKey("motd")) {
								title = m.get("motd");
							} else {
								title = s.ip + ":" + s.port;
							}
							((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverPlayers)).setText(fs.getData().get("numplayers") + "/" + fs.getData().get("maxplayers"));
						} else if (s.response instanceof Reply19) {//PC 1.9~
							Reply19 rep = (Reply19) s.response;
							if (rep.description == null) {
								title = s.ip + ":" + s.port;
							} else {
								title = rep.description.text;
							}
							((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverPlayers)).setText(rep.players.online + "/" + rep.players.max);
						} else if (s.response instanceof Reply) {//PC
							Reply rep = (Reply) s.response;
							if (rep.description == null) {
								title = s.ip + ":" + s.port;
							} else {
								title = rep.description;
							}
							((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverPlayers)).setText(rep.players.online + "/" + rep.players.max);
						} else if (s.response instanceof SprPair) {//PE?
							SprPair sp = ((SprPair) s.response);
							if (sp.getB() instanceof UnconnectedPing.UnconnectedPingResult) {
								UnconnectedPing.UnconnectedPingResult res = (UnconnectedPing.UnconnectedPingResult) sp.getB();
								title = res.getServerName();
								((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverPlayers)).setText(res.getPlayersCount() + "/" + res.getMaxPlayers());
							} else if (sp.getA() instanceof FullStat) {
								FullStat fs = (FullStat) sp.getA();
								Map<String, String> m = fs.getData();
								if (m.containsKey("hostname")) {
									title = m.get("hostname");
								} else if (m.containsKey("motd")) {
									title = m.get("motd");
								} else {
									title = s.ip + ":" + s.port;
								}
								((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverPlayers)).setText(fs.getData().get("numplayers") + "/" + fs.getData().get("maxplayers"));
							} else {
								title = s.ip + ":" + s.port;
								((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverPlayers)).setText("-/-");
							}
						} else if (s.response instanceof UnconnectedPing.UnconnectedPingResult) {//PE
							UnconnectedPing.UnconnectedPingResult res = (UnconnectedPing.UnconnectedPingResult) s.response;
							title = res.getServerName();
							((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverPlayers)).setText(res.getPlayersCount() + "/" + res.getMaxPlayers());
						} else {//Unreachable
							title = s.ip + ":" + s.port;
							((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverPlayers)).setText("-/-");
						}
						if (act().pref.getBoolean("colorFormattedText", false)) {
							if (act().pref.getBoolean("darkBackgroundForServerName", false)) {
								((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverName)).setText(parseMinecraftFormattingCodeForDark(title));
							} else {
								((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverName)).setText(parseMinecraftFormattingCode(title));
							}
						} else {
							((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverName)).setText(deleteDecorations(title));
						}
						((TextView) act().sl.getViewQuick(i_).findViewById(R.id.pingMillis)).setText(s.ping + " ms");
						((TextView) act().sl.getViewQuick(i_).findViewById(R.id.serverAddress)).setText(s.ip + ":" + s.port);
						act().list.set(i_, s);
						act().pinging.put(act().list.get(i_), false);
						if (statTabOfs != -1) {
							ServerInfoActivity.stat.add(s);
							int ofs = ServerInfoActivity.stat.lastIndexOf(s);
							Intent caller = new Intent(act(), ServerInfoActivity.class).putExtra("offset", statTabOfs).putExtra("statListOffset", ofs);
							if (obj != null) {
								caller.putExtra("object", obj);
							}
							act().startActivityForResult(caller, 0);
						}
						if (closeDialog) {
							act().wd.hideWorkingDialog();
						}

						if (!act().pinging.containsValue(true)) {
							act().srl.setRefreshing(false);
						}
						act().statLayout.setStatusAt(i_, 2);
					} catch (final Throwable e) {
						DebugWriter.writeToE("ServerListActivity", e);
						CollectorMain.reportError("ServerListActivity#onPingArrives", e);
						onPingFailed(s);
					}
				}
			});
		}
		private ServerListActivityImpl act(){
			return ServerListActivityImpl.instance.get();
		}
	}
	class MenuExecClickListener implements View.OnClickListener {
		int o;
		public MenuExecClickListener(int d) {
			o = d;
		}
		@Override
		public void onClick(View p1) {
			// TODO: Implement this method
			execOption(o);
		}
	}
	class NetworkStatusCheckWorker extends AsyncTask<Void,String,String> {
		@Override
		protected String doInBackground(Void[] p1) {
			// TODO: Implement this method
			return fetchNetworkState();
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO: Implement this method
			if (result != null) {
				networkState.setText(result);
				networkState.show();
			} else {
				networkState.dismiss();
			}
		}
	}
	class NetworkStateBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context p1, Intent p2) {
			// TODO: Implement this method
			Log.d("ServerListActivity - NSBB", "received");
			new NetworkStatusCheckWorker().execute();
		}
	}

	private String fetchNetworkState() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		String conName;
		if (cm.getActiveNetworkInfo() == null) {
			conName = "offline";
		} else {
			conName = cm.getActiveNetworkInfo().getTypeName();
		}
		if (conName == null) {
			conName = "offline";
		}
		conName = conName.toLowerCase();

		if (conName.equalsIgnoreCase("offline")) {
			pref.edit().putInt("offline", pref.getInt("offline", 0) + 1).apply();
			if (pref.getInt("offline", 0) > 6) {
				pref.edit().putBoolean("sendInfos_force", true).putInt("offline", 0).apply();
			}
			return getResources().getString(R.string.offline);
		}
		if ("mobile".equalsIgnoreCase(conName)) {
			return getResources().getString(R.string.onMobileNetwork);
		}
		return null;
	}

	public static class MenuPreferenceActivity extends PreferenceActivity {
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// TODO: Implement this method
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_blank);

			List<String> values=getIntent().getStringArrayListExtra("values");
			PreferenceScreen scr=getPreferenceScreen();
			for (String s:values) {
				StartPref p=new StartPref(this);
				p.setTitle(s);
				p.setOnClickListener(new PrefHandler());
				scr.addPreference(p);
			}
		}
		@Override
		protected void attachBaseContext(Context newBase) {
			super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
		}

		class PrefHandler implements HandledPreference.OnClickListener {
			@Override
			public void onClick(String var1, String var2, String var3) {
				// TODO: Implement this method
				ServerListActivityImpl ins=ServerListActivityImpl.instance.get();
				ins.execOption(ins.appMenu.indexOf(var2));
			}
		}
	}
	class OpenCloseListener implements DrawerLayout.DrawerListener{
		public void onDrawerSlide(View v, float slide) {

		}
		public void onDrawerStateChanged(int state) {

		}
		public void onDrawerClosed(View v) {
			drawerOpened = false;
		}
		public void onDrawerOpened(View v) {
			drawerOpened = true;
		}
	}
}
class ServerListActivityBase1 extends ServerListActivityBase2
{
	SecureRandom sr=new SecureRandom();
	HashMap<Integer,Metadata> permRequire=new HashMap<>();
	HashMap<Integer,Boolean> permReqResults=new HashMap<Integer,Boolean>(){
		@Override
		public Boolean get(Object key) {
			// TODO: Implement this method
			Boolean b = super.get(key);
			if (b == null) {
				return false;
			}
			return b;
		}
	};


	public void doAfterRequirePerm(RequirePermissionResult r,String[] perms){
		int call=Math.abs(sr.nextInt())&0xf;
		while(permRequire.containsKey(call)){
			call=Math.abs(sr.nextInt())&0xf;
		}
		ArrayList<String> notAllowed=new ArrayList<>();
		ArrayList<String> unconfirmable=new ArrayList<>();
		for(String perm:perms)
			if(PermissionChecker.checkSelfPermission(this,perm)!=PermissionChecker.PERMISSION_GRANTED)
				notAllowed.add(perm);
		for(String s:notAllowed)Log.d("ServerListActivity","notAllowed:"+s);
		for(String s:unconfirmable)Log.d("ServerListActivity","unconfirmable:"+s);
		if(perms.length==unconfirmable.size()){
			Log.d("ServerListActivity","denied");
			r.onFailed(perms,Factories.strArray(unconfirmable));
			return;
		}
		if(notAllowed.isEmpty()&unconfirmable.isEmpty()){
			Log.d("ServerListActivity","nothing to ask");
			r.onSuccess();
			return;
		}
		Metadata md=new Metadata();
		md.rpr=r;
		md.currentlyDenied=Factories.strArray(unconfirmable);
		permRequire.put(call,md);
		ActivityCompat.requestPermissions(this,Factories.strArray(notAllowed),call);
	}

	protected boolean dispatchActivityResult(int request,int result,Intent data){
		super.onActivityResult(request,result,data);
		return permReqResults.get(request);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO: Implement this method
		dispatchActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		// TODO: Implement this method
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if(!permRequire.containsKey(requestCode)){
			return;
		}
		Metadata md=permRequire.get(requestCode);

		ArrayList lst=new ArrayList();
		for (int i=0;i < grantResults.length;i++)
			if (grantResults[i]!=PackageManager.PERMISSION_GRANTED)
				lst.add(permissions[i]);
		lst.addAll(Arrays.asList(md.currentlyDenied));

		if(lst.isEmpty()){
			md.rpr.onSuccess();
			permReqResults.put(requestCode,true);
		}else{
			md.rpr.onFailed(Factories.strArray(lst),md.currentlyDenied);
			permReqResults.put(requestCode,false);
		}
		permRequire.remove(requestCode);
	}

	public static interface RequirePermissionResult{
		public void onSuccess();
		public void onFailed(String[] corruptPerms,String[] unconfirmable);
	}

	class Metadata{
		RequirePermissionResult rpr;
		String[] currentlyDenied;
	}
}
class ServerListActivityBase2 extends AppCompatListActivity
{
	SharedPreferences pref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		pref=PreferenceManager.getDefaultSharedPreferences(this);
	}

	public void doSort(final List<Server> sl,final com.nao20010128nao.Wisecraft.misc.ServerListActivityBase2.SortKind sk){
		new Thread(){
			public void run(){
				final List<Server> sortingServer=sk.doSort(sl);
				runOnUiThread(new Runnable(){
						public void run() {
							finish();
							ServerListActivityImpl.instance.clear();
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
}
public class ServerListActivity extends ServerListActivityImpl{
	//Internal class
}
