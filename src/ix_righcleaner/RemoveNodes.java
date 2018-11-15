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
public class RemoveNodes extends ContentServerTask {
    List list = Collections.synchronizedList(new ArrayList<NodeRights>());
    private final ArrayList<Long> ids;
    private final boolean debug;
    public RemoveNodes(Logger logger, String user, String password,ArrayList<Long> ids,boolean debug, boolean export) {
        super(logger, user, password, export);
        this.ids = ids;
        this.debug = debug;
    }
    
    @Override
    public String getNameOfTask() {
        return "Control-Rights";
    }
    
    @Override
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        ids.parallelStream().forEach(id -> {
            logger.info("Removing node ... " + docManClient.getNode(id).getName() + "(id:" + id + ")");
            if(!debug)docManClient.deleteNode(id);
        });
    }
}
