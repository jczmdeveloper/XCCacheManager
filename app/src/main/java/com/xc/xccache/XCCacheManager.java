package com.xc.xccache;

import android.content.Context;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by caizhiming on 2015/11/27.
 * 使用内存缓存和Disk缓存双缓存的Http缓存管理类
 */
public class XCCacheManager {

    private static XCCacheManager mInstance = null;

    private Strategy mStrategy = Strategy.MEMORY_FIRST;
    //线程池
    private ExecutorService mExecutor = null;
    //内存缓存
    private MemoryCache mMemoryCache;
    //Disk缓存
    private DiskCache mDiskCache;

    public static XCCacheManager getInstance(Context context) {
        return getInstance(context, Strategy.MEMORY_FIRST);
    }

    public static XCCacheManager getInstance(Context context, Strategy strategy) {
        if (mInstance == null) {
            synchronized (XCCacheManager.class) {
                if (mInstance == null) {
                    mInstance = new XCCacheManager(context.getApplicationContext(), strategy);
                }
            }
        } else {
            mInstance.setStrategy(strategy);
        }
        return mInstance;
    }

    private XCCacheManager(Context context, Strategy strategy) {
        this.mStrategy = strategy;
        init(context);
    }

    public void setStrategy(XCCacheManager.Strategy strategy) {
        this.mStrategy = strategy;
        switch (mStrategy) {
            case MEMORY_FIRST:
                if (!mMemoryCache.hasEvictedListener()) {
                    mMemoryCache.setEvictedListener(new MemoryCache.EvictedListener() {
                        @Override
                        public void handleEvictEntry(String evictKey, String evictValue) {
                            mDiskCache.put(evictKey, evictValue);
                        }
                    });
                }
                break;
            case MEMORY_ONLY:
                if (mMemoryCache.hasEvictedListener())
                    mMemoryCache.setEvictedListener(null);
                break;
            case DISK_ONLY:
                break;
        }
    }

    public String getCurStrategy() {
        return mStrategy.name();
    }

    /**
     * 初始化 DiskLruCache
     */
    private void init(Context context) {
        mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        mDiskCache = new DiskCache(context);
        mMemoryCache = new MemoryCache();
    }

    /**
     * 从缓存中读取value
     */
    public String readCache(final String key) {
        Future<String> ret = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String result = null;
                switch (mStrategy) {
                    case MEMORY_ONLY:
                        result = mMemoryCache.get(key);
                        break;
                    case MEMORY_FIRST:
                        result = mMemoryCache.get(key);
                        if (result == null) {
                            result = mDiskCache.get(key);
                        }
                        break;
                    case DISK_ONLY:
                        result = mDiskCache.get(key);
                        break;
                }
                return result;
            }
        });
        try {
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将value 写入到缓存中
     */
    public void writeCache(final String key, final String value) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                switch (mStrategy) {
                    case MEMORY_FIRST:
                        mMemoryCache.put(key, value);
                        mDiskCache.put(key,value);
                        break;
                    case MEMORY_ONLY:
                        mMemoryCache.put(key, value);
                        break;
                    case DISK_ONLY:
                        mDiskCache.put(key, value);
                        break;
                }
            }
        });
    }

    enum Strategy {
        MEMORY_ONLY(0), MEMORY_FIRST(1), DISK_ONLY(3);
        int id;

        Strategy(int id) {
            this.id = id;
        }
    }
}
