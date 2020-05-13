package com.cloudhelios.cache.core;

import com.cloudhelios.cache.config.HeliosCacheProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class HeliosCacheManager implements CacheManager {

    private final CacheManager remoteCacheManager;
    private final CaffeineCacheManager caffeineCacheManager;
    private final ConcurrentMap<String,Cache> multiCaches;
    private final HeliosCacheProperties heliosCacheProperties;

    public final static String machineKey = getMachineKey();

    private static String getMachineKey() {
        Enumeration<NetworkInterface> allNetInterfaces;
        String resultIP = null;
        try {
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        InetAddress ip;
        assert allNetInterfaces != null;
        while (allNetInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = allNetInterfaces.nextElement();
            Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                ip =  addresses.nextElement();
                if (ip instanceof Inet4Address) {
                    if (resultIP == null)
                        resultIP = ip.getHostAddress();
                }
            }
        }
        if(resultIP!=null){
            Integer ipSum= Arrays.stream(resultIP.split("\\.")).map(Integer::valueOf).reduce(Integer::sum).orElse(-1);
            return ipSum.toString();
        }else{
            throw new RuntimeException("can not get machine key");
        }

    }

    public HeliosCacheManager(CacheManager remoteCacheManager, HeliosCacheProperties heliosCacheProperties) {
        this.heliosCacheProperties = heliosCacheProperties;
        this.remoteCacheManager = remoteCacheManager;
        this.caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder().expireAfterAccess(heliosCacheProperties.getExpireSecondsAfterAccess(),TimeUnit.SECONDS));

        this.multiCaches =new ConcurrentHashMap<>();
    }

    @Override
    public Cache getCache(String s) {
        Cache cache= multiCaches.get(s);
        if(cache != null){
            return cache;
        }else{
            synchronized(this.multiCaches) {
                cache = new MultiStepCacheDecorator(remoteCacheManager.getCache(s), caffeineCacheManager.getCache(s),this.heliosCacheProperties);
                multiCaches.put(s, cache);
            }
            return cache;
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return remoteCacheManager.getCacheNames();
    }

}
