package com.cloudhelios.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( prefix = "cache.super")
public class HeliosCacheProperties {
    private boolean enableLocalCache;
    /**
     * 最后一次读后失效chache的时间，秒为单位
     */
    private int expireSecondsAfterAccess;
    /**
     * 开头为 localCachePrefix 才会启用本地缓存
     */
    private String localCachePrefix;
    /**
     * 决定缓存在本地的对象最小大小
     */
    private int minimumLocalKeySize;

    public HeliosCacheProperties(){
        enableLocalCache  = true;
        expireSecondsAfterAccess = 600;
        localCachePrefix = "";
        minimumLocalKeySize = 0;
    }
    public boolean isEnableLocalCache() {
        return enableLocalCache;
    }

    public void setEnableLocalCache(boolean enableLocalCache) {
        this.enableLocalCache = enableLocalCache;
    }

    public long getExpireSecondsAfterAccess() {
        return expireSecondsAfterAccess;
    }

    public void setExpireSecondsAfterAccess(int expireSecondsAfterAccess) {
        this.expireSecondsAfterAccess = expireSecondsAfterAccess;
    }

    public String getLocalCachePrefix() {
        return localCachePrefix;
    }

    public void setLocalCachePrefix(String localCachePrefix) {
        this.localCachePrefix = localCachePrefix;
    }

    public int getMinimumLocalKeySize() {
        return minimumLocalKeySize;
    }

    public void setMinimumLocalKeySize(int minimumLocalKeySize) {
        this.minimumLocalKeySize = minimumLocalKeySize;
    }
}
