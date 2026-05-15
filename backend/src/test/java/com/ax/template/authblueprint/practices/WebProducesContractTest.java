package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag("PRACTICES")
@Tag("PRACTICES-WEB-002")
class WebProducesContractTest {

    @Test
    void practices_WEB_002_controllerDeclaresExplicitProducesContentType() {
        // The default content-negotiation behavior derives a content type from the
        // Accept header — clients can negotiate XML, text/plain, or any other configured
        // converter. A JSON API must declare its produces contract explicitly so the
        // contract is part of the @RequestMapping and not a default that can shift.
        RequestMapping ann = PracticesDemoController.class.getAnnotation(RequestMapping.class);
        assertThat(ann)
                .as("controller must carry @RequestMapping for produces declaration")
                .isNotNull();
        assertThat(ann.produces())
                .as("@RequestMapping must declare an explicit produces media type")
                .isNotEmpty();
        boolean hasJson = Arrays.stream(ann.produces())
                .anyMatch(m -> m.contains("application/json"));
        assertThat(hasJson)
                .as("the produces declaration must include application/json")
                .isTrue();
    }
}
