/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.core.Attachment;
import com.opentext.livelink.service.docman.AttributeGroup;
import com.opentext.livelink.service.docman.CategoryInheritance;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Metadata;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Taccu
 */
public class UploadDocument extends ContentServerTask{
    private final Logger logger;
    private File document;
    public UploadDocument(Logger logger, String user, String password, File document) {
        super(logger, user, password);
        this.logger = logger;
        this.document = document;
    }
    
    @Override
    public String getNameOfTask() {
        return "Upload-Document";
    }
    
    @Override
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        Attachment attach = new Attachment();
        try {
            attach.setContents(FileUtils.readFileToByteArray(document));
            
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
        attach.setFileName(document.getName());
        try {
            attach.setFileSize(Files.size(document.toPath()));
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date(document.lastModified()));
        XMLGregorianCalendar date2 = null;
        try {
            date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException ex) {
            logger.error(ex.getMessage());
        }
        attach.setCreatedDate(date2);
        attach.setModifiedDate(date2);
        
        Metadata mData = new Metadata();
        List<AttributeGroup> attributeGroups = mData.getAttributeGroups();
        AttributeGroup aGroup = new AttributeGroup();
        List<CategoryInheritance> categoryInheritance = docManClient.getCategoryInheritance(81674l);
        docManClient.createDocument(2000l, "Test", "not", false, mData, attach);

    }
}
