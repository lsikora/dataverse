package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileCategory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import org.junit.Before;
import org.junit.Test;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class JsonPrinterTest {

    // Centralize JsonParserTest.MockDatasetFieldSvc? See also https://github.com/IQSS/dataverse/issues/3413 and https://github.com/IQSS/dataverse/issues/3777
    JsonParserTest.MockDatasetFieldSvc datasetFieldTypeSvc = null;

    @Before
    public void setUp() {
        datasetFieldTypeSvc = new JsonParserTest.MockDatasetFieldSvc();

        DatasetFieldType titleType = datasetFieldTypeSvc.add(new DatasetFieldType("title", FieldType.TEXTBOX, false));
        DatasetFieldType authorType = datasetFieldTypeSvc.add(new DatasetFieldType("author", FieldType.TEXT, true));
        Set<DatasetFieldType> authorChildTypes = new HashSet<>();
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorName", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorAffiliation", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifier", FieldType.TEXT, false)));
        DatasetFieldType authorIdentifierSchemeType = datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifierScheme", FieldType.TEXT, false));
        authorIdentifierSchemeType.setAllowControlledVocabulary(true);
        authorIdentifierSchemeType.setControlledVocabularyValues(Arrays.asList(
                // FIXME: Why aren't these enforced? Should be ORCID, etc.
                new ControlledVocabularyValue(1l, "foo", authorIdentifierSchemeType),
                new ControlledVocabularyValue(2l, "bar", authorIdentifierSchemeType),
                new ControlledVocabularyValue(3l, "baz", authorIdentifierSchemeType)
        ));
        authorChildTypes.add(datasetFieldTypeSvc.add(authorIdentifierSchemeType));
        for (DatasetFieldType t : authorChildTypes) {
            t.setParentDatasetFieldType(authorType);
        }
        authorType.setChildDatasetFieldTypes(authorChildTypes);

        DatasetFieldType datasetContactType = datasetFieldTypeSvc.add(new DatasetFieldType("datasetContact", FieldType.TEXT, true));
        Set<DatasetFieldType> datasetContactTypes = new HashSet<>();
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType(DatasetFieldConstant.datasetContactEmail, FieldType.EMAIL, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactName", FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactAffiliation", FieldType.TEXT, false)));
        for (DatasetFieldType t : datasetContactTypes) {
            t.setParentDatasetFieldType(datasetContactType);
        }
        datasetContactType.setChildDatasetFieldTypes(datasetContactTypes);

        DatasetFieldType keywordType = datasetFieldTypeSvc.add(new DatasetFieldType("keyword", FieldType.TEXT, true));
        DatasetFieldType descriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("description", FieldType.TEXTBOX, false));

        DatasetFieldType subjectType = datasetFieldTypeSvc.add(new DatasetFieldType("subject", FieldType.TEXT, true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues(Arrays.asList(
                new ControlledVocabularyValue(1l, "mgmt", subjectType),
                new ControlledVocabularyValue(2l, "law", subjectType),
                new ControlledVocabularyValue(3l, "cs", subjectType)
        ));

        DatasetFieldType pubIdType = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIdType", FieldType.TEXT, false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues(Arrays.asList(
                new ControlledVocabularyValue(1l, "ark", pubIdType),
                new ControlledVocabularyValue(2l, "doi", pubIdType),
                new ControlledVocabularyValue(3l, "url", pubIdType)
        ));

        DatasetFieldType compoundSingleType = datasetFieldTypeSvc.add(new DatasetFieldType("coordinate", FieldType.TEXT, true));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lat", FieldType.TEXT, false)));
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lon", FieldType.TEXT, false)));

        for (DatasetFieldType t : childTypes) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);
    }

    @Test
    public void testJson_RoleAssignment() {
        DataverseRole aRole = new DataverseRole();
        PrivateUrlUser privateUrlUserIn = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUserIn;
        Dataset dataset = new Dataset();
        dataset.setId(123l);
        String privateUrlToken = "e1d53cf6-794a-457a-9709-7c07629a8267";
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        JsonObjectBuilder job = JsonPrinter.json(ra);
        assertNotNull(job);
        JsonObject jsonObject = job.build();
        assertEquals("#42", jsonObject.getString("assignee"));
        assertEquals(123, jsonObject.getInt("definitionPointId"));
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("privateUrlToken"));
    }

    @Test
    public void testJson_PrivateUrl() {
        DataverseRole aRole = new DataverseRole();
        PrivateUrlUser privateUrlUserIn = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUserIn;
        Dataset dataset = new Dataset();
        String privateUrlToken = "e1d53cf6-794a-457a-9709-7c07629a8267";
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        String dataverseSiteUrl = "https://dataverse.example.edu";
        PrivateUrl privateUrl = new PrivateUrl(ra, dataset, dataverseSiteUrl);
        JsonObjectBuilder job = JsonPrinter.json(privateUrl);
        assertNotNull(job);
        JsonObject jsonObject = job.build();
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("token"));
        assertEquals("https://dataverse.example.edu/privateurl.xhtml?token=e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("link"));
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getJsonObject("roleAssignment").getString("privateUrlToken"));
        assertEquals("#42", jsonObject.getJsonObject("roleAssignment").getString("assignee"));
    }

    @Test
    public void testGetFileCategories() {
        FileMetadata fmd = MocksFactory.makeFileMetadata(10L, "", 0);
        DatasetVersion dsVersion = new DatasetVersion();
        DataFile dataFile = new DataFile();
        dataFile.setProtocol("doi");
        dataFile.setIdentifier("ABC123");
        dataFile.setAuthority("10.5072/FK2");
        List<DataFileTag> dataFileTags = new ArrayList<>();
        DataFileTag tag = new DataFileTag();
        tag.setTypeByLabel("Survey");
        dataFileTags.add(tag);
        dataFile.setTags(dataFileTags);
        fmd.setDatasetVersion(dsVersion);
        fmd.setDataFile(dataFile);
        List<DataFileCategory> fileCategories = new ArrayList<>();
        DataFileCategory dataFileCategory = new DataFileCategory();
        dataFileCategory.setName("Data");
        fileCategories.add(dataFileCategory);
        fmd.setCategories(fileCategories);
        JsonObjectBuilder job = JsonPrinter.json(fmd);
        assertNotNull(job);
        JsonObject jsonObject = job.build();
        System.out.println("json: " + jsonObject);
        assertEquals("", jsonObject.getString("description"));
        assertEquals("", jsonObject.getString("label"));
        assertEquals("Data", jsonObject.getJsonArray("categories").getString(0));
        assertEquals("", jsonObject.getJsonObject("dataFile").getString("filename"));
        assertEquals(-1, jsonObject.getJsonObject("dataFile").getInt("filesize"));
        assertEquals(-1, jsonObject.getJsonObject("dataFile").getInt("rootDataFileId"));
        assertEquals("Survey", jsonObject.getJsonObject("dataFile").getJsonArray("tabularTags").getString(0));
    }

    @Test
    public void testDatasetContactOutOfBoxNoPrivacy() {
        MetadataBlock block = new MetadataBlock();
        block.setName("citation");
        List<DatasetField> fields = new ArrayList<>();
        DatasetField datasetContactField = new DatasetField();
        DatasetFieldType datasetContactDatasetFieldType = datasetFieldTypeSvc.findByName("datasetContact");
        datasetContactDatasetFieldType.setMetadataBlock(block);
        datasetContactField.setDatasetFieldType(datasetContactDatasetFieldType);
        List<DatasetFieldCompoundValue> vals = new LinkedList<>();
        DatasetFieldCompoundValue val = new DatasetFieldCompoundValue();
        val.setParentDatasetField(datasetContactField);
        val.setChildDatasetFields(Arrays.asList(
                constructPrimitive("datasetContactEmail", "foo@bar.com"),
                constructPrimitive("datasetContactName", "Foo Bar"),
                constructPrimitive("datasetContactAffiliation", "Bar University")
        ));
        vals.add(val);
        datasetContactField.setDatasetFieldCompoundValues(vals);
        fields.add(datasetContactField);

        JsonObject jsonObject = JsonPrinter.json(block, fields, false).build();
        assertNotNull(jsonObject);

        System.out.println("json: " + JsonUtil.prettyPrint(jsonObject.toString()));

        assertEquals("Foo Bar", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals("foo@bar.com", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail").getString("value"));

        JsonObject byBlocks = JsonPrinter.jsonByBlocks(fields, false).build();

        System.out.println("byBlocks: " + JsonUtil.prettyPrint(byBlocks.toString()));
        assertEquals("Foo Bar", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals("foo@bar.com", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail").getString("value"));

    }

    @Test
    public void testDatasetContactWithPrivacy() {
        MetadataBlock block = new MetadataBlock();
        block.setName("citation");
        List<DatasetField> fields = new ArrayList<>();
        DatasetField datasetContactField = new DatasetField();
        DatasetFieldType datasetContactDatasetFieldType = datasetFieldTypeSvc.findByName("datasetContact");
        datasetContactDatasetFieldType.setMetadataBlock(block);
        datasetContactField.setDatasetFieldType(datasetContactDatasetFieldType);
        List<DatasetFieldCompoundValue> vals = new LinkedList<>();
        DatasetFieldCompoundValue val = new DatasetFieldCompoundValue();
        val.setParentDatasetField(datasetContactField);
        val.setChildDatasetFields(Arrays.asList(
                constructPrimitive("datasetContactEmail", "foo@bar.com"),
                constructPrimitive("datasetContactName", "Foo Bar"),
                constructPrimitive("datasetContactAffiliation", "Bar University")
        ));
        vals.add(val);
        datasetContactField.setDatasetFieldCompoundValues(vals);
        fields.add(datasetContactField);

        JsonObject jsonObject = JsonPrinter.json(block, fields, true).build();
        assertNotNull(jsonObject);

        System.out.println("json: " + JsonUtil.prettyPrint(jsonObject.toString()));

        assertEquals("Foo Bar", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals(null, jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail"));

        JsonObject byBlocks = JsonPrinter.jsonByBlocks(fields, true).build();

        System.out.println("byBlocks: " + JsonUtil.prettyPrint(byBlocks.toString()));
        assertEquals("Foo Bar", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals(null, byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail"));

    }

    @Test
    public void shouldIncludeEmbargoDate() throws ParseException {
        // given
        Dataset dataset = createDatasetForTests();
        String expectedDate = "2020-01-17";
        Date embargoDate = Util.getDateFormat().parse(expectedDate);
        dataset.setEmbargoDate(embargoDate);

        // when
        JsonObject jsonObject = JsonPrinter.json(dataset).build();

        // then
        assertThat("Should include embargo date", jsonObject.getString("embargoDate"), is(expectedDate));
    }

    @Test
    public void shouldNotIncludeEmptyEmbargoDate() {
        // given
        Dataset datasetWithNoEmbargoDate = createDatasetForTests();

        // when
        JsonObject jsonObject = JsonPrinter.json(datasetWithNoEmbargoDate).build();

        // then
        assertThat("Should not include embargo date if it's null", jsonObject.get("embargoDate"), nullValue());
    }

    @Test
    public void shouldProperlySetWhetherEmbargoIsActive() {
        // given
        Dataset dsWithActiveEmbargo = createDatasetForTests();
        Date dateInFuture = Date.from(LocalDate.now()
                .plusDays(7L)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
        dsWithActiveEmbargo.setEmbargoDate(dateInFuture);

        Dataset dsWithInactiveEmbargo = createDatasetForTests();
        Date dateInPast = Date.from(LocalDate.now()
                .minusDays(7L)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
        dsWithInactiveEmbargo.setEmbargoDate(dateInPast);

        // when
        JsonObject active = JsonPrinter.json(dsWithActiveEmbargo).build();
        JsonObject inactive = JsonPrinter.json(dsWithInactiveEmbargo).build();

        // then
        assertThat("Object with embargo in future should have embargo flag set to TRUE",
                active.getBoolean("embargoActive"), is(true));
        assertThat("Object with embargo in past should have embargo flag set to FALSE",
                inactive.getBoolean("embargoActive"), is(false));
    }

    // -------------------- PRIVATE --------------------

    private Dataset createDatasetForTests() {
        Dataset dataset = new Dataset();
        dataset.setId(234L);
        dataset.setOwner(new Dataverse());
        dataset.setIdentifier("identifier");
        dataset.setProtocol("doi");
        return dataset;
    }

    private DatasetField constructPrimitive(String datasetFieldTypeName, String value) {
        DatasetField retVal = new DatasetField();
        retVal.setDatasetFieldType(datasetFieldTypeSvc.findByName(datasetFieldTypeName));
        retVal.setDatasetFieldValues(Collections.singletonList(new DatasetFieldValue(retVal, value)));
        return retVal;
    }
}
