/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.classifications.Classifications;
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

    private final long sourceFolderId, invoiceId;
    private final boolean inheritPermFromDest,useDestinationCategories,excludeCopies,clearClassifcations;
    
    public MoveRechnungen(Logger logger, String user, String password, long sourceFolderId, long invoiceId, boolean inheritPermFromDest, boolean useDestinationCategories, boolean excludeCopies, boolean clearClassifcations, boolean export) {
        super(logger, user, password, export);
        this.sourceFolderId = sourceFolderId;
        this.invoiceId = invoiceId;
        this.inheritPermFromDest = inheritPermFromDest;
        this.useDestinationCategories = useDestinationCategories;
        this.excludeCopies = excludeCopies;
        this.clearClassifcations = clearClassifcations;
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
        docManClient = getDocManClient(true);
        int i = 0;
        for(Node node : nodesInSourceFolder) {
            i++;
            if(i%30==0) docManClient = getDocManClient(true);
            logger.debug("Processing " + node.getName() + "(id:" + node.getID() + ")");
            if(!node.isIsContainer())try {
                long destinationIdForRechnung = getDestinationIdForRechnung(node, docManClient);
                Node destination = docManClient.getNode(destinationIdForRechnung);
                if(destinationIdForRechnung == 0l) {
                    continue;
                }
                else {
                    if(excludeCopies && node.getName().matches("(?i:.*(copy).*)")) {
                        logger.info("Ignoriere Dokument "  + node.getName() + "(id:" + node.getID() + ")");
                        continue;
                    }
                    logger.info("Verschiebe Dokument" + node.getName() + "(id:" + node.getID() + ") nach " + docManClient.getNode(destination.getParentID()).getName()+ "(id:" +destination.getParentID()+ ")\\"+ destination.getName() + "(id:" + destination.getID() + ")");
                }
                if(clearClassifcations) {
                    Classifications classifyClient = getClassifyClient();
                    boolean unClassify = classifyClient.unClassify(node.getID());
                    if(!unClassify) {
                        logger.error("Konnte die Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") nicht entfernen");
                    }
                    logger.info("Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") entfernt");
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
                exportIds.add(node.getID());
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
                for(DataValue value : group.getValues())  {
                    if(value instanceof StringValue) {
                        StringValue str_Value = (StringValue) value;
                        for(String string : str_Value.getValues()) {
                            if(str_Value.getDescription().equals("Kreditor")) {
                                System.out.println("Kreditor:"+string);
                                bpName = string;
                            }
                            if(str_Value.getDescription().equals("Mandant")) {
                                System.out.println("Mandant:"+string);
                                mandantName = string;
                            }
                        }
                    }
                }
            }
        }
       if((bpName==null ||bpName.isEmpty()) || (mandantName==null || mandantName.isEmpty())) {
           return 0l;
       }
       return guessFolder(bpName, mandantName,rechnung, docManClient);
    }
        
    private long guessFolder(String bpName, String mandantName, Node rechnung, DocumentManagement docManClient) {
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
        if( (48 <= ascii_char && ascii_char <= 57)) folder = "0-9";
        System.out.println(folder);
        if(folder.isEmpty()) {
            logger.error("Erstes Zeichen der Kredtior Kategorie für Dokument "+ rechnung.getName() + "(id:"+ rechnung.getID() +") war nicht innerhalb von a-zA-Z0-9");
            return 0l;
        }
        Node alphabetFolder = docManClient.getNodeByName(bpFolder.getID(), folder);
        if(alphabetFolder == null) {
            logger.warn("Kein Alphabet Ordner für " + folder + " in " + bpFolder.getName() + "(id:" + bpFolder.getID() + ") gefunden...");
            return 0l;
        }
        System.out.println(folder+":"+alphabetFolder.getName()+"(id:" + alphabetFolder.getID()+")");
        Node node = docManClient.getNodeByName(alphabetFolder
                .getID(), bpName);
       
        long id = 0l;
        if(node == null) {
            logger.warn("Kein Business Partner Objekt für Business Partner"+ bpName+" in"+ alphabetFolder.getName() + "(id:" + alphabetFolder.getID() + ")"+ " Mandant "+ mandantName+" vorhanden");
            return id;
        }
        logger.debug("Business Partner " + node.getName() + "(id:"+node.getID() + ") ausgewählt.");
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(1);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodesInBP = docManClient.getNodesInContainer(node.getID(), options);
        for(Node nodeInBP : nodesInBP) {
            if(nodeInBP.isIsContainer() && nodeInBP.getName().equals("Accounting")) {
                logger.debug("Accounting Ordner in " + node.getName() + "(id:"+node.getID() + ") gefunden");
                return nodeInBP.getID();
            }
        }
        return id;
    }
}
