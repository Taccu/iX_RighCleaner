/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

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
        options.setMaxDepth(Integer.MAX_VALUE);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodes = docManClient.getNodesInContainer(baseFolder, options);
        
        for(Node node : nodes) {
            if(node.getName().matches(".*([.])tif.*$")) {
                logger.debug("Processing " + node.getName() + "(id:" + node.getID() + ")...");
                Metadata metadata = processMetadata(node,node.getMetadata());
                for(int i = 0; i < 3; i++) {
                    if(updateMetadata(docManClient, node, metadata)) continue;
                    
                    docManClient = getDocManClient(true);
                    logger.warn("Session timed-out. Getting new login");
                    logger.warn("Retrying "+ node.getName() + "(id:" + node.getID() + ")" );
                }
            }
        }      
    }
    
    private boolean updateMetadata(DocumentManagement docManClient, Node node, Metadata metadata) {
        try {
            docManClient.setNodeMetadata( node.getID(), metadata);
            return true;
        }
        catch(Exception ex) {
            if(ex.getMessage().contains("Your session has timed-out.")) {
                return false;
            }
            else {
                handleError(ex);
            }
        }
        return false;
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
                exportIds.add(node.getID());
            }
        }
        if(nodeHasId && nodeHasIdToRemove) {
            logger.info("Removing " + removeGroup.getDisplayName() + " from " +  node.getName() + "(id:" + node.getID() + ")");
            metadata.getAttributeGroups().remove(removeGroup);
        }
        if(!nodeHasId && nodeHasIdToRemove) {
            logger.warn("Found node "  +  node.getName() + "(id:" + node.getID() + ")" + " with only category id " + idToRemove);
        }
        return metadata;
    }
}
