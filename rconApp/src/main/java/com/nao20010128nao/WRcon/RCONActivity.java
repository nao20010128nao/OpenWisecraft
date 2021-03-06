package com.nao20010128nao.WRcon;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v4.graphics.drawable.*;
import android.support.v4.view.*;
import android.support.v7.app.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.nao20010128nao.Wisecraft.rcon.*;

import java.math.*;
import java.util.*;

public class RCONActivity extends RCONActivityBase implements TabHost.OnTabChangeListener {
    boolean didSuccess = false;
    String password;
    FragmentTabHost fth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(getIntent().getStringExtra("ip") + ":" + getIntent().getIntExtra("port", 0));
        ssb.setSpan(new ForegroundColorSpan(Color.WHITE), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        setTitle(ssb);
    }

    @Override
    public void exitActivity() {
        new AlertDialog.Builder(this, R.style.AppAlertDialog)
                .setMessage(R.string.auSure_exit)
                .setNegativeButton(android.R.string.ok, (di, w) -> RCONActivity.super.exitActivity())
                .setPositiveButton(android.R.string.cancel, (di, w) -> RCONActivity.super.cancelExitActivity())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onConnectionSuccess(String s) {
        didSuccess = true;
        password = s;
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!didSuccess) return super.onCreateOptionsMenu(menu);
        MenuItem reconnect = menu.add(Menu.NONE, 0, 0, R.string.reconnect).setIcon(TheApplication.instance.getTintedDrawable(com.nao20010128nao.MaterialIcons.R.drawable.ic_refresh_black_48dp, Color.WHITE));
        MenuItemCompat.setShowAsAction(reconnect, MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                Intent restart = (Intent) getIntent().clone();
                restart.putExtra("password", password);
                super.exitActivity();
                startActivity(restart);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //onTabChanged("");
    }

    public void onTabChanged(String a) {
        int selected = fth.getCurrentTab();
        int[] colors = new int[fth.getTabWidget().getTabCount()];
        Arrays.fill(colors, Color.argb(new BigDecimal(0xff).multiply(new BigDecimal("0.3")).intValue(), 0xff, 0xff, 0xff));
        colors[selected] = Color.WHITE;
        Drawable tabUnderlineSelected = DrawableCompat.wrap(getResources().getDrawable(R.drawable.abc_tab_indicator_mtrl_alpha));
        DrawableCompat.setTint(tabUnderlineSelected, 0xff_ffffff);
        for (int i = 0; i < fth.getTabWidget().getChildCount(); i++) {
            TextView tv = fth.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextColor(colors[i]);
            fth.getTabWidget().getChildAt(i).setBackgroundColor(getResources().getColor(R.color.mainColor));
        }
        fth.getTabWidget().getChildAt(selected).setBackgroundDrawable(tabUnderlineSelected);
        Log.d("TabChild", fth.getTabWidget().getChildAt(selected).getClass().getName());
    }
}
