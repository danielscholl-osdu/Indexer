package org.opengroup.osdu.indexer.service.mock;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TermMock implements Terms {
    @Override
    public List<BucketMock> getBuckets() {
        return null;
    }

    @Override
    public Bucket getBucketByKey(String s) {
        return null;
    }

    @Override
    public long getDocCountError() {
        return 0;
    }

    @Override
    public long getSumOfOtherDocCounts() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return null;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder xContentBuilder, Params params) throws IOException {
        return null;
    }
}
