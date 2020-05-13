package com.cloudhelios.cache.config;

import com.cloudhelios.cache.core.HeliosCacheManager;
import com.cloudhelios.cache.core.HeliosCacheResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties(HeliosCacheProperties.class)
public class HeliosCacheConfiguration {
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private CacheAspectSupport cacheAspectSupport;
    @Autowired
    private HeliosCacheProperties heliosCacheProperties;

    @PostConstruct
    public void initCacheManager() {
        HeliosCacheManager heliosCacheManager = new HeliosCacheManager(cacheManager, heliosCacheProperties);
        cacheAspectSupport.setCacheManager(heliosCacheManager);
        cacheAspectSupport.setCacheResolver(new HeliosCacheResolver(heliosCacheManager));

    }
}
