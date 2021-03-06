package com.nao20010128nao.WRcon;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.preference.*;
import android.support.design.widget.*;
import android.support.v4.view.*;
import android.support.v7.app.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.google.gson.*;
import com.nao20010128nao.MaterialIcons.R.*;
import com.nao20010128nao.WRcon.misc.*;
import com.nao20010128nao.Wisecraft.misc.*;

import java.io.*;
import java.util.*;

public class MainActivity extends MainActivityBase1 {
    List<Server> list;
    ServerListAdapter sla;
    Gson gson = new Gson();
    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TheApplication.instance.prepareFirebase();
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        sla = new ServerListAdapter();
        setListAdapter(sla);
        getListView().setLongClickable(true);
        getListView().setOnItemClickListener(sla);
        getListView().setOnItemLongClickListener(sla);
        loadServers();
        new Thread(() -> {
            int launched;
            pref.edit().putInt("launched", (launched = pref.getInt("launched", 0)) + 1).commit();
            if (launched > 5)
                pref.edit().putBoolean("sendInfos_force", true).commit();
        }).start();
        Bundle log = new Bundle();
        log.putString("class", getClass().getName());
        TheApplication.instance.firebaseAnalytics.logEvent("launch", log);
        TheApplication.instance.fbCfgLoader.addOnCompleteListener(result -> TheApplication.instance.collect());
        TheApplication.instance.fbCfgLoader.addOnFailureListener(result -> {
            Log.e("ServerListActivity", "Firebase: failed to load remote config");
            DebugWriter.writeToE("ServerListActivity", result);
            TheApplication.instance.collect();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int foreground = getResources().getColor(R.color.light_icons_foreground);

        SubMenu add = menu.addSubMenu(Menu.NONE, 0, 1, R.string.add).setIcon(TheApplication.instance.getTintedDrawable(drawable.ic_add_black_48dp, Color.WHITE));
        add.add(Menu.NONE, 1, 1, R.string.addSingle).setIcon(TheApplication.instance.getTintedDrawable(drawable.ic_add_black_48dp, foreground));
        add.add(Menu.NONE, 2, 1, R.string.imporT).setIcon(TheApplication.instance.getTintedDrawable(drawable.ic_file_download_black_48dp, foreground));
        add.add(Menu.NONE, 3, 1, R.string.export).setIcon(TheApplication.instance.getTintedDrawable(drawable.ic_file_upload_black_48dp, foreground));
        MenuItemCompat.setShowAsAction(add.getItem(), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        SubMenu misc = menu.addSubMenu(Menu.NONE, 4, 1, R.string.other).setIcon(TheApplication.instance.getTintedDrawable(com.nao20010128nao.MaterialIcons.R.drawable.ic_more_vert_black_48dp, Color.WHITE));
        misc.add(Menu.NONE, 5, 1, R.string.aboutApp).setIcon(TheApplication.instance.getTintedDrawable(com.nao20010128nao.MaterialIcons.R.drawable.ic_info_outline_black_48dp, getResources().getColor(R.color.light_icons_foreground)));
        MenuItemCompat.setShowAsAction(misc.getItem(), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1: {
                final View dialogView = getLayoutInflater().inflate(R.layout.server_add_dialog_new, null);
                new AlertDialog.Builder(this, R.style.AppAlertDialog)
                        .setView(dialogView)
                        .setTitle(R.string.add)
                        .setPositiveButton(android.R.string.ok, (di, w) -> {
                            String ip = ((EditText) dialogView.findViewById(R.id.serverIp)).getText().toString();
                            String portStr = ((EditText) dialogView.findViewById(R.id.serverPort)).getText().toString();
                            int port;
                            try {
                                port = Integer.valueOf(portStr);
                            } catch (Throwable e) {
                                Snackbar.make(findViewById(android.R.id.content), R.string.numError, Snackbar.LENGTH_LONG).show();
                                return;
                            }
                            Server sv = new Server();
                            sv.ip = ip;
                            sv.port = port;
                            sla.add(sv);
                            saveServers();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
            }
            case 2: {
                View dialogView_ = getLayoutInflater().inflate(R.layout.server_list_imp_exp, null);
                final EditText et_ = dialogView_.findViewById(R.id.filePath);
                et_.setText(new File(Environment.getExternalStorageDirectory(), "/Wisecraft/rcon_servers.json").toString());
                dialogView_.findViewById(R.id.selectFile).setOnClickListener(v -> startChooseFileForOpen(new File(et_.getText().toString()), new FileChooserResult() {
                    public void onSelected(File f) {
                        et_.setText(f.toString());
                    }

                    public void onSelectCancelled() {/*No-op*/}
                }));
                new AlertDialog.Builder(MainActivity.this, R.style.AppAlertDialog)
                        .setTitle(R.string.export_typepath)
                        .setView(dialogView_)
                        .setPositiveButton(android.R.string.ok, (di, w) -> {
                            Toast.makeText(MainActivity.this, R.string.exporting, Toast.LENGTH_LONG).show();
                            new AsyncTask<String, Void, File>() {
                                public File doInBackground(String... texts) {
                                    Server[] servs = new Server[list.size()];
                                    servs = list.toArray(servs);
                                    File f = new File(Environment.getExternalStorageDirectory(), "/Wisecraft");
                                    f.mkdirs();
                                    if (Utils.writeToFile(f = new File(texts[0]), gson.toJson(servs, Server[].class)))
                                        return f;
                                    else
                                        return null;
                                }

                                public void onPostExecute(File f) {
                                    if (f != null) {
                                        Toast.makeText(MainActivity.this, getResources().getString(R.string.export_complete).replace("[PATH]", f + ""), Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, getResources().getString(R.string.export_failed), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }.execute(et_.getText().toString());
                        })
                        .show();
                break;
            }
            case 3: {
                View dialogView = getLayoutInflater().inflate(R.layout.server_list_imp_exp, null);
                final EditText et = dialogView.findViewById(R.id.filePath);
                et.setText(new File(Environment.getExternalStorageDirectory(), "/Wisecraft/rcon_servers.json").toString());
                dialogView.findViewById(R.id.selectFile).setOnClickListener(v -> {
                    File f = new File(et.getText().toString());
                    if (f.isFile()) f = f.getParentFile();
                    startChooseFileForSelect(f, new FileChooserResult() {
                        public void onSelected(File f) {
                            et.setText(f.toString());
                        }

                        public void onSelectCancelled() {/*No-op*/}
                    });
                });
                new AlertDialog.Builder(MainActivity.this, R.style.AppAlertDialog)
                        .setTitle(R.string.import_typepath)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.ok, (di, w) -> {
                            Toast.makeText(MainActivity.this, R.string.importing, Toast.LENGTH_LONG).show();
                            new Thread(() -> {
                                File file = new File(et.getText().toString());
                                if (!file.exists()) {
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.fileNotExist, Toast.LENGTH_LONG).show());
                                    return;
                                }
                                String json;
                                try {
                                    json = Utils.readWholeFile(file);
                                } catch (Throwable e) {
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.failedImport, Toast.LENGTH_LONG).show());
                                    return;
                                }
                                if (json == null) {
                                    return;
                                }
                                final Server[] sv = gson.fromJson(json, Server[].class);
                                runOnUiThread(() -> {
                                    sla.addAll(sv);
                                    saveServers();
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.imported).replace("[PATH]", et.getText().toString()), Toast.LENGTH_LONG).show();
                                });
                            }).start();
                        })
                        .show();
                break;
            }
            case 5:
                startActivity(new Intent(this, AboutAppActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item) | true;
    }

    public void loadServers() {
        int version = pref.getInt("serversJsonVersion", 0);
        switch (version) {
            case 0:
                Server[] sa = gson.fromJson(pref.getString("servers", "[]"), Server[].class);
                sla.clear();
                sla.addAll(sa);
                break;
        }
    }

    public void saveServers() {
        new Thread(() -> {
            String json;
            pref.edit().putInt("serversJsonVersion", 0).putString("servers", json = gson.toJson(list)).commit();
            Log.d("json", json);
        }).start();
        sla.notifyDataSetChanged();
    }

    class ServerListAdapter extends AppBaseArrayAdapter<Server> implements ListView.OnItemClickListener, ListView.OnItemLongClickListener {
        public ServerListAdapter() {
            super(MainActivity.this, 0, list = new ArrayList<Server>());
        }

        @Override
        public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
            Intent launcher;
            startActivity(launcher = new Intent(MainActivity.this, RCONActivity.class).putExtra("ip", getItem(p3).ip).putExtra("port", getItem(p3).port));
            Bundle log = new Bundle();
            log.putString("class", getClass().getName());
            log.putAll(launcher.getExtras());
            TheApplication.instance.firebaseAnalytics.logEvent("selectServer", log);
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> p1, View p2, final int position, long p4) {
            final Server server = getItem(position);
            new AlertDialog.Builder(MainActivity.this)
                    .setItems(R.array.rconAppServerSubMenu, (di, w) -> {
                        switch (w) {
                            case 0:
                                new AlertDialog.Builder(MainActivity.this, R.style.AppAlertDialog)
                                        .setMessage(R.string.auSure)
                                        .setPositiveButton(android.R.string.no, null)
                                        .setNegativeButton(android.R.string.yes, (di12, w12) -> {
                                            sla.remove(server);
                                            saveServers();
                                        })
                                        .show();
                                break;
                            case 1:
                                final View dialogView = getLayoutInflater().inflate(R.layout.server_add_dialog_new, null);
                                ((EditText) dialogView.findViewById(R.id.serverIp)).setText(server.ip);
                                ((EditText) dialogView.findViewById(R.id.serverPort)).setText(server.port + "");
                                new AlertDialog.Builder(MainActivity.this, R.style.AppAlertDialog)
                                        .setView(dialogView)
                                        .setTitle(R.string.edit)
                                        .setPositiveButton(android.R.string.ok, (di1, w1) -> {
                                            String ip = ((EditText) dialogView.findViewById(R.id.serverIp)).getText().toString();
                                            String portStr = ((EditText) dialogView.findViewById(R.id.serverPort)).getText().toString();
                                            int port;
                                            try {
                                                port = Integer.valueOf(portStr);
                                            } catch (Throwable e) {
                                                Snackbar.make(findViewById(android.R.id.content), R.string.numError, Snackbar.LENGTH_LONG).show();
                                                return;
                                            }
                                            server.ip = ip;
                                            server.port = port;
                                            saveServers();
                                        })
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .show();
                                break;
                        }
                    })
                    .show();
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.main_server_entry, parent, false);
            ((ImageView) convertView.findViewById(R.id.forward)).setImageDrawable(TheApplication.instance.getTintedDrawable(R.drawable.ic_chevron_right_black_48dp, 0xff_666666));
            ((TextView) convertView.findViewById(R.id.serverIp)).setText(getItem(position).toString());
            return convertView;
        }


        @Override
        public void add(Server object) {
            if (!list.contains(object)) {
                super.add(object);
            }
        }

        @Override
        public void addAll(Server[] items) {
            addAll(Arrays.asList(items));
        }

        @Override
        public void addAll(Collection<? extends Server> collection) {
            for (Server s : collection) add(s);
        }
    }
}
