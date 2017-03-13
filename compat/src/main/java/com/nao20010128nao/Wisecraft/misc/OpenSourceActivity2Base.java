package com.nao20010128nao.Wisecraft.misc;
import android.os.*;
import android.support.v7.app.*;
import android.view.*;
import com.nao20010128nao.Wisecraft.misc.compat.*;
import java.io.*;
import us.feras.mdv.*;
import java.net.*;
import com.nao20010128nao.Wisecraft.*;

public abstract class OpenSourceActivity2Base extends AppCompatActivity
{
	static final String WISECRAFT_OPEN_SOURCE_LICENSE_ONLINE_DIR="https://github.com/nao20010128nao/Wisecraft/raw/master/OPEN_SOURCE_LICENSES.md";
	
	MarkdownView markdownView;
	File oslMdCache;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.only_toolbar);
		getLayoutInflater().inflate(R.layout.open_source_markdown,(ViewGroup)findViewById(R.id.frame));
		markdownView=(MarkdownView)findViewById(R.id.markdownView);
		oslMdCache=new File(getCacheDir(),"openSourceLicense.md");
		if(CompatUtils.isOnline(this)){
			markdownView.loadMarkdownFile(WISECRAFT_OPEN_SOURCE_LICENSE_ONLINE_DIR);
			new CacheDownloader().execute();
		}else{
			if(oslMdCache.exists()){
				try {
					markdownView.loadMarkdownFile(oslMdCache.toURI().toURL().toString());
				} catch (MalformedURLException e) {
					// unreachable, so dont handle
				}
			} else {
				markdownView.loadMarkdown("Sorry, but we cannot show you Open Source License.    \nPlease connect to the Internet and open again.    \nThanks.");
			}
		}
	}
	
	class CacheDownloader extends AsyncTask<Void,Void,Void> {
		@Override
		protected Void doInBackground(Void[] p1) {
			Reader r=null;
			Writer w=null;
			try{
				r=new BufferedReader(new InputStreamReader(new URL(WISECRAFT_OPEN_SOURCE_LICENSE_ONLINE_DIR).openStream()));
				w=new FileWriter(oslMdCache);
				char[] buf=new char[1024];
				while(true){
					int ra=r.read(buf);
					if(ra<=0)break;
					w.write(buf,0,ra);
				}
			}catch(Throwable e){
				WisecraftError.report("OpenSourceActivity2Base",e);
			}finally{
				try {
					if (r != null)r.close();
				} catch (IOException e) {}
				try {
					if (w != null)w.close();
				} catch (IOException e) {}
			}
			return null;
		}
	}
}
