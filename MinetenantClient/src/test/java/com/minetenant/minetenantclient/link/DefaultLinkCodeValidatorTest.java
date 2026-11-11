package com.minetenant.minetenantclient.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultLinkCodeValidatorTest {

    private final LinkCodeValidator validator = new DefaultLinkCodeValidator();

    @Test
    void nullAndBlankAreEmpty() {
        assertEquals(LinkError.EMPTY, validator.validate(null).error());
        assertEquals(LinkError.EMPTY, validator.validate("").error());
        assertEquals(LinkError.EMPTY, validator.validate("   ").error());
    }

    @Test
    void innerWhitespaceIsInvalidFormat() {
        assertEquals(LinkError.INVALID_FORMAT, validator.validate("AB CD").error());
        assertEquals(LinkError.INVALID_FORMAT, validator.validate("AB\tCD").error());
    }

    @Test
    void controlCharacterIsInvalidFormat() {
        assertEquals(LinkError.INVALID_FORMAT, validator.validate("AB\u0000CD").error());
    }

    @Test
    void tooLongIsInvalidFormat() {
        String atLimit = "A".repeat(DefaultLinkCodeValidator.MAX_LENGTH);
        assertTrue(validator.validate(atLimit).isValid());
        assertEquals(LinkError.INVALID_FORMAT, validator.validate(atLimit + "A").error());
    }

    @Test
    void validCodeIsTrimmed() {
        LinkCodeValidator.Result result = validator.validate("  ABC-123  ");
        assertTrue(result.isValid());
        assertEquals("ABC-123", result.code());
    }

    @Test
    void invalidResultHasNoCode() {
        assertFalse(validator.validate("").isValid());
        assertEquals(null, validator.validate("").code());
    }
}
