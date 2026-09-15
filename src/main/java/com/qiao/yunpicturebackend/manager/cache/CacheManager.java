package com.qiao.yunpicturebackend.manager.cache;

import cn.hutool.core.util.StrUtil;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * 缓存管理器抽象类（模板方法模式）
 *
 * <p>定义缓存存取的公共逻辑（Key 前缀、默认过期时间、缓存回源），
 * 具体存储介质相关的操作由子类通过实现 {@link #doGet} / {@link #doPut} / {@link #doDelete} / {@link #doExists}
 * 等钩子方法完成，从而支持以继承方式扩展（如 Redis、Caffeine 等）。
 *
 */
public abstract class CacheManager {
    /**
     * 缓存名称，用于拼接 Key 前缀，区分不同业务缓存空间
     */
    private final String name;

    /**
     * 默认过期时间（秒）
     */
    private final long defaultExpireSeconds;

    /**
     * 默认过期时间（秒）
     */
    public static final long DEFAULT_EXPIRE_SECONDS = 30 * 60L;

    /**
     * 过期时间随机偏移比例，取值 0.2 表示在基准值上下各浮动 20%
     */
    private static final double EXPIRE_RANDOM_OFFSET_FACTOR = 0.2;

    /**
     * 偏移后的最小过期时间（秒），防止出现 0 或负值
     */
    private static final long MIN_EXPIRE_SECONDS = 1;

    protected CacheManager(String name, long defaultExpireSeconds) {
        this.name = name;
        this.defaultExpireSeconds = defaultExpireSeconds > 0 ? defaultExpireSeconds : DEFAULT_EXPIRE_SECONDS;
    }

    // ==================== 模板方法（公共逻辑） ====================

    /**
     * 读取缓存，未命中时回源加载并写入缓存（缓存穿透时不会缓存空值）
     *
     * @param key    缓存键（不含前缀）
     * @param loader 回源加载函数，仅在缓存未命中时触发
     * @return 缓存值
     */
    public<V> V get(String key, Class<V> valueType, Function<String, V> loader) {
        return get(key, valueType, loader, defaultExpireSeconds);
    }

    /**
     * 读取缓存，未命中时回源加载并以指定过期时间写入缓存
     *
     * @param key           缓存键（不含前缀）
     * @param loader        回源加载函数
     * @param expireSeconds 过期时间（秒），小于等于 0 时使用默认过期时间
     * @return 缓存值
     */
    public<V> V get(String key, Class<V> valueType, Function<String, V> loader, long expireSeconds) {
        String cacheKey = buildKey(key);
        V value = doGet(cacheKey, valueType);
        if (value != null) {
            return value;
        }
        // 缓存未命中，回源加载
        value = loader.apply(key);
        if (value != null) {
            // 查询成功，写入缓存
            doPut(cacheKey, value, resolveExpire(expireSeconds));
        }
        return value;
    }

    // ==================== 基础操作方法（公共逻辑） ====================

    public<V> V get(String key, Class<V> valueType) {
        return doGet(buildKey(key), valueType);
    }

    public<V> void put(String key, V value) {
        put(key, value, defaultExpireSeconds);
    }

    public<V> void put(String key, V value, long expireSeconds) {
        doPut(buildKey(key), value, resolveExpire(expireSeconds));
    }

    public void delete(String key) {
        doDelete(buildKey(key));
    }

    public boolean exists(String key) {
        return doExists(buildKey(key));
    }

    // ==================== 公共逻辑（供子类复用） ====================

    /**
     * 拼接缓存键前缀：{name}:{key}
     */
    protected String buildKey(String key) {
        return StrUtil.isNotBlank(name) ? name + ":" + key : key;
    }

    /**
     * 解析过期时间：非法值时回退到默认过期时间，并附加随机偏移以避免缓存雪崩。
     *
     * <p>若大量缓存使用相同过期时间，会在同一时刻集中失效，瞬时回源击穿数据库造成缓存雪崩。
     * 这里让每个 Key 的过期时间在基准值上下随机浮动，使过期时间错峰、分散。
     *
     * @param expireSeconds 基准过期时间（秒）
     * @return 附加随机偏移后的实际过期时间（秒）
     */
    protected long resolveExpire(long expireSeconds) {
        long base = expireSeconds > 0 ? expireSeconds : defaultExpireSeconds;
        // 在 [1 - FACTOR, 1 + FACTOR] 区间随机取一个比例，使 TTL 上下浮动
        double factor = 1 + ThreadLocalRandom.current().nextDouble(-EXPIRE_RANDOM_OFFSET_FACTOR, EXPIRE_RANDOM_OFFSET_FACTOR);
        long result = Math.round(base * factor);
        // 保证偏移后至少存活 1 秒，避免取到 0 或负值
        return Math.max(result, MIN_EXPIRE_SECONDS);
    }

    protected String getName() {
        return name;
    }

    // ==================== 抽象钩子方法（由子类实现） ====================

    /**
     * 从具体缓存介质读取值
     *
     * @param fullKey 已拼接前缀的完整键
     * @param valueType 缓存值类型
     * @return 缓存值，未命中返回 null
     */
    protected abstract <V> V doGet(String fullKey, Class<V> valueType);

    /**
     * 向具体缓存介质写入值
     *
     * @param fullKey       已拼接前缀的完整键
     * @param value         缓存值
     * @param expireSeconds 过期时间（秒）
     */
    protected abstract <V> void doPut(String fullKey, V value, long expireSeconds);

    /**
     * 从具体缓存介质删除值
     *
     * @param fullKey 已拼接前缀的完整键
     */
    protected abstract void doDelete(String fullKey);

    /**
     * 判断值是否存在于具体缓存介质
     *
     * @param fullKey 已拼接前缀的完整键
     * @return 存在返回 true
     */
    protected abstract boolean doExists(String fullKey);
}
