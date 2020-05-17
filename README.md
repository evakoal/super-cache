# super-cache
Super Cache 提供了更强大的缓存功能支持，包括本地二级缓存，
节点间二级缓存同步，更加灵活的范围失效（batch evict）key。更适用于逻辑复杂，超高并发，超多节点，
超大key数量的生产环境。


**本地缓存**

引入Super Cache 将自动启用应用节点本地缓存，缓存值实际存储在各个应用节点本地，Redis只存储
各个节点 key 的有效性指示器，极大地降低Redis的内存占用（一般10倍以上）。

由于缓存内容实际从本地节点内存读出，Redis的出口流量也大大降低，如果Redis目前能支撑10个节点的应用服务器，
在启用Super Cache 后，可支持 5到6倍的应用节点数量(取决于使用Super Cache前，Redis key值的大小)。

Super Cache会自动处理节点间的缓存同步，当有节点更新了本地的key缓存时，其余节点的key本地缓存将自动失效，
保障本地缓存的及时性。

**更灵活的缓存失效机制**

Spring Boot提供了按 cache name 批量失效同一cache name下所有key，但只支持固定的Cache Name。

而在实际业务中，经常需要动态计算需要失效的key值范围。

比如，修改了租户下某一个公司的规则（Cache Name ="RULE" && Tenant = "TENANT A" && Company = "COMPANY A"），需要失效并重算整个租户下所有公司的规则缓存（Cache Name ="RULE" && Tenant = "TENANT A"），
然而Spring目前只支持失效Cache Name= "Rule" 下的所有缓存，这并不是我们想要的。我们只想失效A租户所有相关的规则缓存。

Super Cache 提供了动态Cache Name 支持, 建立缓存时可以动态指定Cache Name 为 RULE_TENANT_A,从而可以更灵活地失效指定范围的key。


## Quick Start
### 引入Maven依赖
引入Super Cache maven依赖
```xml

```
### 启用@EnableSuperCache
在 项目启动类上启用super cache注解
```java

@EnableSuperCache
public class AuthServiceApplication {
    SpringApplication app = new SpringApplication(AuthServiceApplication.class);
}   

```

到此为止，Super Cache已经可用了。

## 如何使用
### 本地缓存
完成Quick Start 后，Super Cache 会自动启用本地缓存，所有@Cacheable注解的缓存将自动存储在本地节点内存中，远端缓存
只存储各个节点的缓存有效性指示器。这一切对开发者都是透明的。
例如以下代码中,readAccessToken的返回值将实际缓存在本地内存中，远端缓存只维护该key的节点有效性指示器。
```java

@Cacheable(key = "#tokenValue")
public OAuth2AccessToken readAccessToken(String tokenValue) {return super.readAccessToken(tokenValue);}

```

### 只将超过指定大小的value缓存在本地
你可能不想把系统里所有缓存都缓存在本地内存，Super Cache 提供了参数，可以让你只缓存过大的value在本地。

例如以下配置，只将缓存内容序列化为Json后，长度大于1000的对象在本地，其余缓存值仍然存储在远端缓存中。

由于Json序列化大小判断只在更新缓存时发生，频率较低，所以不用担心效率问题。
```yaml
cache:
    super:
      minimumLocalKeySize: 1000
```

### 只将Cache Name以指定前缀开头的内容缓存在本地
你可能不想把系统里所有缓存都缓存在本地内存，Super Cache 提供了参数，可以让你只缓存指定前缀的内容在本地。

例如以下配置，将只缓存Cache Name 以LOCAL开头的内容在本地，其余缓存值仍然存储在远端Redis中。

```yaml
cache:
    super:
      localCachePrefix: LOCAL
```

### 动态计算失效范围
Super Cache 支持开发者在@Cacheable 和 @CacheEvict 中动态指定Cache Name的后缀，这样的好处是可以动态失效指定范围的缓存。

只需要在@Cacheable和对应的@CacheEvict 注解修饰的方法参数中，添加@CacheNameSuffix注解。

例如以下代码中，当tenantID入参为123时，实际产生的Cache Name 将会拼接为 SOME_CACHE_NAME123.

```java
@Cacheable(value="SOME_CACHE_NAME", key="'_tenantId_'+#tenantID+'_companyId_'+#companyID") 
public ExpRptPrintOptionDTO getCompanyOption( Long companyID,@CacheNameSuffix Long tenantID) {
  ...
}

```

这样，就可以实现动态失效指定租户123的所有相关注解，举例如下方法运行时，假设入参tenantID值为123时，将批量失效Cache Name为
SOME_CACHE_NAME123的所有key。

```java
@CacheEvict(value="SOME_CACHE_NAME", allEntries = true) 
public ExpRptPrintOptionDTO getCompanyOption( Long companyID,@CacheNameSuffix Long tenantID) {
  ...
}
```

