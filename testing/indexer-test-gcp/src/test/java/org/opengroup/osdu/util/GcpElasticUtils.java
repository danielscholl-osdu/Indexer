package org.opengroup.osdu.util;

public class GcpElasticUtils extends ElasticUtils {

    private IndexerClientUtil indexerClientUtil;

    public GcpElasticUtils(HTTPClient httpClient) {
        super();
        this.indexerClientUtil = new IndexerClientUtil(httpClient);
    }

    @Override
    public void deleteIndex(String index) {
//        indexerClientUtil.deleteIndex(convertIndexToKindName(index));
    }

    private String convertIndexToKindName(String index) {
        return index.replaceAll("-",":");
    }
}
