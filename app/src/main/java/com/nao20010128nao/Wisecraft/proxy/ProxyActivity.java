package com.nao20010128nao.Wisecraft.proxy;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.nao20010128nao.MCProxy.MultipleUdpConnectionProxy;
import com.nao20010128nao.Wisecraft.R;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ProxyActivity extends Activity {
	MultipleUdpConnectionProxy prox;
	TextView serverIp,serverCon;
	Button stop;
	String ip;
	int port;
	Thread proxyThread;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.proxy_screen);
		serverIp = (TextView)findViewById(R.id.serverIp);
		serverCon = (TextView)findViewById(R.id.serverCon);
		stop=(Button)findViewById(R.id.stop);
		ip = getIntent().getStringExtra("ip");
		port = getIntent().getIntExtra("port", 19132);
		
		serverIp.setText(ip+":"+port);
		serverCon.setText("localhost:64321");
		
		stop.setOnClickListener(new OnClickListener(){
			public void onClick(View a){
				finish();
			}
		});
		
		dialog1();
	}

	public void dialog1(){
		new AlertDialog.Builder(this)
			.setMessage(R.string.proxy_attention_1)
			.setCancelable(false)
			.setPositiveButton(R.string.next,new AlertDialog.OnClickListener(){
				public void onClick(DialogInterface di,int w){
					dialog2();
				}
			})
			.setNegativeButton(R.string.close,new AlertDialog.OnClickListener(){
				public void onClick(DialogInterface di,int w){
					finish();
				}
			})
			.setTitle("1/2")
			.show();
	}
	
	public void dialog2(){
		new AlertDialog.Builder(this)
			.setMessage(R.string.proxy_attention_2)
			.setCancelable(false)
			.setPositiveButton(R.string.next,new AlertDialog.OnClickListener(){
				public void onClick(DialogInterface di,int w){
					start();
				}
			})
			.setNegativeButton(R.string.close,new AlertDialog.OnClickListener(){
				public void onClick(DialogInterface di,int w){
					finish();
				}
			})
			.setTitle("2/2")
			.show();
	}
	
	public void start(){
		proxyThread=new Thread(prox=new MultipleUdpConnectionProxy(ip,port,64321));
		proxyThread.start();
	}
	
	@Override
	public void finish() {
		// TODO: Implement this method
		super.finish();
		if(proxyThread!=null)proxyThread.interrupt();
	}
	
	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
	}
}
