package com.nao20010128nao.Wisecraft;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.support.v7.app.*;
import android.widget.*;
import com.nao20010128nao.ToolBox.*;
import com.nao20010128nao.Wisecraft.misc.Factories;
import com.nao20010128nao.Wisecraft.misc.compat.*;
import com.nao20010128nao.Wisecraft.misc.pref.*;
import java.lang.reflect.*;
import java.util.*;
import uk.co.chrisjenx.calligraphy.*;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import com.nao20010128nao.Wisecraft.misc.SetTextColor;
import android.support.v4.content.ContextCompat;

public class FragmentSettingsActivity extends AppCompatActivity {
	public static final Map<String,Class<? extends BaseFragment>> FRAGMENT_CLASSES=new HashMap<String,Class<? extends BaseFragment>>(){{
			put("root",HubPrefFragment.class);
			put("basics",Basics.class);
			put("features",Features.class);
			put("asfsls",Asfsls.class);
	}};
	
	int which;
	SharedPreferences pref;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		pref=PreferenceManager.getDefaultSharedPreferences(this);
		if(pref.getBoolean("useBright",false)){
			setTheme(R.style.AppTheme_Bright);
			getTheme().applyStyle(R.style.AppTheme_Bright,true);
		}
		super.onCreate(savedInstanceState);
		getSupportFragmentManager()
			.beginTransaction()
			.replace(android.R.id.content,new HubPrefFragment())
			.addToBackStack("root")
			.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
			.commit();
	}
	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
	}

	@Override
	public void onBackPressed() {
		// TODO: Implement this method
		FragmentManager sfm=getSupportFragmentManager();
		if(sfm.getBackStackEntryCount()<2){
			finish();
			return;
		}
		sfm.popBackStack();
	}
	
	
	public static class HubPrefFragment extends BaseFragment {
		SharedPreferences pref;
		@Override
		public void onCreatePreferences(Bundle p1, String p2) {
			// TODO: Implement this method
			addPreferencesFromResource(R.xml.settings_parent_compat);
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			// TODO: Implement this method
			pref=PreferenceManager.getDefaultSharedPreferences(getContext());
			super.onCreate(savedInstanceState);
			sH("basics", new HandledPreference.OnClickListener(){
					public void onClick(String a, String b, String c) {
						getActivity().getSupportFragmentManager()
							.beginTransaction()
							.replace(android.R.id.content,new Basics())
							.addToBackStack("basics")
							.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
							.commit();
					}
				});
			sH("features", new HandledPreference.OnClickListener(){
					public void onClick(String a, String b, String c) {
						getActivity()
							.getSupportFragmentManager()
							.beginTransaction()
							.replace(android.R.id.content,new Features())
							.addToBackStack("features")
							.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
							.commit();
					}
				});
			sH("asfsls",new HandledPreference.OnClickListener(){
					public void onClick(String a,String b,String c){
						getActivity()
							.getSupportFragmentManager()
							.beginTransaction()
							.replace(android.R.id.content,new Asfsls())
							.addToBackStack("asfsls")
							.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
							.commit();
					}
				});
			sH("osl",new HandledPreference.OnClickListener(){
					public void onClick(String a,String b,String c){
						startActivity(new Intent(getContext(),OpenSourceActivity.class));
					}
				});
			sH("aboutApp",new HandledPreference.OnClickListener(){
					public void onClick(String a,String b,String c){
						startActivity(new Intent(getContext(),AboutAppActivity.class));
					}
				});
			findPreference("asfsls").setEnabled(pref.getBoolean("feature_asfsls",false));
			((SetTextColor)findPreference("settingsAttention")).setTextColor(ContextCompat.getColor(getContext(),R.color.color888));
		}
		@Override
		public void onResume() {
			// TODO: Implement this method
			super.onResume();
			getActivity().setTitle(R.string.settings);
			findPreference("asfsls").setEnabled(pref.getBoolean("feature_asfsls",false));
		}
	}

	
	public abstract static class BaseFragment extends SHablePreferenceFragment {
		protected SharedPreferences pref;
		@Override
		public void onCreate(Bundle savedInstanceState) {
			// TODO: Implement this method
			pref=PreferenceManager.getDefaultSharedPreferences(getContext());
			super.onCreate(savedInstanceState);
		}

		@Override
		public LayoutInflater getLayoutInflater(Bundle savedInstanceState) {
			// TODO: Implement this method
			return super.getLayoutInflater(savedInstanceState).cloneInContext(getActivity());
		}

		@Override
		public Context getContext() {
			// TODO: Implement this method
			return CalligraphyContextWrapper.wrap(super.getContext());
		}
	}


	public static class Basics extends BaseFragment {
		int which;
		@Override
		public void onCreatePreferences(Bundle p1, String p2) {
			// TODO: Implement this method
			addPreferencesFromResource(R.xml.settings_basic_compat);
			sH("serverListStyle", new HandledPreference.OnClickListener(){
					public void onClick(String a, String b, String c) {
						new AppCompatAlertDialog.Builder(getContext(),R.style.AppAlertDialog)
							.setTitle(R.string.serverListStyle)
							.setSingleChoiceItems(getResources().getStringArray(R.array.serverListStyles),pref.getInt("serverListStyle2",0),new DialogInterface.OnClickListener(){
								public void onClick(DialogInterface di,int w){
									which=w;
								}
							})
							.setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener(){
								public void onClick(DialogInterface di,int w){
									pref.edit().putInt("serverListStyle2",which).commit();
								}
							})
							.setNegativeButton(android.R.string.cancel,new DialogInterface.OnClickListener(){
								public void onClick(DialogInterface di,int w){

								}
							})
							.show();
					}
				});
			sH("selectFont",new HandledPreference.OnClickListener(){
					public void onClick(String a,String b,String c){
						String[] choice=getFontChoices();
						String[] display=TheApplication.instance.getDisplayFontNames(choice);
						final List<String> choiceList=Arrays.<String>asList(choice);
						new AppCompatAlertDialog.Builder(getContext(),R.style.AppAlertDialog)
							.setSingleChoiceItems(display, choiceList.indexOf(TheApplication.instance.getFontFieldName())
							, new DialogInterface.OnClickListener(){
								public void onClick(DialogInterface di, int w) {
									di.cancel();
									TheApplication.instance.setFontFieldName(choiceList.get(w));
									Toast.makeText(getContext(),R.string.saved_fonts,Toast.LENGTH_LONG).show();
								}
							})
							.show();
					}
					String[] getFontChoices() {
						List<String> l=new ArrayList();
						for (Field f:TheApplication.fonts) {
							l.add(f.getName());
						}
						l.remove("icomoon1");
						return Factories.strArray(l);
					}
				});
			findPreference("useBright").setEnabled(getResources().getBoolean(R.bool.useBrightEnabled));
		}

		@Override
		public void onResume() {
			// TODO: Implement this method
			super.onResume();
			getActivity().setTitle(R.string.basics);
		}
	}


	public static class Features extends BaseFragment {
		int which;
		@Override
		public void onCreatePreferences(Bundle p1, String p2) {
			// TODO: Implement this method
			addPreferencesFromResource(R.xml.settings_features_compat);
		}

		@Override
		public void onResume() {
			// TODO: Implement this method
			super.onResume();
			getActivity().setTitle(R.string.features);
		}
	}


	public static class Asfsls extends BaseFragment {
		int which;
		@Override
		public void onCreatePreferences(Bundle p1, String p2) {
			// TODO: Implement this method
			addPreferencesFromResource(R.xml.settings_asfsls_compat);
			SharedPreferences slsVersCache=getContext().getSharedPreferences("sls_vers_cache", 0);
			findPreference("currentSlsVersion").setSummary(slsVersCache.getString("dat.vcode",getResources().getString(R.string.unknown)));
		}

		@Override
		public void onResume() {
			// TODO: Implement this method
			super.onResume();
			getActivity().setTitle(R.string.addServerFromServerListSite);
		}
	}
}
