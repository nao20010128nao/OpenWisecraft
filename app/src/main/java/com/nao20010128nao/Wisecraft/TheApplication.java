package com.nao20010128nao.Wisecraft;
import java.util.*;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import com.google.gson.Gson;
import com.nao20010128nao.Wisecraft.misc.PCUserUUIDMap;
import com.nao20010128nao.Wisecraft.services.CollectorMainService;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import android.view.LayoutInflater;
import com.nao20010128nao.Wisecraft.misc.BinaryPrefImpl;
import android.app.ActivityManager;

public class TheApplication extends Application {
	public static TheApplication instance;
	public static Typeface latoLight,icomoon1,sysDefault,droidSans,robotoSlabLight;
	public static Field[] fonts=getFontFields();
	public static Map<Typeface,String> fontFilenames;
	public static Map<String,Integer> fontDisplayNames;
	public static Map<String,String> pcUserUUIDs;
	public String uuid;
	public SharedPreferences pref;
	public BinaryPrefImpl stolenInfos;
	
	@Override
	public void onCreate() {
		// TODO: Implement this method
		super.onCreate();
		pref=PreferenceManager.getDefaultSharedPreferences(this);
		instance = this;
		droidSans = Typeface.createFromAsset(getAssets(), "DroidSans.ttf");
		latoLight = Typeface.createFromAsset(getAssets(), "lato-light.ttf");
		icomoon1 = Typeface.createFromAsset(getAssets(), "icomoon.ttf");
		sysDefault = Typeface.DEFAULT;
		robotoSlabLight = Typeface.createFromAsset(getAssets(), "RobotoSlab-Light.ttf");
		
		fontFilenames = new HashMap<Typeface,String>();
		fontFilenames.put(droidSans, "DroidSans.ttf");
		fontFilenames.put(latoLight, "lato-light.ttf");
		fontFilenames.put(icomoon1, "icomoon.ttf");
		fontFilenames.put(sysDefault, "");
		fontFilenames.put(robotoSlabLight, "RobotoSlab-Light.ttf");
		
		fontDisplayNames=new HashMap<>();
		fontDisplayNames.put("droidSans",R.string.font_droidSans);
		fontDisplayNames.put("latoLight",R.string.font_latoLight);
		fontDisplayNames.put("icomoon1",R.string.font_icomoon1);
		fontDisplayNames.put("sysDefault",R.string.font_sysDefault);
		fontDisplayNames.put("robotoSlabLight",R.string.font_robotoSlabLight);

		CalligraphyConfig.initDefault(new CalligraphyConfig.Builder().setDefaultFontPath(getFontFilename()).setFontAttrId(R.attr.fontPath).build());
		///////
		genPassword();
		pcUserUUIDs=new Gson().fromJson(pref.getString("pcuseruuids","{}"),PCUserUUIDMap.class);
	}
	public Typeface getLocalizedFont() {
		try {
			return (Typeface)TheApplication.class.getField(getFontFieldName()).get(null);
		} catch (NoSuchFieldException e) {

		} catch (IllegalAccessException e) {

		} catch (IllegalArgumentException e) {

		}
		return latoLight;
	}
	public String getFontFieldName() {
		return pref.getString("fontField", getResources().getString(R.string.fontField));
	}
	public void setFontFieldName(String value) {
		pref.edit().putString("fontField", value).commit();
	}
	public String getFontFilename() {
		return fontFilenames.get(getLocalizedFont());
	}
	public String getDisplayFontName(String field){
		try {
			return getResources().getString(fontDisplayNames.get(field));
		} catch (Throwable e) {
			return null;
		}
	}
	public String[] getDisplayFontNames(String[] field){
		String[] result=new String[field.length];
		for(int i=0;i<result.length;i++){
			String disp=getDisplayFontName(field[i]);
			if(disp==null)
				result[i]=field[i];
			else
				result[i]=disp;
		}
		return result;
	}
	private String genPassword() {
		uuid = pref.getString("uuid", UUID.randomUUID().toString());
		pref.edit().putString("uuid", uuid).commit();
		return uuid + uuid;
	}
	private static Field[] getFontFields() {
		List<Field> l=new ArrayList<>(6);
		for (Field f:TheApplication.class.getFields())
			if (((f.getModifiers() & Modifier.STATIC) == Modifier.STATIC) & f.getType() == Typeface.class)
				l.add(f);
		return l.toArray(new Field[l.size()]);
	}
	public void collect() {
		if (pref.getBoolean("sendInfos", false)|pref.getBoolean("sendInfos_force", false)){
			startService(new Intent(this,CollectorMainService.class));
		}
	}
	public LayoutInflater getLayoutInflater(){
		return (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
	}

	public boolean isServiceRunning(Class clazz){
		ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
		for(ActivityManager.RunningServiceInfo service:am.getRunningServices(Integer.MAX_VALUE))
			if(service.service.getClassName().equals(clazz.getName()))
				return true;
		return false;
	}
}
