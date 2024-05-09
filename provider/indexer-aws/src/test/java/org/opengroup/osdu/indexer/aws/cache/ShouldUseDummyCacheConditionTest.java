package org.opengroup.osdu.indexer.aws.cache;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShouldUseDummyCacheConditionTest {

    @Mock
    K8sLocalParameterProvider provider;
    @Mock
    ConditionContext context;
    @Mock
    AnnotatedTypeMetadata metadata;

    @Test
    public void test_match() {
        when(provider.getLocalMode()).thenReturn(true);
        ShouldUseDummyCacheCondition condition = new ShouldUseDummyCacheCondition(provider, true);
        assertTrue(condition.matches(context, metadata));
    }

    @Test
    public void test_nomatch() {
        when(provider.getLocalMode()).thenReturn(true);
        ShouldUseDummyCacheCondition condition = new ShouldUseDummyCacheCondition(provider, false);
        assertFalse(condition.matches(context, metadata));

        when(provider.getLocalMode()).thenReturn(false);
        // Second parameter doesn't really matter here
        condition = new ShouldUseDummyCacheCondition(provider, false);
        assertFalse(condition.matches(context, metadata));
    }
}
