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
    
    public MoveRechnungen(Logger logger, String user, String password, long sourceFolderId, long invoiceId, boolean inheritPermFromDest, boolean useDestinationCategories, boolean excludeCopies, boolean clearClassifcations, long bpId, long mandantId,boolean debug, boolean export) {
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
    }
    
    @Override
    public String getNameOfTask() {
        return "Move-Rechnungen";
    }
    
    @Override
    public void doWork() {
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(Integer.MAX_VALUE);
        options.setMaxResults(Integer.MAX_VALUE);
        //Alle Belege in dem Ordner
        List<Node> nodesInSourceFolder = getDocManClient().getNodesInContainer(sourceFolderId, options);
        SearchService searchClient = getSearchClient();
        //Alle Business Workspaces
        List<Node> nodes = getDocManClient().getNodes(getNodesBySearch(searchClient));
        
        nodes.stream().filter(node -> node.getType().equals("EcmWorkspace"))
            .forEach( node -> {
                double random = Math.random();
                DocumentManagement docManClient;
                if(random < 0.85) {
                    docManClient = getDocManClient();
                }
                else {
                    docManClient = getDocManClient(true);
                }
                AdvancedNode workspace = new AdvancedNode();
                workspace.setNode(node);
                node.getMetadata().getAttributeGroups().stream().peek(peeek -> {if(debug)System.out.println(peeek.getKey());})
                    .filter(group -> {
                        if(group.getKey().startsWith(String.valueOf(bpId)) || group.getKey().startsWith(String.valueOf(mandantId))) {
                            if(debug)System.out.println("Matching");
                            return true;
                        }
                        if(debug)System.out.println("Not Matching " + group.getKey() + "|" + String.valueOf(bpId)+"|"+String.valueOf(mandantId));
                        return false;
                    })
                        .peek(peeek -> {if(debug)System.out.println(peeek.getKey());})
                        .forEach(group -> group.getValues().stream()
                        .forEach(str_value -> {
                            StringValue str_Value = (StringValue)str_value;
                            str_Value.getValues().stream().forEach(string -> {
                                switch(string){
                                    case "Cost Center":
                                        if(debug)System.out.println("Setting Cost Center to " + string +" at " + node.getName());
                                        workspace.setCostCenter(string);
                                        break;
                                    case "Type":
                                        if(debug)System.out.println("Setting Type to " + string +" at " + node.getName());
                                        workspace.setType(string);
                                        break;
                                    case "BusinessPartnerID":
                                        if(debug)System.out.println("Setting BusinessPartnerID to " + string +" at " + node.getName());
                                        workspace.setbPartner(string);
                                        break;
                                    case "Business Partner ID":
                                        if(debug)System.out.println("Setting Business Partner ID to " + string +" at " + node.getName());
                                        workspace.setbPartner(string);
                                        break;
                                    case "Mandant":
                                        if(debug)System.out.println("Setting Mandant to " + string +" at " + node.getName());
                                        workspace.setMandant(string);
                                        break;
                                    default:
                                }
                            });
                        }));
                if(debug)logger.debug("Adding " + workspace.getNode().getName() + ":"+ workspace.getType() +":" + workspace.getCostCenter() + ":" + workspace.getMandant() + ":" + workspace.getbPartner());
                B_WORKSPACES.add(workspace);
            });
        oldMove(nodesInSourceFolder, getDocManClient());
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
    
    private void oldMove(List<Node> nodesInSourceFolder, DocumentManagement docManClient) {
        int i = 0;
        for(Node node : nodesInSourceFolder) {
            i++;
            if(i%10==0)docManClient = getDocManClient(true);
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
            destination = getWorkspace(docManClient, lookForCopyFolder, kostId, bpName, mandantName);
            if(destination == null) {
                if(!bpName.isEmpty())logger.warn("Couldn't find the Accounting folder in Bussiness Partner for " + node.getName() + "(id:" + node.getID() + "). Was looking in Mandant " + mandantName + " for BP " + bpName);
                if(!kostId.isEmpty())logger.warn("Couldn't find the Accounting folder in Pharmacy for " + node.getName() + "(id:" + node.getID() + "). Was looking in Mandant " + mandantName + " for PharmacyId " + kostId);
                continue;
            }
            move(node, destination, docManClient, getClassifyClient());
        }
    }
    
    
    private Node getWorkspace(DocumentManagement docManClient, boolean lookForCopyFolder, String kostId, String bpName, String mandantName) {
        for(AdvancedNode workspace : B_WORKSPACES) {
            Node node = workspace.getNode();
                if(lookForCopyFolder) {
                    if(workspace.getType()!=null && workspace.getType().equalsIgnoreCase("Pharmacy")) {
                        if((workspace.getCostCenter() != null && workspace.getCostCenter().equals(kostId)) && workspace.getMandant().equals(mandantName)) {
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
    //Findet den Accounting Ordner im BW
    private Node findAccounting( DocumentManagement docManClient, Node node) {
        Node accounting = docManClient.getNodeByName(node.getID(), "Accounting");
        if(accounting != null) {
            logger.debug("Accounting Ordner in " + node.getName() + "(id:"+node.getID() + ") gefunden");
            return accounting;
        }
        return null;
    }
    // Verschiebt den Knoten "node" nach "destination"
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
