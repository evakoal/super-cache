package com.cloudhelios.cache.core;

import com.alibaba.fastjson.JSON;
import com.cloudhelios.cache.config.HeliosCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.Callable;
@Slf4j
public class MultiStepCacheDecorator implements Cache {

    private final Cache remoteCache;
    private final Cache localCache;
    private final HeliosCacheProperties heliosCacheProperties;
    private static final String CACHE_INDICATOR_PREFIX = "&IND&";
    private static final String UNKNOWN_STATUS = "unrecognized cache status";

    public MultiStepCacheDecorator(Cache remoteCache, Cache localCache, HeliosCacheProperties heliosCacheProperties) {
        Assert.notNull(remoteCache, "Target Cache must not be null");
        this.remoteCache = remoteCache;
        this.localCache = localCache;
        this.heliosCacheProperties = heliosCacheProperties;
    }

    @Override
    public String getName() {
        return remoteCache.getName();
    }

    @Override
    public Object getNativeCache() {
        return remoteCache.getNativeCache();
    }

    @Override
    public ValueWrapper
    get(Object o) {
        ValueWrapper remoteCache = this.remoteCache.get(o);
        LocalCacheStatusEnum localCacheStatusEnum = localCacheStatus(o, remoteCache);
        if (LocalCacheStatusEnum.USE_REMOTE.equals(localCacheStatusEnum)) {
            return remoteCache;

        } else if (LocalCacheStatusEnum.VALID.equals(localCacheStatusEnum)) {
            return localCache.get(o);
        } else if (LocalCacheStatusEnum.INVALID.equals(localCacheStatusEnum)) {
            return null;
        }
        throw new RuntimeException(UNKNOWN_STATUS);
    }

    @Override
    public <T> T get(Object o, Class<T> aClass) {
        T remoteCache = this.remoteCache.get(o, aClass);
        LocalCacheStatusEnum localCacheStatusEnum = localCacheStatus(o, remoteCache);
        if (LocalCacheStatusEnum.USE_REMOTE.equals(localCacheStatusEnum)) {
            return remoteCache;
        } else if (LocalCacheStatusEnum.VALID.equals(localCacheStatusEnum)) {
            return localCache.get(o, aClass);
        } else if (LocalCacheStatusEnum.INVALID.equals(localCacheStatusEnum)) {
            return null;
        }
        throw new RuntimeException(UNKNOWN_STATUS);
    }

    @Override
    public <T> T get(Object o, Callable<T> callable) {
        T remoteCache = this.remoteCache.get(o, callable);
        LocalCacheStatusEnum localCacheStatusEnum = localCacheStatus(o, remoteCache);

        if (LocalCacheStatusEnum.USE_REMOTE.equals(localCacheStatusEnum)) {
            return remoteCache;
        } else if (LocalCacheStatusEnum.VALID.equals(localCacheStatusEnum)) {
            return localCache.get(o, callable);
        } else if (LocalCacheStatusEnum.INVALID.equals(localCacheStatusEnum)) {
            return null;
        }
        throw new RuntimeException(UNKNOWN_STATUS);
    }

    @Override
    public void put(Object o, Object o1) {

        if (enablePutLocalCache(o1)) {
            updateCacheIndicator(o);
            localCache.put(o, o1);
        } else {
            remoteCache.put(o, o1);
        }

    }

    @Override
    public ValueWrapper putIfAbsent(Object o, Object o1) {
        if (enablePutLocalCache(o1)) {
            updateCacheIndicator(o);
            return localCache.putIfAbsent(o, o1);
        } else {
            return remoteCache.putIfAbsent(o, o1);
        }
    }

    @Override
    public void evict(Object o) {
        remoteCache.evict(o);
        localCache.evict(o);
    }

    @Override
    public void clear() {
        localCache.clear();
        remoteCache.clear();
    }

    private <T> LocalCacheStatusEnum localCacheStatus(Object key, Object remoteCache) {


        if (remoteCache == null) {
            return LocalCacheStatusEnum.INVALID;
        }

        String validMachines = "";
        if (remoteCache instanceof ValueWrapper) {
            validMachines = ((ValueWrapper) remoteCache).get() == null ? "" : ((ValueWrapper) remoteCache).get().toString();
        }

        if (validMachines.contains(HeliosCacheManager.machineKey) && validMachines.contains(CACHE_INDICATOR_PREFIX)) {
            return LocalCacheStatusEnum.VALID;
        }
        if (!validMachines.contains(HeliosCacheManager.machineKey) && validMachines.contains(CACHE_INDICATOR_PREFIX)) {
            return LocalCacheStatusEnum.INVALID;
        }

        if (!validMachines.contains(CACHE_INDICATOR_PREFIX)) {
            return LocalCacheStatusEnum.USE_REMOTE;
        }
        return LocalCacheStatusEnum.INVALID;
    }


    private boolean enablePutLocalCache(Object value) {
        if (!heliosCacheProperties.isEnableLocalCache()) {
            return false;
        }

        //prefix check
        boolean enablePutLocalCachePrefixCheck = StringUtils.isEmpty(heliosCacheProperties.getLocalCachePrefix()) || getName().startsWith(heliosCacheProperties.getLocalCachePrefix());

        //size check
        boolean enablePutLocalCacheSizeCheck = heliosCacheProperties.getMinimumLocalKeySize() <= 0 || getObjectSize(value) >= heliosCacheProperties.getMinimumLocalKeySize();

        return enablePutLocalCachePrefixCheck && enablePutLocalCacheSizeCheck;
    }


    /**
     * @param o Object to calculate size
     * @return Object Size
     */
    private int getObjectSize(Object o) {
        if (o == null) {
            return 0;
        }
        try {
            String json = JSON.toJSONString(o);
            return json.length();
        } catch (Exception e) {
            log.error("error calc size for object {}",o.getClass());
            return 0;
        }

    }

    /**
     * 获取远端 cache indicator的内容
     *
     * @return calculate indicator value when put indicator to remote cache
     */
    private void updateCacheIndicator(Object key) {
        String validMachines = remoteCache.get(key, String.class);
        if (validMachines != null && validMachines.contains(HeliosCacheManager.machineKey)) {
            return;
        }
        validMachines = validMachines == null ? HeliosCacheManager.machineKey + ";" : validMachines + ";" + HeliosCacheManager.machineKey;

        if (!validMachines.startsWith(CACHE_INDICATOR_PREFIX)) {
            validMachines = CACHE_INDICATOR_PREFIX + validMachines;
        }
        this.remoteCache.put(key, validMachines);
    }

    public static void main(String [] args){
        Date d=new Date();
        ZonedDateTime dateTime=ZonedDateTime.now();
        log.info(JSON.toJSONString(dateTime));
    }

}
