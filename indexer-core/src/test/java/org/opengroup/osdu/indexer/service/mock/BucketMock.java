package org.opengroup.osdu.indexer.service.mock;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import java.io.IOException;

public class BucketMock implements Terms.Bucket {
    @Override
    public Number getKeyAsNumber() {
        return null;
    }

    @Override
    public long getDocCountError() {
        return 0;
    }

    @Override
    public Object getKey() {
        return null;
    }

    @Override
    public String getKeyAsString() {
        return null;
    }

    @Override
    public long getDocCount() {
        return 0;
    }

    @Override
    public Aggregations getAggregations() {
        return null;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder xContentBuilder, Params params) throws IOException {
        return null;
    }
}
