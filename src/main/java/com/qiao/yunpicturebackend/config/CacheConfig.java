package com.qiao.yunpicturebackend.config;

import com.qiao.yunpicturebackend.manager.cache.CaffeineCacheManager;
import com.qiao.yunpicturebackend.manager.cache.RedisCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CacheConfig {
    @Bean
    public RedisCacheManager redisCacheManager(StringRedisTemplate stringRedisTemplate) {
        return new RedisCacheManager(
                "yunpicture",
                5* 60,
                stringRedisTemplate);
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        return new CaffeineCacheManager("yunpicture", 5 * 60, 10000);
    }
}
