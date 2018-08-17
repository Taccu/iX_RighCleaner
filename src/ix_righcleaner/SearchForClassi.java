/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.classifications.ClassificationInfo;
import com.opentext.livelink.service.classifications.Classifications;
import com.opentext.livelink.service.classifications.ManagedTypeInfo;
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
        SearchService searchClient = getSearchClient();
        logger.info("Searching for Nodes");
        //List<Long> documentIds = getNodesBySearch(searchClient);
        DocumentManagement docManClient = getDocManClient();
        Classifications classifyClient = getClassifyClient();
        ArrayList<Long> classIds = new ArrayList<>();
        classIds.add(lookForClassId);
        List<ClassificationInfo> itemClassifications = classifyClient.getItemClassifications(lookForClassId, false);
        for(ClassificationInfo classification : itemClassifications) {
            Node myNode = classification.getMyNode();
            if(myNode.getParentID()==75014l)logger.debug(myNode.getName() + "(id:" + myNode.getID() + ")");
        }
        //classifyClient.unClassify(lookForClassId);
        /*for(long id : documentIds) {
            Node currentNode = docManClient.getNode(id);
            List<ClassificationInfo> itemClassifications = classifyClient.getItemClassifications(id, export);
            
            for(ClassificationInfo classification : itemClassifications) {
                logger.debug("Status:"+classification.getStatus()+"Selectable:"+classification.isSelectable());
            }
        }*/
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
