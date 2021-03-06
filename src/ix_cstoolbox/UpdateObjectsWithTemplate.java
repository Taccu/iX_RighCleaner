/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

import com.opentext.livelink.service.core.ChunkedOperationContext;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRightUpdateInfo;
import com.opentext.livelink.service.docman.RightOperation;
import com.opentext.livelink.service.docman.RightPropagation;
import com.opentext.livelink.service.searchservices.DataBagType;
import com.opentext.livelink.service.searchservices.SGraph;
import com.opentext.livelink.service.searchservices.SResultPage;
import com.opentext.livelink.service.searchservices.SearchService;
import com.opentext.livelink.service.searchservices.SingleSearchRequest;
import com.opentext.livelink.service.searchservices.SingleSearchResponse;
import static ix_cstoolbox.ContentServerTask.SEARCH_API;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author bho
 */
public class UpdateObjectsWithTemplate extends ContentServerTask{
    
    private final Long templateId;
    private final boolean processSubItems;
    private final String dbServer, dbName;
    private final String select = "SELECT ExtendedData from csadmin.DTreeCore where SubType = 848 and DataID = ?;";
    public UpdateObjectsWithTemplate(Logger logger, String user, String password, Long templateId,String dbServer, String dbName,boolean processSubItems,boolean export){
        super(logger, user, password, export);
        this.templateId = templateId;
        this.dbServer = dbServer;
        this.dbName = dbName;
        this.processSubItems = processSubItems;
    }
    
    public String getNameOfTask(){
        return "Update-Objects-With-Template_" + templateId +"_" + String.valueOf(new SimpleDateFormat("yyyy-MM-dd_hhmm").format(new Date()));
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
        try {
            connectToDatabase(dbServer, dbName);
        } catch (ClassNotFoundException | SQLException ex) {
            handleError(ex);
        }
        PreparedStatement ps = null;
        try {
           ps = CONNECTION.prepareStatement(select);
        } catch (SQLException ex) {
            handleError(ex);
        }
        if(ps == null) 
        {
            handleError(new Exception("ps is null"));
        }
        for(Node bWorkspace : workspaces) {
            try {
                
                ps.setLong(1, bWorkspace.getID());
                ResultSet rs = ps.executeQuery();
                long idString = 0l;
                if(rs.next()) {
                    try {
                        String temp = rs.getString("ExtendedData").replaceAll("(.*)templateid'=", "").replaceAll(">(.*)", "");
                        System.out.println("|"+temp+"|");
                        if(checkIfLong(temp)) {
                            idString = Long.valueOf(temp);  
                        } else {
                            idString = 0l;
                            logger.warn("Node doesn't have a valid templateId " + bWorkspace.getName() + "(id:" + bWorkspace.getID() + ")");
                        }
                        
                    }catch(Exception e) {
                        logger.error(e.getMessage());
                    }
                }
                if(templateId == idString){
                    Node parentNode = docManClient.getNode(bWorkspace.getParentID());
                    logger.info("Found node with match:" + bWorkspace.getName() + "(id:" + bWorkspace.getID() + ") Parent is:" + parentNode.getName() + "(id:" + parentNode.getID() + ")");

                    NodeItem item = new NodeItem(bWorkspace.getName(), false);
                    item.setNode(bWorkspace);
                    foundMatchingNodes.add(item);
                }
        } catch (SQLException ex) {
                handleError(ex);
            }
        }
        
        ObservableList<NodeItem> data = FXCollections.observableArrayList();
        foundMatchingNodes.stream().forEach(data::add);
        FilteredList<NodeItem> filteredData = new FilteredList<>(data, s -> true);
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
                filteredData.stream().forEach((node) -> {
                    if(node.getName().matches("[A-Z]{3}[0-9]{3}$")){
                        
                    }else {
                        node.setOn(true);
                    }
                });
                Label count = new Label(String.valueOf(filteredData.size()));
                TextField filterInput = new TextField();
                filterInput.textProperty().addListener(obs->{
                    String filter = filterInput.getText(); 
                    if(filter == null || filter.length() == 0) {
                        filteredData.setPredicate(s -> true);
                    }
                    else {
                        filteredData.setPredicate(s -> s.getName().contains(filter));
                    }
                });
                ListView<NodeItem> listView = new ListView<>();

                listView.getItems().addAll(filteredData);
                
                listView.setCellFactory(CheckBoxListCell.forListView(NodeItem::onProperty));
                VBox vbox = new VBox(count,filterInput, listView);
                vbox.setPadding(new Insets(5,5,5,5));
                dialog.getDialogPane().setContent(vbox);

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
                        toBeProcessed.add(item.origNode);
                    }
                });
                return toBeProcessed;
            }
            });
        Platform.runLater(query);
        docManClient = getDocManClient(true);
        logger.info("Reauthenticating...");
        ArrayList<Node> get = new ArrayList<>();
        try {
            get= (ArrayList<Node>) query.get();
        } catch (InterruptedException | ExecutionException ex) {
            handleError(ex);
        }
        int i = 0;
        
        for(Node node : get) {
            i++;
            if((i%10) == 0) {
                docManClient = getDocManClient(true);
                logger.info("Reauthenticating...");
            }
            applyRights(docManClient, templateNode, node);
            if(processSubItems) {
                GetNodesInContainerOptions options = new GetNodesInContainerOptions();
                options.setMaxDepth(0);
                options.setMaxResults(5000000);
                List<Node> nodesInTemplate = docManClient.getNodesInContainer(templateNode.getID(), options);
                List<Node> nodesInWorkspace = docManClient.getNodesInContainer(node.getID(), options);
                for(Node nTemplate : nodesInTemplate) {
                    for(Node nWorkspace : nodesInWorkspace){
                        if(nTemplate.getName().equalsIgnoreCase(nWorkspace.getName())){
                            
                            applyRights(docManClient, nTemplate, nWorkspace);
                            inheritRights(docManClient, nWorkspace);
                        }
                        else {
                            System.out.println("Template:" + nTemplate.getName()+"|Workspace:" +nWorkspace.getName());
                        }
                    }
                }
            }
            exportIds.add(node.getID());
            //logger.info("Setting node rights from node " + templateNode.getName() + "(id:" + templateNode.getID() +")" + " to node " + node.getName() + "(id:" + node.getID() + ")");
        }
    }

    private boolean checkIfLong(String string) {
        try{
            Long.valueOf(string);
        }
        catch(Exception e) {
            return false;
        }
        return true;
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
