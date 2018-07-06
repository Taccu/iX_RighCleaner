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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bho
 */
public class SetCategoryToId extends ContentServerTask{
    private final long idFrom, dummyId;
    private Node dummyNode;
    private List<AttributeGroup> dummyAttributes;
    private DocumentManagement docManClient;
    public SetCategoryToId(Logger logger, String user, String password, long idFrom, long dummyId, boolean export){
        super(logger, user, password, export);
        this.idFrom = idFrom;
        this.dummyId = dummyId;
    }
    
    public String getNameOfTask() {
        return "SetCategoryToId";
    }
    
    public void doWork() {
        docManClient = getDocManClient();
        Node node = docManClient.getNode(idFrom);
        logger.info("Found folder " + node.getName() + "(id:" + node.getID() + ")");
        dummyNode = docManClient.getNode(dummyId);
        Metadata metaData = dummyNode.getMetadata();
        dummyAttributes = metaData.getAttributeGroups();
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
        
        for(int index = 0; index < nodesInContainer.size(); index = index) {
            Node currentNode = nodesInContainer.get(index);
            System.out.println(currentNode.getName() + "(id:" + currentNode.getID() + ")" );
            try {
                if(processNode(currentNode, catNames))index++;
            }
            catch(Exception  ex) {
                if(ex.getMessage().contains("Your session has timed-out.")) {
                    docManClient = getDocManClient(true);
                    logger.warn("Session timed-out. Getting new login");
                    logger.warn("Retrying "+ currentNode.getName() + "(id:" + currentNode.getID() + ")" );
                }
            }
        }
        logger.info("Processed " +exportIds.size() + " objects");  
    }
    private boolean processNode(Node currentNode, ArrayList<String> catNames){
        //Wenn noch nicht verarbeitet
        if(!exportIds.contains(currentNode.getID())) {
            //Getting metadata of node
            Metadata cnMetaData = currentNode.getMetadata();
            //Getting categories
            List<AttributeGroup> cnAttributes = cnMetaData.getAttributeGroups();
            logger.debug("Building node metadata for " + currentNode.getName() + "(id:" + currentNode.getID() + ")");
            //Create new Metdata object
            Metadata newMetaData = new Metadata();
            //List of attributes for the new metadata object
            ArrayList<AttributeGroup> newAttributes  = new ArrayList<>();

            //Loop through the attribute groups currently on that node
            for(AttributeGroup group : cnAttributes) {
                //this checks if the attribute on the node is not on the dummy node
                //if so adds the attribute group to the new metadata of the node
                if(!catNames.contains(group.getDisplayName())) {
                    //category is only on currentObject not on dummyObject, so add the currentObject attribute to the newMetadata
                    newAttributes.add(group);
                } else {
                    //category is on currentobject and on the dummyobject, so get the category metadata from the the dummy object
                    for(AttributeGroup dummyGroup : dummyAttributes) {
                        if(dummyGroup.getDisplayName().equals("External Attributes")) {
                            logger.debug("Ignoring Attribute group External Attributes. This is intentionally.");
                            continue;
                        }
                        if(dummyGroup.getDisplayName().equals(group.getDisplayName())) {
                            logger.debug("Acquiring the metadata for " + group.getDisplayName() +" from " + dummyNode.getName() + "(id:" + dummyNode.getID() + ")");
                            newAttributes.add(dummyGroup);   
                        }      
                    }
                }
            }
            //add all attribute groups to the new metadata object
            newMetaData.getAttributeGroups().addAll(newAttributes);

            logger.info("Adding metadata from "  + dummyNode.getName() + "(id:" + dummyNode.getID() + ") to " + currentNode.getName() + "(id:" + currentNode.getID() + ")" );
            docManClient.setNodeMetadata(currentNode.getID(), newMetaData);
            exportIds.add(currentNode.getID());
        } else {
            logger.warn("Tried node a second time "+ currentNode.getName() + "(id:" + currentNode.getID() + ")" );
        }
        return true;
    }
}