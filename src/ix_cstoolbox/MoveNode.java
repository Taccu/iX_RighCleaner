/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

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
import java.util.concurrent.Callable;

/**
 *
 * @author bho
 */
public class MoveNode implements Callable<Node> {
    private final Node node;
    private final DocumentManagement docManClient;
    private final Logger logger;
    private final Classifications classifyClient;
    private final boolean inheritPermFromDest,useDestinationCategories,excludeCopies,clearClassifcations;
    private final long invoiceId, bpId;
    public MoveNode(Node node, DocumentManagement docManClient, Classifications classifyClient, boolean inheritPermFromDest, boolean useDestinationCategories, boolean excludeCopies, boolean clearClassifcations, long invoiceId, long bpId, Logger logger) {
        this.node = node;
        this.docManClient = docManClient;
        this.logger = logger;
        this.classifyClient = classifyClient;
        this.inheritPermFromDest = inheritPermFromDest;
        this.useDestinationCategories = useDestinationCategories;
        this.excludeCopies = excludeCopies;
        this.clearClassifcations = clearClassifcations;
        this.invoiceId = invoiceId;
        this.bpId = bpId;
    }
    @Override
    public Node call() {
        logger.debug("Processing " + node.getName() + "(id:" + node.getID() + ")");
        try {
            if(clearClassifcations) {
                boolean unClassify = classifyClient.unClassify(node.getID());
                if(!unClassify) {
                    logger.error("Konnte die Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") nicht entfernen");
                }
                logger.info("Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") entfernt");
            }
            if(!excludeCopies && node.getName().matches("(?i:.*(copy).*)")) {
                long destinationIdForRechnung = getDestinationIdForRechnungCopy(node, docManClient);
                if(destinationIdForRechnung == 0l) {
                    return null;
                }
                Node destination = docManClient.getNode(destinationIdForRechnung);
                move(node, destination, docManClient);
                return node;
            }
            else{
                if(!node.getName().matches("(?i:.*(copy).*)")){
                    long destinationIdForRechnung = getDestinationIdForRechnung(node, docManClient);
                    if(destinationIdForRechnung == 0l) {
                        return null;
                    }
                    Node destination = docManClient.getNode(destinationIdForRechnung);
                    move(node, destination, docManClient);
                    return node;
                } else{
                    logger.info("Ignoriere Dokument "  + node.getName() + "(id:" + node.getID() + ")");
                }
            }

        } catch(Exception ex) { 
            ex.printStackTrace();
        }
        return null;
    }
    private void move(Node node, Node destination, DocumentManagement docManClient) {
        logger.info("Verschiebe Dokument" + node.getName() + "(id:" + node.getID() + ") nach " + docManClient.getNode(destination.getParentID()).getName()+ "(id:" +destination.getParentID()+ ")\\"+ destination.getName() + "(id:" + destination.getID() + ")");

        MoveOptions moveOptions = new MoveOptions();
        moveOptions.setAddVersion(false);
        moveOptions.setForceInheritPermissions(inheritPermFromDest);
        if(useDestinationCategories){
            moveOptions.setAttrSourceType(AttributeSourceType.DESTINATION);
        }
        else{
            moveOptions.setAttrSourceType(AttributeSourceType.ORIGINAL);
        }
        //docManClient.moveNode(node.getID(), destination.getID(), node.getName(), moveOptions);
    } 
    private long getDestinationIdForRechnungCopy(Node rechnung, DocumentManagement docManClient) {
       Metadata data = rechnung.getMetadata();
       long kostId = 0l;
       String mandantName = "";
       String kostName = "";
       for(AttributeGroup group : data.getAttributeGroups()) {
            if(group.getKey().startsWith(String.valueOf(invoiceId))) {
                logger.debug(rechnung.getName() + "(id:" + rechnung.getID() + ") found category " + invoiceId + "...");
                for(DataValue value : group.getValues())  {
                    if(value instanceof StringValue) {
                        StringValue str_Value = (StringValue) value;
                        for(String string : str_Value.getValues()) {
                            if(str_Value.getDescription().equals("Freigebende Kostenstelle ID")) {
                                System.out.println("Freigebende Kostenstelle ID:"+string);
                                kostId = Long.valueOf(string);
                            }
                            if(str_Value.getDescription().equals("Mandant")) {
                                System.out.println("Mandant:"+string);
                                mandantName = string;
                            }
                            if(str_Value.getDescription().equals("Freigebende Kostenstelle Name")) {
                                System.out.println("Freigebende Kostenstelle Name:"+string);
                                kostName = string;
                            }
                        }
                    }
                }
            }
        }
       if(kostId >0l &&(mandantName==null || mandantName.isEmpty())) {
           return 0l;
       }
       return guessFolderCopy(kostId, kostName, mandantName,rechnung, docManClient);
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
    private String extractAlphabet(char alphabet) {
        int ascii_char = (int) alphabet;
        if( (65 <= ascii_char && ascii_char <= 70) || (97 <= ascii_char && ascii_char <= 102)) return "A-F";
        if( (71 <= ascii_char && ascii_char <= 76) || (103 <= ascii_char && ascii_char <= 108)) return "G-L";
        if( (77 <= ascii_char && ascii_char <= 82) || (109 <= ascii_char && ascii_char <= 114)) return "M-R";
        if( (83 <= ascii_char && ascii_char <= 90) || (115 <= ascii_char && ascii_char <= 122)) return "S-Z";
        if( (48 <= ascii_char && ascii_char <= 57)) return "0-9";
        return "";
    } 
    private long guessFolder(String bpName, String mandantName, Node rechnung, DocumentManagement docManClient) {
        Node mandantFolder = docManClient.getNodeByName(2000l, mandantName);
        Node bpFolder = docManClient.getNodeByName(mandantFolder.getID(), "Business Partner");
        char alphabet = bpName.charAt(0);
        String folder = extractAlphabet(alphabet);
        System.out.println(folder);
        if(folder.isEmpty()) {
            logger.error("Erstes Zeichen der Kredtior Kategorie für Dokument "+ rechnung.getName() + "(id:"+ rechnung.getID() +") war nicht innerhalb von a-zA-Z0-9");
            return 0l;
        }
        Node alphabetFolder = getAlphabetFolder(bpFolder, folder, docManClient);
        if(alphabetFolder == null) {
            logger.warn("Kein Alphabet Ordner für " + folder + " in " + bpFolder.getName() + "(id:" + bpFolder.getID() + ") gefunden...");
            return 0l;
        }
        System.out.println(folder+":"+alphabetFolder.getName()+"(id:" + alphabetFolder.getID()+")");
        Node node = docManClient.getNodeByName(alphabetFolder
                .getID(), bpName);
       
        long id = 0l;
        if(node == null) {
            logger.warn("Kein Business Partner Objekt für Business Partner "+ bpName+" im Ordner "+ alphabetFolder.getName() + "(id:" + alphabetFolder.getID() + ")"+ " Mandant "+ mandantName+" vorhanden");
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
    private Node getAlphabetFolder(Node baseNode, String folder, DocumentManagement docManClient) {
        Node alphabetFolder = docManClient.getNodeByName(baseNode.getID(), folder);
        if(alphabetFolder == null) {
            logger.warn("Kein Alphabet Ordner für " + folder + " in " + baseNode.getName() + "(id:" + baseNode.getID() + ") gefunden...");
            return null;
        }
        return alphabetFolder;
    }
    private long guessFolderCopy(long kostId, String kostName, String mandantName, Node rechnung, DocumentManagement docManClient) {
        Node mandantFolder = docManClient.getNodeByName(2000l, mandantName);
        String folder = extractAlphabet(kostName.charAt(0));
        if(kostName.isEmpty()) {
            logger.error("Erstes Zeichen der frg. Kostenstelle Name für Dokument "+ rechnung.getName() + "(id:"+ rechnung.getID() +") war nicht innerhalb von a-zA-Z0-9");
            return 0l;
        }
        Node bpFolder = docManClient.getNodeByName(mandantFolder.getID(), "Business Partner");
        Node node = null;
         GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(1);
        options.setMaxResults(Integer.MAX_VALUE);
        Node alphabetFolder = getAlphabetFolder(bpFolder, folder, docManClient);
        if(alphabetFolder != null) {
            for(Node inAlphabet : docManClient.getNodesInContainer(alphabetFolder.getID(), options)) {
                if(inAlphabet.getType().equals("EcmWorkspace")) {
                    for(AttributeGroup group : inAlphabet.getMetadata().getAttributeGroups()) {
                        if(group.getKey().startsWith(String.valueOf(bpId))) {
                            boolean pharmType = false;
                            boolean costCenterMatches = false;
                            for(DataValue value : group.getValues())  {
                                if(value instanceof StringValue) {
                                    StringValue str_Value = (StringValue) value;
                                    for(String string : str_Value.getValues()) {
                                        if(str_Value.getDescription().equals("Cost Center")) {
                                            System.out.println("Cost Center"+string);
                                            if(kostId == Long.valueOf(string))costCenterMatches=true;
                                        }
                                        if(str_Value.getDescription().equals("Type")) {
                                            System.out.println("Type:"+string);
                                            if(string.equals("Pharmacy"))pharmType=true;
                                        }
                                    }
                                }
                            }
                            if(pharmType && costCenterMatches) {
                                logger.info("Found correct Business Partner " + inAlphabet.getName() +"(id:" + inAlphabet.getID() + ") on the quick way");
                                node = inAlphabet;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if(node == null) {
           
            for(Node alphabet : docManClient.getNodesInContainer(bpFolder.getID(), options)) {
                //check for kostId = CostCenter Attribute and type=Pharmacy
                if(alphabet.getType().equals("EcmWorkspace")) {
                    for(AttributeGroup group : alphabet.getMetadata().getAttributeGroups()) {
                        if(group.getKey().startsWith(String.valueOf(bpId))) {
                            boolean pharmType = false;
                            boolean costCenterMatches = false;
                            for(DataValue value : group.getValues())  {
                                if(value instanceof StringValue) {
                                    StringValue str_Value = (StringValue) value;
                                    for(String string : str_Value.getValues()) {
                                        if(str_Value.getDescription().equals("Cost Center")) {
                                            System.out.println("Cost Center"+string);
                                            if(kostId == Long.valueOf(string))costCenterMatches=true;
                                        }
                                        if(str_Value.getDescription().equals("Type")) {
                                            System.out.println("Type:"+string);
                                            if(string.equals("Pharmacy"))pharmType=true;
                                        }
                                    }
                                }
                            }
                            if(pharmType && costCenterMatches) {
                                logger.info("Found correct Business Partner " + alphabet.getName() +"(id:" + alphabet.getID() + ") on the slow way");
                                node = alphabet;
                                break;
                            }
                        }
                    }
                }
                //node = alphabet;
            }
        }
        long id = 0l;
        if(node == null) {
            logger.warn("Kein Business Partner Objekt für Rechnung "+ rechnung.getName() + "(id:" + rechnung.getID() +") in"+ bpFolder.getName() + "(id:" + bpFolder.getID() + ")"+ " Mandant "+ mandantName+" vorhanden");
            return id;
        }
        logger.debug("Business Partner " + node.getName() + "(id:"+node.getID() + ") ausgewählt.");
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
