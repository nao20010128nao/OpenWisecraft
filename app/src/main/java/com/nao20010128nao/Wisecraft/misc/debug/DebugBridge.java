package com.nao20010128nao.Wisecraft.misc.debug;

import android.content.*;
import android.support.annotation.*;
import com.nao20010128nao.Wisecraft.*;

public abstract class DebugBridge{
    public abstract boolean isAvailable();
    public abstract void init(Context ctx);
    public abstract void openDebugActivity(Context ctx);

    @NonNull
    private static final DebugBridge instance;
    static{
        DebugBridge tmp=null;
        try {
            tmp=(DebugBridge) Class.forName("com.nao20010128nao.Wisecraft.misc.debug.DebugBridge$Debug2").newInstance();
        } catch (Throwable e) {
            WisecraftError.report("DebugBridge",e);
        }
        if(tmp==null)
            tmp=new Null();
        instance=tmp;
    }

    @NonNull
    public static DebugBridge getInstance() {
        return instance;
    }

    private static class Null extends DebugBridge{
        @Override public boolean isAvailable() {return false;}
        @Override public void init(Context ctx) {}
        @Override public void openDebugActivity(Context ctx) {}
    }
}