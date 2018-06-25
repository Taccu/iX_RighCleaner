/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.DocumentManagement;
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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;

/**
 *
 * @author bho
 */
public class UpdateObjectsWithTemplate extends ContentServerTask{
    
    private final Long templateId;
    
    public UpdateObjectsWithTemplate(Logger logger, String user, String password, Long templateId,boolean export){
        super(logger, user, password, export);
        this.templateId = templateId;
    }
    
    public String getNameOfTask(){
        return "Update-Objects-With-Template";
    }
    
    @Override
    public void doWork(){
        DocumentManagement docManClient = getDocManClient();
        SearchService sService = getSearchClient();
        Node templateNode = docManClient.getNode(templateId);
        ArrayList<NodeItem> foundMatchingNodes = new ArrayList<>();
        ArrayList<Node> toBeProcessed = new ArrayList<>();
        logger.info("Searching for Nodes");
        List<Long> workspaceIds = getNodesBySearch(sService);
        List<Node> workspaces = docManClient.getNodes(workspaceIds);
        logger.info("Found " + workspaces.size() + " workspaces in total");
        for(Node bWorkspace : workspaces) {
            bWorkspace.getMetadata().getAttributeGroups().forEach((attribute ) -> {
                logger.debug("Name:" + bWorkspace.getName()+"|Metadata:" + attribute.getDisplayName());
                if(templateNode.getName().equals(attribute.getDisplayName())) {
                    Node parentNode = docManClient.getNode(bWorkspace.getParentID());
                    logger.info("Found node with match:" + bWorkspace.getName() + "(id:" + bWorkspace.getID() + ") Parent is:" + parentNode.getName() + "(id:" + parentNode.getID() + ")");
                    //applyRights(docManClient, templateNode, bWorkspace);
                    NodeItem item = new NodeItem(bWorkspace.getName(), false);
                    item.setNode(bWorkspace);
                    foundMatchingNodes.add(item);
                }
            });
        }
        final FutureTask query = new FutureTask(new Callable() {
            @Override
            public ArrayList<Node> call ()throws Exception {
                
                Dialog<ArrayList<NodeItem>> dialog = new Dialog<>();
                dialog.setTitle("Select Dialog");
                dialog.setHeaderText("Wähle die falschen Workspaces ab :( Sorry");
                ButtonType confirm  = new ButtonType("Confirm" , ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(confirm, ButtonType.CANCEL);
                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(20, 150, 10, 10));
                ListView<NodeItem> listView = new ListView<>();

                listView.getItems().addAll(foundMatchingNodes);
                listView.setCellFactory(CheckBoxListCell.forListView(NodeItem::onProperty));

                dialog.getDialogPane().setContent(listView);

                dialog.setResultConverter(dialogButton -> {
                    if(dialogButton == confirm) {
                        FilteredList<NodeItem> filtered = listView.getItems().filtered((item) -> {
                            return item.isOn();
                        });
                        ArrayList<NodeItem> returns = new ArrayList<>();
                        returns.addAll(filtered);
                        return returns;
                    }
                    return null;
                });
                listView.requestFocus();
                Optional<ArrayList<NodeItem>> result = dialog.showAndWait();

                result.ifPresent(list -> {
                    for(NodeItem item : list) {
                        System.out.println(item.node);
                        toBeProcessed.add(item.origNode);
                    }
                });
                return toBeProcessed;
            }
            });
        Platform.runLater(query);
        ArrayList<Node> get = new ArrayList<>();
        try {
            get= (ArrayList<Node>) query.get();
        } catch (InterruptedException | ExecutionException ex) {
            handleError(ex);
        }
        for(Node node : get) {
            applyRights(docManClient, templateNode, node);
            exportIds.add(node.getID());
            //logger.info("Setting node rights from node " + templateNode.getName() + "(id:" + templateNode.getID() +")" + " to node " + node.getName() + "(id:" + node.getID() + ")");
        }
    }
    
    private void applyRights(DocumentManagement docManClient, Node from, Node to) {
        logger.info("Setting node rights from node " + from.getName() + "(id:" + from.getID() +")" + " to node " + to.getName() + "(id:" + to.getID() + ")");
        docManClient.setNodeRights(to.getID(), docManClient.getNodeRights(from.getID()));
        
    }
    
    public List<Long> getNodesBySearch(SearchService sService){
        SingleSearchRequest query = new SingleSearchRequest();
        List<String> dataCollections = sService.getDataCollections();
        String regionName = "OTSubType";
        String value = "848";
        System.out.println(dataCollections.get(dataCollections.size()-1));
        query.setDataCollectionSpec("'LES Enterprise'");
        query.setQueryLanguage(SEARCH_API);
        query.setFirstResultToRetrieve(1);
        query.setNumResultsToRetrieve(10000);
        query.setResultSetSpec("where1=(\"OTSubType\":\"848\")&lookfor1=complexquery");
        query.setResultOrderSpec("sortByRegion=OTCreatedBy");
        query.getResultTransformationSpec().add("OTName");
        query.getResultTransformationSpec().add("OTLocation");
        
        SingleSearchResponse results = sService.search(query, "");
        System.out.println(results.getResults().getItem().size());
        SResultPage srp= results.getResults();
        List<SGraph> sra = results.getResultAnalysis();
        System.out.println("where1=(\"OTSubType\":\"848\")");
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
    class NodeItem {
        private final StringProperty node  = new SimpleStringProperty();
        private final BooleanProperty on = new SimpleBooleanProperty();
        private  Node origNode;
    public NodeItem(String node, boolean on) {
            setName(node);
            setOn(on);
        }
        
        public void setNode(Node node) {
            this.origNode = node;
        }
    
        public Node getNode() {
            return this.origNode;
        }
        public final StringProperty nameProperty() {
            return this.node;
        }

        public final String getName() {
            return this.nameProperty().get();
        }

        public final void setName(final String name) {
            this.nameProperty().set(name);
        }

        public final BooleanProperty onProperty() {
            return this.on;
        }

        public final boolean isOn() {
            return this.onProperty().get();
        }

        public final void setOn(final boolean on) {
            this.onProperty().set(on);
        }

        @Override
        public String toString() {
            return getName();
        }

    }
}
