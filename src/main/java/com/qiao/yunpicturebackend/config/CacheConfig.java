package com.qiao.yunpicturebackend.config;

import com.qiao.yunpicturebackend.manager.cache.CaffeineCacheManager;
import com.qiao.yunpicturebackend.manager.cache.RedisCacheManager;
import com.qiao.yunpicturebackend.model.vo.picture.PictureUserVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CacheConfig {
    @Bean
    public RedisCacheManager<PictureUserVO> pictureUserCacheManager(StringRedisTemplate stringRedisTemplate) {
        return new RedisCacheManager<>(
                "yunpicture",
                5* 60,
                stringRedisTemplate,
                PictureUserVO.class);
    }

    @Bean
    public CaffeineCacheManager<PictureUserVO> caffeineCacheManager() {
        return new CaffeineCacheManager<>("", 5 * 60, 10000);
    }
}
