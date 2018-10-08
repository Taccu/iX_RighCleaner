/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRights;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javafx.util.Pair;

/**
 *
 * @author bho
 */
public class ControlRights extends ContentServerTask {
    List list = Collections.synchronizedList(new ArrayList<NodeRights>());
   
    public ControlRights(Logger logger, String user, String password, boolean export) {
        super(logger, user, password, export);
        
    }
    
    public String getNameOfTask() {
        return "Control-Rights";
    }
    
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(20);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodes = docManClient.getNodesInContainer(2000l, options);
        nodes
                .stream()
                .parallel()
                .filter(a -> a.isIsContainer())
                .forEach(node -> list.add(docManClient.getNodeRights(node.getID()))
                );
        
        list.forEach(a -> {
            NodeRights cRights = (NodeRights) a;
            printRights(cRights);
        });
    }
    
    void printRights(NodeRights cRights) {
        cRights.getACLRights().forEach(cRight -> {
            System.out.println("RightID:"+cRight.getRightID() + "" + cRight.getPermissions().isSeePermission());
        });
    }
    class PermGroup {
        String groupName;
        HashSet<Pair<Long, Long>> objectPerm = new HashSet<>();
        
        void addObjectPerm(Pair<Long,Long> object) {
            objectPerm.add(object);
        }
        HashSet<Pair<Long, Long>> getObjects() {
            return objectPerm;
        }
    }
    
}
