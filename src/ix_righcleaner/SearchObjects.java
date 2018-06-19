/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRights;
import com.opentext.livelink.service.memberservice.Group;
import com.opentext.livelink.service.memberservice.MemberRight;
import com.opentext.livelink.service.memberservice.MemberService;
import com.opentext.livelink.service.searchservices.DataBagType;
import com.opentext.livelink.service.searchservices.SGraph;
import com.opentext.livelink.service.searchservices.SNode;
import com.opentext.livelink.service.searchservices.SResultPage;
import com.opentext.livelink.service.searchservices.SearchService;
import com.opentext.livelink.service.searchservices.SingleSearchRequest;
import com.opentext.livelink.service.searchservices.SingleSearchResponse;
import static java.lang.Integer.MAX_VALUE;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bho
 */
public class SearchObjects extends ContentServerTask{
    private final ArrayList<String> groups;
    public SearchObjects(Logger logger, String user, String password, ArrayList<String> groups, boolean export) {
        super(logger, user ,password, export);
        this.groups = groups;
    }
    
    @Override
    public String getNameOfTask(){
        return "Search-Objects";
    }
    
    @Override
    public void doWork() {
        logger.debug("Dowork");
        ArrayList<Long> groupIds = new ArrayList<>();
        DocumentManagement docManClient = getDocManClient();
        MemberService msClient = getMsClient();
        SearchService sService = getSearchClient();
         System.out.println("Groups size:" + groups.size());
        for(String string :  groups) {
            Group groupByName = msClient.getGroupByName(string);
            logger.debug(string + ": " + groupByName.getID());
            groupIds.add(groupByName.getID());
            List<MemberRight> listRightsByID = msClient.listRightsByID(groupByName.getID());
            for(MemberRight right : listRightsByID) {
                logger.debug("Right: " + right.getID() + "|" + right.getName());
            }
        }
        SingleSearchRequest query = new SingleSearchRequest();
        
        List<String> dataCollections = sService.getDataCollections();
        
        String whereClause = "*";
        query.setDataCollectionSpec("'"+dataCollections.get(0)+"'");
        query.setQueryLanguage("Livelink Search API V1.1");
        query.setFirstResultToRetrieve(1);
        query.setNumResultsToRetrieve(10000);
        query.setResultSetSpec("where1="+whereClause);
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
                    System.out.println(extractId);
                    nodes.add(Long.valueOf(extractId));
                }
            }
        }
        List<Node> nodes1 = docManClient.getNodes(nodes);
        
        for(Node node : nodes1) {
            NodeRights nodeRights = docManClient.getNodeRights(node.getID());
            for(NodeRight right : nodeRights.getACLRights()) {
                if(groupIds.contains(right.getRightID())) {
                    logger.debug("Parent: " + docManClient.getNode(node.getParentID()).getName() + "|"+node.getName() + " contains right for group ");
                }
            }
        }
    }
    private String extractId(String string){
        return string.replaceAll("(.*)DataId=", "").replaceAll("&(.*)", "");
    }
}
