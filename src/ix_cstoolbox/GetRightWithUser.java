/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodePermissions;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRights;
import com.opentext.livelink.service.memberservice.MemberService;
import java.util.List;

/**
 *
 * @author bho
 */
public class GetRightWithUser extends ContentServerTask{
    private Logger logger;
    private Long userId, baseFolderId;
    public GetRightWithUser(Logger logger, String user, String password, Long userId, Long baseFolderId, boolean export){
        super(logger,user,password, export);
        this.logger = logger;
        this.userId = userId;
        this.baseFolderId = baseFolderId;
    }
    
    @Override
    public String getNameOfTask() {
        return "Get-Right-with-user";
    }
    
    public String permissionToString(NodePermissions perm){
        String rights = "";
        if(perm.isSeePermission()) rights = rights.concat("1"); else rights = rights.concat("0");
        if(perm.isSeeContentsPermission()) rights = rights.concat("1"); else rights = rights.concat("0");
        if(perm.isModifyPermission()) rights = rights.concat("1"); else rights = rights.concat("0");
        if(perm.isEditAttributesPermission()) rights = rights.concat("1"); else rights = rights.concat("0");
        if(perm.isReservePermission()) rights = rights.concat("1"); else rights = rights.concat("0");
        if(perm.isDeleteVersionsPermission()) rights = rights.concat("1"); else rights = rights.concat("0");
        if(perm.isDeletePermission()) rights = rights.concat("1"); else rights = rights.concat("0");
        if(perm.isEditPermissionsPermission()) rights = rights.concat("1"); else rights = rights.concat("0");
        return rights;
    }

    @Override
    public void doWork(){
        DocumentManagement docManClient = getDocManClient();
        MemberService msClient = getMsClient();
        Node node = docManClient.getNode(baseFolderId);
        NodeRights nodeRights = docManClient.getNodeRights(node.getID());
        List<NodeRight> aclRights = nodeRights.getACLRights();
        for(NodeRight right : aclRights) {
            if(right.getRightID() == userId) {
                //Is permission from user
                logger.info("Right found for " + msClient.getMemberById(right.getRightID()).getName() + ": " + permissionToString(right.getPermissions()));
            }
        }
    }
    public Node permNode(Node node){
        if(node.getID() == 2000)return node;
        DocumentManagement docManClient = getDocManClient();
        MemberService msClient = getMsClient();
        NodeRights nodeRights = docManClient.getNodeRights(node.getID());
        List<NodeRight> aclRights = nodeRights.getACLRights();
        for(NodeRight right : aclRights) {
            if(right.getRightID() == userId) {
                //Is permission from user
                logger.info("Right found for " + msClient.getMemberById(right.getRightID()).getName() + ": " + permissionToString(right.getPermissions()));
            }
        }
        return permNode(docManClient.getNode(node.getParentID()));
    }
}
