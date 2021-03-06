package com.nao20010128nao.Wisecraft.widget;

import android.content.*;
import android.os.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.*;
import com.annimon.stream.*;
import com.nao20010128nao.Wisecraft.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.remoteServerList.*;

import java.util.*;

public class WidgetsEditorActivity extends AppCompatActivity {
    SharedPreferences widgetPref;
    RecyclerView rv;
    Adapter adap;
    BroadcastHandler bh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePatcher.applyThemeForActivity(this);
        super.onCreate(savedInstanceState);
        widgetPref = PingWidget.getWidgetPref(this);
        setContentView(R.layout.recycler_view_content);
        rv = findViewById(android.R.id.list);
        adap = new Adapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adap);
        reload();
        IntentFilter inf = new IntentFilter();
        inf.addAction(PingWidget.STATUS_OBSERVE_ACTION);
        registerReceiver(bh = new BroadcastHandler(), inf);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bh);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, R.string.update_all);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                for (String s : listWidgets()) {
                    int wid = Integer.valueOf(s);
                    doUpdate(wid);
                }
                return true;
        }
        return false;
    }

    private void reload() throws NumberFormatException {
        adap.clear();
        for (String s : listWidgets()) {
            int wid = Integer.valueOf(s);
            adap.add(new Trio<>(PingWidget.getServer(this, wid), PingWidget.getWidgetData(this, wid), wid));
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TheApplication.injectContextSpecial(newBase));
    }

    List<String> listWidgets() {
        ArrayList<String> list = new ArrayList<>();
        Stream.of(widgetPref.getAll().keySet())
            .filter(s -> s.endsWith(".data"))
            .filter(s -> s.endsWith(".status"))
            .filter(s -> s.endsWith("_version"))
            .sortBy(a -> a)
            .forEach(list::add);
        Collections.sort(list);
        return list;
    }

    class Adapter extends ListRecyclerViewAdapter<PingWidgetEditorViewHolder, Trio<Server, PingWidget.WidgetData, Integer>> {

        @Override
        public PingWidgetEditorViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            return new PingWidgetEditorViewHolder(WidgetsEditorActivity.this, p1);
        }

        @Override
        public void onBindViewHolder(PingWidgetEditorViewHolder p1, final int p2) {
            p1.setServer(getItem(p2).getA());
            p1.findViewById(R.id.update).setOnClickListener(v -> doUpdate(getItem(p2).getC()));
            p1.findViewById(R.id.edit).setOnClickListener(v -> doEdit(getItem(p2).getC()));
        }
    }

    class BroadcastHandler extends BroadcastReceiver {

        @Override
        public void onReceive(Context p1, Intent p2) {
            adap.notifyItemRangeChanged(0, adap.size());
        }
    }

    public void doEdit(final int wid) {
        Server sv = PingWidget.getServer(this, wid);
        PingWidget.WidgetData data = PingWidget.getWidgetData(this, wid);

        View dialog = getLayoutInflater().inflate(R.layout.server_add_dialog_new, null);
        final LinearLayout peFrame = dialog.findViewById(R.id.pe);
        final LinearLayout pcFrame = dialog.findViewById(R.id.pc);
        final EditText pe_ip = dialog.findViewById(R.id.pe).findViewById(R.id.serverIp);
        final EditText pe_port = dialog.findViewById(R.id.pe).findViewById(R.id.serverPort);
        final EditText pc_ip = dialog.findViewById(R.id.pc).findViewById(R.id.serverIp);
        final CheckBox split = dialog.findViewById(R.id.switchFirm);
        final EditText serverName = dialog.findViewById(R.id.serverName);
        serverName.setVisibility(View.GONE);

        switch (sv.mode) {
            case PE:
                pe_ip.setText(sv.ip);
                pe_port.setText(sv.port + "");
                split.setChecked(false);
                pcFrame.setVisibility(View.GONE);
                peFrame.setVisibility(View.VISIBLE);
                break;
            case PC:
                pc_ip.setText(sv.toString());
                split.setChecked(true);
                peFrame.setVisibility(View.GONE);
                pcFrame.setVisibility(View.VISIBLE);
                break;
        }

        split.setOnClickListener(v -> {
            if (split.isChecked()) {
                //PE->PC
                peFrame.setVisibility(View.GONE);
                pcFrame.setVisibility(View.VISIBLE);
                split.setText(R.string.pc);
                StringBuilder result = new StringBuilder();
                result.append(pe_ip.getText());

                int port = Integer.valueOf(pe_port.getText().toString()).intValue();
                if (!(port == 25565 | port == 19132))
                    result.append(':').append(pe_port.getText());
                pc_ip.setText(result);
            } else {
                //PC->PE
                pcFrame.setVisibility(View.GONE);
                peFrame.setVisibility(View.VISIBLE);
                split.setText(R.string.pe);
                Server s = Utils.convertServerObject(Arrays.asList(MslServer.makeServerFromString(pc_ip.getText().toString(), false))).get(0);
                pe_ip.setText(s.ip);
                pe_port.setText(s.port + "");
            }
        });

        new AlertDialog.Builder(this, ThemePatcher.getDefaultDialogStyle(this)).
            setTitle(sv + " (" + wid + ")").
            setView(dialog).
            setPositiveButton(android.R.string.yes, (d, sel) -> {
                Server s;
                if (split.isChecked()) {
                    s = Utils.convertServerObject(Arrays.asList(MslServer.makeServerFromString(pc_ip.getText().toString(), false))).get(0);
                } else {
                    s = new Server();
                    s.ip = pe_ip.getText().toString();
                    s.port = Integer.valueOf(pe_port.getText().toString());
                    s.mode = Protobufs.Server.Mode.PE;
                }

                PingWidget.setServer(WidgetsEditorActivity.this, wid, s);
                doUpdate(wid);
                reload();
            }).
            setNegativeButton(android.R.string.no, (d, sel) -> {
            }).
            show();

    }

    public void doUpdate(int wid) {
        sendBroadcast(new Intent(this, PingWidget.PingHandler.class).putExtra("wid", wid));//let PingWidget to update the view
    }
}
