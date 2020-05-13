package com.cloudhelios.cache.core;

import com.cloudhelios.cache.annotation.CacheNameSuffix;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.AbstractCacheResolver;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * 通过对@CacheName的解析
 * 支持动态指定@Cacheable 和@CacheEvict的Cache Name，实现批量失效指定范围的缓存
 * Cache Name会尝试自动拼接@CacheName注解解析出的内容
 */
public class HeliosCacheResolver extends AbstractCacheResolver {
    public HeliosCacheResolver(CacheManager cacheManager){
        super(cacheManager);
    }
    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> cacheOperationInvocationContext) {
        Collection<String> cacheNames = this.getCacheNames(cacheOperationInvocationContext);
        if (cacheNames == null) {
            return Collections.emptyList();
        } else {
            String suffix= getCacheNameSuffix(cacheOperationInvocationContext);
            Collection<Cache> result = new ArrayList(cacheNames.size());
            //显示指定了cacheName
            Iterator var4 = cacheNames.iterator();
            while(var4.hasNext()) {
                String cacheName = var4.next()+suffix;
                    Cache cache = this.getCacheManager().getCache(cacheName);
                    if (cache == null) {
                        throw new IllegalArgumentException("Cannot find cache named '" + cacheName + "' for " + cacheOperationInvocationContext.getOperation());
                    }
                    result.add(cache);
            }
            return result;
        }
    }

    @Override
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> cacheOperationInvocationContext) {
        return cacheOperationInvocationContext.getOperation().getCacheNames();
    }

    /**
     * 根据注解获取cache name尾缀
     * @param cacheOperationInvocationContext
     * @return
     */
    private String getCacheNameSuffix(CacheOperationInvocationContext<?> cacheOperationInvocationContext){
        //尝试获取自定义cache name
        Annotation[][]  annotations= cacheOperationInvocationContext.getMethod().getParameterAnnotations();
        Integer argIndex=null;
        String spel="";
        for (int i=0;i<annotations.length;i++){
            for(int j=0;j<annotations[i].length;j++){
                if(annotations[i][j].annotationType().isAssignableFrom(CacheNameSuffix.class)){
                    argIndex=i;
                    spel =((CacheNameSuffix) annotations[i][j]).value();
                    break;
                }
            }
        }
        if(argIndex==null){
            return "";
        }

        if(StringUtils.isEmpty(spel)){
            //spel为空，直接用参数值作为cache name
            if(cacheOperationInvocationContext.getArgs()[argIndex] == null){
                throw new RuntimeException("cache name param should not be null");
            }
            String calcName= cacheOperationInvocationContext.getArgs()[argIndex].toString();
            return calcName;
        }else {
            //解析spel表达式
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(spel);
            String calcName = exp.getValue(cacheOperationInvocationContext.getArgs()[argIndex]).toString();
            return calcName;
        }
    }
}
