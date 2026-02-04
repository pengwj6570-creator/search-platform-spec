package com.search.config.generator;

import com.search.config.model.FieldConfig;
import com.search.config.model.FieldType;
import com.search.config.model.SearchObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MappingGenerator
 */
class MappingGeneratorTest {

    private MappingGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MappingGenerator();
    }

    @Test
    void testGenerateMappingWithTextField() {
        SearchObject product = new SearchObject("product", "source1");
        product.setAppKey("ecommerce");

        FieldConfig titleField = new FieldConfig("title", FieldType.TEXT);
        titleField.setSearchable(true);
        titleField.setAnalyzer("ik_max_word");
        titleField.setBoost(2.0f);

        product.setFields(List.of(titleField));

        String mapping = generator.generate(product);

        assertNotNull(mapping);
        assertTrue(mapping.contains("\"type\" : \"text\""));
        assertTrue(mapping.contains("\"analyzer\" : \"ik_max_word\""));
        assertTrue(mapping.contains("\"boost\""));
    }

    @Test
    void testGenerateMappingWithKeywordField() {
        SearchObject product = new SearchObject("product", "source1");

        FieldConfig categoryField = new FieldConfig("category", FieldType.KEYWORD);
        categoryField.setFilterable(true);

        product.setFields(List.of(categoryField));

        String mapping = generator.generate(product);

        assertNotNull(mapping);
        assertTrue(mapping.contains("\"type\" : \"keyword\""));
    }

    @Test
    void testGenerateMappingWithNumericField() {
        SearchObject product = new SearchObject("product", "source1");

        FieldConfig priceField = new FieldConfig("price", FieldType.DOUBLE);
        priceField.setFilterable(true);

        product.setFields(List.of(priceField));

        String mapping = generator.generate(product);

        assertNotNull(mapping);
        assertTrue(mapping.contains("\"type\" : \"double\""));
    }

    @Test
    void testGenerateMappingWithVectorField() {
        SearchObject product = new SearchObject("product", "source1");

        FieldConfig vectorField = new FieldConfig("title_vector", FieldType.DENSE_VECTOR);
        vectorField.setVectorize(true);
        vectorField.setVectorDim(768);

        product.setFields(List.of(vectorField));

        String mapping = generator.generate(product);

        assertNotNull(mapping);
        assertTrue(mapping.contains("\"type\" : \"knn_vector\""));
        assertTrue(mapping.contains("\"dims\" : 768"));
        assertTrue(mapping.contains("\"similarity\" : \"cosine\""));
        assertTrue(mapping.contains("\"index\" : true"));
    }

    @Test
    void testGenerateMappingWithMultipleFields() {
        SearchObject product = new SearchObject("product", "source1");
        product.setAppKey("ecommerce");

        FieldConfig titleField = new FieldConfig("title", FieldType.TEXT);
        titleField.setSearchable(true);
        titleField.setAnalyzer("ik_max_word");

        FieldConfig priceField = new FieldConfig("price", FieldType.DOUBLE);
        priceField.setFilterable(true);

        FieldConfig vectorField = new FieldConfig("title_vector", FieldType.DENSE_VECTOR);
        vectorField.setVectorize(true);
        vectorField.setVectorDim(768);

        product.setFields(List.of(titleField, priceField, vectorField));

        String mapping = generator.generate(product);

        assertNotNull(mapping);
        assertTrue(mapping.contains("\"type\" : \"text\""));
        assertTrue(mapping.contains("\"analyzer\" : \"ik_max_word\""));
        assertTrue(mapping.contains("\"type\" : \"double\""));
        assertTrue(mapping.contains("\"type\" : \"knn_vector\""));
        assertTrue(mapping.contains("\"dims\" : 768"));
        assertTrue(mapping.contains("\"properties\""));
    }

    @Test
    void testGenerateMappingWithNullObjectThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null));
    }

    @Test
    void testGenerateMappingWithEmptyFieldsThrowsException() {
        SearchObject product = new SearchObject("product", "source1");
        product.setFields(List.of());

        assertThrows(IllegalArgumentException.class, () -> generator.generate(product));
    }

    @Test
    void testMapAllFieldTypes() {
        SearchObject testObj = new SearchObject("test", "source1");

        FieldConfig textField = new FieldConfig("text_field", FieldType.TEXT);
        FieldConfig keywordField = new FieldConfig("keyword_field", FieldType.KEYWORD);
        FieldConfig intField = new FieldConfig("int_field", FieldType.INTEGER);
        FieldConfig longField = new FieldConfig("long_field", FieldType.LONG);
        FieldConfig doubleField = new FieldConfig("double_field", FieldType.DOUBLE);
        FieldConfig dateField = new FieldConfig("date_field", FieldType.DATE);
        FieldConfig boolField = new FieldConfig("bool_field", FieldType.BOOLEAN);
        FieldConfig vectorField = new FieldConfig("vector_field", FieldType.DENSE_VECTOR);

        testObj.setFields(List.of(textField, keywordField, intField, longField,
                                   doubleField, dateField, boolField, vectorField));

        String mapping = generator.generate(testObj);

        assertNotNull(mapping);
        assertTrue(mapping.contains("\"type\" : \"text\""));
        assertTrue(mapping.contains("\"type\" : \"keyword\""));
        assertTrue(mapping.contains("\"type\" : \"integer\""));
        assertTrue(mapping.contains("\"type\" : \"long\""));
        assertTrue(mapping.contains("\"type\" : \"double\""));
        assertTrue(mapping.contains("\"type\" : \"date\""));
        assertTrue(mapping.contains("\"type\" : \"boolean\""));
        assertTrue(mapping.contains("\"type\" : \"knn_vector\""));
    }

    @Test
    void testGenerateMappingContainsPropertiesRoot() {
        SearchObject product = new SearchObject("product", "source1");

        FieldConfig field = new FieldConfig("id", FieldType.KEYWORD);
        product.setFields(List.of(field));

        String mapping = generator.generate(product);

        assertNotNull(mapping);
        assertTrue(mapping.trim().startsWith("{"));
        assertTrue(mapping.contains("\"properties\""));
    }
}
