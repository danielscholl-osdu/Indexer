package org.opengroup.osdu.indexer.aws.cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShouldUseVMCacheConditionTest {

    @Mock
    K8sLocalParameterProvider provider;
    @Mock
    ConditionContext context;
    @Mock
    AnnotatedTypeMetadata metadata;

    @Test
    public void test_match() {
        when(provider.getLocalMode()).thenReturn(true);
        ShouldUseVMCacheCondition condition = new ShouldUseVMCacheCondition(provider, false);
        assertTrue(condition.matches(context, metadata));
    }

    @Test
    public void test_nomatch() {
        when(provider.getLocalMode()).thenReturn(true);
        ShouldUseVMCacheCondition condition = new ShouldUseVMCacheCondition(provider, true);
        assertFalse(condition.matches(context, metadata));

        when(provider.getLocalMode()).thenReturn(false);
        // Second parameter doesn't really matter here
        condition = new ShouldUseVMCacheCondition(provider, false);
        assertFalse(condition.matches(context, metadata));
    }
}
