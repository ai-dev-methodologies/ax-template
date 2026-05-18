/**
 * @ax-template-meta
 * template_id: backend/data/JsonbConverter
 * layer: backend-infrastructure
 * domain: data
 * anchors_rule: persistence-no-n-plus-1.md (PRACTICES-PERS-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Hibernate ORM 6 User Guide — @JdbcTypeCode(SqlTypes.JSON) maps a Java object to a JSONB PostgreSQL column without a custom AttributeConverter; used for structured metadata fields"
 *     url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#basic-mapping-json"
 *   - source_type: external
 *     citation: "Jakarta Persistence 3.1 — AttributeConverter<X,Y> converts between a Java type X and a JDBC type Y; applied via @Convert on entity fields or globally via @Converter(autoApply=true)"
 *     url: "https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/converter"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Add jackson-databind to your build dependencies (included transitively via spring-boot-starter-web).
 *   Apply @Convert(converter = JsonbConverter.class) to Map<String, Object> or List<?> entity fields,
 *   OR use @JdbcTypeCode(SqlTypes.JSON) for Hibernate 6+ native JSON mapping (preferred for new code).
 *   This class is kept for compatibility with legacy AttributeConverter-based code.
 */
package com.example.app.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.Map;

/**
 * JPA {@link AttributeConverter} that serialises {@code Map<String, Object>} to/from
 * a PostgreSQL {@code JSONB} column.
 *
 * <p>Usage on an entity field:
 * <pre>
 * {@literal @}Convert(converter = JsonbConverter.class)
 * {@literal @}Column(columnDefinition = "jsonb")
 * private Map{@literal <}String, Object{@literal >} metadata;
 * </pre>
 *
 * <p>For Hibernate 6+ projects prefer {@code @JdbcTypeCode(SqlTypes.JSON)} which
 * delegates serialisation to the Hibernate dialect without requiring a converter.
 * This class exists for explicit control over the Jackson configuration (e.g.
 * {@code WRITE_DATES_AS_TIMESTAMPS=false} for Instant fields inside the map).
 *
 * <p>Thread-safe: the shared {@link ObjectMapper} is configured once at class load.
 */
@Converter
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize metadata to JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, MAP_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize metadata from JSON: " + dbData, e);
        }
    }
}
