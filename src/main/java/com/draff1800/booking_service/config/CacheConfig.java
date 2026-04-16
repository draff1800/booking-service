package com.draff1800.booking_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

  public static final String EVENT_DETAIL_CACHE = "eventDetails";
  public static final String PUBLISHED_UPCOMING_EVENTS_CACHE = "publishedUpcomingEvents";

  private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

  @Bean
  @ConditionalOnProperty(name = "app.cache.redis-enabled", havingValue = "true")
  @ConditionalOnBean(RedisConnectionFactory.class)
  CacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

    RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
      .disableCachingNullValues()
      .serializeValuesWith(
        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
      );

    Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
      EVENT_DETAIL_CACHE,
      defaults.entryTtl(Duration.ofMinutes(5)),
      PUBLISHED_UPCOMING_EVENTS_CACHE,
      defaults.entryTtl(Duration.ofSeconds(45))
    );

    return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(defaults)
      .withInitialCacheConfigurations(cacheConfigurations)
      .transactionAware()
      .build();
  }

  @Bean
  @ConditionalOnMissingBean(CacheManager.class)
  CacheManager fallbackCacheManager() {
    return new TransactionAwareCacheManagerProxy(
      new ConcurrentMapCacheManager(EVENT_DETAIL_CACHE, PUBLISHED_UPCOMING_EVENTS_CACHE)
    );
  }

  @Bean
  CacheErrorHandler cacheErrorHandler() {
    return new SimpleCacheErrorHandler() {
      @Override
      public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
        logger.warn("Cache GET failed for cache={} key={}", cache.getName(), key, exception);
      }

      @Override
      public void handleCachePutError(
        RuntimeException exception,
        org.springframework.cache.Cache cache,
        Object key,
        Object value
      ) {
        logger.warn("Cache PUT failed for cache={} key={}", cache.getName(), key, exception);
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
        logger.warn("Cache EVICT failed for cache={} key={}", cache.getName(), key, exception);
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
        logger.warn("Cache CLEAR failed for cache={}", cache.getName(), exception);
      }
    };
  }
}
