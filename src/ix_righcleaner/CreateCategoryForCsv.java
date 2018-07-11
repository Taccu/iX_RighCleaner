/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.core.DataValue;
import com.opentext.livelink.service.core.StringValue;
import com.opentext.livelink.service.docman.AttributeGroup;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Node;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author bho
 */
public class CreateCategoryForCsv extends ContentServerTask{
    private final Path xmlDir;
    private final ArrayList<TriaScan> items = new ArrayList<>();
    private final String catName;
    public CreateCategoryForCsv(Logger logger, String user, String password, String xmlDir, String catName, boolean export) {
        super(logger, user, password, export);
        this.xmlDir = Paths.get(xmlDir);
        this.catName = catName;
    }
    
    @Override
    public String getNameOfTask() {
        return "Create-Category-For-Csv";
    }
 
    @Override
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
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
           // Node node = docManClient.getNode(item.getId());
            AttributeGroup createCategory = createCategory(item);
            //node.getMetadata().getAttributeGroups().add(createCategory);
            logger.debug(String.valueOf(item.getId()));
            logger.debug(item.getPrescriberName());
        }
    }
    
    private AttributeGroup createCategory(TriaScan item) {
        AttributeGroup group = new AttributeGroup();
        group.setDisplayName(catName);
        ArrayList<DataValue> values = new ArrayList<>();
        Field[] declaredFields = item.getClass().getDeclaredFields();
        for(Field field : declaredFields) {
            try {
                System.out.println(field.getName()+":"+field.get(item));
                if(field.getName().equals("id")) {
                    continue;
                }
                StringValue value = new StringValue();
            } catch (IllegalArgumentException ex) {
                java.util.logging.Logger.getLogger(CreateCategoryForCsv.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(CreateCategoryForCsv.class.getName()).log(Level.SEVERE, null, ex);
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
