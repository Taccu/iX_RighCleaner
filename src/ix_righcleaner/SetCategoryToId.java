/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.core.DataValue;
import com.opentext.livelink.service.docman.AttributeGroup;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Metadata;
import com.opentext.livelink.service.docman.Node;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bho
 */
public class SetCategoryToId extends ContentServerTask{
    private final long idFrom, dummyId;
    public SetCategoryToId(Logger logger, String user, String password, long idFrom, long dummyId, boolean export){
        super(logger, user, password, export);
        this.idFrom = idFrom;
        this.dummyId = dummyId;
    }
    
    public String getNameOfTask() {
        return "SetCategoryToId";
    }
    
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        Node node = docManClient.getNode(idFrom);
        logger.info("Found folder " + node.getName() + "(id:" + node.getID() + ")");
        Node dummyNode = docManClient.getNode(dummyId);
        Metadata metaData = dummyNode.getMetadata();
        List<AttributeGroup> dummyAttributes = metaData.getAttributeGroups();
        logger.info("Using " + dummyNode.getName() + "(id:" + dummyNode.getID() + ") as dummy node.");
        ArrayList<String> catNames = new ArrayList<>();
        
        for(AttributeGroup group : dummyAttributes) {
            logger.debug("Detecting category " + group.getDisplayName() + " in dummy object");
            catNames.add(group.getDisplayName());
        }
        if(!node.isIsContainer()) handleError(new Exception("Folder was not a container"));
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(200);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodesInContainer = docManClient.getNodesInContainer(idFrom, options);
        logger.info("Found " + nodesInContainer.size() + " objects in " + node.getName() + "(id:" + node.getID() + ")");
        nodesInContainer.forEach((currentNode) -> {
            
            //Getting metadata of node
            Metadata cnMetaData = currentNode.getMetadata();
            //Getting categories
            List<AttributeGroup> cnAttributes = cnMetaData.getAttributeGroups();
            logger.debug("Building node metadata for " + currentNode.getName() + "(id:" + currentNode.getID() + ")");
            //Create new Metdata object
            Metadata newMetaData = new Metadata();
            //List of attributes for the new metadata object
            ArrayList<AttributeGroup> newAttributes  = new ArrayList<>();
            //Loop through current dummyAttributegroups and add them all          
            for(AttributeGroup dummyGroup : dummyAttributes) {
                if(dummyGroup.getDisplayName().equals("External Attributes")) {
                    logger.debug("Ignoring Attribute group External Attributes. This is intentionally.");
                    continue;
                }
                newAttributes.add(dummyGroup);         
            }
            
            //Loop through the attribute groups currently on that node
            for(AttributeGroup group : cnAttributes) {
                //this checks if the attribute on the node is not on the dummy node
                //if so adds the attribute group to the new metadata of the node
                if(!catNames.contains(group.getDisplayName())) {
                    newAttributes.add(group);
                }
            }
            //add all attribute groups to the new metadata object
            newMetaData.getAttributeGroups().addAll(newAttributes);
            
            logger.info("Adding metadata from "  + dummyNode.getName() + "(id:" + dummyNode.getID() + ") to " + currentNode.getName() + "(id:" + currentNode.getID() + ")" );
            docManClient.setNodeMetadata(currentNode.getID(), newMetaData);
            exportIds.add(currentNode.getID());
        });
        logger.info("Processed " +exportIds.size() + " objects");  
    }
}
