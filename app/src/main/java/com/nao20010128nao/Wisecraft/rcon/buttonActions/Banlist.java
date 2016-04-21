package com.nao20010128nao.Wisecraft.rcon.buttonActions;
import android.content.DialogInterface;
import android.view.View;
import com.nao20010128nao.Wisecraft.Constant;
import com.nao20010128nao.Wisecraft.R;
import com.nao20010128nao.Wisecraft.misc.compat.AppCompatAlertDialog;
import com.nao20010128nao.Wisecraft.rcon.RCONActivity;

public class Banlist extends BaseAction {
	public Banlist(RCONActivity act) {
		super(act);
	}

	@Override
	public void onClick(View p1) {
		// TODO: Implement this method
		new AppCompatAlertDialog.Builder(this,R.style.AppAlertDialog)
			.setMessage(R.string.auSure)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int w) {
					getActivity().performSend("banlist");
				}
			})
			.setNegativeButton(android.R.string.cancel, Constant.BLANK_DIALOG_CLICK_LISTENER)
			.show();
	}

	@Override
	public int getViewId() {
		// TODO: Implement this method
		return R.id.banlist;
	}
}
