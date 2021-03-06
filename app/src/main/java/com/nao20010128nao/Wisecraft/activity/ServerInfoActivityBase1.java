package com.nao20010128nao.Wisecraft.activity;

import android.content.*;
import com.ipaulpro.afilechooser.*;
import com.nao20010128nao.Wisecraft.misc.*;
import permissions.dispatcher.*;

import java.io.*;
import java.security.*;
import java.util.*;


//Wrapper for aFileChooser
@RuntimePermissions
abstract class ServerInfoActivityBase1 extends WisecraftBaseActivity {
    SecureRandom sr = new SecureRandom();
    Map<Integer, FileChooserResult> results = new HashMap<>();

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (results.containsKey(requestCode)) {
            switch (resultCode) {
                case RESULT_OK:
                    results.get(requestCode).onSelected(new File(data.getStringExtra("path")));
                    break;
                case RESULT_CANCELED:
                    results.get(requestCode).onSelectCancelled();
                    break;
            }
            results.remove(requestCode);
            return /*true*/;
        }
    }

    @NeedsPermission({"android.permission.WRITE_EXTERNAL_STORAGE"})
    public void startChooseFileForOpen(File startDir, FileChooserResult result) {
        int call = Math.abs(sr.nextInt()) & 0xf;
        while (results.containsKey(call)) {
            call = Math.abs(sr.nextInt()) & 0xf;
        }
        Intent intent = new Intent(this, FileOpenChooserActivity.class);
        if (startDir != null) {
            intent.putExtra("path", startDir.toString());
        }
        results.put(call, Utils.requireNonNull(result));
        startActivityForResult(intent, call);
    }

    @NeedsPermission({"android.permission.WRITE_EXTERNAL_STORAGE"})
    public void startChooseFileForSelect(File startDir, FileChooserResult result) {
        int call = Math.abs(sr.nextInt()) & 0xf;
        while (results.containsKey(call)) {
            call = Math.abs(sr.nextInt()) & 0xf;
        }
        Intent intent = new Intent(this, FileChooserActivity.class);
        if (startDir != null) {
            intent.putExtra("path", startDir.toString());
        }
        results.put(call, Utils.requireNonNull(result));
        startActivityForResult(intent, call);
    }

    @NeedsPermission({"android.permission.WRITE_EXTERNAL_STORAGE"})
    public void startChooseDirectory(File startDir, FileChooserResult result) {
        int call = Math.abs(sr.nextInt()) & 0xf;
        while (results.containsKey(call)) {
            call = Math.abs(sr.nextInt()) & 0xf;
        }
        Intent intent = new Intent(this, DirectoryChooserActivity.class);
        if (startDir != null) {
            intent.putExtra("path", startDir.toString());
        }
        results.put(call, Utils.requireNonNull(result));
        startActivityForResult(intent, call);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ServerInfoActivityBase1PermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    public interface FileChooserResult {
        void onSelected(File f);

        void onSelectCancelled();
    }
}
