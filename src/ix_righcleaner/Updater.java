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
import com.opentext.livelink.service.memberservice.Member;
import com.opentext.livelink.service.memberservice.MemberService;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.apache.commons.collections4.ListUtils.partition;

/**
 *
 * @author bho
 */
public class Updater extends ContentServerTask{
    
    private final Logger logger;
    private final String group;
    private final Integer items, depth, partitonSize;
    private final ArrayList<Long> folderIds;
    private ExecutorService executor;
    private final boolean export;
    private final List<Long> updatedIds = new ArrayList<>();
    
    public Updater(Logger logger,String user, String password, Integer items, Integer depth,Integer partitonSize,String group, ArrayList<Long> folderIds, Boolean export) {
        super(logger, user , password);
        this.logger = logger;
        this.group = group;
        this.folderIds = folderIds;
        this.items = items;
        this.depth = depth;
        this.partitonSize = partitonSize;
        this.export = export;
    }
    
    @Override
    public String getNameOfTask() {
        return "Update-with-folder";
    }
    
    @Override
    public void doWork(){
        DocumentManagement docManClient = getDocManClient();

        //Base Node
        for(Long baseId : folderIds) {
            Node baseNode = docManClient.getNode(baseId);
            logger.debug("Found basenode:" +baseNode.getName() +"(id:" + baseNode.getID() + ")");
            //Rest of children
            logger.info("Calculating number of items...");
            GetNodesInContainerOptions options;
            options = new GetNodesInContainerOptions();
            options.setMaxDepth(depth);
            options.setMaxResults(items);
            List<Node> nodesInContainer = docManClient.getNodesInContainer(baseId, options);
            logger.debug("Total nodes:" + nodesInContainer.size());

            List<List<Node>> partitions = partition(nodesInContainer, partitonSize);
            executor = Executors.newFixedThreadPool(10);
            CompletionService<List<Long>>  completionService =
                    new ExecutorCompletionService<>(executor);
            for(List<Node> partition : partitions) {
                logger.info("Creating new parition thread with " +partition.size());
                completionService.submit(new UpdateNodes(partition));
            }

            int received = 0;
            boolean erros = false;
            while(received < partitions.size() && !erros) {
                try {
                    Future<List<Long>> resultFuture = completionService.take();
                    updatedIds.addAll(resultFuture.get());
                    received ++;
                } catch(Exception e) {
                    logger.error(e.getMessage());
                    erros = true;
                    logger.debug("Interrupting thread");
                    Thread.currentThread().interrupt();
                }
            }
        }
        executor.shutdown();
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
    class UpdateNodes implements Callable<List<Long>>{
        private final List<Node> partition;
        private final List<Long> processedIds = new ArrayList<>();
        
        public UpdateNodes(List<Node> partition ) {
            this.partition = partition;
            
        }

        @Override
        public List<Long> call() {
            // Create the DocumentManagement service client
            DocumentManagement docManClient = getDocManClient();
            MemberService msClient = getMsClient();
            long startTime = System.currentTimeMillis();

            for(Node node : partition)  {
                if(node == null) {
                    logger.error("node was null");
                    continue;
                }
                logger.debug("Processing " + node.getName() + "...");
                NodeRights nodeRights = docManClient.getNodeRights(node.getID());
                List<NodeRight> aclRights = nodeRights.getACLRights();
                logger.debug("Total right count for " +node.getName() +"(id:" + node.getID() + "):"+ aclRights.size());
                if(aclRights.size() > 0) {
                    logger.debug("Processing " +node.getName() +"(id:" + node.getID() + ")");
                    for(NodeRight right : aclRights) {
                        Member rightOwner = msClient.getMemberById(right.getRightID());
                        logger.debug("Processing item " + node.getName() + "(id:" + node.getID() + ")" + ": current right entry:" + rightOwner.getName() + "(id:" +right.getRightID() + ")");
                        if(group.isEmpty()) {
                            logger.info("Removing "+ rightOwner.getName() +"(id:" + right.getRightID() + ")" + " from " +node.getName() +"(id:" + node.getID() + ")");
                            docManClient.removeNodeRight(node.getID(), right);
                            processedIds.add(node.getID());
                        } else {
                            if(rightOwner.getName().equals(group)) {
                                logger.info("Removing "+ rightOwner.getName() +"(id:" + right.getRightID() + ")" + " from " +node.getName() +"(id:" + node.getID() + ")");

                                docManClient.removeNodeRight(node.getID(), right);
                                processedIds.add(node.getID());
                            }   
                        }
                    }
                } else {
                    logger.error(node.getID() + " has no rights.");
                }    
            }
            //don't
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            logger.info("Partition has updated items in basefolder "+ folderIds.get(0) + "in " + elapsedTime + " milliseconds...");
            return processedIds;
        }
    }
}
