/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRights;
import com.opentext.livelink.service.memberservice.Member;
import com.opentext.livelink.service.memberservice.MemberService;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bho
 */
public class UpdaterDataIds extends ContentServerTask {
    private final String  group;
    private final ArrayList<Long> dataIds;
    private final boolean exportParentIds;
    private final List<Long> parentIds = new ArrayList<>();
    public UpdaterDataIds(Logger logger,String user, String password,String group, ArrayList<Long> dataIds,boolean export, boolean exportParentIds) {
        super(logger, user, password, export);
        this.group = group;
        this.dataIds = dataIds;
        this.exportParentIds = exportParentIds;
    }

    @Override
    public String getNameOfTask() {
        return "Update-Data-Ids";
    }
    
    @Override
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        MemberService msClient = getMsClient();
        for(Long dataId : dataIds) {
            Node node = docManClient.getNode(dataId);
            NodeRights nodeRights = docManClient.getNodeRights(node.getID());
            List<NodeRight> aclRights = nodeRights.getACLRights();
            logger.debug("Total right count for " +node.getName() +"(id:" + node.getID() + "):"+ aclRights.size());
            if(aclRights.isEmpty()) {
                logger.error(node.getID() + " has no rights.");
            }
            for(NodeRight right : aclRights) {
                logger.debug("Processing " +node.getName() +"(id:" + node.getID() + ")");
                Member rightOwner = msClient.getMemberById(right.getRightID());
                logger.debug("Processing item " + node.getName() + "(id:" + node.getID() + ")" + ": current right entry:" + rightOwner.getName() + "(id:" +right.getRightID() + ")");
                if(rightOwner.getName().equals(group) || group.isEmpty()) {
                    logger.info("Removing "+ rightOwner.getName() +"(id:" + right.getRightID() + ")" + " from " +node.getName() +"(id:" + node.getID() + ")");
                    docManClient.removeNodeRight(node.getID(), right);
                    exportIds.add(node.getID());
                    parentIds.add(node.getParentID());
                }
            }
        }
        setProcessedItems(exportIds.size());
        if(exportParentIds) {
            Path out = Paths.get(getNameOfTask() + "_parents.txt");
            try {
                writeArrayToPath(parentIds, out);
            } catch (IOException ex) {
                logger.error("Couldn't write " + getNameOfTask() + "_parents.txt");
                logger.error(ex.getMessage());
            }
        }
    }
}