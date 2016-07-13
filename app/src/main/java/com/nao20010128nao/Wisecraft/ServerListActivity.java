package com.nao20010128nao.Wisecraft;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.support.design.widget.*;
import android.support.v4.view.*;
import android.support.v4.widget.*;
import android.support.v7.widget.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.google.gson.*;
import com.nao20010128nao.ToolBox.*;
import com.nao20010128nao.Wisecraft.collector.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.compat.*;
import com.nao20010128nao.Wisecraft.misc.contextwrappers.extender.*;
import com.nao20010128nao.Wisecraft.misc.pinger.pc.*;
import com.nao20010128nao.Wisecraft.misc.pinger.pe.*;
import com.nao20010128nao.Wisecraft.misc.pref.*;
import com.nao20010128nao.Wisecraft.misc.server.*;
import com.nao20010128nao.Wisecraft.misc.view.*;
import com.nao20010128nao.Wisecraft.pingEngine.*;
import com.nao20010128nao.Wisecraft.provider.*;
import com.nao20010128nao.Wisecraft.proxy.*;
import com.nao20010128nao.Wisecraft.rcon.*;
import com.nao20010128nao.Wisecraft.services.*;
import com.nao20010128nao.Wisecraft.settings.*;
import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.util.*;
import uk.co.chrisjenx.calligraphy.*;

import static com.nao20010128nao.Wisecraft.misc.Utils.*;

abstract class ServerListActivityImpl extends ServerListActivityBase1 implements ServerListActivityInterface {
	public static WeakReference<ServerListActivityImpl> instance=new WeakReference(null);

	static final File mcpeServerList=new File(Environment.getExternalStorageDirectory(), "/games/com.mojang/minecraftpe/external_servers.txt");

	final List<String> appMenu=new ArrayList<>();
	ServerPingProvider spp,updater;
	Gson gson=new Gson();
	SharedPreferences pref;
	RecycleServerList sl;
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
	RecyclerView rv;
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
				setContentView(R.layout.server_list_content_nodrawer);
				Utils.getToolbar(this).setOverflowIcon(TheApplication.instance.getTintedDrawable(R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha,getResources().getColor(R.color.upd_2)));
				break;
			case 1:
				setContentView(R.layout.server_list_content);
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
					Button btn=(Button)getLayoutInflater().inflate(R.layout.server_list_bar_button, ll,false).findViewById(R.id.menu_btn);
					//((ViewGroup)btn.getParent()).removeView(btn);
					btn.setText(s);
					btn.setOnClickListener(new MenuExecClickListener(appMenu.indexOf(s)));
					ll.addView(btn);
				}
				
				setupDrawer();
				break;
			case 2:
				setContentView(R.layout.server_list_content_listview);
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
		rv=(RecyclerView)findViewById(android.R.id.list);
		rv.setLayoutManager(new LinearLayoutManager(this));
		
		srl = (SwipeRefreshLayout)findViewById(R.id.swipelayout);
		srl.setColorSchemeResources(R.color.upd_1, R.color.upd_2, R.color.upd_3, R.color.upd_4);
		srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
				public void onRefresh() {
					execOption(2);
				}
			});
		statLayout=(StatusesLayout)findViewById(R.id.serverStatuses);
		statLayout.setColorRes(R.color.stat_error,R.color.stat_pending,R.color.stat_ok);
		if(pref.getBoolean("statusBarTouchScroll",false))
			statLayout.setOnTouchListener(new View.OnTouchListener(){
					@Override
					public boolean onTouch(View v,MotionEvent event) {
						switch (event.getAction()) {
							case MotionEvent.ACTION_MOVE:
							case MotionEvent.ACTION_UP:
								rv.smoothScrollToPosition((int)Math.floor(event.getX()/(statLayout.getWidth()/sl.getItemCount())));
								break;
						}
						return true;
					}
				});
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
			rv.setAdapter(sl);
		} else {
			spp = updater = new MultiServerPingProvider(Integer.parseInt(pref.getString("parallels", "6")));
			if (pref.getBoolean("updAnotherThread", false))
				updater = new NormalServerPingProvider();
			rv.setAdapter(sl = new RecycleServerList(this));
		}
		rv.setLongClickable(true);
		wd = new WorkingDialog(this);
		if (!usesOldInstance){
			loadServers();
			statLayout.initStatuses(list.size(),1);
			for (int i=0;i < list.size();i++)
				dryUpdate(list.get(i),false);
		}
		if (pref.getBoolean("colorFormattedText", false) & pref.getBoolean("darkBackgroundForServerName", false)) {
			BitmapDrawable bd=(BitmapDrawable)getResources().getDrawable(R.drawable.soil);
			bd.setTargetDensity(getResources().getDisplayMetrics());
			bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
			rv.setBackgroundDrawable(bd);
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
						pinging.put(list.get(clicked), true);
						statLayout.setStatusAt(clicked, 1);
						sl.notifyItemChanged(clicked);
						wd.showWorkingDialog();
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
								spp.putInQueue(s, new PingHandlerImpl(true, -1));
								pinging.put(s, true);
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
						sv.removeAll(list);
						runOnUiThread(new Runnable(){
								public void run() {
									if(sv.size()!=0){
										for(Server s:sv){
											if(!list.contains(s)){
												spp.putInQueue(s, new PingHandlerImpl(true, -1));
												pinging.put(s, true);
												sl.add(s);
											}
										}
									}
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
				View dialogView_=getLayoutInflater().inflate(R.layout.server_list_imp_exp,null);
				final EditText et_=(EditText)dialogView_.findViewById(R.id.filePath);
				et_.setText(new File(Environment.getExternalStorageDirectory(), "/Wisecraft/servers.json").toString());
				dialogView_.findViewById(R.id.selectFile).setOnClickListener(new View.OnClickListener(){
					public void onClick(View v){
						startChooseFileForOpen(new File(et_.getText().toString()),new FileChooserResult(){
							public void onSelected(File f){
								et_.setText(f.toString());
							}
							public void onSelectCancelled(){/*No-op*/}
						});
					}
				});
				new AppCompatAlertDialog.Builder(ServerListActivityImpl.this, R.style.AppAlertDialog)
					.setTitle(R.string.export_typepath)
					.setView(dialogView_)
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
				View dialogView=getLayoutInflater().inflate(R.layout.server_list_imp_exp,null);
				final EditText et=(EditText)dialogView.findViewById(R.id.filePath);
				et.setText(new File(Environment.getExternalStorageDirectory(), "/Wisecraft/servers.json").toString());
				dialogView.findViewById(R.id.selectFile).setOnClickListener(new View.OnClickListener(){
						public void onClick(View v){
							File f=new File(et.getText().toString());
							if(f.isFile())f=f.getParentFile();
							startChooseFileForSelect(f,new FileChooserResult(){
									public void onSelected(File f){
										et.setText(f.toString());
									}
									public void onSelectCancelled(){/*No-op*/}
								});
						}
					});
				new AppCompatAlertDialog.Builder(ServerListActivityImpl.this, R.style.AppAlertDialog)
					.setTitle(R.string.import_typepath)
					.setView(dialogView)
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
					.setSingleChoiceItems(R.array.serverSortMenu,-1,new DialogInterface.OnClickListener(){
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
				int prevLen=list.size();
				list.clear();
				sl.notifyItemRangeRemoved(0,prevLen);
				int curLen=sa.length;
				list.addAll(Arrays.asList(sa));
				sl.notifyItemRangeInserted(0,curLen);
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

	public void dryUpdate(Server s,boolean isUpdate) {
		if (pinging.get(s))return;
		if(isUpdate)updater.putInQueue(s, new PingHandlerImpl(true, -1));
		else spp.putInQueue(s, new PingHandlerImpl(true, -1));
		pinging.put(s, true);
		sl.notifyItemChanged(list.indexOf(s));
	}

	public List<Server> getServers() {
		return new ArrayList<Server>(list);
	}

	@Override
	public void addIntoList(Server s) {
		// TODO: Implement this method
		if(list.contains(s))return;
		sl.add(s);
		spp.putInQueue(s, new PingHandlerImpl(true, -1));
		pinging.put(s, true);
	}

	static class RecycleServerList extends RecyclerView.Adapter<RecycleServerList.OriginalViewHolder> implements AdapterView.OnItemClickListener,AdapterView.OnItemLongClickListener{
		ServerListActivityImpl sla;
		public RecycleServerList(ServerListActivityImpl sla) {
			sla.list = new ServerListArrayList();
			this.sla = sla;
		}
		
		@Override
		public OriginalViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
			// 表示するレイアウトを設定
			int layout;
			if (sla.pref.getBoolean("colorFormattedText", false)) {
				if (sla.pref.getBoolean("darkBackgroundForServerName", false)) {
					layout = R.layout.quickstatus_dark;
				} else {
					layout = R.layout.quickstatus;
				}
			} else {
				layout = R.layout.quickstatus;
			}
			return new OriginalViewHolder(LayoutInflater.from(sla).inflate(layout, viewGroup, false));
		}

		@Override
		public void onBindViewHolder(OriginalViewHolder viewHolder, final int position) {
			// データ表示
			final View layout=viewHolder.itemView;
			if (sla.list != null && sla.list.size() > position && sla.list.get(position) != null) {
				Server sv=getItem(position);
				layout.setTag(sv);
				if(sla.pinging.get(sv)){
					((TextView)layout.findViewById(R.id.serverName)).setText(R.string.working);
					((TextView)layout.findViewById(R.id.pingMillis)).setText(R.string.working);
					((TextView)layout.findViewById(R.id.serverAddress)).setText(sv.ip + ":" + sv.port);
					((TextView)layout.findViewById(R.id.serverPlayers)).setText("-/-");
					((ExtendedImageView)layout.findViewById(R.id.statColor)).setColor(sla.getResources().getColor(R.color.stat_pending));
				}else{
					if (sv instanceof ServerStatus) {
						ServerStatus s=(ServerStatus)sv;
						((ExtendedImageView) layout.findViewById(R.id.statColor)).setColor(sla.getResources().getColor(R.color.stat_ok));
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
							((TextView) layout.findViewById(R.id.serverPlayers)).setText(fs.getData().get("numplayers") + "/" + fs.getData().get("maxplayers"));
						} else if (s.response instanceof Reply19) {//PC 1.9~
							Reply19 rep = (Reply19) s.response;
							if (rep.description == null) {
								title = s.ip + ":" + s.port;
							} else {
								title = rep.description.text;
							}
							((TextView) layout.findViewById(R.id.serverPlayers)).setText(rep.players.online + "/" + rep.players.max);
						} else if (s.response instanceof Reply) {//PC
							Reply rep = (Reply) s.response;
							if (rep.description == null) {
								title = s.ip + ":" + s.port;
							} else {
								title = rep.description;
							}
							((TextView) layout.findViewById(R.id.serverPlayers)).setText(rep.players.online + "/" + rep.players.max);
						} else if (s.response instanceof SprPair) {//PE?
							SprPair sp = ((SprPair) s.response);
							if (sp.getB() instanceof UnconnectedPing.UnconnectedPingResult) {
								UnconnectedPing.UnconnectedPingResult res = (UnconnectedPing.UnconnectedPingResult) sp.getB();
								title = res.getServerName();
								((TextView) layout.findViewById(R.id.serverPlayers)).setText(res.getPlayersCount() + "/" + res.getMaxPlayers());
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
								((TextView) layout.findViewById(R.id.serverPlayers)).setText(fs.getData().get("numplayers") + "/" + fs.getData().get("maxplayers"));
							} else {
								title = s.ip + ":" + s.port;
								((TextView) layout.findViewById(R.id.serverPlayers)).setText("-/-");
							}
						} else if (s.response instanceof UnconnectedPing.UnconnectedPingResult) {//PE
							UnconnectedPing.UnconnectedPingResult res = (UnconnectedPing.UnconnectedPingResult) s.response;
							title = res.getServerName();
							((TextView) layout.findViewById(R.id.serverPlayers)).setText(res.getPlayersCount() + "/" + res.getMaxPlayers());
						} else {//Unreachable
							title = s.ip + ":" + s.port;
							((TextView) layout.findViewById(R.id.serverPlayers)).setText("-/-");
						}
						if (sla.pref.getBoolean("colorFormattedText", false)) {
							if (sla.pref.getBoolean("darkBackgroundForServerName", false)) {
								((TextView) layout.findViewById(R.id.serverName)).setText(parseMinecraftFormattingCodeForDark(title));
							} else {
								((TextView) layout.findViewById(R.id.serverName)).setText(parseMinecraftFormattingCode(title));
							}
						} else {
							((TextView) layout.findViewById(R.id.serverName)).setText(deleteDecorations(title));
						}
						((TextView) layout.findViewById(R.id.pingMillis)).setText(s.ping + " ms");
						((TextView) layout.findViewById(R.id.serverAddress)).setText(s.ip + ":" + s.port);
					} else {
						((ExtendedImageView)layout.findViewById(R.id.statColor)).setColor(sla.getResources().getColor(R.color.stat_error));
						((TextView)layout.findViewById(R.id.serverName)).setText(sv.ip + ":" + sv.port);
						((TextView)layout.findViewById(R.id.pingMillis)).setText(R.string.notResponding);
						((TextView)layout.findViewById(R.id.serverPlayers)).setText("-/-");
						((TextView)layout.findViewById(R.id.serverAddress)).setText(sv.ip + ":" + sv.port);
					}
				}
			}

			applyHandlersForViewTree(viewHolder.itemView,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onItemClick(null,layout,sla.list.indexOf(layout.getTag()),Long.MIN_VALUE);
					}
				}
			,
				new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						return onItemLongClick(null,layout,sla.list.indexOf(layout.getTag()),Long.MIN_VALUE);
					}
				}
			);
		}

		@Override
		public int getItemCount() {
			// TODO: Implement this method
			return sla.list.size();
		}

		@Override
		public int getItemViewType(int position) {
			// TODO: Implement this method
			return sla.statLayout.getStatusAt(position);
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
				sla.pinging.put(sla.list.get(sla.clicked), true);
				sla.statLayout.setStatusAt(sla.clicked, 1);
				sla.sl.notifyItemChanged(sla.clicked);
				sla.wd.showWorkingDialog();
			}
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> p1, View p2, final int p3, long p4) {
			// TODO: Implement this method
			sla.clicked = p3;
			new AppCompatAlertDialog.Builder(sla)
				.setTitle(getItem(p3).toString())
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
									sla.pinging.put(sla.list.get(p3), true);
									sla.statLayout.setStatusAt(p3,1);
									sla.sl.notifyItemChanged(p3);
									sla.wd.showWorkingDialog();
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
													sla.sl.notifyItemChanged(ofs);
													sla.dryUpdate(s,true);
													sla.statLayout.setStatusAt(sla.clicked, 1);
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

		private String[] generateSubMenu(boolean isPC) {
			List<String> result=new ArrayList<String>(Arrays.<String>asList(sla.getResources().getStringArray(R.array.serverSubMenu)));
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
		
		public Server getItem(int ofs){
			return sla.list.get(ofs);
		}
		

		public void add(Server object) {
			// TODO: Implement this method
			if (!sla.list.contains(object)){
				sla.statLayout.addStatuses(1);
				sla.list.add(object);
				notifyItemInserted(getItemCount());
			}
		}

		public void addAll(Server[] items) {
			// TODO: Implement this method
			for (Server s:items)add(s);
		}

		public void addAll(Collection<? extends Server> collection) {
			// TODO: Implement this method
			for (Server s:collection)add(s);
		}

		public void remove(Server object) {
			// TODO: Implement this method
			int ofs=sla.list.indexOf(object);
			sla.list.remove(object);
			notifyItemRemoved(ofs);
		}
		
		class OriginalViewHolder extends RecyclerView.ViewHolder{
			View localView;
			public OriginalViewHolder(View v){
				super(v);
				localView=v;
			}
			public View findViewById(int resId){
				return localView.findViewById(resId);
			}
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
							Server sn=s.cloneAsServer();
							act().list.set(i_, sn);
							act().pinging.put(act().list.get(i_), false);
							act().statLayout.setStatusAt(i_,0);
							act().sl.notifyItemChanged(i_);
							if (closeDialog)
								act().wd.hideWorkingDialog();
							if (statTabOfs != -1)
								Toast.makeText(act(), R.string.serverOffline, Toast.LENGTH_SHORT).show();
							if (!act().pinging.containsValue(true))
								act().srl.setRefreshing(false);
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
						act().list.set(i_, s);
						act().pinging.put(act().list.get(i_), false);
						act().statLayout.setStatusAt(i_, 2);
						act().sl.notifyItemChanged(i_);
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
public class ServerListActivity extends CompatActivityGroup {
	public static WeakReference<ServerListActivity> instance=new WeakReference(null);

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
		Bundle log=new Bundle();
		log.putString("class",getClass().getName());
		TheApplication.instance.firebase.logEvent("launch",log);
		if(pref.getBoolean("useOldActivity",false))
			setContentView(getLocalActivityManager().startActivity("main", new Intent(this, Content$Old.class)).getDecorView());
		else
			setContentView(getLocalActivityManager().startActivity("main", new Intent(this, Content.class)).getDecorView());
	}
	public static class Content extends ServerListActivityImpl {public static void deleteRef(){instance=new WeakReference<>(null);}}
	public static class Content$Old extends com.nao20010128nao.Wisecraft.old.ServerListActivity {public static void deleteRef(){instance=new WeakReference<>(null);}}
	
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
