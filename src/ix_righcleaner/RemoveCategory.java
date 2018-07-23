/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.AttributeGroup;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Metadata;
import com.opentext.livelink.service.docman.Node;
import java.util.List;

/**
 *
 * @author bho
 */
public class RemoveCategory extends ContentServerTask {
 
    private final long idToRemove, hasId, baseFolder;
    /**
     *
     * @param logger
     * @param user
     * @param password
     * @param export
     */
    public RemoveCategory(Logger logger, String user, String password, long baseFolder, long hasId, long idToRemove, boolean export) {
        super(logger, user, password, export);
        this.hasId = hasId;
        this.idToRemove = idToRemove;
        this.baseFolder = baseFolder;
    }
    
    public String getNameOfTask() {
        return "Remove-Category";
    }
    
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(MAX_PRIORITY);
        options.setMaxResults(MAX_PRIORITY);
        List<Node> nodes = docManClient.getNodesInContainer(baseFolder, options);
        
        for(Node node : nodes) {
            if(node.getName().matches(".*([.])tif.*$")) {
                logger.debug("Processing " + node.getName() + "(id:" + node.getID() + ")...");
                Metadata metadata = processMetadata(node,node.getMetadata());
                docManClient.setNodeMetadata( node.getID(), metadata);
            }
        }      
    }
    private Metadata processMetadata(Node node, Metadata metadata) {
        boolean nodeHasId = false;
        boolean nodeHasIdToRemove = false;
        AttributeGroup removeGroup = null;
        for(AttributeGroup group :  metadata.getAttributeGroups()) {
            if(group.getKey().matches(hasId + ".*")) {
                nodeHasId = true;
            }
            if(group.getKey().matches(idToRemove + ".*")) {
                nodeHasIdToRemove = true;
                removeGroup = group;
            }
        }
        if(nodeHasId && nodeHasIdToRemove) {
            logger.debug("Removing " + removeGroup.getDisplayName() + " from " +  node.getName() + "(id:" + node.getID() + ")");
            metadata.getAttributeGroups().remove(removeGroup);
        }
        if(!nodeHasId && nodeHasIdToRemove) {
            logger.warn("Found node "  +  node.getName() + "(id:" + node.getID() + ")" + " with only category id " + idToRemove);
        }
        return metadata;
    }
}
