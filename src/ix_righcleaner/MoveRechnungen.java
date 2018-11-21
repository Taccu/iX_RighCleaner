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
import com.opentext.livelink.service.searchservices.DataBagType;
import com.opentext.livelink.service.searchservices.SGraph;
import com.opentext.livelink.service.searchservices.SResultPage;
import com.opentext.livelink.service.searchservices.SearchService;
import com.opentext.livelink.service.searchservices.SingleSearchRequest;
import com.opentext.livelink.service.searchservices.SingleSearchResponse;
import static ix_righcleaner.ContentServerTask.SEARCH_API;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author bho
 */
public class MoveRechnungen extends ContentServerTask{

    private final long sourceFolderId, invoiceId, bpId, mandantId;
    private final boolean inheritPermFromDest,useDestinationCategories,excludeCopies,clearClassifcations,debug;
    private static final ArrayList<AdvancedNode> B_WORKSPACES = new ArrayList<>();
    private final String dbServer, dbName;
    public MoveRechnungen(Logger logger, String user, String password, long sourceFolderId, long invoiceId, boolean inheritPermFromDest, boolean useDestinationCategories, boolean excludeCopies, boolean clearClassifcations, long bpId, long mandantId, String dbServer, String dbName, boolean debug, boolean export) {
        super(logger, user, password, export);
        this.sourceFolderId = sourceFolderId;
        this.invoiceId = invoiceId;
        this.inheritPermFromDest = inheritPermFromDest;
        this.useDestinationCategories = useDestinationCategories;
        this.excludeCopies = excludeCopies;
        this.clearClassifcations = clearClassifcations;
        this.bpId = bpId;
        this.mandantId = mandantId;
        this.debug = debug;
        this.dbServer = dbServer;
        this.dbName = dbName;
    }
    
    @Override
    public String getNameOfTask() {
        return "Move-Rechnungen";
    }
    
    @Override
    public void doWork() {
        SearchService searchClient = getSearchClient();
        //Alle Business Workspaces
        List<Node> nodes = newSearch(searchClient);
        logger.debug("Found " + nodes.size() + " Business Workspaces");
        newStream(nodes);
        nodes = null;
        logger.debug("Gathered data from " + B_WORKSPACES.size());
        //Alle Belege in dem Ordner
        oldMove(newGetNodes(sourceFolderId));
    }
    private List<Node> newGetNodes(Long parentID) {
        List<Node> nodes = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        try {
        connectToDatabase(dbServer, dbName);
        PreparedStatement ps = CONNECTION.prepareStatement("SELECT DataID\n" +
                "FROM csadmin.DTree\n" +
                "WHERE SubType = 144\n" +
                "AND ParentID = ?");
        ps.setLong(1, parentID);
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            ids.add(rs.getLong("DataID"));
        }
        }catch(Exception ex)  {
            handleError(ex);
        }
        nodes.addAll(getDocManClient().getNodes(ids));
        return nodes;
    }
    private List<Node> newSearch(SearchService searchClient){
        List<Node> nodes = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        try {
        connectToDatabase(dbServer, dbName);
        PreparedStatement ps = CONNECTION.prepareStatement("SELECT DataID\n" +
                "FROM csadmin.DTree\n" +
                "WHERE SubType = 848");
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            ids.add(rs.getLong("DataID"));
        }
        }catch(Exception ex)  {
            handleError(ex);
        }
        nodes.addAll(getDocManClient().getNodes(ids));
        return nodes;
    }
    private void newStream(List<Node> nodes) {
        nodes.stream()
                .filter(node -> node.getType().equals("EcmWorkspace"))
                .forEach(node -> {
                    if(debug)logger.debug("Collecting data from " + node.getName() + "(id:" + node.getID());
                    //Create new AdvancedNode
                    DocumentManagement docManClient = getDocManClient();
                    AdvancedNode workspace = new AdvancedNode();
                    workspace.setNode(node);
                    List<AttributeGroup> attrGroups = node.getMetadata().getAttributeGroups();
                    //Loop through all assigned categories of node
                    for(AttributeGroup attrGroup : attrGroups)  {
                        List<DataValue> values = attrGroup.getValues();
                        if(attrGroup.getKey().startsWith(String.valueOf(bpId))){
                            //Business Partner Category
                            values.stream().forEach(value -> {
                                StringValue str_value = (StringValue) value;
                                if(str_value.getValues().size()<1)return;
                                String attrValue = str_value.getValues().get(0);
                                if(str_value.getDescription().equalsIgnoreCase("Cost Center")) {
                                    workspace.setCostCenter(attrValue);
                                }
                                if(str_value.getDescription().equalsIgnoreCase("Type")) {
                                    workspace.setType(attrValue);
                                    if(attrValue.equalsIgnoreCase("pharmacy"))workspace.setIsPharmacy(true);
                                    else workspace.setIsPharmacy(false);
                                }
                                if(str_value.getDescription().equalsIgnoreCase("BusinessPartnerID")) {
                                    workspace.setbPartner(attrValue);
                                }
                                if(str_value.getDescription().equalsIgnoreCase("Business Partner ID")) {
                                    workspace.setbPartner(attrValue);
                                }
                            });
                        }
                        if(attrGroup.getKey().startsWith(String.valueOf(mandantId))){
                            //Mandant Category
                            values.stream().forEach(value -> {
                                StringValue str_value = (StringValue) value;
                                String attrValue = str_value.getValues().get(0);
                                if(str_value.getDescription().equalsIgnoreCase("Mandant")) {
                                    workspace.setMandant(attrValue);
                                }
                            });
                        }
                        //Doesn't match anything
                    }
                    //if(debug)logger.debug("Adding " + workspace.getNode().getName() + ":"+ workspace.getType() +":" + workspace.getCostCenter() + ":" + workspace.getMandant() + ":" + workspace.getbPartner());
                    B_WORKSPACES.add(workspace);
                });
    }
    private void newMove(List<Node> nodesInSourceFolder, DocumentManagement docManClient) {
        if(!excludeCopies)nodesInSourceFolder.stream().filter(a -> a.getName().matches("(?i:.*(copy).*)"))
                .parallel().forEach(node -> {
                    node.getMetadata().getAttributeGroups().stream()
                            .filter(a -> a.getKey().startsWith(String.valueOf(invoiceId)))
                            .forEach(
                            values -> values.getValues().stream().forEach(value -> {
                                
                            })
                            );
                });
        
        nodesInSourceFolder.stream().filter(a -> !a.getName().matches("(?i:.*(copy).*)"))
                .parallel().forEach(node -> {
                node.getMetadata().getAttributeGroups().stream()
                        .filter(a -> a.getKey().startsWith(String.valueOf(invoiceId)))
                            .forEach(
                            values -> values.getValues().stream().forEach(value -> {
                                
                            })
                            );
                 });
    }
    private void oldMove(List<Node> nodesInSourceFolder) {
        for(Node node : nodesInSourceFolder) {
            boolean lookForCopyFolder;
            Node destination = null;
            String mandantName = "";
            String kostId = "";
            String bpName = "";
            Metadata data = node.getMetadata();
            if(!excludeCopies && node.getName().matches("(?i:.*(copy).*)")) {
                //Node is a copy and we are processing copies
                lookForCopyFolder = true;
            }
            else {
                if(!node.getName().matches("(?i:.*(copy).*)")){
                    //node is not a copy
                    lookForCopyFolder = false;
                }
                 else{
                    //We are ignoring this document
                    logger.info("Ignoriere Dokument "  + node.getName() + "(id:" + node.getID() + ")");
                    continue;
                }
            }
            for(AttributeGroup group : data.getAttributeGroups()) {
                if(group.getKey().startsWith(String.valueOf(invoiceId))) {
                    logger.debug(node.getName() + "(id:" + node.getID() + ") found category " + invoiceId + "...");
                    for(DataValue value : group.getValues())  {
                        if(value instanceof StringValue) {
                            StringValue str_Value = (StringValue) value;
                            if(lookForCopyFolder) {
                                for(String string : str_Value.getValues()) {
                                    if(str_Value.getDescription().equals("Freigebende Kostenstelle ID")) {
                                        kostId = string;
                                    }
                                    if(str_Value.getDescription().equals("Mandant")) {
                                        mandantName = string;
                                    }
                                }
                            }
                            else {
                                for(String string : str_Value.getValues()) {
                                    if(str_Value.getDescription().equals("KreditorID")) {
                                        bpName = string;
                                    }
                                    if(str_Value.getDescription().equals("Mandant")) {
                                        mandantName = string;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(debug) logger.debug("Checking " + kostId + ":" + bpName + ":" + mandantName);
            destination = getWorkspace(getDocManClient(), lookForCopyFolder, kostId, bpName, mandantName);
            if(destination == null) {
                if(!bpName.isEmpty()){
                    logger.warn("Couldn't find the Accounting folder in Bussiness Partner for " + node.getName() + "(id:" + node.getID() + "). Was looking in Mandant " + mandantName + " for BP " + bpName);
                }
                if(!kostId.isEmpty()){
                    logger.warn("Couldn't find the Accounting folder in Pharmacy for " + node.getName() + "(id:" + node.getID() + "). Was looking in Mandant " + mandantName + " for PharmacyId " + kostId);
                }
                continue;
            }
            logger.debug("Moving Node " + node.getName() + "(id:" + node.getID() + ") to destination " + destination.getName() + "(id:" + destination.getID() + ")");
            this.move(node, destination);
            logger.debug("Moved Node " + node.getName() + "(id:" + node.getID() + ") to destination " + destination.getName() + "(id:" + destination.getID() +")");
        }
    }
    private Node getWorkspace(DocumentManagement docManClient, boolean lookForCopyFolder, String kostId, String bpName, String mandantName) {
        for(AdvancedNode workspace : B_WORKSPACES) {
                if(lookForCopyFolder && workspace.isIsPharmacy()) {                   
                    if(workspace.getCostCenter() != null && workspace.getCostCenter().equals(kostId) && workspace.getMandant().equals(mandantName)) {
                        logger.debug("Found correct Pharmacy " + workspace.getNode().getName() + "(id:" + workspace.getNode().getID()+")");
                        return findAccounting(docManClient, workspace.getNode());
                    }
                }
                else {
                    if(workspace.getbPartner()!=null && workspace.getbPartner().equals(bpName) && workspace.getMandant().equals(mandantName)) {
                        logger.debug("Found correct Business Partner " + workspace.getNode().getName() + "(id:" + workspace.getNode().getID()+")");
                        return findAccounting(docManClient, workspace.getNode());
                    }
                }
            }
        return null;
    }
    //Findet den Accounting Ordner im BW
    private Node findAccounting( DocumentManagement docManClient, Node node) {
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(1);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodesInBP = docManClient.getNodesInContainer(node.getID(), options);
        for(Node nodeInBP : nodesInBP) {
            if(nodeInBP.isIsContainer() && nodeInBP.getName().equals("Accounting")) {
                logger.debug(nodeInBP.getName() + "(id:"+ nodeInBP.getID() +") Ordner in " + node.getName() + "(id:"+node.getID() + ") gefunden");
                return nodeInBP;
            }
        }
        return null;
    }
    // Verschiebt den Knoten "node" nach "destination"
    private void move(Node node, Node destination) {
        if(debug)logger.debug("bug bug");
        try {
            //if(clearClassifcations) {
               // boolean unClassify = getClassifyClient().unClassify(node.getID());
               // if(!unClassify) {
                    logger.error("Konnte die Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") nicht entfernen");
                //}
                //logger.info("Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") entfernt");
           // }
            logger.info("Verschiebe Dokument "
                    + node.getName()
                    + "(id:" + node.getID() + ") nach " 
                    + getDocManClient().getNode(destination.getParentID()).getName()
                    + "(id:" +destination.getParentID()
                    + ")\\"
                    + destination.getName()
                    + "(id:" + destination.getID()
                    + ")");

            MoveOptions moveOptions = new MoveOptions();
            moveOptions.setAddVersion(false);
            moveOptions.setForceInheritPermissions(inheritPermFromDest);
            if(useDestinationCategories){
                moveOptions.setAttrSourceType(AttributeSourceType.DESTINATION);
            }
            else{
                moveOptions.setAttrSourceType(AttributeSourceType.ORIGINAL);
            }
            getDocManClient().moveNode(node.getID(), destination.getID(), node.getName(), moveOptions);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    } 
    public List<Long> getNodesBySearch(SearchService sService){
        SingleSearchRequest query = new SingleSearchRequest();
        List<String> dataCollections = sService.getDataCollections();
        query.setDataCollectionSpec("'LES Enterprise'");
        query.setQueryLanguage(SEARCH_API);
        query.setFirstResultToRetrieve(1);
        query.setNumResultsToRetrieve(500000);
        query.setResultSetSpec("where1=(\"OTSubType\":\"848\")&lookfor1=complexquery");
        query.setResultOrderSpec("sortByRegion=OTCreatedBy");
        query.getResultTransformationSpec().add("OTName");
        query.getResultTransformationSpec().add("OTLocation");
        
        SingleSearchResponse results = sService.search(query, "");
        SResultPage srp= results.getResults();
        List<SGraph> sra = results.getResultAnalysis();
        ArrayList<Long> nodes = new ArrayList<>();
        
        if(srp != null) {
            List<SGraph> items = srp.getItem();
            List<DataBagType> types = srp.getType();
            
            if(items != null && types != null && items.size() > 0) {
                for(SGraph item : items) {
                    String extractId = extractId(item.getID());
                    nodes.add(Long.valueOf(extractId));
                }
            }
        }
       return nodes;
    }
    private String extractId(String string){
        return string.replaceAll("(.*)DataId=", "").replaceAll("&(.*)", "");
    }
    class AdvancedNode {
        private Node node;
        private String mandant, bPartner, type, costCenter;
        private boolean isPharmacy;

        public boolean isIsPharmacy() {
            return isPharmacy;
        }

        public void setIsPharmacy(boolean isPharmacy) {
            this.isPharmacy = isPharmacy;
        }
        
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCostCenter() {
            return costCenter;
        }

        public void setCostCenter(String costCenter) {
            this.costCenter = costCenter;
        }

        public Node getNode() {
            return node;
        }

        public void setNode(Node node) {
            this.node = node;
        }

        public String getMandant() {
            return mandant;
        }

        public void setMandant(String mandant) {
            this.mandant = mandant;
        }

        public String getbPartner() {
            return bPartner;
        }

        public void setbPartner(String bPartner) {
            this.bPartner = bPartner;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 11 * hash + Objects.hashCode(this.mandant);
            hash = 11 * hash + Objects.hashCode(this.bPartner);
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
            final AdvancedNode other = (AdvancedNode) obj;
            if (!Objects.equals(this.mandant, other.mandant)) {
                return false;
            }
            if (!Objects.equals(this.bPartner, other.bPartner)) {
                return false;
            }
            return true;
        }
        
    }
}
