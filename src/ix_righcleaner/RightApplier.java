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
import com.opentext.livelink.service.memberservice.MemberService;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bho
 */
public class RightApplier extends ContentServerTask{
    private final ArrayList<String> folderIds;
    private final Long nodeToCopyFrom;
    private final boolean inheritToChilds;
    public RightApplier(Logger logger, String user, String password, ArrayList<String> folderIds, Long nodeToCopyFrom, boolean inheritToChilds, boolean export) {
        super(logger,user, password, export);
        this.folderIds = folderIds;
        this.nodeToCopyFrom = nodeToCopyFrom;
        this.inheritToChilds = inheritToChilds;
    }
    @Override
    public String getNameOfTask(){
        return "Right-Applier";
    }
    @Override
    public void doWork(){
        DocumentManagement docClient = getDocManClient();
        MemberService msClient = getMsClient();
        Node nNodeToCopyFrom = docClient.getNode(nodeToCopyFrom);
        NodeRights baseNodeRights = docClient.getNodeRights(nodeToCopyFrom);
        folderIds.forEach(id -> {
            logger.info("Applying Node Permissions from " + nNodeToCopyFrom.getName() + " to Folder ID:"+id);
            docClient.setNodeRights(Long.valueOf(id), baseNodeRights);
            if(inheritToChilds) {
                NodeRights nodeRights = docClient.getNodeRights(Long.valueOf(id));
                exportIds.add(Long.valueOf(id));
                List<NodeRight> aclRights = nodeRights.getACLRights();
                ChunkedOperationContext updateNodeRightsContext = docClient.updateNodeRightsContext(Long.valueOf(id), RightOperation.ADD_REPLACE, aclRights, RightPropagation.CHILDREN_ONLY);
                NodeRightUpdateInfo chunkIt = chunkIt(docClient.updateNodeRights(updateNodeRightsContext),updateNodeRightsContext);
            }
        });
    }
}
