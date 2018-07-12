/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.core.DataValue;
import com.opentext.livelink.service.core.DateValue;
import com.opentext.livelink.service.core.StringValue;
import com.opentext.livelink.service.docman.Attribute;
import com.opentext.livelink.service.docman.AttributeGroup;
import com.opentext.livelink.service.docman.AttributeGroupDefinition;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Metadata;
import com.opentext.livelink.service.docman.Node;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author bho
 */
public class CreateCategoryForCsv extends ContentServerTask{
    private final Path xmlDir;
    private final ArrayList<TriaScan> items = new ArrayList<>();
    private final Long catName;
    public CreateCategoryForCsv(Logger logger, String user, String password, String xmlDir, String catId, boolean export) {
        super(logger, user, password, export);
        this.xmlDir = Paths.get(xmlDir);
        this.catName = Long.valueOf(catId);
    }
    
    @Override
    public String getNameOfTask() {
        return "Create-Category-For-Csv";
    }
 
    @Override
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        AttributeGroupDefinition def = docManClient.getCategoryDefinition(catName);
        AttributeGroup temp = docManClient.getCategoryTemplate(catName);
        
        try {
            Files.list(xmlDir).forEach((xml) -> {
                try {
                    items.add(parseXml(xml));
                } catch (JAXBException ex) {
                    handleError(ex);
                }
            });
        } catch (IOException ex) {
            handleError(ex);
        }
        for(TriaScan item : items) {
            Node node = docManClient.getNode(item.getId());
            Metadata newMetadata = node.getMetadata();
            AttributeGroup createCategory;
            try {
                createCategory = createCategory(item);
                newMetadata.getAttributeGroups().add(createCategory);
                docManClient.setNodeMetadata(node.getID(), newMetadata);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                logger.error("Couldn't create category for " + item.getId() + ". Error was " + ex.getMessage());
            }
            logger.debug(String.valueOf(item.getId()));
            logger.debug(item.getPrescriberName());
        }
    }
    
    private AttributeGroup createCategory(TriaScan item) throws IllegalArgumentException, IllegalAccessException {
        //AttributeGroup group = getDocManClient().getCategoryTemplate(catName);
        AttributeGroupDefinition def = getDocManClient().getCategoryDefinition(catName);
        AttributeGroup group = new AttributeGroup();
        group.setDisplayName(def.getDisplayName());
        group.setKey(def.getKey());
        group.setType(def.getType());
        for(Attribute attr : def.getAttributes()) {
            switch(attr.getDisplayName()) {
                case "DocumentDate":
                        XMLGregorianCalendar result;
                        DateValue date_value = new DateValue();
                        date_value.setDescription(attr.getDisplayName());
                        date_value.setKey(attr.getKey());
                        try {
                            
                            result = DatatypeFactory.newInstance().newXMLGregorianCalendar(item.getDocumentDate());
                            date_value.getValues().add(result);
                            group.getValues().add(date_value);
                        } catch (DatatypeConfigurationException ex) {
                            java.util.logging.Logger.getLogger(CreateCategoryForCsv.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                case "PatientDateOfBirth":
                    XMLGregorianCalendar pdb_result;
                    DateValue pdb_value = new DateValue();
                    pdb_value.setDescription(attr.getDisplayName());
                    pdb_value.setKey(attr.getKey());
                        try {
                            pdb_result = DatatypeFactory.newInstance().newXMLGregorianCalendar(item.getPatientDateOfBirth());
                            pdb_value.getValues().add(pdb_result);
                            group.getValues().add(pdb_value);
                        } catch (DatatypeConfigurationException ex) {
                            java.util.logging.Logger.getLogger(CreateCategoryForCsv.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    break;
                case "BatchDate":
                    XMLGregorianCalendar bd_result;
                    DateValue bd_value = new DateValue();
                    bd_value.setDescription(attr.getDisplayName());
                    bd_value.setKey(attr.getKey());
                        try {
                            bd_result = DatatypeFactory.newInstance().newXMLGregorianCalendar(item.getBatchDate());
                            bd_value.getValues().add(bd_result);
                            group.getValues().add(bd_value);
                        } catch (DatatypeConfigurationException ex) {
                            java.util.logging.Logger.getLogger(CreateCategoryForCsv.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    break;
                default:
                    StringValue str_value = new StringValue();
                    str_value.setDescription(attr.getDisplayName());
                    str_value.setKey(attr.getKey());
                    Field[] declaredFields = item.getClass().getDeclaredFields();
                    for(Field field : declaredFields) {
                        if(field.getName().equals("id")) {
                            continue;
                        }
                        if(field.getName().equalsIgnoreCase(str_value.getDescription()) && (field.get(item) != null)) {
                            try {
                                String strVal = String.valueOf(field.get(item));
                                if(strVal.charAt(0) == '\'') {
                                    strVal = strVal.replaceFirst("'", "");
                                }
                                if(strVal.charAt(strVal.length() -1) == '\'') {
                                    strVal = strVal.substring(0,strVal.length()-1);
                                }
                                str_value.getValues().add(strVal);
                                group.getValues().add(str_value);
                                System.out.println(strVal);
                            } catch (IllegalArgumentException | IllegalAccessException ex) {
                                java.util.logging.Logger.getLogger(CreateCategoryForCsv.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
            }
        }
        
        return group;
    }
    private TriaScan parseXml(Path xml) throws JAXBException{
        JAXBContext jc = JAXBContext.newInstance(TriaScan.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();
        TriaScan triascan = (TriaScan) unmarshaller.unmarshal(xml.toFile());
        return triascan;
    }
      
}
