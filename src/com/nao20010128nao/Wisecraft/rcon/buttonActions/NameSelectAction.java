package com.nao20010128nao.Wisecraft.rcon.buttonActions;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.rconclient.rcon.*;
import com.nao20010128nao.Wisecraft.*;
import com.nao20010128nao.Wisecraft.rcon.*;
import java.io.*;

public abstract class NameSelectAction extends BaseAction
{
	EditText name;
	ListView online;
	Button submit;
	AlertDialog dialog;
	public NameSelectAction(RCONActivity act){
		super(act);
	}

	@Override
	public void onClick(View p1) {
		// TODO: Implement this method
		dialog=new AlertDialog.Builder(this)
			.setView(inflatePlayersView())
			.show();
	}
	private View inflatePlayersView(){
		View v=((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.rcon_players_base,null,false);
		online=(ListView)v.findViewById(R.id.players);
		name=(EditText)v.findViewById(R.id.playerName);
		submit=(Button)v.findViewById(R.id.ok);
		final ArrayAdapter aa=new ArrayAdapter(this,android.R.layout.simple_list_item_1);
		online.setAdapter(aa);
		online.setOnItemClickListener(new ListView.OnItemClickListener(){
			public void onItemClick(AdapterView a,View v,int o,long i){
				name.setText(aa.getItem(o).toString());
			}
		});
		submit.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View p1) {
				// TODO: Implement this method
				try{
					dialog.cancel();
					dialog.dismiss();
					dialog.hide();
					dialog=null;
				}finally{
					onSelected(name.getText().toString());
				}
			}
		});
		String hint=onPlayerNameHint();
		if(hint!=null){
			name.setHint(hint);
		}
		new AsyncTask<Void,Void,String[]>(){
			public String[] doInBackground(Void[] a){
				try {
					return onPlayersList();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (AuthenticationException e) {
					e.printStackTrace();
				}
				return null;
			}
			public void onPostExecute(String[] s){
				aa.addAll(s);
			}
		}.execute();
		return v;
	}
	public String[] onPlayersList()throws IOException,AuthenticationException{
		return getActivity().getRCon().list();
	}
	public String onPlayerNameHint(){
		return null;
	}
	public abstract void onSelected(String name);
}
