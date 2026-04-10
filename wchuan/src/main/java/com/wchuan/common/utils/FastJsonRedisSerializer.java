package com.wchuan.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import com.alibaba.fastjson.parser.ParserConfig;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public record FastJsonRedisSerializer<T>(Class<T> clazz) implements RedisSerializer<T> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    static {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) return new byte[0];
        return JSON.toJSONString(t, SerializerFeature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) return null;
        return JSON.parseObject(new String(bytes, DEFAULT_CHARSET), clazz);
    }
}