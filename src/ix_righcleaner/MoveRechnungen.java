/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.GetNodesInContainerOptions;
import com.opentext.livelink.service.docman.Node;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author bho
 */
public class MoveRechnungen extends ContentServerTask{

    private final long sourceFolderId, invoiceId, bpId;
    private final boolean inheritPermFromDest,useDestinationCategories,excludeCopies,clearClassifcations;
    private final MoveRechnungenKeeper mKeeper;
    public MoveRechnungen(Logger logger, String user, String password, long sourceFolderId, long invoiceId, boolean inheritPermFromDest, boolean useDestinationCategories, boolean excludeCopies, boolean clearClassifcations, long bpId, boolean export) {
        super(logger, user, password, export);
        this.sourceFolderId = sourceFolderId;
        this.invoiceId = invoiceId;
        this.inheritPermFromDest = inheritPermFromDest;
        this.useDestinationCategories = useDestinationCategories;
        this.excludeCopies = excludeCopies;
        this.clearClassifcations = clearClassifcations;
        this.bpId = bpId;
        mKeeper = new MoveRechnungenKeeper(exportIds,logger);
    }
    
    @Override
    public String getNameOfTask() {
        return "Move-Rechnungen";
    }
    
    @Override
    public void doWork() {
        DocumentManagement docManClient = getDocManClient();
        GetNodesInContainerOptions options = new GetNodesInContainerOptions();
        options.setMaxDepth(Integer.MAX_VALUE);
        options.setMaxResults(Integer.MAX_VALUE);
        List<Node> nodesInSourceFolder = docManClient.getNodesInContainer(sourceFolderId, options);
        for(Node node : nodesInSourceFolder) {
            //Node node, DocumentManagement docManClient, Classifications classifyClient, boolean inheritPermFromDest, boolean useDestinationCategories, boolean excludeCopies, boolean clearClassifcations, long invoiceId, long bpId, Logger logger
            try {
                if(!node.isIsContainer())
                {
                    logger.info("Queueing " + node.getName() + "(id:" + node.getID() + ") for move");
                    MoveNode move_1  = new MoveNode(node, getDocManClient(),getClassifyClient(), inheritPermFromDest, useDestinationCategories, excludeCopies, clearClassifcations, invoiceId, bpId, logger);
                    mKeeper.addNewTask(move_1);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("testtesttewasdaskphdasjbndlaskndas");
        while(!MoveRechnungenKeeper.FUTURES.isEmpty()) {
            try {
                Thread.sleep(10000l);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            logger.info("Waiting for threads....");
        }
    }
}
