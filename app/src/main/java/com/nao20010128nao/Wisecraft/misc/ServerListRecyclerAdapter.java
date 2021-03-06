package com.nao20010128nao.Wisecraft.misc;

import android.content.*;
import android.preference.*;
import android.text.*;
import android.view.*;
import com.nao20010128nao.Wisecraft.misc.ping.methods.pc.*;
import com.nao20010128nao.Wisecraft.misc.ping.methods.pe.*;

import java.util.*;

import static com.nao20010128nao.Wisecraft.misc.Utils.*;

public class ServerListRecyclerAdapter extends ListRecyclerViewAdapter<ServerStatusWrapperViewHolder, Server> {
    ServerListArrayList slist;
    boolean isGrid;
    Context context;
    List<BindViewHolderListener> bhListeners = new ArrayList<>();
    SharedPreferences pref;
    PingingMap pinging = new PingingMap();
    boolean showTitle;
    ServerListStyleLoader slsl;

    public ServerListRecyclerAdapter(Context ctx) {
        this(new ArrayList<>(), ctx);
    }

    public ServerListRecyclerAdapter(List<Server> list, Context ctx) {
        this(new ServerListArrayList(list), ctx);
    }

    private ServerListRecyclerAdapter(ServerListArrayList list, Context ctx) {
        super(list);
        slist = list;
        context = ctx;
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        configure();
        slsl = new ServerListStyleLoader(ctx);
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
        if (showTitle) notifyItemRangeChanged(0, size());
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public void useGridLayout(boolean isGrid) {
        this.isGrid = isGrid;
    }

    public boolean useGridLayout() {
        return isGrid;
    }

    public void setLayoutMode(int mode) {
        switch (mode) {
            case 0:
            default:
                isGrid = false;
                break;
            case 1:
            case 2:
                isGrid = true;
                break;
        }
        notifyItemRangeChanged(0, size());
    }

    public void addBindViewHolderListener(BindViewHolderListener b) {
        bhListeners.add(b);
    }

    public void removeBindViewHolderListener(BindViewHolderListener b) {
        bhListeners.remove(b);
    }

    public Map<Server, Boolean> getPingingMap() {
        return pinging;
    }


    private void configure() {
        setLayoutMode(pref.getInt("serverListStyle2", 0));
    }


    @Override
    public ServerStatusWrapperViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        return new ServerStatusWrapperViewHolder(context, isGrid, parent);
    }

    @Override
    @ShowsServerList
    public void onBindViewHolder(ServerStatusWrapperViewHolder viewHolder, int offset) {
        Server s = getItem(offset);
        viewHolder.setServer(s).setServerPlayers("-/-");
        slsl.applyTextColorTo(viewHolder);
        if ((TextUtils.isEmpty(s.name) || s.toString().equals(s.name)) & (!showTitle)) {
            viewHolder.hideServerTitle();
        } else {
            if (pref.getBoolean("serverListColorFormattedText", false)) {
                viewHolder.setServerTitle(parseMinecraftFormattingCode(s.name));
            } else {
                viewHolder.setServerTitle(deleteDecorations(s.name));
            }
            viewHolder.showServerTitle();
        }
        if (pinging.get(s)) {
            viewHolder.pending(s, context);
        } else {
            if (s.isOnline()) {
                ServerStatus sv = (ServerStatus) s;
                viewHolder.online(context);
                final String title;
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
                if (pref.getBoolean("serverListColorFormattedText", false)) {
                    viewHolder.setServerName(parseMinecraftFormattingCode(title));
                } else {
                    viewHolder.setServerName(deleteDecorations(title));
                }
                viewHolder
                    .setPingMillis(sv.ping);
            } else {
                viewHolder.offline(s, context);
            }
        }

        for (BindViewHolderListener lis : bhListeners) lis.onBindViewHolder(viewHolder, offset);
    }

    public interface BindViewHolderListener {
        void onBindViewHolder(ServerStatusWrapperViewHolder parent, int offset);
    }

    private class PingingMap extends NonNullableMap<Server> {
        @Override
        public Boolean put(Server key, Boolean value) {
            Boolean b = super.put(key, value);
            notifyItemChanged(slist.indexOf(key));
            return b;
        }
    }
}
