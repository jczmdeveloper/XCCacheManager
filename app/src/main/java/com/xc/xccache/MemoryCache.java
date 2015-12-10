package com.xc.xccache;

import android.support.v4.util.LruCache;

/**
 * 内存缓存类
 * Created by caizhiming on 2015/12/4.
 */
public class MemoryCache implements Cache {
    private LruCache<String, String> mMemoryLruCache;
    private EvictedListener mEvictedListener;

    public MemoryCache() {
        init();
    }

    public MemoryCache(EvictedListener listener) {
        init();
        this.mEvictedListener = listener;
    }

    public void setEvictedListener(EvictedListener listener) {
        this.mEvictedListener = listener;
    }

    public boolean hasEvictedListener() {
        return mEvictedListener != null;
    }

    private void init() {
        // 计算可使用的最大内存
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // 取可用内存空间的1/4作为缓存
        final int cacheSize = maxMemory / 4;
        mMemoryLruCache = new LruCache<String, String>(cacheSize) {
            @Override
            protected int sizeOf(String key, String value) {
                return value.getBytes().length;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, String oldValue, String newValue) {
                if (evicted) {
                    if (mEvictedListener != null) {
                        mEvictedListener.handleEvictEntry(key, oldValue);
                    }
                }
            }
        };
    }

    @Override
    public String get(String key) {
        return mMemoryLruCache.get(key);
    }

    @Override
    public void put(String key, String value) {
        mMemoryLruCache.put(key, value);
    }

    @Override
    public boolean remove(String key) {
        return Boolean.parseBoolean(mMemoryLruCache.remove(key));
    }

    /**
     * called when mMemoryLruCache evict entrys,
     * <p/>
     * using by CacheManager.Strategy.MEMORY_FIRST
     */
    public interface EvictedListener {
        void handleEvictEntry(String evictKey, String evictValue);
    }

}
