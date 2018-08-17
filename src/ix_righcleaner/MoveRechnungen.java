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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bho
 */
public class MoveRechnungen extends ContentServerTask{

    private final long sourceFolderId, invoiceId, bpId;
    private final boolean inheritPermFromDest,useDestinationCategories,excludeCopies,clearClassifcations;
    private final ArrayList<KostenstelleMapping> kostMapping = new ArrayList<>();
    public MoveRechnungen(Logger logger, String user, String password, long sourceFolderId, long invoiceId, boolean inheritPermFromDest, boolean useDestinationCategories, boolean excludeCopies, boolean clearClassifcations, long bpId, boolean export) {
        super(logger, user, password, export);
        this.sourceFolderId = sourceFolderId;
        this.invoiceId = invoiceId;
        this.inheritPermFromDest = inheritPermFromDest;
        this.useDestinationCategories = useDestinationCategories;
        this.excludeCopies = excludeCopies;
        this.clearClassifcations = clearClassifcations;
        this.bpId = bpId;
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
                
                
                if(clearClassifcations) {
                    Classifications classifyClient = getClassifyClient();
                    boolean unClassify = classifyClient.unClassify(node.getID());
                    if(!unClassify) {
                        logger.error("Konnte die Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") nicht entfernen");
                    }
                    logger.info("Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") entfernt");
                }
                if(!excludeCopies && node.getName().matches("(?i:.*(copy).*)")) {
                    long destinationIdForRechnung = getDestinationIdForRechnungCopy(node, docManClient);
                    if(destinationIdForRechnung == 0l) {
                        continue;
                    }
                    Node destination = docManClient.getNode(destinationIdForRechnung);
                    move(node, destination, docManClient);
                    docManClient = getDocManClient(true);
                }
                else{
                    if(!node.getName().matches("(?i:.*(copy).*)")){
                        long destinationIdForRechnung = getDestinationIdForRechnung(node, docManClient);
                        if(destinationIdForRechnung == 0l) {
                            continue;
                        }
                        Node destination = docManClient.getNode(destinationIdForRechnung);
                        move(node, destination, docManClient);
                    } else{
                        logger.info("Ignoriere Dokument "  + node.getName() + "(id:" + node.getID() + ")");
                    }
                }
                
                exportIds.add(node.getID());
                //if(i==1)break;
                
            } catch(Exception ex) { ex.printStackTrace();}
        }
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
                        }
                    }
                }
            }
        }
       if(kostId >0l &&(mandantName==null || mandantName.isEmpty())) {
           return 0l;
       }
      
       return guessFolderCopy(kostId, mandantName,rechnung, docManClient);
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
            logger.warn("Kein Business Partner Objekt für Business Partner "+ bpName+" in"+ alphabetFolder.getName() + "(id:" + alphabetFolder.getID() + ")"+ " Mandant "+ mandantName+" vorhanden");
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
    private long guessFolderCopy(long kostId, String mandantName, Node rechnung, DocumentManagement docManClient) {
        Node mandantFolder = docManClient.getNodeByName(2000l, mandantName);
        for(KostenstelleMapping mapping : kostMapping) {
            if(mapping.getKostId() == kostId && mapping.getMandantId() == mandantFolder.getID() ) {
                //mapping already there
                logger.info("Mapping for frg. Kostenstelle ID:" + kostId +" and Mandant:" + mandantFolder.getID() + " was already found. Using existing one");
                return mapping.getTargetNode();
            }
        }
        Node bpFolder = docManClient.getNodeByName(mandantFolder.getID(), "Business Partner");
        Node node = null;
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(1);
        options.setMaxResults(Integer.MAX_VALUE);
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
                            logger.info("Found correct Business Partner " + alphabet.getName() +"(id:" + alphabet.getID() + ")");
                            node = alphabet;
                            break;
                        }
                    }
                }
            }
            //node = alphabet;
        }
        long id = 0l;
        if(node == null) {
            logger.warn("Kein Business Partner Objekt für Rechnung "+ rechnung.getName() + "(id:" + rechnung.getID() +") in"+ bpFolder.getName() + "(id:" + bpFolder.getID() + ")"+ " Mandant "+ mandantName+" vorhanden");
            return id;
        }
        logger.debug("Business Partner " + node.getName() + "(id:"+node.getID() + ") ausgewählt.");
        options = new GetNodesInContainerOptions();
        options.setMaxDepth(1);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodesInBP = docManClient.getNodesInContainer(node.getID(), options);
        for(Node nodeInBP : nodesInBP) {
            if(nodeInBP.isIsContainer() && nodeInBP.getName().equals("Accounting")) {
                logger.debug("Accounting Ordner in " + node.getName() + "(id:"+node.getID() + ") gefunden");
                kostMapping.add(new KostenstelleMapping(kostId, mandantFolder.getID(), nodeInBP.getID()));
                return nodeInBP.getID();
            }
        }
        return id;
    }
       
    class KostenstelleMapping {
        long kostId;
        long mandantId;
        long targetNode;
        public KostenstelleMapping(long kostId, long mandantId, long targetNode) {
            this.kostId = kostId;
            this.mandantId = mandantId;
            this.targetNode = targetNode;
        }

        public long getKostId() {
            return kostId;
        }

        public long getTargetNode() {
            return targetNode;
        }

        public long getMandantId() {
            return mandantId;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (int) (this.kostId ^ (this.kostId >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final KostenstelleMapping other = (KostenstelleMapping) obj;
            if (this.kostId != other.kostId) {
                return false;
            }
            return true;
        }
    }
}
