package com.xc.xccache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Disk磁盘缓存类
 * Created by caizhiming on 2015/12/4.
 */
public class DiskCache implements Cache{

    private DiskLruCache mDiskLruCache = null;
    public DiskCache(Context context){
        init(context);
    }
    /**
     * 初始化 DiskLruCache
     */
    public void init(Context context){
        try {
            File cacheDir = getDiskCacheDir(context, "http_cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            Log.v("czm", "cache file=" + cacheDir.getAbsolutePath());
            mDiskLruCache = DiskLruCache.open(cacheDir, getAppVersion(context), 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public String get(String key) {
        String result = null;
        try {
            DiskLruCache.Snapshot snapShot = mDiskLruCache.get(hashKeyForDisk(key));
            if (snapShot != null) {
                result = snapShot.getString(0);
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    @Override
    public void put(String key, String value) {
        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskLruCache.edit(hashKeyForDisk(key));
            if (editor != null) {
                editor.set(0, value);
                editor.commit();
            }
            mDiskLruCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean remove(String key) {
        try {
            return mDiskLruCache.remove(hashKeyForDisk(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Bitmap getImageCache(String key){
        Bitmap bitmap = null;
        try {
            DiskLruCache.Snapshot snapShot = mDiskLruCache.get(hashKeyForDisk(key));
            if (snapShot != null) {
                InputStream is = snapShot.getInputStream(0);
                bitmap = BitmapFactory.decodeStream(is);
                return bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
    public void putImageCache(final String key){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DiskLruCache.Editor editor = mDiskLruCache.edit(hashKeyForDisk(key));
                    if (editor != null) {
                        OutputStream outputStream = editor.newOutputStream(0);
                        if (downloadUrlToStream(key, outputStream)) {
                            editor.commit();
                        } else {
                            editor.abort();
                        }
                    }
                    mDiskLruCache.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            CloseUtils.closeCloseable(out);
            CloseUtils.closeCloseable(in);
        }
        return false;
    }


    public String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
    public File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    public int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }


}
