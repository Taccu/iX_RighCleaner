/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRights;
import com.opentext.livelink.service.memberservice.Group;
import com.opentext.livelink.service.memberservice.MemberRight;
import com.opentext.livelink.service.memberservice.MemberService;
import static java.lang.Integer.MAX_VALUE;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bho
 */
public class SearchObjects extends ContentServerTask{
    private final ArrayList<String> groups;
    public SearchObjects(Logger logger, String user, String password, ArrayList<String> groups, boolean export) {
        super(logger, user ,password, export);
        this.groups = groups;
    }
    
    @Override
    public String getNameOfTask(){
        return "Search-Objects";
    }
    
    @Override
    public void doWork() {
        logger.debug("Dowork");
        DocumentManagement docManClient = getDocManClient();
        MemberService msClient = getMsClient();
        ArrayList<Long> groupIds = new ArrayList<>();
        for(String string :  groups) {
            Group groupByName = msClient.getGroupByName(string);
            logger.debug(string + ": " + groupByName.getID());
            groupIds.add(groupByName.getID());
            List<MemberRight> listRightsByID = msClient.listRightsByID(groupByName.getID());
            for(MemberRight right : listRightsByID) {
                logger.debug("Right: " + right.getID() + "|" + right.getName());
            }
            
        }
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(40);
        options.setMaxResults(MAX_VALUE);
        List<Node> nodesInContainer = docManClient.getNodesInContainer(2000l, options);
        for(Node node : nodesInContainer) {
            NodeRights nodeRights = docManClient.getNodeRights(node.getID());
            List<NodeRight> aclRights = nodeRights.getACLRights();
            for(NodeRight right : aclRights) {
                if(groupIds.contains(right.getRightID())) {
                    logger.debug("Parent: " + docManClient.getNode(node.getParentID()).getName() + "|"+node.getName() + " contains right for group ");
                }
            }
        }
    }
}
