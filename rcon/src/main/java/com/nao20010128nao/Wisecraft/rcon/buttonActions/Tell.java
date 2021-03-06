package com.nao20010128nao.Wisecraft.rcon.buttonActions;

import android.support.v7.app.*;
import android.view.*;
import android.widget.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.rcon.*;
import com.nao20010128nao.Wisecraft.rcon.*;

import java.io.*;

import static com.nao20010128nao.Wisecraft.misc.RconModule_Utils.*;

public class Tell extends NameSelectAction {
    String player, item;
    Button changePlayer, changeItem;
    TextView playerView, itemView;
    String playerHint, itemHint;
    /*      |             ,           |*/

    Button executeButton;
    AlertDialog dialog;

    String[] list;
    String hint;
    int selecting;

    public Tell(RCONActivityBase r) {
        super(r);
    }

    @Override
    public void onClick(View p1) {
        dialog = new AlertDialog.Builder(this, getActivity().getPresenter().getDialogStyleId())
                .setView(inflateDialogView())
                .show();
    }

    @Override
    public int getViewId() {
        return R.id.tell;
    }

    @Override
    public void onSelected(String s) {
        switch (selecting) {
            case 1:
                player = s;
                playerView.setText(s);
                break;
            case 2:
                item = s;
                itemView.setText(s);
                break;
        }
        selecting = -1;
    }

    public View inflateDialogView() {
        View v = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.tell_screen, null, false);
        changePlayer = v.findViewById(R.id.changePlayer);
        changeItem = v.findViewById(R.id.changeItem);

        executeButton = v.findViewById(R.id.execute);

        playerView = v.findViewById(R.id.playerName);
        itemView = v.findViewById(R.id.itemId);

        changePlayer.setOnClickListener(v13 -> {
            hint = getResString(R.string.givePlayerHint);
            list = null;
            selecting = 1;
            Tell.super.onClick(v13);
        });
        changeItem.setOnClickListener(v12 -> {
            hint = getResString(R.string.tellMessageHint);
            list = RconModule_Constant.EMPTY_STRING_ARRAY;
            selecting = 2;
            Tell.super.onClick(v12);
        });
        executeButton.setOnClickListener(v1 -> {
            if (isNullString(player) || isNullString(item)) {
                AlertDialog.Builder b = new AlertDialog.Builder(Tell.this, getActivity().getPresenter().getDialogStyleId());
                String mes = "";
                if (isNullString(player)) {
                    mes += getResString(R.string.giveSelectPlayer) + "\n";
                }
                if (isNullString(item)) {
                    mes += getResString(R.string.tellSetMessage) + "\n";
                }
                b.setMessage(mes);
                b.setPositiveButton(android.R.string.ok, RconModule_Constant.BLANK_DIALOG_CLICK_LISTENER);
                b.show();
            } else {
                getActivity().performSend("tell " + player + " " + item);
                dialog.dismiss();
            }
        });
        return v;
    }

    @Override
    public String[] onPlayersList() throws IOException, AuthenticationException {
        if (list == null) {
            return super.onPlayersList();
        } else {
            return list;
        }
    }

    @Override
    public String onPlayerNameHint() {
        return hint;
    }

    @Override
    public int getTitleId() {
        return R.string.tell;
    }
}
