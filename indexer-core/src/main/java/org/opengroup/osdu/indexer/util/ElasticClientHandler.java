package org.opengroup.osdu.indexer.util;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import lombok.extern.java.Log;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log
public class ElasticClientHandler {

  // Elastic cluster Rest client settings
  private static final int CLOUD_REST_CLIENT_PORT = 9243;
  private static final int REST_CLIENT_CONNECT_TIMEOUT = 60000;
  private static final int REST_CLIENT_SOCKET_TIMEOUT = 60000;
  private static final int REST_CLIENT_RETRY_TIMEOUT = 60000;

  @Value("#{new Boolean('${security.https.certificate.trust}')}")
  private Boolean isSecurityHttpsCertificateTrust;

  @Autowired
  private IElasticSettingService elasticSettingService;

  public RestHighLevelClient createRestClient() {
    return getCloudRestClient(elasticSettingService.getElasticClusterInformation());
  }

  // TODO: Remove this temporary implementation when ECE CCS is utilized
  public RestHighLevelClient createRestClient(final ClusterSettings clusterSettings) {
    return getCloudRestClient(clusterSettings);
  }

  private RestHighLevelClient getCloudRestClient(final ClusterSettings clusterSettings) {

    String cluster = null;
    String host = null;
    int port = CLOUD_REST_CLIENT_PORT;
    String protocolScheme = "https";
    String tls = "true";

    try {
      cluster = clusterSettings.getHost();
      host = clusterSettings.getHost();
      port = clusterSettings.getPort();
      if (!clusterSettings.isHttps()) {
        protocolScheme = "http";
      }

      if (!clusterSettings.isTls()) {
        tls = "false";
      }
      String basicEncoded = Base64
          .getEncoder().encodeToString(clusterSettings.getUserNameAndPassword().getBytes());
      String basicAuthenticationHeaderVal = String.format("Basic %s", basicEncoded);

      RestClientBuilder builder = createClientBuilder(host, basicAuthenticationHeaderVal, port,
          protocolScheme, tls);

      return new RestHighLevelClient(builder);
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          "search client error",
          "error creating search client",
          String
              .format("Elastic client connection params, cluster: %s, host: %s, port: %s", cluster,
                  host, port),
          e);
    }
  }

  public RestClientBuilder createClientBuilder(String host, String basicAuthenticationHeaderVal,
      int port, String protocolScheme, String tls) {
    RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, protocolScheme));
    builder.setRequestConfigCallback(
        requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(REST_CLIENT_CONNECT_TIMEOUT)
            .setSocketTimeout(REST_CLIENT_SOCKET_TIMEOUT));
    builder.setMaxRetryTimeoutMillis(REST_CLIENT_RETRY_TIMEOUT);

    Header[] defaultHeaders = new Header[]{
        new BasicHeader("client.transport.nodes_sampler_interval", "30s"),
        new BasicHeader("client.transport.ping_timeout", "30s"),
        new BasicHeader("client.transport.sniff", "false"),
        new BasicHeader("request.headers.X-Found-Cluster", host),
        new BasicHeader("cluster.name", host),
        new BasicHeader("xpack.security.transport.ssl.enabled", tls),
        new BasicHeader("Authorization", basicAuthenticationHeaderVal),
    };
    log.info(String.format(
        "Elastic client connection uses protocolScheme = %s with a flag "
            + "'security.https.certificate.trust' = %s",
        protocolScheme, isSecurityHttpsCertificateTrust));
    if ("https".equals(protocolScheme) && isSecurityHttpsCertificateTrust) {
      log.warning("Elastic client connection uses TrustSelfSignedStrategy()");
      SSLContext sslContext = createSSLContext();
      builder.setHttpClientConfigCallback(httpClientBuilder ->
      {
        HttpAsyncClientBuilder httpAsyncClientBuilder = httpClientBuilder.setSSLContext(sslContext)
            .setSSLHostnameVerifier(
                NoopHostnameVerifier.INSTANCE);
        return httpAsyncClientBuilder;
      });
    }

    builder.setDefaultHeaders(defaultHeaders);
    return builder;
  }

  private SSLContext createSSLContext() {
    SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
    try {
      sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      return sslContextBuilder.build();
    } catch (NoSuchAlgorithmException e) {
      log.severe(e.getMessage());
    } catch (KeyStoreException e) {
      log.severe(e.getMessage());
    } catch (KeyManagementException e) {
      log.severe(e.getMessage());
    }
    return null;
  }

  public Boolean isSecurityHttpsCertificateTrust() {
    return isSecurityHttpsCertificateTrust;
  }

  public void setSecurityHttpsCertificateTrust(Boolean isSecurityHttpsCertificateTrust) {
    this.isSecurityHttpsCertificateTrust = isSecurityHttpsCertificateTrust;
  }
}