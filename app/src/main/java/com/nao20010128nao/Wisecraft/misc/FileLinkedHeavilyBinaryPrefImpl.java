package com.nao20010128nao.Wisecraft.misc;
import com.nao20010128nao.Wisecraft.*;
import java.io.*;
import java.util.*;

public class FileLinkedHeavilyBinaryPrefImpl extends HeavilyEncryptedBinaryPrefImpl {
	File fileDir;
	long lastMod;
	public FileLinkedHeavilyBinaryPrefImpl(File f)throws IOException {
		super(f);
		fileDir = f;
		lastMod=f.lastModified();
	}
	public boolean reload(){
		try {
			data = (Map)new HeavilyEncryptedBinaryPrefImpl(fileDir).getAll();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	private class BpiEdt extends PreferencesEditorWrapper {
		public BpiEdt() {
			super(FileLinkedHeavilyBinaryPrefImpl.super.edit());
		}

		@Override
		public boolean commit() {
			// TODO: Implement this method
			if(lastMod!=fileDir.lastModified())reload();
			return super.commit() & Utils.writeToFileByBytes(fileDir, toBytes());
		}

		@Override
		public void apply() {
			// TODO: Implement this method
			if(lastMod!=fileDir.lastModified())reload();
			super.apply();
			Utils.writeToFileByBytes(fileDir, toBytes());
		}
	}
}
