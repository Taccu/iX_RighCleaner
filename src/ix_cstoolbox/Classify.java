/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

import com.opentext.livelink.service.classifications.Classifications;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Node;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bho
 */
public class Classify extends ContentServerTask{
    private final Long idFrom;
    private final ArrayList<Long> classifyIds;
    
    public Classify(Logger logger, String user, String password, Long idFrom, ArrayList<Long> classifyIds, boolean export) {
        super(logger, user , password, export);
        this.idFrom = idFrom;
        this.classifyIds = classifyIds;
    }
    
    public String getNameOfTask(){
        return "Classify";
        
    }
    
    public void doWork() {
        Classifications classify  = getClassifyClient();
        DocumentManagement docManClient = getDocManClient();
        Node node = docManClient.getNode(idFrom);
        logger.debug("Found folder " + node.getName() + "(id:" + node.getID() + ")");
        if(!node.isIsContainer()) handleError(new Exception("Folder was not a container"));
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(200);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodesInContainer = docManClient.getNodesInContainer(idFrom, options);
        logger.debug("Found " + nodesInContainer.size() + " objects in " + node.getName() + "(id:" + node.getID() + ")");
        for(Node child : nodesInContainer) {
            logger.info("Removing classifications from "  + child.getName() + "(id:" + child.getID() + ")");
            classify.removeClassifications(child.getID(), classifyIds);
            exportIds.add(child.getID());
        }
        logger.info("Processed " +exportIds.size() + " objects");
    }
}
