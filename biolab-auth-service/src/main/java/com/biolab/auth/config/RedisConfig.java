package com.biolab.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration — enables Spring Data Redis repositories for MFA
 * pending-token storage and configures serialization.
 *
 * <p>The {@code @EnableRedisRepositories} annotation activates
 * {@link com.biolab.auth.repository.MfaPendingTokenRepository} and
 * any future Redis-backed repositories in the auth service.</p>
 *
 * <h3>Connection properties (from {@code application.yml}):</h3>
 * <pre>
 *   spring.data.redis.host = ${REDIS_HOST:localhost}
 *   spring.data.redis.port = ${REDIS_PORT:6379}
 * </pre>
 *
 * @author BioLab Engineering Team
 */
@Configuration
@EnableRedisRepositories(basePackages = "com.biolab.auth.repository")
public class RedisConfig {

    /**
     * Configures a {@link RedisTemplate} with String keys and JSON values.
     * Used for any manual Redis operations outside of Spring Data repositories.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
