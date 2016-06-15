package edu.harvard.iq.dataverse.export.ddi;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class DdiExportUtil {

    private static final Logger logger = Logger.getLogger(DdiExportUtil.class.getCanonicalName());

    public static String datasetDtoAsJson2ddi(String datasetDtoAsJson) {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson, DatasetDTO.class);
        try {
            return dto2ddi(datasetDto);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DdiExportUtil.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static OutputStream datasetJson2ddi(JsonObject datasetDtoAsJson) {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson.toString()));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson.toString(), DatasetDTO.class);
        try {
            return dtoddi(datasetDto);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DdiExportUtil.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    private static OutputStream dtoddi(DatasetDTO datasetDto) throws XMLStreamException {
        OutputStream outputStream = new ByteArrayOutputStream();
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("codeBook");
        xmlw.writeDefaultNamespace("http://www.icpsr.umich.edu/DDI");
        writeAttribute(xmlw, "version", "2.0");
        createStdyDscr(xmlw, datasetDto);
        createdataDscr(xmlw, datasetDto.getDatasetVersion().getFiles());
        xmlw.writeEndElement(); // codeBook
        xmlw.flush();
        return outputStream;
    }

    private static String dto2ddi(DatasetDTO datasetDto) throws XMLStreamException {
        OutputStream outputStream = dtoddi(datasetDto);
        String xml = outputStream.toString();
        return XmlPrinter.prettyPrintXml(xml);
    }
    
    /**
     * @todo This is just a stub, copied from DDIExportServiceBean. It should
     * produce valid DDI based on
     * http://guides.dataverse.org/en/latest/developers/tools.html#msv but it is
     * incomplete and will be worked on as part of
     * https://github.com/IQSS/dataverse/issues/2579 . We'll want to reference
     * the DVN 3.x code for creating a complete DDI.
     *
     * @todo Rename this from "study" to "dataset".
     */
    private static void createStdyDscr(XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
        DatasetVersionDTO version = datasetDto.getDatasetVersion();
        String persistentAgency = datasetDto.getProtocol();
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();

        String citation = datasetDto.getDatasetVersion().getCitation();
        String producer = dto2producers(datasetDto.getDatasetVersion());
        xmlw.writeStartElement("stdyDscr");
        xmlw.writeStartElement("citation");
        xmlw.writeStartElement("titlStmt");
       
        writeFullElement(xmlw, "titl", dto2Primitive(version, DatasetFieldConstant.title));
        
        xmlw.writeStartElement("IDNo");
        writeAttribute(xmlw, "agency", persistentAgency);
        xmlw.writeCharacters(persistentAuthority + "/" + persistentId);
        xmlw.writeEndElement(); // IDNo
               
        writeFullElement(xmlw, "subTitl", dto2Primitive(version, DatasetFieldConstant.subTitle));
        writeFullElement(xmlw, "altTitl", dto2Primitive(version, DatasetFieldConstant.alternativeTitle));
        
        xmlw.writeEndElement(); // titlStmt

        writeAuthorsElement(xmlw, version);
 //       writeProducersElement(xmlw, version);
        
        if (producer != null && !producer.isEmpty()) {
            xmlw.writeStartElement("rspStmt");
            xmlw.writeStartElement("producer");
            xmlw.writeCharacters(producer);
            xmlw.writeEndElement(); // producer
            xmlw.writeEndElement(); // rspStmt
        }
        
        xmlw.writeStartElement("biblCit");
        xmlw.writeCharacters(citation);
        xmlw.writeEndElement(); // biblCit
        xmlw.writeEndElement(); // citation
        //End Citation Block
        
        //Start Study Info Block
        // Study Info
        xmlw.writeStartElement("stdyInfo");
        
        writeSubjectElement(xmlw, version); //Subject and Keywords
        writeAbstractElement(xmlw, version); // Description
        writeFullElement(xmlw, "notes", dto2Primitive(version, DatasetFieldConstant.notesText));
        
        writeSummaryDescriptionElement(xmlw, version);
        writeRelPublElement(xmlw, version);
        writeFullElement(xmlw, "prodDate", dto2Primitive(version, DatasetFieldConstant.productionDate));    
        writeFullElement(xmlw, "prodPlac", dto2Primitive(version, DatasetFieldConstant.productionPlace));  
        
        writeGrantElement(xmlw, version);
        xmlw.writeEndElement(); // stdyInfo
        // End Info Block
        
        xmlw.writeEndElement(); // stdyDscr

    }
    
    private static void writeSummaryDescriptionElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        xmlw.writeStartElement("sumDscr");
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                Integer per = 0;
                Integer coll = 0;
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.timePeriodCovered.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            per++;
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.timePeriodCoveredStart.equals(next.getTypeName())) {
                                    dateValStart = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.timePeriodCoveredEnd.equals(next.getTypeName())) {
                                    dateValEnd = next.getSinglePrimitive();
                                }
                            }
                            if (!dateValStart.isEmpty()) {
                                writeDateElement(xmlw, "timePrd", "P"+ per.toString(), "start", dateValStart );
                            }
                            if (!dateValEnd.isEmpty()) {
                                writeDateElement(xmlw, "timePrd",  "P"+ per.toString(), "end", dateValEnd );
                            }
                        }
                    }
                    if (DatasetFieldConstant.dateOfCollection.equals(fieldDTO.getTypeName())) {
                        String dateValStart = "";
                        String dateValEnd = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            coll++;
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.dateOfCollectionStart.equals(next.getTypeName())) {
                                    dateValStart = next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.dateOfCollectionEnd.equals(next.getTypeName())) {
                                    dateValEnd = next.getSinglePrimitive();
                                }
                            }
                            if (!dateValStart.isEmpty()) {
                                writeDateElement(xmlw, "collDate",  "P"+ coll.toString(), "start", dateValStart );
                            }
                            if (!dateValEnd.isEmpty()) {
                                writeDateElement(xmlw,  "collDate",  "P"+ coll.toString(), "end", dateValEnd );
                            }
                        }
                    }
                    if (DatasetFieldConstant.kindOfData.equals(fieldDTO.getTypeName())) {
                        writeMultipleElement(xmlw, "dataKind", fieldDTO);                     
                    }
                }
            }
            
            if("geospatial".equals(key)){                
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.geographicCoverage.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.country.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "nation", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.city.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "georgCover", next.getSinglePrimitive());
                                }
                                if (DatasetFieldConstant.state.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "georgCover", next.getSinglePrimitive());
                                }                               
                                if (DatasetFieldConstant.otherGeographicCoverage.equals(next.getTypeName())) {
                                    writeFullElement(xmlw, "georgCover", next.getSinglePrimitive());
                                }
                            }
                        }
                    }
                }              
            }

            if("socialscience".equals(key)){                
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.universe.equals(fieldDTO.getTypeName())) {
                        writeMultipleElement(xmlw, "universe", fieldDTO);
                    }
                    if (DatasetFieldConstant.unitOfAnalysis.equals(fieldDTO.getTypeName())) {
                        writeMultipleElement(xmlw, "anlyUnit", fieldDTO);                     
                    }
                }              
            }
        }
        xmlw.writeEndElement(); //sumDscr     
    }
    
    private static void writeMultipleElement(XMLStreamWriter xmlw, String element, FieldDTO fieldDTO) throws XMLStreamException {
        for (String value : fieldDTO.getMultiplePrimitive()) {
            writeFullElement(xmlw, element, value);
        }
    }
    
    private static void writeDateElement(XMLStreamWriter xmlw, String element, String cycle, String event, String dateIn) throws XMLStreamException {

        xmlw.writeStartElement(element);
        writeAttribute(xmlw, "cycle",  cycle);
        writeAttribute(xmlw, "event", event);
        writeAttribute(xmlw, "date", dateIn);
        xmlw.writeCharacters(dateIn);
        xmlw.writeEndElement(); 

    }
    
    private static void writeSubjectElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException{ 
        
        //Key Words and Topic Classification
        
        xmlw.writeStartElement("subject");        
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.subject.equals(fieldDTO.getTypeName())){
                        for ( String subject : fieldDTO.getMultipleVocab()){
                            xmlw.writeStartElement("keyword");
                            xmlw.writeCharacters(subject);
                            xmlw.writeEndElement(); //Keyword
                        }
                    }
                    
                    if (DatasetFieldConstant.keyword.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String keywordValue = "";
                            String keywordVocab = "";
                            String keywordURI = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.keywordValue.equals(next.getTypeName())) {
                                    keywordValue =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.keywordVocab.equals(next.getTypeName())) {
                                    keywordVocab =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.keywordVocabURI.equals(next.getTypeName())) {
                                    keywordURI =  next.getSinglePrimitive();
                                }
                            }
                            if (!keywordValue.isEmpty()){
                                xmlw.writeStartElement("keyword"); 
                                if(!keywordVocab.isEmpty()){
                                   writeAttribute(xmlw,"vocab",keywordVocab); 
                                }
                                if(!keywordURI.isEmpty()){
                                   writeAttribute(xmlw,"URI",keywordURI); 
                                } 
                                xmlw.writeCharacters(keywordValue);
                                xmlw.writeEndElement(); //Keyword
                            }

                        }
                    }
                    if (DatasetFieldConstant.topicClassification.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String topicClassificationValue = "";
                            String topicClassificationVocab = "";
                            String topicClassificationURI = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.topicClassValue.equals(next.getTypeName())) {
                                    topicClassificationValue =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.topicClassVocab.equals(next.getTypeName())) {
                                    topicClassificationVocab =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.topicClassVocabURI.equals(next.getTypeName())) {
                                    topicClassificationURI =  next.getSinglePrimitive();
                                }
                            }
                            if (!topicClassificationValue.isEmpty()){
                                xmlw.writeStartElement("topcClas"); 
                                if(!topicClassificationVocab.isEmpty()){
                                   writeAttribute(xmlw,"vocab",topicClassificationVocab); 
                                } 
                                if(!topicClassificationURI.isEmpty()){
                                   writeAttribute(xmlw,"URI",topicClassificationURI); 
                                } 
                                xmlw.writeCharacters(topicClassificationValue);
                                xmlw.writeEndElement(); //topcClas
                            }
                        }
                    }
                }
            }
        }        
        xmlw.writeEndElement(); // subject       
    }
    
    private static void writeAuthorsElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        xmlw.writeStartElement("rspStmt");
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.author.equals(fieldDTO.getTypeName())) {
                        String authorName = "";
                        String authorAffiliation = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.authorName.equals(next.getTypeName())) {
                                    authorName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.authorAffiliation.equals(next.getTypeName())) {
                                    authorAffiliation =  next.getSinglePrimitive();
                                }
                            }
                            if (!authorName.isEmpty()){
                                xmlw.writeStartElement("AuthEnty"); 
                                if(!authorAffiliation.isEmpty()){
                                   writeAttribute(xmlw,"affiliation",authorAffiliation); 
                                } 
                                xmlw.writeCharacters(authorName);
                                xmlw.writeEndElement(); //AuthEnty
                            }
                        }
                    }
                }
            }
        }
        xmlw.writeEndElement(); //rspStmt
    }
    
    private static void writeProducersElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        xmlw.writeStartElement("rspStmt");
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.producer.equals(fieldDTO.getTypeName())) {
                        String producerName = "";
                        String producerAffiliation = "";
                        String producerAbbreviation = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.producerName.equals(next.getTypeName())) {
                                    producerName =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.producerAffiliation.equals(next.getTypeName())) {
                                    producerAffiliation =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.producerAffiliation.equals(next.getTypeName())) {
                                    producerAffiliation =  next.getSinglePrimitive();
                                }
                            }
                            if (!producerName.isEmpty()){
                                xmlw.writeStartElement("producer"); 
                                if(!producerAffiliation.isEmpty()){
                                   writeAttribute(xmlw,"affiliation",producerAffiliation); 
                                } 
                                if(!producerAbbreviation.isEmpty()){
                                   writeAttribute(xmlw,"abbr",producerAbbreviation); 
                                }
                                xmlw.writeCharacters(producerName);
                                xmlw.writeEndElement(); //AuthEnty
                            }
                        }
                    }
                }
            }
        }
        xmlw.writeEndElement(); //rspStmt
    }
    
    private static void writeRelPublElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.publication.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            String pubString = "";
                            String citation = "";
                            String IDType = "";
                            String IDNo = "";
                            String url = "";
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.publicationCitation.equals(next.getTypeName())) {
                                    citation =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationIDType.equals(next.getTypeName())) {
                                    IDType =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationIDNumber.equals(next.getTypeName())) {
                                    IDNo =   next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.publicationURL.equals(next.getTypeName())) {
                                    url =  next.getSinglePrimitive();
                                }
                            }
                            pubString = appendCommaSeparatedValue(citation, IDType);
                            pubString = appendCommaSeparatedValue(pubString, IDNo);
                            pubString = appendCommaSeparatedValue(pubString, url);
                            if (!pubString.isEmpty()){
                                xmlw.writeStartElement("relPubl"); 
                                xmlw.writeCharacters(pubString);
                                xmlw.writeEndElement(); //relPubl
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static String appendCommaSeparatedValue(String inVal, String next) {
        if (!next.isEmpty()) {
            if (!inVal.isEmpty()) {
                return inVal + ", " + next;
            } else {
                return next;
            }
        }
        return inVal;
    }
    
    private static void writeAbstractElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.description.equals(fieldDTO.getTypeName())) {
                        String descriptionText = "";
                        String descriptionDate = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.descriptionText.equals(next.getTypeName())) {
                                    descriptionText =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.descriptionDate.equals(next.getTypeName())) {
                                    descriptionDate =  next.getSinglePrimitive();
                                }
                            }
                            if (!descriptionText.isEmpty()){
                                xmlw.writeStartElement("abstract"); 
                                if(!descriptionDate.isEmpty()){
                                   writeAttribute(xmlw,"date",descriptionDate); 
                                } 
                                xmlw.writeCharacters(descriptionText);
                                xmlw.writeEndElement(); //abstract
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeGrantElement(XMLStreamWriter xmlw, DatasetVersionDTO datasetVersionDTO) throws XMLStreamException {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.grantNumber.equals(fieldDTO.getTypeName())) {
                        String grantNumber = "";
                        String grantAgency = "";
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.grantNumberValue.equals(next.getTypeName())) {
                                    grantNumber =  next.getSinglePrimitive();
                                }
                                if (DatasetFieldConstant.grantNumberAgency.equals(next.getTypeName())) {
                                    grantAgency =  next.getSinglePrimitive();
                                }
                            }
                            if (!grantNumber.isEmpty()){
                                xmlw.writeStartElement("grantNo"); 
                                if(!grantAgency.isEmpty()){
                                   writeAttribute(xmlw,"agency",grantAgency); 
                                } 
                                xmlw.writeCharacters(grantNumber);
                                xmlw.writeEndElement(); //grantno
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @todo Create a full dataDscr and otherMat sections of the DDI. This stub
     * adapted from the minimal DDIExportServiceBean example.
     */
    private static void createdataDscr(XMLStreamWriter xmlw, List<FileDTO> fileDtos) throws XMLStreamException {
        if (fileDtos.isEmpty()) {
            return;
        }
        xmlw.writeStartElement("dataDscr");
        xmlw.writeEndElement(); // dataDscr
        for (FileDTO fileDTo : fileDtos) {
            xmlw.writeStartElement("otherMat");
            writeAttribute(xmlw, "ID", "f" + fileDTo.getDatafile().getId());
            writeAttribute(xmlw, "level", "datafile");
            xmlw.writeStartElement("labl");
            xmlw.writeCharacters(fileDTo.getDatafile().getName());
            xmlw.writeEndElement(); // labl
            writeFileDescription(xmlw, fileDTo);
            xmlw.writeEndElement(); // otherMat
        }
    }

    private static void writeFileDescription(XMLStreamWriter xmlw, FileDTO fileDTo) throws XMLStreamException {
        xmlw.writeStartElement("txt");
        String description = fileDTo.getDatafile().getDescription();
        if (description != null) {
            xmlw.writeCharacters(description);
        }
        xmlw.writeEndElement(); // txt
    }
    
    private static String dto2Primitive(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
           // if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                        return fieldDTO.getSinglePrimitive();
                    }
                }
           // }
        }
        return null;
    }
    
    private static String dto2ChildVal(DatasetVersionDTO datasetVersionDTO, String parentDatasetFieldTypeName, String childDatasetFieldTypeName) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
           // if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (parentDatasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (childDatasetFieldTypeName.equals(next.getTypeName())) {
                                    return next.getSinglePrimitive();
                                }
                            }
                        }
                    }
                }
           // }
        }
        return null;
    }
  
    private static String dto2producers(DatasetVersionDTO datasetVersionDTO) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.producer.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.producerName.equals(next.getTypeName())) {
                                    return next.getSinglePrimitive();
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private static void writeFullElement (XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        //For the simplest Elements we can 
        if (!StringUtilisEmpty(value)) {
            xmlw.writeStartElement(name);
            xmlw.writeCharacters(value);
            xmlw.writeEndElement(); // labl
        }

    }

    private static void writeAttribute(XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        if (!StringUtilisEmpty(value)) {
            xmlw.writeAttribute(name, value);
        }
    }

    private static boolean StringUtilisEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    private static void saveJsonToDisk(String datasetVersionAsJson) throws IOException {
        Files.write(Paths.get("/tmp/out.json"), datasetVersionAsJson.getBytes());
    }

}
