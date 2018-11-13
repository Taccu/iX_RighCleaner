/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRights;
import java.util.ArrayList;

/**
 *
 * @author bho
 */
public class RemoveOwnerPermissions extends ContentServerTask{
    final boolean debug;
    final ArrayList<Long> ids;
    public RemoveOwnerPermissions(Logger logger, String user, String password, ArrayList<Long> ids, boolean debug, boolean export) {
        super(logger, user, password, export);
        this.debug = debug;
        this.ids = ids;
    }
    
    @Override
    public String getNameOfTask() {
        return "Remove-Owner-Permissions";
    }
    
    @Override
    public void doWork() {
        ids.parallelStream().forEach(nodeId -> {
            DocumentManagement docManClient = getDocManClient();
            NodeRights nodeRights = docManClient.getNodeRights(nodeId);
            nodeRights.setOwnerRight(docManClient.getNodeRights(14702340l).getOwnerRight());
            docManClient.setNodeRights(nodeId,nodeRights);
        });
    }
}
