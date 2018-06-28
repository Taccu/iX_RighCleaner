/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.core.ChunkedOperationContext;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRightUpdateInfo;
import com.opentext.livelink.service.docman.NodeRights;
import com.opentext.livelink.service.docman.RightOperation;
import com.opentext.livelink.service.docman.RightPropagation;
import java.util.List;

/**
 *
 * @author bho
 */
public class RightVererber extends ContentServerTask{
    private final Logger logger;
    private final List<Long> folderIds;
    private int numItems = 0;
    public RightVererber(Logger logger, String user, String password,List<Long> folderIds, boolean export) {
        super(logger, user, password, export);
        this.logger = logger;
        this.folderIds = folderIds;
    }

    @Override
    public String getNameOfTask() {
        return "Rechte-Vererber";
    }
    
    /**
     *
     */
    @Override
    public void doWork() {
        //do
        DocumentManagement docManClient = getDocManClient();
        for(Long id : folderIds) {
            Node node = docManClient.getNode(id);
            logger.debug("Found folder " + node.getName() + "(id:" + node.getID() + ")");
            NodeRights nodeRights = docManClient.getNodeRights(id);
            List<NodeRight> aclRights = nodeRights.getACLRights();
            logger.debug("Applying rights to childs of folder " + node.getName() + "(id:" + node.getID() + ")...");
            ChunkedOperationContext updateNodeRightsContext = docManClient.updateNodeRightsContext(id, RightOperation.ADD_REPLACE, aclRights, RightPropagation.CHILDREN_ONLY);
            updateNodeRightsContext.setChunkSize(1);
            NodeRightUpdateInfo chunkIt = chunkIt(docManClient.updateNodeRights(updateNodeRightsContext));
            numItems += chunkIt.getNodeCount();
            logger.info("Sucessfully applied rights to " + numItems + " child objects of folder " + node.getName() + "(id:" + node.getID() + ")");
            exportIds.add(node.getID());
            
         }
        setProcessedItems(exportIds.size());
    }
    
    private NodeRightUpdateInfo chunkIt(NodeRightUpdateInfo nrui){
        if(nrui.getNodeCount() > 0 || nrui.getSkippedNodeCount() != nrui.getNodeCount()) {
            logger.debug("Updated " + nrui.getNodeCount() + " items...");
            numItems += nrui.getNodeCount();
            DocumentManagement docManClient = getDocManClient();
            ChunkedOperationContext context = nrui.getContext();
            context.setChunkSize(200);
            nrui = chunkIt(docManClient.updateNodeRights(context));
        }
            return nrui;
    }
}