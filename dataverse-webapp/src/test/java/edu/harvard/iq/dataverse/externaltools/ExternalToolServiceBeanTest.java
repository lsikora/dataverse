package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExternalToolServiceBeanTest {

    public ExternalToolServiceBeanTest() {
    }

    @Test
    public void testfindAll() {
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        FileMetadata fmd = new FileMetadata();
        DatasetVersion dv = new DatasetVersion();
        Dataset ds = new Dataset();
        dv.setDataset(ds);
        fmd.setDatasetVersion(dv);
        List<FileMetadata> fmdl = new ArrayList<FileMetadata>();
        fmdl.add(fmd);
        dataFile.setFileMetadatas(fmdl);
        List<DataTable> dataTables = new ArrayList<DataTable>();
        dataTables.add(new DataTable());
        dataFile.setDataTables(dataTables);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalTool.Type type = ExternalTool.Type.EXPLORE;
        ExternalTool externalTool = new ExternalTool("displayName", "description", type, "http://foo.com", "{}", TextMimeType.TSV_ALT.getMimeValue());
        List<ExternalTool> externalTools = new ArrayList<>();
        externalTools.add(externalTool);
        List<ExternalTool> availableExternalTools = ExternalToolServiceBean.findExternalToolsByFile(externalTools, dataFile);
        assertEquals(availableExternalTools.size(), 1);
    }

    @Test
    public void testParseAddExternalToolInput() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                     .add("fileid", "{fileId}")
                                     .build())
                        .add(Json.createObjectBuilder()
                                     .add("key", "{apiToken}")
                                     .build())
                        .build())
                .build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        assertEquals("AwesomeTool", externalTool.getDisplayName());
        assertEquals("This tool is awesome.", externalTool.getDescription());
        assertEquals(ExternalTool.Type.EXPLORE, externalTool.getType());
        assertEquals("http://awesometool.com", externalTool.getToolUrl());
        assertEquals("{\"queryParameters\":[{\"fileid\":\"{fileId}\"},{\"key\":\"{apiToken}\"}]}", externalTool.getToolParameters());
        assertEquals(TextMimeType.TSV_ALT.getMimeValue(), externalTool.getContentType());
    }

    @Test
    public void testParseAddExternalToolInputNoFileId() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                     .add("key", "{apiToken}")
                                     .build())
                        .build())
                .build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("Required reserved word not found: {fileId}", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNull() {
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(null);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("External tool manifest was null or empty!", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputEmptyString() {
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest("");
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("External tool manifest was null or empty!", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputUnknownReservedWord() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                     .add("fileid", "{fileId}")
                                     .build())
                        .add(Json.createObjectBuilder()
                                     .add("key", "{apiToken}")
                                     .build())
                        .add(Json.createObjectBuilder()
                                     .add("mode", "mode1")
                                     .build())
                        .build())
                .build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("Unknown reserved word: mode1", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoDisplayName() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("description", "This tool is awesome.");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("displayName is required.", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoDescription() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("description is required.", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoToolUrl() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("toolUrl is required.", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputWrongType() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "noSuchType");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        System.out.println("exception: " + expectedException);
        assertEquals("Type must be one of these values: [explore, configure].", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoContentType() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("toolUrl", "http://awesometool.com");

        job.add("toolParameters", Json.createObjectBuilder().add("queryParameters", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                             .add("fileid", "{fileId}")
                             .build())
                .add(Json.createObjectBuilder()
                             .add("key", "{apiToken}")
                             .build())
                .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        ExternalTool externalTool = null;
        try {
            externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        assertNotNull(externalTool);
        assertEquals(externalTool.getContentType(), TextMimeType.TSV_ALT.getMimeValue());
    }

}
