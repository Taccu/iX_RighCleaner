/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Metadata;
import com.opentext.livelink.service.docman.Node;
import java.util.List;

/**
 *
 * @author bho
 */
public class SetCategoryToId extends ContentServerTask{
    private final long idFrom;
    public SetCategoryToId(Logger logger, String user, String password, long idFrom, boolean export){
        super(logger, user, password, export);
        this.idFrom = idFrom;
    }
    
    public String getNameOfTask() {
        return "SetCategoryToId";
    }
    
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        Node node = docManClient.getNode(idFrom);
        logger.debug("Found folder " + node.getName() + "(id:" + node.getID() + ")");
        if(!node.isIsContainer()) handleError(new Exception("Folder was not a container"));
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(200);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodesInContainer = docManClient.getNodesInContainer(idFrom, options);
        logger.debug("Found " + nodesInContainer.size() + " objects in " + node.getName() + "(id:" + node.getID() + ")");
        nodesInContainer.forEach((object) -> {
            logger.info("Setting metadata from "  + node.getName() + "(id:" + node.getID() + ") to " + object.getName() + "(id:" + object.getID() + ")" );
            docManClient.setNodeMetadata(object.getID(), node.getMetadata());
            exportIds.add(object.getID());
        });
        logger.info("Processed " +exportIds.size() + " objects");  
    }
}
