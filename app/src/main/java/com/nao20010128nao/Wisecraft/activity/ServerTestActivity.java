package com.nao20010128nao.Wisecraft.activity;

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.support.v4.content.*;
import android.support.v4.view.*;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.*;
import com.nao20010128nao.Wisecraft.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.contextwrappers.extender.*;
import com.nao20010128nao.Wisecraft.misc.ping.methods.pc.*;
import com.nao20010128nao.Wisecraft.misc.ping.methods.pe.*;
import com.nao20010128nao.Wisecraft.misc.ping.processors.*;

import java.lang.ref.*;
import java.util.*;

import static com.nao20010128nao.Wisecraft.misc.Utils.*;

@ShowsServerList
class ServerTestActivityImpl extends AppCompatActivity implements ServerListActivityInterface {
    static WeakReference<ServerTestActivityImpl> instance = new WeakReference(null);

    ServerPingProvider spp = new NormalServerPingProvider();
    ServerList sl;
    List<Server> list;
    int clicked = -1;
    ProgressDialog waitDialog;
    int times, port;
    String ip;
    Protobufs.Server.Mode mode;
    View dialog;
    SharedPreferences pref;
    RecyclerView rv;
    ServerListStyleLoader slsl;

    Map<Integer, Boolean> pinging = new HashMap<Integer, Boolean>() {
        @Override
        public Boolean get(Object key) {
            Boolean b = super.get(key);
            if (b == null) {
                return false;
            }
            return b;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        ThemePatcher.applyThemeForActivity(this);
        super.onCreate(savedInstanceState);
        boolean usesOldInstance = false;
        if (instance.get() != null) {
            sl = instance.get().sl;
            pinging = instance.get().pinging;
            list = instance.get().list;
            usesOldInstance = true;
        } else {
            sl = new ServerList(this);
        }
        instance = new WeakReference(this);
        slsl = (ServerListStyleLoader) getSystemService(ContextWrappingExtender.SERVER_LIST_STYLE_LOADER);
        setContentView(R.layout.recycler_view_content);
        rv = findViewById(android.R.id.list);
        switch (pref.getInt("serverListStyle2", 0)) {
            case 0:
            default:
                rv.setLayoutManager(new LinearLayoutManager(this));
                break;
            case 1:
                GridLayoutManager glm = new GridLayoutManager(this, calculateRows(this));
                rv.setLayoutManager(glm);
                break;
            case 2:
                StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(calculateRows(this), StaggeredGridLayoutManager.VERTICAL);
                rv.setLayoutManager(sglm);
                break;
        }
        rv.setAdapter(sl);
        ip = getIntent().getStringExtra("ip");
        port = getIntent().getIntExtra("port", -1);
        mode = (Protobufs.Server.Mode) getIntent().getSerializableExtra("mode");
        if (!(usesOldInstance & sl.getItemCount() != 0)) {
            new AlertDialog.Builder(this, ThemePatcher.getDefaultDialogStyle(this))
                .setTitle(R.string.testServer)
                .setView(dialog = getLayoutInflater().inflate(R.layout.test_server_dialog, null, false))
                .setPositiveButton(android.R.string.ok, (di, w) -> {
                    di.dismiss();
                    String nu = ((EditText) dialog.findViewById(R.id.pingTimes)).getText().toString();
                    try {
                        times = Integer.valueOf(nu);
                    } catch (NumberFormatException e) {
                        finish();
                        return;
                    }
                    setTitle(ip + ":" + port + " x " + times);
                    for (int i = 0; i < times; i++) {
                        Server s = new Server();
                        s.ip = ip;
                        s.port = port;
                        s.mode = mode;
                        sl.add(s);
                        final int position = i;
                        pinging.put(position, true);
                        spp.putInQueue(s, new ServerPingProvider.PingHandler() {
                            public void onPingFailed(final Server s) {
                                runOnUiThread(() -> {
                                    list.set(position, s);
                                    sl.notifyItemChanged(position);
                                    pinging.put(position, false);
                                });
                            }

                            public void onPingArrives(final ServerStatus sv) {
                                runOnUiThread(() -> {
                                    list.set(position, sv);
                                    sl.notifyItemChanged(position);
                                    pinging.put(position, false);
                                });
                            }
                        });
                    }
                })
                .setNegativeButton(android.R.string.cancel, (di, w) -> {
                    di.dismiss();
                    finish();
                })
                .setOnCancelListener(di -> finish())
                .show();
        }

        ViewCompat.setBackground(findViewById(android.R.id.content), slsl.load());
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TheApplication.injectContextSpecial(newBase));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        instance = new WeakReference(null);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (rv.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) rv.getLayoutManager()).setSpanCount(calculateRows(this, rv));
        }
        if (rv.getLayoutManager() instanceof GridLayoutManager) {
            ((GridLayoutManager) rv.getLayoutManager()).setSpanCount(calculateRows(this, rv));
        }
    }

    @Override
    public void addIntoList(Server s) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    static class ServerList extends ListRecyclerViewAdapter<ServerStatusWrapperViewHolder, Server> implements AdapterView.OnItemClickListener {
        ServerTestActivityImpl sta;

        public ServerList(ServerTestActivityImpl parent) {
            super(parent.list = new ArrayList<>());
            sta = parent;
        }

        @Override
        public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
            Server s = getItem(p3);
            sta.clicked = p3;
            if (s.isOnline()) {
                sta.startActivityForResult(new Intent(sta, ServerInfoActivity.class).putExtra("nonUpd", true).putExtra("sta", Utils.encodeForServerInfo((ServerStatus) s)), 0);
            }
        }

        @Override
        @ServerInfoParser
        public void onBindViewHolder(ServerStatusWrapperViewHolder viewHolder, final int offset) {
            Server s = getItem(offset);
            viewHolder.setServer(s).setServerPlayers("-/-");
            sta.slsl.applyTextColorTo(viewHolder);
            viewHolder.hideServerTitle();
            if (sta.pinging.get(offset)) {
                viewHolder.pending(s, sta);
            } else {
                if (s.isOnline()) {
                    ServerStatus sv = (ServerStatus) s;
                    viewHolder.setStatColor(ContextCompat.getColor(sta, R.color.stat_ok));
                    final CharSequence title;
                    if (sv.response instanceof FullStat) {//PE
                        FullStat fs = (FullStat) sv.response;
                        Map<String, String> m = fs.getDataAsMap();
                        if (m.containsKey("hostname")) {
                            title = m.get("hostname");
                        } else if (m.containsKey("motd")) {
                            title = m.get("motd");
                        } else {
                            title = sv.toString();
                        }
                        viewHolder.setServerPlayers(m.get("numplayers"), m.get("maxplayers"));
                    } else if (sv.response instanceof RawJsonReply) {//PC (Obfuscated)
                        RawJsonReply rep = (RawJsonReply) sv.response;
                        if (!rep.json.has("description")) {
                            title = s.toString();
                        } else {
                            title = Utils.parseMinecraftDescriptionJson(rep.json.get("description"));
                        }
                        viewHolder.setServerPlayers(rep.json.get("players").get("online").getAsInt(), rep.json.get("players").get("max").getAsInt());
                    } else if (sv.response instanceof SprPair) {//PE?
                        SprPair sp = ((SprPair) sv.response);
                        if (sp.getB() instanceof UnconnectedPing.UnconnectedPingResult) {
                            UnconnectedPing.UnconnectedPingResult res = (UnconnectedPing.UnconnectedPingResult) sp.getB();
                            title = res.getServerName();
                            viewHolder.setServerPlayers(res.getPlayersCount(), res.getMaxPlayers());
                        } else if (sp.getA() instanceof FullStat) {
                            FullStat fs = (FullStat) sp.getA();
                            Map<String, String> m = fs.getDataAsMap();
                            if (m.containsKey("hostname")) {
                                title = m.get("hostname");
                            } else if (m.containsKey("motd")) {
                                title = m.get("motd");
                            } else {
                                title = sv.toString();
                            }
                            viewHolder.setServerPlayers(m.get("numplayers"), m.get("maxplayers"));
                        } else {
                            title = sv.toString();
                            viewHolder.setServerPlayers();
                        }
                    } else if (sv.response instanceof UnconnectedPing.UnconnectedPingResult) {
                        UnconnectedPing.UnconnectedPingResult res = (UnconnectedPing.UnconnectedPingResult) sv.response;
                        title = res.getServerName();
                        viewHolder.setServerPlayers(res.getPlayersCount(), res.getMaxPlayers());
                    } else {//Unreachable
                        title = sv.toString();
                        viewHolder.setServerPlayers();
                    }
                    if (sta.pref.getBoolean("serverListColorFormattedText", false)) {
                        if (title instanceof String) {
                            viewHolder.setServerName(parseMinecraftFormattingCode(title.toString()));
                        } else {
                            viewHolder.setServerName(title);
                        }
                    } else {
                        if (title instanceof String) {
                            viewHolder.setServerName(deleteDecorations(title.toString()));
                        } else {
                            viewHolder.setServerName(title.toString());
                        }
                    }
                    viewHolder
                        .setPingMillis(sv.ping);
                } else {
                    viewHolder.offline(s, sta);
                }
            }
            applyHandlersForViewTree(viewHolder.itemView, v -> {
                onItemClick(null, v, offset, Long.MIN_VALUE);
            });
        }

        @Override
        public ServerStatusWrapperViewHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
            switch (sta.pref.getInt("serverListStyle2", 0)) {
                case 0:
                default:
                    return new ServerStatusWrapperViewHolder(sta, false, viewGroup);
                case 1:
                case 2:
                    return new ServerStatusWrapperViewHolder(sta, true, viewGroup);
            }
        }

        public void attachNewActivity(ServerTestActivityImpl newSta) {
            sta = newSta;
        }
    }
}

public class ServerTestActivity extends ServerTestActivityImpl {
    public static WeakReference<ServerTestActivity> instance = new WeakReference(null);
}
