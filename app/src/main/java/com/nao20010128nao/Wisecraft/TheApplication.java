package com.nao20010128nao.Wisecraft;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.multidex.MultiDex;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.view.LayoutInflater;

import com.annimon.stream.Stream;
import com.google.android.gms.tasks.Task;
import com.google.common.io.Files;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.nao20010128nao.Wisecraft.activity.ServerListActivity;
import com.nao20010128nao.Wisecraft.misc.CompatConstants;
import com.nao20010128nao.Wisecraft.misc.MinecraftPeInformationProvider;
import com.nao20010128nao.Wisecraft.misc.PCUserUUIDMap;
import com.nao20010128nao.Wisecraft.misc.ThemePatcher;
import com.nao20010128nao.Wisecraft.misc.Utils;
import com.nao20010128nao.Wisecraft.misc.WisecraftInformationProvider;
import com.nao20010128nao.Wisecraft.misc.collector.CollectorMain;
import com.nao20010128nao.Wisecraft.misc.compat.CompatCharsets;
import com.nao20010128nao.Wisecraft.misc.contextwrappers.extender.ContextWrappingExtender;
import com.nao20010128nao.Wisecraft.misc.debug.DebugBridge;
import com.nao20010128nao.Wisecraft.misc.tfl.TypefaceLoader;
import com.nao20010128nao.Wisecraft.rcon.KeyChain;
import com.nao20010128nao.Wisecraft.services.CollectorMainService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

class TheApplicationImpl extends Application implements com.nao20010128nao.Wisecraft.rcon.Presenter,
    com.ipaulpro.afilechooser.Presenter,
    InformationCommunicatorReceiver.DisclosureResult,
    Application.ActivityLifecycleCallbacks {
    public static TheApplicationImpl implInstance;
    public static TypefaceLoader latoLight, icomoon1, sysDefault, droidSans, robotoSlabLight, ubuntuFont, mplus1p;
    public static Field[] fonts = getFontFields();
    public static Map<TypefaceLoader, String> fontFilenames;
    public static Map<String, Integer> fontDisplayNames;
    public static Map<String, TypefaceLoader> fontFieldNames;
    public static Map<String, String> pcUserUUIDs;
    public String uuid;
    public SharedPreferences pref;
    public SharedPreferences stolenInfos;
    public FirebaseAnalytics firebaseAnalytics;
    public FirebaseRemoteConfig firebaseRemoteCfg;
    public Task<Void> fbCfgLoader;
    public Context extenderWrapped;
    boolean disclosurePending = true, disclosureEnded = false;
    boolean activitiesLaunches = false;
    WeakHashMap<Activity, Application> activities = new WeakHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        implInstance = this;

        ///////
        pcUserUUIDs = Utils.newGson().fromJson(pref.getString("pcuseruuids", "{}"), PCUserUUIDMap.class);
        ///////
        InformationCommunicatorReceiver.startDisclosureRequestIfNeeded(this, this);
        genPassword();

        pref.edit()
            .remove("showDetailsIfNoDetails")
            .remove("useOldActivity")
            .remove("serverListStyle")
            .remove("main_style")
            .remove("specialDrawer1")
            .remove("useBright")
            .remove("feature_asfsls")
            .commit();

        registerActivityLifecycleCallbacks(this);

        CollectorMain.INFORMATIONS.add(new MinecraftPeInformationProvider());
        CollectorMain.INFORMATIONS.add(new WisecraftInformationProvider());

        DebugBridge.getInstance().init(this);
    }

    public TypefaceLoader getLocalizedFont() {
        return fontFieldNames.get(getFontFieldName());
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

    public String getDisplayFontName(String field) {
        String res;
        try {
            res = getResources().getString(fontDisplayNames.get(field));
            if (TextUtils.isEmpty(res)) {
                res = field;
            }
        } catch (Throwable e) {
            res = field;
        }
        return res;
    }

    public String[] getDisplayFontNames(String[] field) {
        return Stream.of(field)
            .map(this::getDisplayFontName)
            .toArray(String[]::new);
    }

    public void initForActivities() {
        if (activitiesLaunches) {
            CalligraphyConfig.initDefault(new CalligraphyConfig.Builder().setDefaultFontPath(getFontFilename()).setFontAttrId(R.attr.fontPath).build());
            return;
        }
        activitiesLaunches = true;

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        firebaseRemoteCfg = FirebaseRemoteConfig.getInstance();
        fbCfgLoader = firebaseRemoteCfg.fetch();

        droidSans = TypefaceLoader.newInstance(getAssets(), "DroidSans.ttf");
        latoLight = TypefaceLoader.newInstance(getAssets(), "lato-light.ttf");
        icomoon1 = TypefaceLoader.newInstance(getAssets(), "icomoon.ttf");
        sysDefault = TypefaceLoader.newInstance(Typeface.DEFAULT);
        ubuntuFont = TypefaceLoader.newInstance(getAssets(), "Ubuntu-Regular.ttf");
        mplus1p = TypefaceLoader.newInstance(getAssets(), "Mplus1p-Light.ttf");

        fontFilenames = new HashMap<>();
        fontFilenames.put(droidSans, "DroidSans.ttf");
        fontFilenames.put(latoLight, "lato-light.ttf");
        fontFilenames.put(icomoon1, "icomoon.ttf");
        fontFilenames.put(sysDefault, "");
        fontFilenames.put(ubuntuFont, "Ubuntu-Regular.ttf");
        fontFilenames.put(mplus1p, "Mplus1p-Light.ttf");

        fontDisplayNames = new HashMap<>();
        fontDisplayNames.put("droidSans", R.string.font_droidSans);
        fontDisplayNames.put("latoLight", R.string.font_latoLight);
        fontDisplayNames.put("icomoon1", R.string.font_icomoon1);
        fontDisplayNames.put("sysDefault", R.string.font_sysDefault);
        fontDisplayNames.put("ubuntuFont", R.string.font_ubuntu);
        fontDisplayNames.put("mplus1p", R.string.font_mplus1p);

        fontFieldNames = new HashMap<>();
        fontFieldNames.put("droidSans", droidSans);
        fontFieldNames.put("latoLight", latoLight);
        fontFieldNames.put("icomoon1", icomoon1);
        fontFieldNames.put("sysDefault", sysDefault);
        fontFieldNames.put("ubuntuFont", ubuntuFont);
        fontFieldNames.put("mplus1p", mplus1p);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder().setDefaultFontPath(getFontFilename()).setFontAttrId(R.attr.fontPath).build());
    }

    private String genPassword() {
        File uuidFile=new File(CompatConstants.mcpeServerList,"../wisecraft/uuid").getAbsoluteFile();
        uuid = pref.getString("uuid", null);
        if(uuidFile.exists()){
            if (uuid == null) {
                try {
                    uuid = UUID.fromString(Files.readFirstLine(uuidFile, CompatCharsets.UTF_8)).toString();
                } catch (IOException e) {
                    if (uuidFile.exists()) uuidFile.delete();
                    return genPassword();
                }
            }
            pref.edit().putString("uuid", uuid).commit();
            if (!pref.contains("uuidShouldBe")) {
                pref.edit().putString("uuidShouldBe", uuid).commit();
            }
        }else{
            if (!TextUtils.isEmpty(Settings.Secure.getString(getContentResolver(), Settings.System.ANDROID_ID)) || !TextUtils.isEmpty(Build.SERIAL)) {
                String seed = Settings.Secure.getString(getContentResolver(), Settings.System.ANDROID_ID) + Build.SERIAL;
                if (uuid == null) uuid = UUID.nameUUIDFromBytes(seed.getBytes()).toString();
                pref.edit().putString("uuid", uuid).commit();
                if (!pref.contains("uuidShouldBe")) {
                    pref.edit().putString("uuidShouldBe", UUID.nameUUIDFromBytes(seed.getBytes()).toString()).commit();
                }
            }else{
                if (uuid == null)uuid=UUID.randomUUID().toString();
                pref.edit().putString("uuid", uuid).commit();
                if (!pref.contains("uuidShouldBe")) {
                    pref.edit().putString("uuidShouldBe", uuid).commit();
                }
            }
        }
        uuidFile.getParentFile().mkdirs();
        try {
            Files.write(uuid,uuidFile,CompatCharsets.UTF_8);
        } catch (IOException e) {}
        return uuid + uuid;
    }

    private static Field[] getFontFields() {
        return Stream.of(TheApplication.class.getFields())
            .filter(f -> (f.getModifiers() & Modifier.STATIC) != 0)
            .filter(f -> f.getType() == Typeface.class)
            .toArray(Field[]::new);
    }

    public void collect() {
        collectImpl();
    }

    private void collectImpl() {
        if ((pref.getBoolean("sendInfos", false) | pref.getBoolean("sendInfos_force", false)) & !isServiceRunning(CollectorMainService.class))
            startService(new Intent(this, CollectorMainService.class));
    }

    public LayoutInflater getLayoutInflater() {
        return (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
    }

    public boolean isServiceRunning(Class<? extends Service> clazz) {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE))
            if (service.service.getClassName().equals(clazz.getName()))
                return true;
        return false;
    }

    public Drawable getTintedDrawable(int res, int color) {
        return getTintedDrawable(res, color, this);
    }

    public static Drawable getTintedDrawable(int res, int color, Context ctx) {
        Drawable d = ctx.getResources().getDrawable(res);
        d = DrawableCompat.wrap(d);
        DrawableCompat.setTint(d, color);
        return d;
    }

    @Override
    public int getDialogStyleId() {
        return ThemePatcher.getDefaultDialogStyle(this);
    }

    @Override
    public void showSelfMessage(Activity a, int strRes, int duration) {
        Utils.makeSB(a, strRes, duration == com.nao20010128nao.Wisecraft.rcon.Presenter.MESSAGE_SHOW_LENGTH_SHORT ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void showSelfMessage(Activity a, String str, int duration) {
        Utils.makeSB(a, str, duration == com.nao20010128nao.Wisecraft.rcon.Presenter.MESSAGE_SHOW_LENGTH_SHORT ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG).show();
    }

    @Override
    public KeyChain getKeyChain() {
        return null;
    }

    @Override
    public void disclosued() {
        disclosurePending = false;
        disclosureEnded = true;
        collectImpl();
    }

    @Override
    public void disclosureTimeout() {
        disclosurePending = false;
        disclosureEnded = true;
        genPassword();
        collectImpl();
    }

    @Override
    public void nothingToDisclosure() {
        disclosurePending = false;
        disclosureEnded = true;
        collectImpl();
    }

    @Override
    public boolean isLightTheme(Activity a) {
        return true;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        extenderWrapped = ContextWrappingExtender.wrap(base);
    }

    @Override
    public Resources getResources() {
        return extenderWrapped.getResources();
    }

    @Override
    public void onActivityStarted(Activity p1) {
    }

    @Override
    public void onActivityCreated(Activity p1, Bundle p2) {
        activities.put(p1, this);
    }

    @Override
    public void onActivityPaused(Activity p1) {
    }

    @Override
    public void onActivityStopped(Activity p1) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity p1, Bundle p2) {
    }

    @Override
    public void onActivityDestroyed(Activity p1) {
        activities.remove(p1);
    }

    @Override
    public void onActivityResumed(Activity p1) {
    }

    public void restartForMainProcess() {
        Stream.of(activities)
            .map(Map.Entry::getKey)
            .forEach(Activity::finish);
        startActivity(new Intent(this, ServerListActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public static Context injectContextSpecial(final Context base) {
        final Context extender = ContextWrappingExtender.wrap(base);
        final Context calligraphy = CalligraphyContextWrapper.wrap(extender);
        return calligraphy;
    }

    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    }
}

public class TheApplication extends TheApplicationImpl {
    public static TheApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
