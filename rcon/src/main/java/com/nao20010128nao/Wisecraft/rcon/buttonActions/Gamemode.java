package com.nao20010128nao.Wisecraft.rcon.buttonActions;

import android.support.v7.app.*;
import android.util.*;
import com.nao20010128nao.Wisecraft.misc.rcon.*;
import com.nao20010128nao.Wisecraft.rcon.*;

import java.io.*;

public class Gamemode extends NameSelectAction {
    String player = null;
    boolean isPlayer = true;

    public Gamemode(RCONActivityBase a) {
        super(a);
    }

    @Override
    public void onSelected(final String s) {
        Log.d("gamemode", "value:" + s);
        Log.d("gamemode", "playe:" + player);
        if (isPlayer) {
            player = s;
            isPlayer = false;
            onClick(null);
            return;
        }
        new AlertDialog.Builder(this, getActivity().getPresenter().getDialogStyleId())
                .setMessage(getResString(R.string.gamemodeAsk).replace("[PLAYER]", player).replace("[MODE]", s))
                .setPositiveButton(android.R.string.ok, (di, w) -> {
                    getActivity().performSend("gamemode " + s + " " + player);
                    player = null;
                    isPlayer = true;
                })
                .setNegativeButton(android.R.string.cancel, (di, w) -> {
                    player = null;
                    isPlayer = true;
                })
                .show();
    }

    @Override
    public int getViewId() {
        return R.id.gamemode;
    }

    @Override
    public String[] onPlayersList() throws IOException, AuthenticationException {
        if (!isPlayer)
            return getResources().getStringArray(R.array.gamemodeConst);
        else
            return super.onPlayersList();
    }

    @Override
    public String onPlayerNameHint() {
        if (!isPlayer)
            return getResString(R.string.gamemodeHint);
        else
            return null;
    }

    @Override
    public int getTitleId() {
        return R.string.changepgm;
    }
}
