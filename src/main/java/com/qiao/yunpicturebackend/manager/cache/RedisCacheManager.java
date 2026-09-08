package com.qiao.yunpicturebackend.manager.cache;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存管理器
 *
 * <p>基于 {@link StringRedisTemplate} 实现，值以 JSON 字符串形式存储，支持按 Key 设置过期时间。
 * 需要在 Spring 容器中注册为 Bean 时，可配合 {@link org.springframework.context.annotation.Bean} 使用，
 * 并传入缓存名称、默认过期时间、{@link StringRedisTemplate} 以及值类型。
 * 过期时间已实现上下20%的随机抖动，避免缓存过期时大量请求同时击穿。
 *
 * @param <V> 缓存值类型
 */
public class RedisCacheManager<V> extends CacheManager<V> {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 值类型，用于反序列化 JSON
     */
    private final Class<V> valueType;

    public RedisCacheManager(String name, long defaultExpireSeconds,
                             StringRedisTemplate stringRedisTemplate, Class<V> valueType) {
        super(name, defaultExpireSeconds);
        this.stringRedisTemplate = stringRedisTemplate;
        this.valueType = valueType;
    }

    @Override
    protected V doGet(String fullKey) {
        String json = stringRedisTemplate.opsForValue().get(fullKey);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JSONUtil.toBean(json, valueType);
    }

    @Override
    protected void doPut(String fullKey, V value, long expireSeconds) {
        stringRedisTemplate.opsForValue().set(fullKey, JSONUtil.toJsonStr(value), expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    protected void doDelete(String fullKey) {
        stringRedisTemplate.delete(fullKey);
    }

    @Override
    protected boolean doExists(String fullKey) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(fullKey));
    }
}
