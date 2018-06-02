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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author bho
 */
public class RightVererber extends ContentServerTask{
    private final Logger logger;
    private final List<Long> folderIds;
    private final boolean export;
    private final List<Long> updatedIds = new ArrayList<>();
    private int numItems = 0;
    public RightVererber(Logger logger, String user, String password,List<Long> folderIds, Boolean export) {
        super(logger, user, password);
        this.logger = logger;
        this.folderIds = folderIds;
        this.export = export;
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
            if(node.getType().equals("Folder")) {
                logger.debug("Found folder " + node.getName() + "(id:" + node.getID() + ")");
                NodeRights nodeRights = docManClient.getNodeRights(id);
                List<NodeRight> aclRights = nodeRights.getACLRights();
                logger.debug("Applying rights to childs of folder " + node.getName() + "(id:" + node.getID() + ")...");
                ChunkedOperationContext updateNodeRightsContext = docManClient.updateNodeRightsContext(id, RightOperation.ADD_REPLACE, aclRights, RightPropagation.CHILDREN_ONLY);
                NodeRightUpdateInfo chunkIt = chunkIt(docManClient.updateNodeRights(updateNodeRightsContext));
                numItems += chunkIt.getNodeCount();
                logger.info("Sucessfully applied rights to " + numItems + " child objects of folder " + node.getName() + "(id:" + node.getID() + ")");
                updatedIds.add(node.getID());
            } else {
                logger.warn(node.getName() + "(id:" + node.getID() + ") is not a folder");
            }
        }
        setProcessedItems(updatedIds.size());
        if(export) {
            Path out = Paths.get(getNameOfTask()+".txt");
            try {
                writeArrayToPath(updatedIds, out);
            } catch (IOException ex) {
                logger.error("Couldn't write " + getNameOfTask() + ".txt" );
                logger.error(ex.getMessage());
            }
        }
    }
    
    private NodeRightUpdateInfo chunkIt(NodeRightUpdateInfo nrui){
        if(nrui.getTotalNodeCount() > 0 || nrui.getSkippedNodeCount() != nrui.getTotalNodeCount()) {
            logger.debug("Updated " + nrui.getNodeCount() + " items...");
            numItems += nrui.getNodeCount();
            DocumentManagement docManClient = getDocManClient();
            ChunkedOperationContext context = nrui.getContext();
            context.setChunkSize(200);
            chunkIt(docManClient.updateNodeRights(context));
        }
            return nrui;
    }
}
