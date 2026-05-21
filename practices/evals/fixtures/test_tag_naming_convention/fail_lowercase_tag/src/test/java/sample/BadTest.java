package sample;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class BadTest {
    @Test
    @Tag("search")
    void bad_lowercase() {}
}
