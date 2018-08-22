/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.classifications.Classifications;
import com.opentext.livelink.service.core.StringValue;
import com.opentext.livelink.service.docman.AttributeSourceType;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.MoveOptions;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.searchservices.DataBagType;
import com.opentext.livelink.service.searchservices.SGraph;
import com.opentext.livelink.service.searchservices.SResultPage;
import com.opentext.livelink.service.searchservices.SearchService;
import com.opentext.livelink.service.searchservices.SingleSearchRequest;
import com.opentext.livelink.service.searchservices.SingleSearchResponse;
import static ix_righcleaner.ContentServerTask.SEARCH_API;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author bho
 */
public class MoveRechnungen extends ContentServerTask{

    private final long sourceFolderId, invoiceId, bpId, mandantId;
    private final boolean inheritPermFromDest,useDestinationCategories,excludeCopies,clearClassifcations;
    private static final ArrayList<AdvancedNode> B_WORKSPACES = new ArrayList<>();
    public MoveRechnungen(Logger logger, String user, String password, long sourceFolderId, long invoiceId, boolean inheritPermFromDest, boolean useDestinationCategories, boolean excludeCopies, boolean clearClassifcations, long bpId, long mandantId, boolean export) {
        super(logger, user, password, export);
        this.sourceFolderId = sourceFolderId;
        this.invoiceId = invoiceId;
        this.inheritPermFromDest = inheritPermFromDest;
        this.useDestinationCategories = useDestinationCategories;
        this.excludeCopies = excludeCopies;
        this.clearClassifcations = clearClassifcations;
        this.bpId = bpId;
        this.mandantId = mandantId;
    }

    @Override
    public String getNameOfTask() {
        return "Move-Rechnungen_" + String.valueOf(new SimpleDateFormat("yyyy-MM-dd_hhmm").format(new Date()));
    }

    @Override
    public void doWork() {
        SearchService searchClient = getSearchClient();
        try {
        getDocManClient().getNodes(getNodesBySearch(searchClient))
                .parallelStream()
                .filter(currentNode -> currentNode.getType().equals("EcmWorkspace"))
                .forEach(currentNode -> {
                    AdvancedNode workspace = new AdvancedNode();
                    workspace.setNode(currentNode);
                    currentNode
                            .getMetadata()
                            .getAttributeGroups()
                            .parallelStream()
                            .filter(group -> group.getKey().startsWith(String.valueOf(bpId)) | group.getKey().startsWith(String.valueOf(mandantId)))
                            .forEach(group -> {
                                group.getValues()
                                        .parallelStream()
                                        .filter(value -> value instanceof StringValue)
                                        .forEach(value -> {
                                            ((StringValue) value).getValues()
                                                    .forEach(attribute -> {
                                                        switch(attribute){
                                                            case "Cost Center":
                                                                workspace.setCostCenter(attribute);
                                                                break;
                                                            case "Type":
                                                                workspace.setType(attribute);
                                                                break;
                                                            case "BusinessPartnerName":
                                                                workspace.setbPartner(attribute);
                                                                break;
                                                            case "Business Partner Name":
                                                                workspace.setbPartner(attribute);
                                                                break;
                                                            case "Mandant":
                                                                workspace.setMandant(attribute);
                                                                break;
                                                            default:
                                                        }
                                                    });
                                        });
                            });
                    System.out.println(workspace.getNode().getName()+":"+workspace.getMandant());
                    B_WORKSPACES.add(workspace);
        });
        } catch(Exception ex) {ex.printStackTrace();}
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(Integer.MAX_VALUE);
        options.setMaxResults(Integer.MAX_VALUE);
        try {
        getDocManClient(true).getNodesInContainer(sourceFolderId, options)
                .parallelStream()
                .filter(node -> (!excludeCopies && node.getName().matches("(?i:.*(copy).*)")) || !node.getName().matches("(?i:.*(copy).*)"))
                .forEach(node -> {
                    boolean lookForCopyFolder;
                    Node destination = null;
                    if(!node.getName().matches("(?i:.*(copy).*)")){
                    //node is not a copy
                    lookForCopyFolder = false;
                    }
                    else if(!excludeCopies && node.getName().matches("(?i:.*(copy).*)")) {
                        //Node is a copy and we are processing copies
                        lookForCopyFolder = true;
                    } else {return;}
                    final StringProperty mandantName = new SimpleStringProperty();
                    final StringProperty bpName = new SimpleStringProperty();
                    final StringProperty kostId = new SimpleStringProperty();
                    node.getMetadata().getAttributeGroups()
                            .parallelStream()
                            .filter(group -> group.getKey().startsWith(String.valueOf(invoiceId)))
                            .forEach(group -> {
                                group.getValues().parallelStream().filter(value -> value instanceof StringValue)
                                        .forEach(value -> {
                                            ((StringValue) value).getValues()
                                                    .forEach(attribute -> {
                                                        switch(attribute){
                                                            case "Mandant":
                                                                mandantName.set(attribute);
                                                                break;
                                                            case "Kreditor":
                                                                if(!lookForCopyFolder)bpName.set(attribute);
                                                                break;
                                                            case "Freigebende Kostenstelle ID":
                                                                if(lookForCopyFolder)kostId.set(attribute);
                                                                break;
                                                            default:
                                                        }
                                                    });
                                        });
                            });
            destination = getWorkspace(getDocManClient(true), lookForCopyFolder, kostId.get(), bpName.get(), mandantName.get());
            if(destination == null) {
                if( bpName.get()!=null && !bpName.get().isEmpty())logger.warn("Couldn't find the Accounting folder in Bussiness Partner for " + node.getName() + "(id:" + node.getID() + "). Was looking in Mandant " + mandantName + " for BP " + bpName);
                if( kostId.get()!=null && !kostId.get().isEmpty())logger.warn("Couldn't find the Accounting folder in Pharmacy for " + node.getName() + "(id:" + node.getID() + "). Was looking in Mandant " + mandantName + " for PharmacyId " + kostId);
                return;
            }
            move(node, destination, getDocManClient(), getClassifyClient());
        });
        }
        catch(Exception e){e.printStackTrace();}
    }

    private Node getWorkspace(DocumentManagement docManClient, boolean lookForCopyFolder, String kostId, String bpName, String mandantName) {
        for(AdvancedNode workspace : B_WORKSPACES) {
            Node node = workspace.getNode();
            if(lookForCopyFolder) {
                if(workspace.getType()!=null && workspace.getType().equalsIgnoreCase("Pharmacy")) {
                    logger.error("1Was a pharmacy");
                    if((workspace.getCostCenter() != null && workspace.getCostCenter().equals(kostId)) && workspace.getMandant().equals(mandantName)) {
                        logger.error("2Was a pharmacy");
                        return findAccounting(docManClient, node);
                    }
                }
            }
            else {
                if((workspace.getbPartner()!=null && workspace.getbPartner().equals(bpName)) && workspace.getMandant().equals(mandantName)) {
                    return findAccounting(docManClient, node);
                }
            }
        }
        return null;
    }

    private Node findAccounting( DocumentManagement docManClient, Node node) {
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(1);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodesInBP = docManClient.getNodesInContainer(node.getID(), options);
        for(Node nodeInBP : nodesInBP) {
            if(nodeInBP.isIsContainer() && nodeInBP.getName().equals("Accounting")) {
                logger.debug("Accounting Ordner in " + node.getName() + "(id:"+node.getID() + ") gefunden");
                return nodeInBP;
            }
        }
        return null;
    }

    private void move(Node node, Node destination, DocumentManagement docManClient, Classifications classifyClient) {
        if(clearClassifcations) {
            boolean unClassify = classifyClient.unClassify(node.getID());
            if(!unClassify) {
                logger.error("Konnte die Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") nicht entfernen");
            }
            logger.info("Klassifikation von Dokument " + node.getName() + "(id:" + node.getID() + ") entfernt");
        }
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
        docManClient.moveNode(node.getID(), destination.getID(), node.getName(), moveOptions);
        exportIds.add(node.getID());
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
