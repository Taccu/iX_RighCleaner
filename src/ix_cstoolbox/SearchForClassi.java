/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

import com.opentext.livelink.service.classifications.ClassificationInfo;
import com.opentext.livelink.service.classifications.Classifications;
import com.opentext.livelink.service.classifications.ManagedTypeInfo;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRights;
import com.opentext.livelink.service.searchservices.DataBagType;
import com.opentext.livelink.service.searchservices.SGraph;
import com.opentext.livelink.service.searchservices.SResultPage;
import com.opentext.livelink.service.searchservices.SearchService;
import com.opentext.livelink.service.searchservices.SingleSearchRequest;
import com.opentext.livelink.service.searchservices.SingleSearchResponse;
import static ix_cstoolbox.ContentServerTask.SEARCH_API;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author bho
 */
public class SearchForClassi extends ContentServerTask{
    private final long lookForClassId;
    public SearchForClassi(Logger logger, String user, String password, long lookForClassId, boolean export) {
        super(logger, user, password, export);
        this.lookForClassId = lookForClassId;
    }
    
    public String getNameOfTask() {
        return "Search-For-Classifications";
    }
    
    public void doWork() {
        /*SearchService searchClient = getSearchClient();
        logger.info("Searching for Nodes");
        List<Long> documentIds = getNodesBySearch(searchClient);
        DocumentManagement docManClient = getDocManClient();
        Classifications classifyClient = getClassifyClient();
        ArrayList<Long> classIds = new ArrayList<>();
        for(long documentId : documentIds) {
            List<ClassificationInfo> itemClassifications = classifyClient.getItemClassifications(documentId, false);
            //dokument mit nur einer Klassifikation
            if(itemClassifications.size() >0) {
                logger.info("Found document "+ documentId + "with classification " );
                exportIds.add(documentId);
            }
        }*/
        ArrayList<Long> ids = new ArrayList<>();
        try {
            connectToDatabase("scinbecmqdb01.centralinfra.net","GALCS1");
            Statement s = CONNECTION.createStatement();
            ResultSet rs = s.executeQuery("SELECT DataID " +
            "FROM csadmin.DTreeACL " +
            "WHERE RightID = 282029 " +
            "ORDER BY DataID");
            String idString;
            while(rs.next()) {
                idString = rs.getString("DataID");
                ids.add(Long.valueOf(idString));
            }
            
            DocumentManagement docManClient = getDocManClient();
            ids.stream().parallel().forEach(id -> purgeGroupOfId(id, docManClient));
            
        } catch (ClassNotFoundException | SQLException ex) {
            java.util.logging.Logger.getLogger(SearchForClassi.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            CONNECTION.close();
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(SearchForClassi.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void purgeGroupOfId(Long id, DocumentManagement docManClient) {
        try {
        
        NodeRights nodeRights = docManClient.getNodeRights(id);
        nodeRights.getACLRights()
                .stream()
                .filter(right -> { 
                    NodeRight rRight = (NodeRight) right;
                    return rRight.getRightID() == 282029l;
                })
                .peek(right -> {
                    logger.info("Processing object " + id);
                    exportIds.add(id);
                })
                .forEach(right ->  docManClient.removeNodeRight(id, right));
        }catch(Exception ex){
            logger.debug("Object with id:" + id + " is orphaned.");
        }
    }
    
    public List<Long> getNodesBySearch(SearchService sService){
        SingleSearchRequest query = new SingleSearchRequest();
        List<String> dataCollections = sService.getDataCollections();
        query.setDataCollectionSpec("'LES Enterprise'");
        query.setQueryLanguage(SEARCH_API);
        query.setFirstResultToRetrieve(1);
        query.setNumResultsToRetrieve(50000000);
        query.setResultSetSpec("where1=(\"OTSubType\":\"144\")&lookfor1=complexquery");
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
}
