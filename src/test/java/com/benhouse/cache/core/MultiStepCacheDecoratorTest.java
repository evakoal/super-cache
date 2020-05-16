package com.benhouse.cache.core;

import static org.junit.jupiter.api.Assertions.*;

import com.benhouse.cache.config.HeliosCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
class MultiStepCacheDecoratorTest {
    private Cache remoteCache;
    private Cache localCache;
    private HeliosCacheProperties heliosCacheProperties;

    private MultiStepCacheDecorator multiStepCacheDecorator;


    @BeforeEach
    public void initForEach() {
        remoteCache = mock(Cache.class);
        localCache = mock(Cache.class);
        heliosCacheProperties = mock(HeliosCacheProperties.class);
        multiStepCacheDecorator = new MultiStepCacheDecorator(remoteCache, localCache, heliosCacheProperties);
        when(localCache.get(anyString())).thenReturn(new SimpleValueWrapper("local cache value"));
        when(localCache.get(anyString(), eq(String.class))).thenReturn("local cache value");

    }


    @Test
    void getValidLocalCache() {
        when(remoteCache.get(anyString())).thenReturn(new SimpleValueWrapper("&IND&128"));
        when(remoteCache.get(anyString(), eq(String.class))).thenReturn("&IND&128");


        Cache.ValueWrapper valueWrapper = multiStepCacheDecorator.get("a");
        String value = multiStepCacheDecorator.get("a", String.class);

        assertEquals("local cache value", valueWrapper.get());
        assertEquals("local cache value", value);
        verify(remoteCache, times(1)).get(anyString());
        verify(remoteCache, times(1)).get(anyString(), eq(String.class));
        verify(localCache, times(1)).get(anyString());
        verify(localCache, times(1)).get(anyString(), eq(String.class));
    }

    @Test
    void getWhenLocalInvalid() {
        when(remoteCache.get(anyString())).thenReturn(new SimpleValueWrapper("&IND&129"));
        when(remoteCache.get(anyString(), eq(String.class))).thenReturn("&IND&129");

        Cache.ValueWrapper valueWrapper = multiStepCacheDecorator.get("a");
        String value = multiStepCacheDecorator.get("a", String.class);
        verify(remoteCache, times(1)).get(anyString(), eq(String.class));
        verify(remoteCache, times(1)).get(anyString());
        verify(localCache, times(0)).get(anyString(), eq(String.class));
        verify(localCache, times(0)).get(anyString());

        assertEquals(null, valueWrapper);
        assertEquals(null, value);

    }

    @Test
    void getWhenRemoteCacheIsReal() {
        when(remoteCache.get(anyString())).thenReturn(new SimpleValueWrapper("real remote value"));
        when(remoteCache.get(anyString(), eq(String.class))).thenReturn("real remote value");

        Cache.ValueWrapper valueWrapper = multiStepCacheDecorator.get("key");
        String value = multiStepCacheDecorator.get("key", String.class);


        verify(localCache, times(0)).get(anyString());
        verify(remoteCache, times(1)).get(anyString());
        verify(remoteCache, times(1)).get(anyString(), eq(String.class));
        assertEquals("real remote value", valueWrapper.get());
        assertEquals("real remote value", value);
    }

    @Test
    void getWhenRemoteValueIsNull() {
        when(remoteCache.get(anyString())).thenReturn(null);

        Cache.ValueWrapper valueWrapper = multiStepCacheDecorator.get("key");

        verify(remoteCache, times(1)).get(anyString());
        verify(remoteCache, times(1)).get(anyString());
        assertEquals(null, valueWrapper);

    }


    @Test
    void putWhenObjectIsBigEnoughCacheLocallyAndShouldRefreshRemoteCacheIndicator() {
        when(heliosCacheProperties.getMinimumLocalKeySize()).thenReturn(10);
        when(heliosCacheProperties.isEnableLocalCache()).thenReturn(true);
        when(remoteCache.get(anyString())).thenReturn(null);

        multiStepCacheDecorator.put("key", "value length greater than 10");

        verify(remoteCache, times(1)).get("key", String.class);
        verify(localCache, times(1)).put("key", "value length greater than 10");
        verify(remoteCache, times(1)).put("key", "&IND&128;");
    }

    @Test
    void putWhenObjectIsCacheLocallyAndShouldCachUpRemoteCacheIndicator() {
        when(heliosCacheProperties.getMinimumLocalKeySize()).thenReturn(10);
        when(heliosCacheProperties.isEnableLocalCache()).thenReturn(true);
        when(remoteCache.get(anyString(), eq(String.class))).thenReturn("&IND&129;");

        multiStepCacheDecorator.put("key", "value length greater than 10");
        multiStepCacheDecorator.putIfAbsent("key", "value length greater than 10");

        verify(remoteCache, times(2)).get("key", String.class);
        verify(localCache, times(1)).put("key", "value length greater than 10");
        verify(localCache, times(1)).putIfAbsent("key", "value length greater than 10");

        verify(remoteCache, times(2)).put("key", "&IND&129;128;");

    }

    @Test
    void putWhenObjectIsTooSmallToCacheLocally() {
        when(heliosCacheProperties.getMinimumLocalKeySize()).thenReturn(100);
        when(heliosCacheProperties.isEnableLocalCache()).thenReturn(true);
        when(remoteCache.get(anyString())).thenReturn(null);
        when(remoteCache.get(anyString(), eq(String.class))).thenReturn("&IND&129;");

        multiStepCacheDecorator.put("key", "value length greater than 10");

        verify(localCache, times(0)).put(any(), any());
        verify(remoteCache, times(1)).put("key", "value length greater than 10");
    }


    @Test
    void putWhenCachePrefixToCacheLocally() {
        when(heliosCacheProperties.getLocalCachePrefix()).thenReturn("LOCAL");
        when(heliosCacheProperties.isEnableLocalCache()).thenReturn(true);
        when(remoteCache.getName()).thenReturn("LOCAL:BENHOUSE");
        when(remoteCache.get(anyString(), eq(String.class))).thenReturn(null);

        multiStepCacheDecorator.put("key", "value length greater than 10");

        verify(localCache, times(1)).put("key", "value length greater than 10");
        verify(remoteCache, times(1)).put("key", "&IND&128;");

    }

    @Test
    void putWhenCachePrefixNotToCacheLocally() {
        when(heliosCacheProperties.getLocalCachePrefix()).thenReturn("LOCAL");
        when(heliosCacheProperties.isEnableLocalCache()).thenReturn(true);
        when(remoteCache.getName()).thenReturn("OOLOCAL:BENHOUSE");
        when(remoteCache.get(anyString(), eq(String.class))).thenReturn(null);

        multiStepCacheDecorator.put("key", "value length greater than 10");

        verify(localCache, times(0)).put(any(), any());
        verify(remoteCache, times(1)).put("key", "value length greater than 10");

    }

    @Test
    void putWhenRemoteValueWithoutPrefix() {
        when(heliosCacheProperties.getLocalCachePrefix()).thenReturn("LOCAL");
        when(heliosCacheProperties.isEnableLocalCache()).thenReturn(true);
        when(remoteCache.getName()).thenReturn("LOCAL:BENHOUSE");
        when(remoteCache.get(anyString(), eq(String.class))).thenReturn("xxx");

        multiStepCacheDecorator.put("key", "value length greater than 10");

        verify(localCache, times(1)).put("key", "value length greater than 10");
        verify(remoteCache, times(1)).put("key", "&IND&xxx128;");
    }

    @Test
    void getName() {
        when(remoteCache.getName()).thenReturn("remote cache name");
        String remoteCacheName = multiStepCacheDecorator.getName();
        assertEquals("remote cache name", remoteCacheName);
        verify(remoteCache, times(1)).getName();

    }

    @Test
    void getNativeCache() {
        when(remoteCache.getNativeCache()).thenReturn("native cache");
        Object o = multiStepCacheDecorator.getNativeCache();

        assertEquals("native cache", o);
        verify(remoteCache, times(1)).getNativeCache();
    }

    @Test
    void evict() {
        multiStepCacheDecorator.evict("key");
        verify(remoteCache, times(1)).evict("key");
        verify(localCache, times(1)).evict("key");
    }

    @Test
    void clear() {
        multiStepCacheDecorator.clear();
        verify(remoteCache, times(1)).clear();
        verify(localCache, times(1)).clear();
    }
}