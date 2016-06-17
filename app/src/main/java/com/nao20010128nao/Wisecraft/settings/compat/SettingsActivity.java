package com.nao20010128nao.Wisecraft.settings.compat;

import android.content.*;
import android.os.*;
import android.preference.*;
import com.nao20010128nao.Wisecraft.*;
import com.nao20010128nao.Wisecraft.misc.compat.*;
import com.nao20010128nao.Wisecraft.settings.*;
import uk.co.chrisjenx.calligraphy.*;

import com.nao20010128nao.Wisecraft.R;

public class SettingsActivity extends CompatSHablePreferenceActivity {
	SettingsDelegate delegate;
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
		delegate.onCreate();
	}
	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
		delegate=new SettingsDelegate(this);
	}
	@Override
	protected void onResume() {
		// TODO: Implement this method
		super.onResume();
		delegate.onResume();
	}


	public abstract static class BaseSettingsActivity extends CompatSHablePreferenceActivity {
		SharedPreferences pref;
		SettingsDelegate delegate;
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// TODO: Implement this method
			if(pref.getBoolean("useBright",false)){
				setTheme(R.style.AppTheme_Bright);
				getTheme().applyStyle(R.style.AppTheme_Bright,true);
			}
			super.onCreate(savedInstanceState);
			delegate.onCreate();
		}
		@Override
		protected void attachBaseContext(Context newBase) {
			super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
			pref=PreferenceManager.getDefaultSharedPreferences(this);
			delegate=new SettingsDelegate(this);
		}
	}
	public static class Basics extends BaseSettingsActivity {
		
	}
	public static class Features extends BaseSettingsActivity {
		
	}
	public static class Asfsls extends BaseSettingsActivity {
		
	}
}