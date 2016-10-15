package com.nao20010128nao.Wisecraft;
import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.support.v7.app.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import com.nao20010128nao.Wisecraft.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.compat.*;
import com.nao20010128nao.Wisecraft.services.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import uk.co.chrisjenx.calligraphy.*;

import com.nao20010128nao.Wisecraft.R;

public class ProxyActivity extends AppCompatActivity {
	public static ServiceController cont;
	
	TextView serverIp,serverCon;
	Button stop;
	String ip;
	int port;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		ThemePatcher.applyThemeForActivity(this);
		super.onCreate(savedInstanceState);
		
		try {
			if(cont==null)cont = new ServiceController(new DatagramSocket(), 35590);
		} catch (SocketException e) {
			DebugWriter.writeToE("ProxyActivity",e);
			finish();
			return;
		}
		
		setContentView(R.layout.proxy_screen);
		serverIp = (TextView)findViewById(R.id.serverIp);
		serverCon = (TextView)findViewById(R.id.serverCon);
		stop = (Button)findViewById(R.id.stop);
		
		stop.setOnClickListener(new OnClickListener(){
				public void onClick(View a) {
					if(cont!=null)cont.stopService();
					finish();
				}
			});
			
		serverCon.setText("localhost:64321");
		
		String act=getIntent().getAction();
		if(act.equals("start")){
			if(isProxyRunning()){
				new AppCompatAlertDialog.Builder(this)
					.setMessage(R.string.mtlIsAlreadyRunning)
					.setCancelable(false)
					.setTitle(R.string.error)
					.setPositiveButton(android.R.string.ok, new AppCompatAlertDialog.OnClickListener(){
						public void onClick(DialogInterface di, int w) {
							finish();
						}
					})
					.show();
				return;
			}
			ip = getIntent().getStringExtra("ip");
			port = getIntent().getIntExtra("port", 19132);

			serverIp.setText(ip + ":" + port);
			
			dialog1();
		}else if(act.equals("status")){
			if(!isProxyRunning()){
				finish();
				return;
			}
			cont.getServer(new ServiceController$GetServerResult(){
				public void onResult(final Server s){
					runOnUiThread(new Runnable(){
						public void run(){
							serverIp.setText(s.toString());
						}
					});
				}
			});
		}else{
			finish();
			return;
		}
	}

	public void dialog1() {
		new AppCompatAlertDialog.Builder(this)
			.setMessage(R.string.mtl_attention_1)
			.setCancelable(false)
			.setPositiveButton(R.string.next, new AppCompatAlertDialog.OnClickListener(){
				public void onClick(DialogInterface di, int w) {
					dialog2();
				}
			})
			.setNegativeButton(R.string.close, new AppCompatAlertDialog.OnClickListener(){
				public void onClick(DialogInterface di, int w) {
					finish();
				}
			})
			.setTitle("1/2")
			.show();
	}

	public void dialog2() {
		new AppCompatAlertDialog.Builder(this)
			.setMessage(R.string.mtl_attention_2)
			.setCancelable(false)
			.setPositiveButton(R.string.next, new AppCompatAlertDialog.OnClickListener(){
				public void onClick(DialogInterface di, int w) {
					start();
				}
			})
			.setNegativeButton(R.string.close, new AppCompatAlertDialog.OnClickListener(){
				public void onClick(DialogInterface di, int w) {
					finish();
				}
			})
			.setTitle("2/2")
			.show();
	}

	public void start() {
		startService(new Intent(this,MCProxyService.class).putExtra("ip",ip).putExtra("port",port));
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(TheApplication.injectContextSpecial(newBase));
	}

	@Override
	public void finish() {
		// TODO: Implement this method
		try {
			Activity.class.getMethod("finishAndRemoveTask").invoke(this);
			return;
		} catch (IllegalAccessException e) {} catch (NoSuchMethodException e) {} catch (IllegalArgumentException e) {} catch (InvocationTargetException e) {}
		super.finish();
	}
	
	public boolean isProxyRunning(){
		ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
		for(ActivityManager.RunningServiceInfo service:am.getRunningServices(Integer.MAX_VALUE))
			if(service.service.getClassName().equals(MCProxyService.class.getName()))
				return true;
		return false;
	}
	
	public static class ServiceController{
		DatagramSocket sock;
		int port;
		public ServiceController(DatagramSocket d,int p){
			sock=d;
			port=p;
		}
		public void stopService(){
			new Thread(){
				public void run(){
					try {
						DatagramPacket dp=new DatagramPacket(new byte[]{0}, 0, 1);
						dp.setAddress(InetAddress.getLocalHost());
						dp.setPort(port);
						sock.send(dp);
					} catch (IOException e) {}
				}
			}.start();
		}
		public void getServer(final ServiceController$GetServerResult result){
			new Thread(){
				public void run(){
					try {
						DatagramPacket dp=new DatagramPacket(new byte[]{1}, 0, 1);
						dp.setAddress(InetAddress.getLocalHost());
						dp.setPort(port);
						sock.send(dp);
						dp=new DatagramPacket(new byte[1000],0,1000);
						sock.receive(dp);
						Server s=new Server();
						DataInputStream dis=new DataInputStream(new ByteArrayInputStream(dp.getData(),0,dp.getLength()));
						s.ip=dis.readUTF();
						s.port=dis.readInt();
						result.onResult(s);
					} catch (IOException e) {}
				}
			}.start();
		}
	}
	
	public static interface ServiceController$GetServerResult{
		public void onResult(Server s);
	}
}
