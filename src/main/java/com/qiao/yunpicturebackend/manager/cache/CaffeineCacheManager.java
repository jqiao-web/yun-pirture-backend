package com.qiao.yunpicturebackend.manager.cache;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.Value;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存管理器
 *
 * <p>基于 Caffeine 实现，为进程内缓存，无需网络开销，适合单机场景。
 * 通过 {@link Expiry} 支持按条目设置过期时间（与 Redis 的按 Key 过期语义保持一致）。
 * 过期时间已实现上下20%的随机抖动，避免缓存过期时大量请求同时击穿。
 *
 */
public class CaffeineCacheManager extends CacheManager {

    private final Cache<String, CacheEntry> cache;

    public CaffeineCacheManager(String name, long defaultExpireSeconds, long maximumSize) {
        super(name, defaultExpireSeconds);
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                // Caffeine 默认的 expireAfterWrite/expireAfterAccess 只能设置「全局统一」的过期时间，
                // 无法让每个条目拥有不同的 TTL。这里改用 Expiry 接口，从条目本身读取过期时间，
                // 从而实现与 Redis 一致的「按 Key 设置过期时间」。
                .expireAfter(new Expiry<String, CacheEntry>() {
                    /**
                     * 条目「首次写入」时，返回该条目剩余存活时长（纳秒）。
                     * 这里是写入时刻的起点，过期时长直接取条目自带的 expireNanos。
                     */
                    @Override
                    public long expireAfterCreate(String key, CacheEntry entry, long currentTime) {
                        return entry.expireNanos;
                    }

                    /**
                     * 条目「被更新（覆盖写入）」时，返回新的剩余存活时长。
                     * 返回 entry.expireNanos 表示更新后重新计时，等同于 expireAfterWrite 语义。
                     */
                    @Override
                    public long expireAfterUpdate(String key, CacheEntry entry, long currentTime, long currentDuration) {
                        return entry.expireNanos;
                    }

                    /**
                     * 条目「被读取」时，返回读取后剩余存活时长。
                     * 返回 currentDuration（原剩余时长）表示读取不刷新过期时间，
                     * 即「写入后过期」语义；若想实现「访问后过期」，可改为 return entry.expireNanos。
                     */
                    @Override
                    public long expireAfterRead(String key, CacheEntry entry, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Override
    protected <V> V doGet(String fullKey, Class<V> valueType) {
        CacheEntry entry = cache.getIfPresent(fullKey);
        return entry == null ? null : BeanUtil.toBean(entry.value, valueType);
    }

    @Override
    protected <V> void doPut(String fullKey, V value, long expireSeconds) {
        String jsonValue = JSONUtil.toJsonStr(value);
        cache.put(fullKey, new CacheEntry(jsonValue, TimeUnit.SECONDS.toNanos(expireSeconds)));
    }

    @Override
    protected void doDelete(String fullKey) {
        cache.invalidate(fullKey);
    }

    @Override
    protected boolean doExists(String fullKey) {
        return cache.getIfPresent(fullKey) != null;
    }

    /**
     * 缓存条目包装，携带自身的过期时间（纳秒）
     */
    @Value
    private static class CacheEntry {
        String value;
        long expireNanos;
    }
}
