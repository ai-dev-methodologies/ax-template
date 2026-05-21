package com.ax.template.authblueprint.tagcategorization;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("TAGGING")
class TagSluggerTest {

    @Test
    @org.junit.jupiter.api.Tag("TAG-CRUD-001")
    void slugify_lowercasesAndHyphenates() {
        assertThat(TagSlugger.slugify("New Product Line")).isEqualTo("new-product-line");
    }

    @Test
    @org.junit.jupiter.api.Tag("TAG-CRUD-001")
    void slugify_collapsesPunctuationAndDoubleSpaces() {
        assertThat(TagSlugger.slugify("Hello, World!  Test")).isEqualTo("hello-world-test");
    }

    @Test
    @org.junit.jupiter.api.Tag("TAG-CRUD-001")
    void slugify_stripsAccents() {
        assertThat(TagSlugger.slugify("Café à Paris")).isEqualTo("cafe-a-paris");
    }

    @Test
    @org.junit.jupiter.api.Tag("TAG-CRUD-001")
    void slugify_koreanFallsBackToTagPrefix() {
        // The basic ASCII-only slugger cannot transliterate Korean — the catalog
        // policy is "fork-receivers needing 케이팝 → kpop swap this utility."
        // Result must be a stable, unique-by-construction "tag-<uuid8>" string.
        String slug = TagSlugger.slugify("신상품");
        assertThat(slug).startsWith("tag-").hasSize(12);
    }

    @Test
    @org.junit.jupiter.api.Tag("TAG-CRUD-001")
    void slugify_truncatesAt64Chars() {
        String veryLong = "a".repeat(120);
        String slug = TagSlugger.slugify(veryLong);
        assertThat(slug).hasSize(64);
    }

    @Test
    @org.junit.jupiter.api.Tag("TAG-CRUD-001")
    void slugify_emptyAndNullFallBack() {
        assertThat(TagSlugger.slugify(null)).startsWith("tag-");
        assertThat(TagSlugger.slugify("")).startsWith("tag-");
        assertThat(TagSlugger.slugify("   ")).startsWith("tag-");
    }
}
