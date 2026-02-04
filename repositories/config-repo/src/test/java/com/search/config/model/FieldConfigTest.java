package com.search.config.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for FieldConfig model
 */
class FieldConfigTest {

    @Test
    void testFieldConfigCreation() {
        FieldConfig fieldConfig = new FieldConfig();
        fieldConfig.setName("title");
        fieldConfig.setType(FieldType.TEXT);
        fieldConfig.setSearchable(true);
        fieldConfig.setFilterable(false);
        fieldConfig.setSortable(false);
        fieldConfig.setAnalyzer("ik_max_word");
        fieldConfig.setVectorize(true);
        fieldConfig.setVectorType("dense_vector");
        fieldConfig.setVectorDim(768);
        fieldConfig.setBoost(2.0f);

        assertEquals("title", fieldConfig.getName());
        assertEquals(FieldType.TEXT, fieldConfig.getType());
        assertTrue(fieldConfig.isSearchable());
        assertFalse(fieldConfig.isFilterable());
        assertFalse(fieldConfig.isSortable());
        assertEquals("ik_max_word", fieldConfig.getAnalyzer());
        assertTrue(fieldConfig.isVectorize());
        assertEquals("dense_vector", fieldConfig.getVectorType());
        assertEquals(768, fieldConfig.getVectorDim());
        assertEquals(2.0f, fieldConfig.getBoost());
    }

    @Test
    void testFieldTypeEnum() {
        assertEquals(8, FieldType.values().length);

        assertEquals(FieldType.TEXT, FieldType.valueOf("TEXT"));
        assertEquals(FieldType.KEYWORD, FieldType.valueOf("KEYWORD"));
        assertEquals(FieldType.INTEGER, FieldType.valueOf("INTEGER"));
        assertEquals(FieldType.LONG, FieldType.valueOf("LONG"));
        assertEquals(FieldType.DOUBLE, FieldType.valueOf("DOUBLE"));
        assertEquals(FieldType.DATE, FieldType.valueOf("DATE"));
        assertEquals(FieldType.BOOLEAN, FieldType.valueOf("BOOLEAN"));
        assertEquals(FieldType.DENSE_VECTOR, FieldType.valueOf("DENSE_VECTOR"));
    }

    @Test
    void testSourceCreation() {
        Source source = new Source();
        source.setSourceId("source-001");
        source.setSourceType(SourceType.MYSQL);
        source.setConnection("jdbc:mysql://localhost:3306/mydb");

        Map<String, String> props = new HashMap<>();
        props.put("key", "value");
        props.put("timeout", "30000");
        source.setProperties(props);

        assertEquals("source-001", source.getSourceId());
        assertEquals(SourceType.MYSQL, source.getSourceType());
        assertEquals("jdbc:mysql://localhost:3306/mydb", source.getConnection());
        assertEquals("value", source.getProperties().get("key"));
        assertEquals("30000", source.getProperties().get("timeout"));
        assertEquals(2, source.getProperties().size());
    }

    @Test
    void testSourceTypeEnum() {
        assertEquals(4, SourceType.values().length);

        assertEquals(SourceType.MYSQL, SourceType.valueOf("MYSQL"));
        assertEquals(SourceType.POSTGRESQL, SourceType.valueOf("POSTGRESQL"));
        assertEquals(SourceType.ORACLE, SourceType.valueOf("ORACLE"));
        assertEquals(SourceType.FILE, SourceType.valueOf("FILE"));
    }

    @Test
    void testSearchObjectCreation() {
        SearchObject searchObject = new SearchObject();
        searchObject.setObjectId("obj-001");
        searchObject.setSourceId("source-001");
        searchObject.setTable("products");
        searchObject.setPrimaryKey("id");
        searchObject.setAppKey("app-123");

        FieldConfig field1 = new FieldConfig();
        field1.setName("id");
        field1.setType(FieldType.LONG);

        FieldConfig field2 = new FieldConfig();
        field2.setName("name");
        field2.setType(FieldType.TEXT);
        field2.setSearchable(true);

        searchObject.setFields(List.of(field1, field2));

        assertEquals("obj-001", searchObject.getObjectId());
        assertEquals("source-001", searchObject.getSourceId());
        assertEquals("products", searchObject.getTable());
        assertEquals("id", searchObject.getPrimaryKey());
        assertEquals("app-123", searchObject.getAppKey());
        assertEquals(2, searchObject.getFields().size());
        assertEquals("id", searchObject.getFields().get(0).getName());
        assertEquals("name", searchObject.getFields().get(1).getName());
    }
}
