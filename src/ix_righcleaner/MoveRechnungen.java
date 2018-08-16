/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.core.DataValue;
import com.opentext.livelink.service.core.StringValue;
import com.opentext.livelink.service.docman.AttributeGroup;
import com.opentext.livelink.service.docman.AttributeSourceType;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Metadata;
import com.opentext.livelink.service.docman.MoveOptions;
import com.opentext.livelink.service.docman.Node;
import java.util.List;

/**
 *
 * @author bho
 */
public class MoveRechnungen extends ContentServerTask{

    private final long sourceFolderId, invoiceId, mandantId;
    private final boolean inheritPermFromDest,useDestinationCategories;
    
    public MoveRechnungen(Logger logger, String user, String password, long sourceFolderId, long invoiceId, long mandantId, boolean inheritPermFromDest, boolean useDestinationCategories, boolean export) {
        super(logger, user, password, export);
        this.sourceFolderId = sourceFolderId;
        this.invoiceId = invoiceId;
        this.mandantId = mandantId;
        this.inheritPermFromDest = inheritPermFromDest;
        this.useDestinationCategories = useDestinationCategories;
    }
    
    public String getNameOfTask() {
        return "Move-Rechnungen";
    }
    
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(Integer.MAX_VALUE);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodesInSourceFolder = docManClient.getNodesInContainer(sourceFolderId, options);
        for(Node node : nodesInSourceFolder) {
            logger.debug("Processing " + node.getName() + "(id:" + node.getID() + ")");
            if(!node.isIsContainer())try {
                long destinationIdForRechnung = getDestinationIdForRechnung(node, docManClient);
                Node destination = docManClient.getNode(destinationIdForRechnung);
                if(destinationIdForRechnung == 0l) {
                    
                    continue;
                }
                else {
                    logger.info("Verschiebe Dokument" + node.getName() + "(id:" + node.getID() + ") nach " + destination.getName() + "(id:" + destination.getID() + ")");
                }
                MoveOptions moveOptions = new MoveOptions();
                moveOptions.setAddVersion(false);
                moveOptions.setForceInheritPermissions(inheritPermFromDest);
                if(useDestinationCategories){
                    moveOptions.setAttrSourceType(AttributeSourceType.DESTINATION);
                }
                else{
                    moveOptions.setAttrSourceType(AttributeSourceType.ORIGINAL);
                }
                //docManClient.moveNode(node.getID(), destinationIdForRechnung, node.getName(), moveOptions);
            } catch(Exception ex) { ex.printStackTrace();}
        }
    }
    
    private long getDestinationIdForRechnung(Node rechnung, DocumentManagement docManClient) {
       Metadata data = rechnung.getMetadata();
       String bpName = "";
       String mandantName = "";
       for(AttributeGroup group : data.getAttributeGroups()) {
            if(group.getKey().startsWith(String.valueOf(invoiceId))) {
                logger.debug(rechnung.getName() + "(id:" + rechnung.getID() + ") found category " + invoiceId + "...");
                bpName = getBpNameFromCategory(group);
            }
            if(group.getKey().startsWith(String.valueOf(mandantId))) {
                logger.debug(rechnung.getName() + "(id:" + rechnung.getID() + ") found category " + mandantId + "...");
                StringValue val = (StringValue) group.getValues().get(0);
                if(val.getValues().size() > 0) mandantName = val.getValues().get(0);
                else logger.warn("Kategorie " + mandantId + " bei Objekt " + rechnung.getName() + "(id:" +rechnung.getID() + ") ist leer.");
            }
        }
       if((bpName==null ||bpName.isEmpty()) || (mandantName==null || mandantName.isEmpty())) {
           return 0l;
       }
       return guessFolder(bpName, mandantName, docManClient);
    }
    
    private String getBpNameFromCategory(AttributeGroup group) {
        
        for(DataValue value : group.getValues())  {
            if(value instanceof StringValue) {
                StringValue str_Value = (StringValue) value;
                for(String string : str_Value.getValues()) {
                    if(str_Value.getDescription().equals("Mandant")) {
                        System.out.println("Mandant:"+string);
                        return string;
                    }
                }
            }
        }
        return null;
    }
    
    private long guessFolder(String bpName, String mandantName, DocumentManagement docManClient) {
        Node mandantFolder = docManClient.getNodeByName(2000l, mandantName);
        Node bpFolder = docManClient.getNodeByName(mandantFolder.getID(), "Business Partner");
        String folder = "";
        System.out.println(bpName);
        char alphabet = bpName.charAt(0);
        int ascii_char = (int) alphabet;
        System.out.println(bpName+"->"+ascii_char);
        if( (65 <= ascii_char && ascii_char <= 70) || (97 <= ascii_char && ascii_char <= 102)) folder = "A-F";
        if( (71 <= ascii_char && ascii_char <= 76) || (103 <= ascii_char && ascii_char <= 108)) folder = "G-L";
        if( (77 <= ascii_char && ascii_char <= 82) || (109 <= ascii_char && ascii_char <= 114)) folder = "M-R";
        if( (83 <= ascii_char && ascii_char <= 90) || (115 <= ascii_char && ascii_char <= 122)) folder = "S-Z";
        System.out.println(folder);
        if(folder.isEmpty()) {
            handleError(new Exception("Erstes Zeichen der Kredtior Kategorie war nicht innerhalb von a-zA-Z"));
        }
        Node alphabetFolder = docManClient.getNodeByName(bpFolder.getID(), folder);
        System.out.println(folder+":"+alphabetFolder.getName()+"(id:" + alphabetFolder.getID()+")");
        Node node = docManClient.getNodeByName(alphabetFolder
                .getID(), bpName);
       
        long id = 0l;
        if(node != null) {
            
            id = node.getID();
        }
        else{
            logger.warn("Kein Business Partner Objekt vorhanden in " + alphabetFolder.getName() + "(id:" + alphabetFolder.getID() + ")");
            return id;
        }
        Node toReturn = docManClient.getNodeByName(id, "Accounting");
        return toReturn.getID();
    }
    
}
