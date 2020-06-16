package org.opengroup.osdu.indexer.util.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class BooleanParserTest {

    @InjectMocks
    private BooleanParser sut;

    @Test
    public void should_parseBoolean() {
        assertTrue(this.sut.parseBoolean("testBooleanAttribute", "true"));
        assertTrue(this.sut.parseBoolean("testBooleanAttribute", "TRUE"));

        assertFalse(this.sut.parseBoolean("testBooleanAttribute", ""));
        assertFalse(this.sut.parseBoolean("testBooleanAttribute", null));
        assertFalse(this.sut.parseBoolean("testBooleanAttribute", "false"));
        assertFalse(this.sut.parseBoolean("testBooleanAttribute", "truee"));
    }
}