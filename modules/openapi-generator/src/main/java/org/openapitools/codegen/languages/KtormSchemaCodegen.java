/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

import static org.openapitools.codegen.utils.StringUtils.underscore;

// This code was almost entirely based on MySqlSchemaCodegen.

@SuppressWarnings("unchecked")
public class KtormSchemaCodegen extends AbstractKotlinCodegen {
    static Logger LOGGER = LoggerFactory.getLogger(KtormSchemaCodegen.class);

    public static final String VENDOR_EXTENSION_SCHEMA = "x-ktorm-schema";
    public static final String DEFAULT_DATABASE_NAME = "defaultDatabaseName";
    public static final String IDENTIFIER_NAMING_CONVENTION = "identifierNamingConvention";
    public static final Integer IDENTIFIER_MAX_LENGTH = 255;

    protected String defaultDatabaseName = "sqlite.db";
    protected String databaseNamePrefix = "_", databaseNameSuffix = "";
    protected String tableNamePrefix = "_", tableNameSuffix = "";
    protected String columnNamePrefix = "_", columnNameSuffix = "";
    protected String identifierNamingConvention = "original";
    
    // https://ktorm.liuwj.me/api-docs/me.liuwj.ktorm.schema/index.html
    protected class SqlType {
        protected static final String Blob = "blob";
        protected static final String Boolean = "boolean";
        protected static final String Bytes = "bytes";
        protected static final String Date = "date";
        protected static final String Decimal = "decimal";
        protected static final String Double = "double";
        protected static final String Float = "float";
        protected static final String Enum = "enum";
        protected static final String Int = "int";
        protected static final String Long = "long";
        protected static final String Text = "text";
        protected static final String Varchar = "varchar";
        protected static final String Json = "json";
    }

    public KtormSchemaCodegen() {
        super();

        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .wireFormatFeatures(EnumSet.noneOf(WireFormatFeature.class))
                .securityFeatures(EnumSet.noneOf(SecurityFeature.class))
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling
                )
                .excludeSchemaSupportFeatures(
                        SchemaSupportFeature.Polymorphism
                )
                .clientModificationFeatures(EnumSet.noneOf(ClientModificationFeature.class))
        );

        // http://www.sqlite.org/draft/tokenreq.html
        // https://sqlite.org/src/file/src/parse.y
        setReservedWordsLowerCase(
                Arrays.asList(
                        // SQL reserved words
                        "ABORT", "ACTION", "ADD", "AFTER", "ALL", "ALTER", "ALWAYS",
                        "ANALYZE", "AND", "ANY", "AS", "ASC", "ATTACH", "AUTOINCR", 
                        "AUTOINCREMENT", "BEFORE", "BEGIN", "BETWEEN", "BITAND", "BITNOT", 
                        "BITOR", "BLOB", "BY", "CASCADE", "CASE", "CAST", "CHECK", 
                        "COLLATE", "COLUMN", "COMMA", "COMMIT", "CONCAT", "CONFLICT", 
                        "CONSTRAINT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE", 
                        "CURRENT_TIME", "CURRENT_TIMESTAMP", "DATABASE", "DEFAULT", 
                        "DEFERRABLE", "DEFERRED", "DELETE", "DESC", "DETACH", "DISTINCT", 
                        "DO", "DOT", "DROP", "EACH", "ELSE", "END", "EQ", "ESCAPE", 
                        "EXCEPT", "EXCLUDE", "EXCLUSIVE", "EXISTS", "EXPLAIN", "FAIL", 
                        "FILTER", "FIRST", "FLOAT", "FOLLOWING", "FOR", "FOREIGN", "FROM", 
                        "FULL", "GE", "GENERATED", "GLOB", "GROUP", "GROUPS", "GT", 
                        "HAVING", "ID", "IF", "IGNORE", "IMMEDIATE", "IN", "INDEX", 
                        "INDEXED", "INITIALLY", "INNER", "INSERT", "INSTEAD", "INTEGER", 
                        "INTERSECT", "INTO", "IS", "ISNULL", "JOIN", "KEY", "LAST", "LE", 
                        "LEFT", "LIKE", "LIMIT", "LP", "LSHIFT", "LT", "MATCH", "MINUS",
                        "NATURAL", "NE", "NO", "NOT", "NOTHING", "NOTNULL", "NULL", "NULLS", 
                        "OF", "OFFSET", "ON", "OR", "ORDER", "OTHERS", "OUTER", "OVER", 
                        "PARTITION", "PLAN", "PLUS", "PRAGMA", "PRECEDING", "PRIMARY", 
                        "QUERY", "RAISE", "RANGE", "RECURSIVE", "REFERENCES", "REGEXP", 
                        "REINDEX", "RELEASE", "REM", "RENAME", "REPLACE", "RESTRICT", 
                        "RIGHT", "ROLLBACK", "ROW", "ROWS", "RP", "RSHIFT", "SAVEPOINT", 
                        "SELECT", "SET", "SLASH", "STAR", "STRING", "TABLE", "TEMP", 
                        "TEMPORARY", "THEN", "TIES", "TO", "TRANSACTION", "TRIGGER", 
                        "UNBOUNDED", "UNION", "UNIQUE", "UPDATE", "USING", "VACUUM", 
                        "VALUES", "VARIABLE", "VIEW", "VIRTUAL", "WHEN", "WHERE", 
                        "WINDOW", "WITH", "WITHOUT"
                )
        );

        typeMapping = new HashMap<String, String>();
        typeMapping.put("array", SqlType.Blob);
        typeMapping.put("set", SqlType.Blob);
        typeMapping.put("map", SqlType.Blob);
        typeMapping.put("List", SqlType.Blob);
        typeMapping.put("boolean", SqlType.Boolean);
        typeMapping.put("string", SqlType.Text);
        typeMapping.put("int", SqlType.Int);
        typeMapping.put("byte", SqlType.Int);
        typeMapping.put("float", SqlType.Float);
        typeMapping.put("number", SqlType.Double);
        typeMapping.put("date", SqlType.Date);
        typeMapping.put("time", SqlType.Date);
        typeMapping.put("Date", SqlType.Date);
        typeMapping.put("Time", SqlType.Date);
        typeMapping.put("datetime", SqlType.Date);
        typeMapping.put("DateTime", SqlType.Date);
        typeMapping.put("long", SqlType.Int);
        typeMapping.put("short", SqlType.Int);
        typeMapping.put("char", SqlType.Text);
        typeMapping.put("double", SqlType.Double);
        typeMapping.put("real", SqlType.Double);
        typeMapping.put("object", SqlType.Blob);
        typeMapping.put("integer", SqlType.Int);
        typeMapping.put("ByteArray", SqlType.Blob);
        typeMapping.put("binary", SqlType.Blob);
        typeMapping.put("file", SqlType.Blob);
        typeMapping.put("UUID", SqlType.Text);
        typeMapping.put("URI", SqlType.Text);
        typeMapping.put("decimal", SqlType.Decimal);
        typeMapping.put("BigDecimal", SqlType.Decimal);
        typeMapping.put("AnyType", SqlType.Blob);

        artifactId = "ktorm";
        artifactVersion = "1.0.0";
        packageName = "org.openapitools.database";

        outputFolder = "generated-code" + File.separator + "kotlin-client";
        embeddedTemplateDir = templateDir = "ktorm-schema";
        modelTemplateFiles.put("model.mustache", ".kt");
        modelDocTemplateFiles.put("model_doc.mustache", ".md");
        modelPackage = packageName + ".models";

        // cliOptions default redefinition need to be updated
        updateOption(CodegenConstants.ARTIFACT_ID, artifactId);
        updateOption(CodegenConstants.PACKAGE_NAME, packageName);

        // we don't clear clioptions from Kotlin
        // cliOptions.clear();
        addOption(DEFAULT_DATABASE_NAME, "Default database name for all queries", defaultDatabaseName);

        // we used to snake_case table/column names, let's add this option
        CliOption identifierNamingOpt = new CliOption(IDENTIFIER_NAMING_CONVENTION,
                "Naming convention of Ktorm identifiers(table names and column names). This is not related to database name which is defined by " + DEFAULT_DATABASE_NAME + " option");

        identifierNamingOpt.addEnum("original", "Do not transform original names")
                .addEnum("snake_case", "Use snake_case names")
                .setDefault("original");

        cliOptions.add(identifierNamingOpt);
        
    }

    public CodegenType getTag() {
        return CodegenType.SCHEMA;
    }

    public String getName() {
        return "ktorm";
    }

    public String getHelp() {
        return "Generates a kotlin-ktorm schema.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(DEFAULT_DATABASE_NAME)) {
            if (additionalProperties.get(DEFAULT_DATABASE_NAME).equals("")) {
                additionalProperties.remove(DEFAULT_DATABASE_NAME);
            } else {
                setDefaultDatabaseName((String) additionalProperties.get(DEFAULT_DATABASE_NAME));
                // default database name may be escaped, need to overwrite additional prop
                additionalProperties.put(DEFAULT_DATABASE_NAME, getDefaultDatabaseName());
            }
        }

        if (additionalProperties.containsKey(IDENTIFIER_NAMING_CONVENTION)) {
            setIdentifierNamingConvention((String) additionalProperties.get(IDENTIFIER_NAMING_CONVENTION));
        }

        // make model src path available in mustache template
        additionalProperties.put("modelSrcPath", "./" + toSrcPath(modelPackage));

        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
        supportingFiles.add(new SupportingFile("ktorm_schema.mustache", "", "ktorm_schema.sql"));
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        objs = super.postProcessModels(objs);

        List<Object> models = (List<Object>) objs.get("models");
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            CodegenModel model = (CodegenModel) mo.get("model");
            String modelName = model.getName();
            String tableName = toTableName(modelName);
            String modelDescription = model.getDescription();
            Map<String, Object> modelVendorExtensions = model.getVendorExtensions();
            Map<String, Object> ktormSchema = new HashMap<String, Object>();
            Map<String, Object> tableDefinition = new HashMap<String, Object>();

            if (getIdentifierNamingConvention().equals("snake_case") && !modelName.equals(tableName)) {
                // add original name in table comment
                String commentExtra = "Original model name - " + modelName + ".";
                modelDescription = (modelDescription == null || modelDescription.isEmpty()) ? commentExtra : modelDescription + ". " + commentExtra;
            }

            if (modelVendorExtensions.containsKey(VENDOR_EXTENSION_SCHEMA)) {
                // user already specified schema values
                LOGGER.info("Found vendor extension in '" + modelName + "' model, autogeneration skipped");
            } else {
                modelVendorExtensions.put(VENDOR_EXTENSION_SCHEMA, ktormSchema);
                ktormSchema.put("tableDefinition", tableDefinition);
                tableDefinition.put("tblName", tableName);
                tableDefinition.put("tblComment", modelDescription);
            }
        }

        return objs;
    }

    private class KtormSchema extends HashMap<String, Object> {
        private static final long serialVersionUID = -9159755928980443880L;
    }

    /**
     * Processes each model's property mapped
     *
     * @param model    codegen model
     * @param property model's property
     */
    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        Map<String, Object> vendorExtensions = property.getVendorExtensions();
        KtormSchema ktormSchema = new KtormSchema();
        String baseName = property.getBaseName();
        String colName = toColumnName(baseName);
        String description = property.getDescription();

        if (vendorExtensions.containsKey(VENDOR_EXTENSION_SCHEMA)) {
            // user already specified schema values
            LOGGER.info("Found vendor extension in '" + baseName + "' property, autogeneration skipped");
            return;
        }

        vendorExtensions.put(VENDOR_EXTENSION_SCHEMA, ktormSchema);
        
        if (getIdentifierNamingConvention().equals("snake_case") && !baseName.equals(colName)) {
            // add original name in column comment
            String commentExtra = "Original param name - " + baseName + ".";
            description = (description == null || description.isEmpty()) ? commentExtra : description + ". " + commentExtra;
        }

        switch (property.getDataType().toLowerCase(Locale.ROOT)) {
            case SqlType.Boolean:
                processBooleanTypeProperty(model, property, description, ktormSchema);
                break;
            case SqlType.Int:
            case SqlType.Long:
                processIntegerTypeProperty(model, property, description, ktormSchema);
                break;
            case SqlType.Float:
            case SqlType.Double:
            case SqlType.Decimal:
                processRealTypeProperty(model, property, description, ktormSchema);
                break;
            case SqlType.Blob:
            case SqlType.Text:
            case SqlType.Varchar:
            case SqlType.Bytes:
                processStringTypeProperty(model, property, description, ktormSchema);
                break;
            case SqlType.Date:
                processDateTypeProperty(model, property, description, ktormSchema);
                break;
            case SqlType.Json:
                processJsonTypeProperty(model, property, description, ktormSchema);
                break;
            default:
                processUnknownTypeProperty(model, property, description, ktormSchema);
        }
    }

    /**
     * Processes each model's property mapped to integer type and adds related vendor extensions
     *
     * @param model       codegen model
     * @param property    model's property
     * @param description property custom description
     * @param ktormSchema schema
     */
    public void processIntegerTypeProperty(CodegenModel model, CodegenProperty property, String description, KtormSchema ktormSchema) {
        Map<String, Object> columnDefinition = new HashMap<String, Object>();
        String baseName = property.getBaseName();
        String colName = toColumnName(baseName);
        String dataType = property.getDataType();
        String dataFormat = property.getDataFormat();
        String actualType = toColumnType(dataType, dataFormat);

        String minimum = property.getMinimum();
        String maximum = property.getMaximum();
        boolean exclusiveMinimum = property.getExclusiveMinimum();
        boolean exclusiveMaximum = property.getIExclusiveMaximum();
        boolean unsigned = false;
        Boolean isUuid = property.isUuid;
        
        Long cmin = (minimum != null) ? Long.parseLong(minimum) : null;
        Long cmax = (maximum != null) ? Long.parseLong(maximum) : null;
        if (exclusiveMinimum && cmin != null) cmin += 1;
        if (exclusiveMaximum && cmax != null) cmax -= 1;
        if (cmin != null && cmin >= 0) {
            unsigned = true;
        }
        long min = (cmin != null) ? cmin : Long.MIN_VALUE;
        long max = (cmax != null) ? cmax : Long.MAX_VALUE;
        long actualMin = Math.min(min, max); // sometimes min and max values can be mixed up
        long actualMax = Math.max(min, max); // sometimes only minimum specified and it can be pretty high

        ktormSchema.put("columnDefinition", columnDefinition);
        columnDefinition.put("colName", colName);
        columnDefinition.put("colType", dataType);
        columnDefinition.put("colSqlType", actualType);
        columnDefinition.put("colUnsigned", unsigned);
        columnDefinition.put("colMinimum", actualMin);
        columnDefinition.put("colMaximum", actualMax);
        columnDefinition.put("colIsUuid", isUuid);

        processTypeArgs(dataType, dataFormat, actualMin, actualMax, columnDefinition);
        processNullAndDefault(model, property, description, columnDefinition);
    }

    /**
     * Processes each model's property mapped to some real type and adds related vendor extensions
     *
     * @param model       codegen model
     * @param property    model's property
     * @param description property custom description
     * @param ktormSchema schema
     */
    public void processRealTypeProperty(CodegenModel model, CodegenProperty property, String description, KtormSchema ktormSchema) {
        Map<String, Object> columnDefinition = new HashMap<String, Object>();
        String baseName = property.getBaseName();
        String colName = toColumnName(baseName);
        String dataType = property.getDataType();
        String dataFormat = property.getDataFormat();
        String actualType = toColumnType(dataType, dataFormat);

        String minimum = property.getMinimum();
        String maximum = property.getMaximum();
        boolean exclusiveMinimum = property.getExclusiveMinimum();
        boolean exclusiveMaximum = property.getIExclusiveMaximum();
        
        Float cmin = (minimum != null) ? Float.parseFloat(minimum) : null;
        Float cmax = (maximum != null) ? Float.parseFloat(maximum) : null;
        if (exclusiveMinimum && cmin != null) cmin += 1;
        if (exclusiveMaximum && cmax != null) cmax -= 1;
        Float min = (cmin != null) ? cmin : Float.MIN_VALUE;
        Float max = (cmax != null) ? cmax : Float.MAX_VALUE;
        Float actualMin = Math.min(min, max); // sometimes min and max values can be mixed up
        Float actualMax = Math.max(min, max); // sometimes only minimum specified and it can be pretty high
        
        ktormSchema.put("columnDefinition", columnDefinition);
        columnDefinition.put("colName", colName);
        columnDefinition.put("colType", dataType);
        columnDefinition.put("colSqlType", actualType);
        columnDefinition.put("colMinimum", actualMin);
        columnDefinition.put("colMaximum", actualMax);

        processTypeArgs(dataType, dataFormat, actualMin, actualMax, columnDefinition);
        processNullAndDefault(model, property, description, columnDefinition);
    }

    /**
     * Processes each model's property mapped to boolean type and adds related vendor extensions
     *
     * @param model       codegen model
     * @param property    model's property
     * @param description property custom description
     * @param ktormSchema schema
     */
    public void processBooleanTypeProperty(CodegenModel model, CodegenProperty property, String description, KtormSchema ktormSchema) {
        Map<String, Object> columnDefinition = new HashMap<String, Object>();
        String baseName = property.getBaseName();
        String colName = toColumnName(baseName);
        String dataType = property.getDataType();
        String dataFormat = property.getDataFormat();
        String actualType = toColumnType(dataType, dataFormat);

        ktormSchema.put("columnDefinition", columnDefinition);
        columnDefinition.put("colName", colName);
        columnDefinition.put("colType", dataType);
        columnDefinition.put("colSqlType", actualType);

        processTypeArgs(dataType, dataFormat, 0, 1, columnDefinition);
        processNullAndDefault(model, property, description, columnDefinition);
    }

    /**
     * Processes each model's property mapped to string type and adds related vendor extensions
     *
     * @param model       codegen model
     * @param property    model's property
     * @param description property custom description
     * @param ktormSchema schema
     */
    public void processStringTypeProperty(CodegenModel model, CodegenProperty property, String description, KtormSchema ktormSchema) {
        Map<String, Object> columnDefinition = new HashMap<String, Object>();
        String baseName = property.getBaseName();
        String colName = toColumnName(baseName);
        String dataType = property.getDataType();
        String dataFormat = property.getDataFormat();
        String actualType = toColumnType(dataType, dataFormat);

        Integer minLength = property.getMinLength();
        Integer maxLength = property.getMaxLength();
        
        Integer min = (minLength != null) ? minLength : 0;
        Integer max = (maxLength != null) ? maxLength : Integer.MAX_VALUE;
        int actualMin = Math.min(min, max); // sometimes min and max values can be mixed up
        int actualMax = Math.max(min, max); // sometimes only minimum specified and it can be pretty high

        ktormSchema.put("columnDefinition", columnDefinition);
        columnDefinition.put("colName", colName);
        columnDefinition.put("colType", dataType);
        columnDefinition.put("colSqlType", actualType);
        if (actualMin != 0) {
            columnDefinition.put("colMinimum", actualMin);
        }
        if (actualMax != Integer.MAX_VALUE) {
            columnDefinition.put("colMaximum", actualMax);
        }

        processTypeArgs(dataType, dataFormat, actualMin, actualMax, columnDefinition);
        processNullAndDefault(model, property, description, columnDefinition);
    }

    /**
     * Processes each model's property mapped to date type and adds related vendor extensions
     *
     * @param model       codegen model
     * @param property    model's property
     * @param description property custom description
     * @param ktormSchema schema
     */
    public void processDateTypeProperty(CodegenModel model, CodegenProperty property, String description, KtormSchema ktormSchema) {
        Map<String, Object> columnDefinition = new HashMap<String, Object>();
        String baseName = property.getBaseName();
        String colName = toColumnName(baseName);
        String dataType = property.getDataType();
        String dataFormat = property.getDataFormat();
        String actualType = toColumnType(dataType, dataFormat);

        ktormSchema.put("columnDefinition", columnDefinition);
        columnDefinition.put("colName", colName);
        columnDefinition.put("colType", dataType);
        columnDefinition.put("colSqlType", actualType);

        processTypeArgs(dataType, dataFormat, null, null, columnDefinition);
        processNullAndDefault(model, property, description, columnDefinition);
    }

    /**
     * Processes each model's property mapped to JSON type and adds related vendor extensions
     *
     * @param model       codegen model
     * @param property    model's property
     * @param description property custom description
     * @param ktormSchema schema
     */
    public void processJsonTypeProperty(CodegenModel model, CodegenProperty property, String description, KtormSchema ktormSchema) {
        Map<String, Object> columnDefinition = new HashMap<String, Object>();
        String baseName = property.getBaseName();
        String colName = toColumnName(baseName);
        String dataType = property.getDataType();
        String dataFormat = property.getDataFormat();
        String actualType = toColumnType(dataType, dataFormat);

        ktormSchema.put("columnDefinition", columnDefinition);
        columnDefinition.put("colName", colName);
        columnDefinition.put("colType", dataType);
        columnDefinition.put("colSqlType", actualType);

        processTypeArgs(dataType, dataFormat, null, null, columnDefinition);
        processNullAndDefault(model, property, description, columnDefinition);
    }

    /**
     * Processes each model's property not mapped to any type and adds related
     * vendor extensions Most of time it's related to referenced properties eg.
     * \Model\User
     *
     * @param model       codegen model
     * @param property    model's property
     * @param description property custom description
     * @param ktormSchema schema
     */
    public void processUnknownTypeProperty(CodegenModel model, CodegenProperty property, String description, KtormSchema ktormSchema) {
        Map<String, Object> columnDefinition = new HashMap<String, Object>();
        String baseName = property.getBaseName();
        String colName = toColumnName(baseName);
        String dataType = property.getDataType();
        String dataFormat = property.getDataFormat();
        String actualType = toColumnType(dataType, dataFormat);

        ktormSchema.put("columnDefinition", columnDefinition);
        columnDefinition.put("colName", colName);
        columnDefinition.put("colType", dataType);
        columnDefinition.put("colSqlType", actualType);

        processTypeArgs(dataType, dataFormat, null, null, columnDefinition);
        processNullAndDefault(model, property, description, columnDefinition);
    }

    /**
     * Processes each model's property type arguments definitions
     *
     * @param dataType   the choosen sql type
     * @param dataFormat the choosen sql format
     * @param min        the minimum value, if specified, in the target type
     * @param max        the maximum value, if specified, in the target type
     * @param columnDefinition resulting column definition dictionary
     */
    public void processTypeArgs(String dataType, String dataFormat, Object min, Object max, Map<String, Object> columnDefinition) {
        HashMap<String, Object> a = new HashMap<String, Object>();
        SqlTypeArgs args = new SqlTypeArgs();
        toColumnTypeArgs(dataType, dataFormat, min, max, args);
		a.put("isPrimitive", args.isPrimitive);
		a.put("isNumeric", args.isNumeric);
        a.put("isBoolean", args.isBoolean);
        a.put("isInteger", args.isInteger);
        a.put("isFloat", args.isFloat);
        a.put("isDecimal", args.isDecimal);
        a.put("isString", args.isString);
        a.put("isDate", args.isDate);
        a.put("isBlob", args.isBlob);
        a.put("isJson", args.isJson);
        a.put("isNull", args.isNull);
        columnDefinition.put("colSqlTypeArgs", a);
    }

    /**
     * Processes each model's property null/default definitions
     *
     * @param model       model's name
     * @param property    model's property
     * @param description property's customized description
     * @param columnDefinition resulting column definition dictionary
     */
    public void processNullAndDefault(CodegenModel model, CodegenProperty property, String description, Map<String, Object> columnDefinition) {
        String baseName = property.getBaseName();
        Boolean required = property.getRequired();
        String dataType = (String) columnDefinition.get("colDataType");
        String defaultValue = property.getDefaultValue();
        if (Boolean.TRUE.equals(required)) {
            columnDefinition.put("colNotNull", true);
        } else {
            columnDefinition.put("colNotNull", false);
            try {
                columnDefinition.put("colDefault", toColumnTypeDefault(defaultValue, dataType));
            } catch (RuntimeException exception) {
                LOGGER.warn("Property '" + baseName + "' of model '" + model.getName() + "' mapped to data type which doesn't support default value");
                columnDefinition.put("colDefault", null);
            }
        }
        if (description != null) {
            columnDefinition.put("colComment", description);
        }        
    }

    private class SqlTypeArgs {
		public boolean isPrimitive;
		public boolean isNumeric;
        public boolean isBoolean;
        public boolean isInteger;
        public boolean isFloat;
        public boolean isDecimal;
        public boolean isString;
        public boolean isDate;
        public boolean isBlob;
        public boolean isJson;
        public boolean isNull;
    }

    /**
     * Generates codegen type mapping between ktor and sqlite
     * Ref: http://www.sqlite.org/draft/datatype3.html
     *
     * @param dataType   type name
     * @param dataFormat type format
     * @return generated codegen type
     */    
    private String toColumnType(String dataType, String dataFormat) {
        switch (dataType.toLowerCase(Locale.ROOT)) {
            case SqlType.Boolean:
            case SqlType.Int:
            case SqlType.Long:
                return "INTEGER";
            case SqlType.Float:
            case SqlType.Double:
            case SqlType.Decimal:
                return "REAL";
            case SqlType.Text:
            case SqlType.Varchar:
            case SqlType.Date:
                return "TEXT";
            case SqlType.Blob:
            case SqlType.Json:
                return "BLOB";
            default:
                return "BLOB";
        }
    }

    /**
     * Generates codegen type argument mapping between ktor and sqlite
     * Ref: http://www.sqlite.org/draft/datatype3.html
     *
     * @param dataType type name
     * @param dataFormat type format
     * @return generated codegen type
     */    
    private void toColumnTypeArgs(String dataType, String dataFormat, Object min, Object max, SqlTypeArgs args) {
        String sType = dataType.toLowerCase(Locale.ROOT);
        switch (sType) {
            case SqlType.Boolean:
            case SqlType.Int:
            case SqlType.Long:
            case SqlType.Float:
            case SqlType.Double:
            case SqlType.Decimal:
            case SqlType.Text:
            case SqlType.Varchar:
            case SqlType.Date:
                args.isPrimitive = true;
                break;
            case SqlType.Blob:
            case SqlType.Json:
            default:
        }
        switch (sType) {
            case SqlType.Boolean:
            case SqlType.Int:
            case SqlType.Long:
            case SqlType.Float:
            case SqlType.Double:
            case SqlType.Decimal:
                args.isNumeric = true;
                break;
            case SqlType.Text:
            case SqlType.Varchar:
            case SqlType.Date:
            case SqlType.Blob:
            case SqlType.Json:
            default:
        }        
        switch (sType) {
            case SqlType.Boolean:
                args.isBoolean = true;
                break;
            case SqlType.Int:
            case SqlType.Long:
                args.isInteger = true;
                break;
            case SqlType.Float:
            case SqlType.Double:
                args.isFloat = true;
                break;
            case SqlType.Decimal:
                args.isDecimal = true;
                break;
            case SqlType.Text:
            case SqlType.Varchar:
                args.isString = true;
                break;
            case SqlType.Date:
                args.isDate = true;
                break;
            case SqlType.Blob:
                args.isBlob = true;
                break;
            case SqlType.Json:
                args.isJson = true;
                break;
            default:
                args.isNull = true;
                break;
        }
    }

    /**
     * Generates codegen default value mapping between ktor and sqlite
     * Ref: http://www.sqlite.org/draft/lang_createtable.html, sec3.2
     *
     * @param defaultValue  value
     * @param dataType data type
     * @return generated codegen default
     */
    public HashMap<String, Object> toColumnTypeDefault(String defaultValue, String dataType) {
        String sqlType = dataType.toLowerCase(Locale.ROOT);
        String sqlDefault = "";
        if (defaultValue == null || defaultValue.toUpperCase(Locale.ROOT).equals("NULL")) {
            sqlType = "null";
        }        
        //special case for keywords if needed
        switch (sqlType) {
            case SqlType.Boolean:
            case SqlType.Int:
            case SqlType.Long:
            case SqlType.Float:
            case SqlType.Double:
            case SqlType.Decimal:
            case SqlType.Text:
            case SqlType.Varchar:
            case SqlType.Date:
                sqlDefault = defaultValue;
            case SqlType.Blob:
            case SqlType.Json:
                throw new RuntimeException("The BLOB and JSON data types cannot be assigned a default value");
            default:
                sqlDefault = "NULL";
        }
        Map<String, Object> outp = new HashMap<String, Object>();
        processTypeArgs(sqlType, null, null, null, outp);
        HashMap<String, Object> args = (HashMap<String, Object>) outp.get("colSqlTypeArgs");
        args.put("argumentValue", sqlDefault);
        args.put("hasMore", false);
        return args;
    }

    /**
     * Converts name to valid database name
     * Produced name must be used with backticks only, eg. `database_name`
     *
     * @param name source name
     * @return database name
     */
    public String toDatabaseName(String name) {
        String identifier = toIdentifier(name, databaseNamePrefix, databaseNameSuffix);
        if (identifier.length() > IDENTIFIER_MAX_LENGTH) {
            LOGGER.warn("Database name too long. Name '" + name + "' will be truncated");
            identifier = identifier.substring(0, IDENTIFIER_MAX_LENGTH);
        }
        return identifier;
    }

    /**
     * Converts name to valid column name
     * Produced name must be used with backticks only, eg. `table_name`
     *
     * @param name source name
     * @return table name
     */
    public String toTableName(String name) {
        String identifier = toIdentifier(name, tableNamePrefix, tableNameSuffix);
        if (identifierNamingConvention.equals("snake_case")) {
            identifier = underscore(identifier);
        }
        if (identifier.length() > IDENTIFIER_MAX_LENGTH) {
            LOGGER.warn("Table name too long. Name '" + name + "' will be truncated");
            identifier = identifier.substring(0, IDENTIFIER_MAX_LENGTH);
        }
        return identifier;
    }

    /**
     * Converts name to valid column name
     * Produced name must be used with backticks only, eg. `column_name`
     *
     * @param name source name
     * @return column name
     */
    public String toColumnName(String name) {
        String identifier = toIdentifier(name, columnNamePrefix, columnNameSuffix);
        if (identifierNamingConvention.equals("snake_case")) {
            identifier = underscore(identifier);
        }
        if (identifier.length() > IDENTIFIER_MAX_LENGTH) {
            LOGGER.warn("Column name too long. Name '" + name + "' will be truncated");
            identifier = identifier.substring(0, IDENTIFIER_MAX_LENGTH);
        }
        return identifier;
    }

    /**
     * Converts name to valid identifier which can be used as database, table, column name
     * Produced name must be used quoted only, eg. "column_name"
     *
     * @param name   source name
     * @param prefix when escaped name is digits only, prefix will be prepended
     * @param suffix when escaped name is digits only, suffix will be appended
     * @return identifier name
     */
    public String toIdentifier(String name, String prefix, String suffix) {
        String escapedName = escapeQuotedIdentifier(name);
        // Database, table, and column names cannot end with space characters.
        if (escapedName.matches(".*\\s$")) {
            LOGGER.warn("Database, table, and column names cannot end with space characters. Check '" + name + "' name");
            escapedName = escapedName.replaceAll("\\s+$", "");
        }

        // Identifiers may begin with a digit but unless quoted may not consist solely of digits.
        if (escapedName.matches("^\\d+$")) {
            LOGGER.warn("Database, table, and column names cannot consist solely of digits. Check '" + name + "' name");
            escapedName = prefix + escapedName + suffix;
        }

        // identifier name cannot be empty
        if (escapedName.isEmpty()) {
            throw new RuntimeException("Empty database/table/column name for property '" + name.toString() + "' not allowed");
        }
        return escapedName;
    }

    /**
     * Escapes identifier to use it in SQL statements with backticks, eg. SELECT "identifier" FROM
     * Ref: https://www.sqlite.org/draft/tokenreq.html H41130 
     * Spec is similar to MySQL
     *
     * @param identifier source identifier
     * @return escaped identifier
     */
    public String escapeQuotedIdentifier(String identifier) {
        // ASCII: [0-9,a-z,A-Z$_] (basic Latin letters, digits 0-9, dollar, underscore) Extended: U+0080 .. U+FFFF
        // ASCII NUL (U+0000) and supplementary characters (U+10000 and higher) are not permitted in quoted or unquoted identifiers.
        // This does in fact matches against >\xFFFF and against ^\x0000. works only on Java7+
        Pattern regexp = Pattern.compile("[^0-9a-zA-z$_\\x0080-\\xFFFF]");
        Matcher matcher = regexp.matcher(identifier);
        if (matcher.find()) {
            LOGGER.warn("Identifier '" + identifier + "' contains unsafe characters out of [0-9,a-z,A-Z$_] and U+0080..U+FFFF range");
            identifier = identifier.replaceAll("[^0-9a-zA-z$_\\x0080-\\xFFFF]", "");
        }
        return identifier;
    }

    @Override
    public String escapeReservedWord(String name) {
        LOGGER.warn("'" + name + "' is reserved word. Do not use that word or properly escape it with backticks in mustache template");
        return name;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove ' to avoid code injection
        return input.replace("'", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    /**
     * Sets default database name for all queries
     * Provided value will be escaped when necessary
     *
     * @param databaseName source name
     */
    public void setDefaultDatabaseName(String databaseName) {
        String escapedName = toDatabaseName(databaseName);
        if (!escapedName.equals(databaseName)) {
            LOGGER.error("Invalid database name. '" + databaseName + "' cannot be used as identifier. Escaped value '" + escapedName + "' will be used instead.");
        }
        this.defaultDatabaseName = escapedName;
    }

    /**
     * Returns default database name for all queries
     * This value must be used with backticks only, eg. `database_name`
     *
     * @return default database name
     */
    public String getDefaultDatabaseName() {
        return this.defaultDatabaseName;
    }

    /**
     * Sets identifier naming convention for table names and column names.
     * This is not related to database name which is defined by defaultDatabaseName option.
     *
     * @param naming identifier naming convention (original|snake_case)
     */
    public void setIdentifierNamingConvention(String naming) {
        switch (naming) {
            case "original":
            case "snake_case":
                this.identifierNamingConvention = naming;
                break;
            default:
                LOGGER.warn("\"" + (String) naming + "\" is invalid \"identifierNamingConvention\" argument. Current \"" + (String) this.identifierNamingConvention + "\" used instead.");
        }
    }

    /**
     * Returns identifier naming convention for table names and column names.
     *
     * @return identifier naming convention
     */
    public String getIdentifierNamingConvention() {
        return this.identifierNamingConvention;
    }

    /**
     * Slightly modified version of AbstractPhpCodegen.toSrcPath method.
     *
     * @param packageName package name
     *
     * @return path
     */
    public String toSrcPath(String packageName) {
        // Trim prefix file separators from package path
        String packagePath = StringUtils.removeStart(
            // Replace period, backslash, forward slash with file separator in package name
            packageName.replaceAll("[\\.\\\\/]", Matcher.quoteReplacement("/")),
            File.separator
        );

        // Trim trailing file separators from the overall path
        return StringUtils.removeEnd(packagePath, File.separator);
    }

}
