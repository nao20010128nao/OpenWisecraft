package com.nao20010128nao.Wisecraft;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.support.v4.app.*;
import android.support.v4.content.*;
import android.support.v7.app.*;
import android.view.*;
import android.widget.*;
import com.nao20010128nao.ToolBox.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.compat.*;
import com.nao20010128nao.Wisecraft.misc.pref.*;
import java.lang.reflect.*;
import java.util.*;
import uk.co.chrisjenx.calligraphy.*;
import android.support.v7.widget.*;
import android.support.v4.view.*;

public class FragmentSettingsActivity extends AppCompatActivity {
	public static final Map<String,Class<? extends BaseFragment>> FRAGMENT_CLASSES=new HashMap<String,Class<? extends BaseFragment>>(){{
			put("root",HubPrefFragment.class);
			put("basics",Basics.class);
			put("features",Features.class);
			put("asfsls",Asfsls.class);
	}};
	public static final String DIALOG_FRAGMENT_TAG_PREFIX="settings@com.nao20010128nao.Wisecraft#";
	
	int which;
	SharedPreferences pref;
	boolean requireRestart=true;
	List<String> nonRestartKeys=Collections.unmodifiableList(Arrays.asList(new String[]{
		/*"showPcUserFace",
		"selectFont",
		"sendInfos",
		"exitCompletely",
		"useBright",
		"allowAutoUpdateSLSCode",
		"aausc_monnet"*/
	}));
	FrameLayout misc;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		pref=PreferenceManager.getDefaultSharedPreferences(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_settings_with_preview);
		pref.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener(){
				public void onSharedPreferenceChanged(SharedPreferences pref,String key){
					if(nonRestartKeys.contains(key))return;
					requireRestart=true;
				}
			});
		if(savedInstanceState==null){
			getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.preference,new HubPrefFragment())
				.addToBackStack("root")
				.commit();
		}
		misc=(FrameLayout)findViewById(R.id.misc);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO: Implement this method
		MenuItem showPreview=menu.add(Menu.NONE,0,0,R.string.preview);
		showPreview.setIcon(misc.getVisibility()==View.VISIBLE?R.drawable.ic_visibility_black_48dp:R.drawable.ic_visibility_off_black_48dp);
		MenuItemCompat.setShowAsAction(showPreview,MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO: Implement this method
		switch(item.getItemId()){
			case 0:
				boolean isShowing=misc.getVisibility()==View.VISIBLE;
				if(isShowing){
					misc.setVisibility(View.GONE);
				}else{
					misc.setVisibility(View.VISIBLE);
				}
				invalidateOptionsMenu();
				break;
		}
		return true;
	}
	
	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(TheApplication.injectContextSpecial(newBase));
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

	@Override
	public void finish() {
		// TODO: Implement this method
		if(requireRestart){
			ServerListActivityImpl.instance.get().finish();
			startActivity(new Intent(this,ServerListActivity.class));
		}
		super.finish();
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
							.replace(R.id.preference,new Basics())
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
							.replace(R.id.preference,new Features())
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
							.replace(R.id.preference,new Asfsls())
							.addToBackStack("asfsls")
							.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
							.commit();
					}
				});
			sH("changeColor",new HandledPreference.OnClickListener(){
					public void onClick(String a,String b,String c){
						getActivity()
							.getSupportFragmentManager()
							.beginTransaction()
							.replace(R.id.preference,new ColorChanger())
							.addToBackStack("changeColor")
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

	public static class Basics extends BaseFragment {
		public static final String PARALLELS_DIALOG_FRAGMENT_TAG=DIALOG_FRAGMENT_TAG_PREFIX+"parallels-dialog";
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
									//Toast.makeText(getContext(),R.string.saved_fonts,Toast.LENGTH_LONG).show();
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
	
	public static class ColorChanger extends BaseFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			// TODO: Implement this method
			super.onCreate(savedInstanceState);
		}

		@Override
		public void onResume() {
			// TODO: Implement this method
			super.onResume();
			getActivity().setTitle(R.string.colorChange);
		}

		@Override
		public void onCreatePreferences(Bundle p1, String p2) {
			// TODO: Implement this method
			addPreferencesFromResource(R.xml.settings_color_changer_compat);
		}
	}
	
	
	
	
	

	public abstract static class BaseFragment extends SHablePreferenceFragment {
		protected SharedPreferences pref;
		
		LinearLayout miscContent;
		@Override
		public void onCreate(Bundle savedInstanceState) {
			// TODO: Implement this method
			pref=PreferenceManager.getDefaultSharedPreferences(getContext());
			super.onCreate(savedInstanceState);
		}

		@Override
		public LayoutInflater getLayoutInflater(Bundle savedInstanceState) {
			// TODO: Implement this method
			return getActivity().getLayoutInflater().cloneInContext(super.getLayoutInflater(savedInstanceState).getContext());
		}

		@Override
		public Context getContext() {
			// TODO: Implement this method
			return TheApplication.injectContextSpecial(super.getContext());
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			// TODO: Implement this method
			View v=super.onCreateView(getActivity().getLayoutInflater(), container, savedInstanceState);
			miscContent=(LinearLayout)v.findViewById(R.id.misc);
			if(miscContent!=null)onMiscPartAvailable(miscContent);
			return v;
		}

		@Override
		public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
			// TODO: Implement this method
			return super.onCreateRecyclerView(Utils.fixLayoutInflaterIfNeeded(CalligraphyContextWrapper.wrap(inflater.getContext()),getActivity()),
				parent, 
				savedInstanceState);
		}
		
		
		
		protected void onMiscPartAvailable(LinearLayout misc){}
		
		public LinearLayout getMiscContent(){return miscContent;}
	}
}
