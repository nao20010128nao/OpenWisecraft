package com.nao20010128nao.WRcon.misc;

import android.content.*;
import android.content.pm.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.compat.*;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

public class Utils {
    public static String deleteDecorations(String decorated) {
        StringBuilder sb = new StringBuilder();
        char[] chars = decorated.toCharArray();
        int offset = 0;
        while (chars.length > offset) {
            if (chars[offset] == '§') {
                offset += 2;
                continue;
            }
            sb.append(chars[offset]);
            offset++;
        }
        return sb.toString();
    }

    public static boolean isNullString(String s) {
        if (s == null)
            return true;
        if ("".equals(s))
            return true;
        return false;
    }

    public static String[] lines(String s) {
        try {
            BufferedReader br = new BufferedReader(new StringReader(s));
            List<String> tmp = new ArrayList<>(4);
            String line = null;
            while (null != (line = br.readLine())) tmp.add(line);
            return tmp.toArray(new String[tmp.size()]);
        } catch (Throwable e) {
            return Constant.EMPTY_STRING_ARRAY;
        }
    }

    public static boolean writeToFile(File f, String content) {
        return writeToFileByBytes(f, content.getBytes(CompatCharsets.UTF_8));
    }

    public static String readWholeFile(File f) {
        return new String(readWholeFileInBytes(f), CompatCharsets.UTF_8);
    }

    public static boolean writeToFileByBytes(File f, byte[] content) {
        FileOutputStream fos = null;
        try {
            (fos = new FileOutputStream(f)).write(content);
            return true;
        } catch (Throwable e) {
            return false;
        } finally {
            CompatUtils.safeClose(fos);
        }
    }

    public static byte[] readWholeFileInBytes(File f) {
        FileInputStream fis = null;
        byte[] buf = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        try {
            fis = new FileInputStream(f);
            while (true) {
                int r = fis.read(buf);
                if (r <= 0)
                    break;
                baos.write(buf, 0, r);
            }
            return baos.toByteArray();
        } catch (Throwable e) {
            return null;
        } finally {
            CompatUtils.safeClose(fis);
        }
    }

    public static void copyAndClose(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[100];
        try {
            while (true) {
                int r = is.read(buf);
                if (r <= 0)
                    break;
                os.write(buf, 0, r);
            }
        } finally {
            CompatUtils.safeClose(is, os);
        }
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

    public static String randomText() {
        return randomText(16);
    }

    public static String randomText(int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        byte[] buf = new byte[len];
        new SecureRandom().nextBytes(buf);
        for (byte b : buf)
            sb.append(Character.forDigit(b >> 4 & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        return sb.toString();
    }

    public static int getVersionCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            DebugWriter.writeToE("Utils", e);
            return 0;
        }
    }

    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            DebugWriter.writeToE("Utils", e);
            return "";
        }
    }

    public static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1000);
        byte[] buf = new byte[1000];
        try {
            while (true) {
                int r = is.read(buf);
                if (r <= 0)
                    break;
                os.write(buf, 0, r);
            }
        } finally {
            CompatUtils.safeClose(is);
        }
        return os.toByteArray();
    }

    public <T> List<T> trueValues(List<T> all, boolean[] balues) {
        List<T> lst = new ArrayList<T>();
        for (int i = 0; i < balues.length; i++)
            if (balues[i])
                lst.add(all.get(i));
        return lst;
    }

    public <T> T[] trueValues(T[] all, boolean[] balues) {
        List<T> lst = new ArrayList<T>();
        for (int i = 0; i < balues.length; i++)
            if (balues[i])
                lst.add(all[i]);
        return lst.toArray((T[]) Array.newInstance(all.getClass().getComponentType(), lst.size()));
    }
}
