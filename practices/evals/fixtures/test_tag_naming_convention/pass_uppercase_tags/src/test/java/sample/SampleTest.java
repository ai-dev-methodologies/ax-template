package sample;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SampleTest {
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-CAPTURE-001")
    void sample_one() {}

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.1.1")
    void sample_two() {}
}
