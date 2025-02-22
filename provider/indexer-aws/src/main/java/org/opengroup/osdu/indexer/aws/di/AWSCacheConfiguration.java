package org.opengroup.osdu.indexer.aws.di;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;


@Primary
@Configuration
@ConfigurationProperties
@Getter
@Setter
public class AWSCacheConfiguration {

    @Value("${aws.elasticache.cluster.endpoint}")
    String redisSearchHost;
    @Value("${aws.elasticache.cluster.port}")
    String redisSearchPort;
    @Value("${aws.elasticache.cluster.key}")
    String redisSearchKey;
    @Value("${aws.elasticache.cluster.index.expiration}")
    String indexCacheExpiration;
    private String cacheClusterKey;
    private int cacheClusterPort;
    private String cacheClusterHost;
    private int cacheExpireTimeInSeconds;
    private boolean localMode;

    public AWSCacheConfiguration() throws K8sParameterNotFoundException, JsonProcessingException {
        this(new K8sLocalParameterProvider());
    }

    public AWSCacheConfiguration(K8sLocalParameterProvider provider ) throws K8sParameterNotFoundException, JsonProcessingException {
        this.cacheExpireTimeInSeconds = 3600;

        this.localMode = provider.getLocalMode();
        this.cacheClusterPort = Integer.parseInt(provider.getParameterAsStringOrDefault("CACHE_CLUSTER_PORT", redisSearchPort));
        this.cacheClusterHost = provider.getParameterAsStringOrDefault("CACHE_CLUSTER_ENDPOINT", redisSearchHost);

        Map<String, String> credential = provider.getCredentialsAsMap("CACHE_CLUSTER_KEY");
        if (credential != null) {
            this.cacheClusterKey = credential.get("token");
        } else {
            this.cacheClusterKey = redisSearchKey;
        }
    }
}
