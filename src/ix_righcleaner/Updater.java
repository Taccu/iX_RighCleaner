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
import static ix_righcleaner.ContentServerTask.CONNECTION;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import static org.apache.commons.collections4.ListUtils.partition;

/**
 *
 * @author bho
 */
public class Updater extends ContentServerTask{
    
    private final Logger logger;
    private final String group, dbServer, dbName;
    private final Integer items, depth, partitonSize;
    private final ArrayList<Long> folderIds;
    private ExecutorService executor;
    private final String sql = "WITH DCTE AS\n" +
"(\n" +
"SELECT DataID,ParentID,Name\n" +
"FROM csadmin.DTree\n" +
"WHERE DataID = ?\n" +
"AND SubType IN (0,144,848)\n" +
"UNION ALL\n" +
"SELECT dt.DataID, dt.ParentID, dt.Name\n" +
"FROM csadmin.DTree dt\n" +
"INNER JOIN DCTE s ON dt.ParentID = s.DataID\n" +
"WHERE SubType IN (0,144, 848)\n" +
")\n" +
"SELECT DataID FROM DCTE;";
    
    public Updater(Logger logger,String user, String password, Integer items, Integer depth,Integer partitonSize,String group, ArrayList<Long> folderIds, String dbServer, String dbName, boolean export) {
        super(logger, user , password, export);
        this.logger = logger;
        this.group = group;
        this.folderIds = folderIds;
        this.items = items;
        this.depth = depth;
        this.partitonSize = partitonSize;
        this.dbServer = dbServer;
        this.dbName = dbName;
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
            //docManClient.listNodes(baseId, false);
            List<Long> nodesInContainer = newerWay(baseId);
            logger.debug("Total nodes:" + nodesInContainer.size());

            List<List<Long>> partitions = partition(nodesInContainer, partitonSize);
            executor = Executors.newFixedThreadPool(10);
            CompletionService<List<Long>>  completionService =
                    new ExecutorCompletionService<>(executor);
            for(List<Long> partition : partitions) {
                logger.info("Creating new parition thread with " +partition.size());
                completionService.submit(new UpdateNodes(partition));
            }

            int received = 0;
            boolean erros = false;
            while(received < partitions.size() && !erros) {
                try {
                    Future<List<Long>> resultFuture = completionService.take();
                    exportIds.addAll(resultFuture.get());
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
        setProcessedItems(exportIds.size());
    }

    private List<Long> newerWay(long baseId) {
        List<Long> dataIds = new ArrayList<>();
        try {
            connectToDatabase(dbServer, dbName);
        } catch (ClassNotFoundException | SQLException ex) {
            handleError(ex);
        }
        PreparedStatement ps = null;
        try {
           ps = CONNECTION.prepareStatement(sql);
        } catch (SQLException ex) {
            handleError(ex);
        }
        if(ps == null) 
        {
            handleError(new Exception("ps is null"));
        }
        
        try {
            ps.setLong(1, baseId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                dataIds.add(rs.getLong("DataID"));
            }
            
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
        }
        return dataIds;
    }
    
    private List<Node> newWay(long baseId){
        return getDocManClient().listNodes(baseId, false);
        //return getNodesBySearch(getSearchClient(), "where1=(\"OTSubType\":\"144\")&lookfor1=complexquery");
    }
    
    private List<Node> oldWay(long baseId) {
        GetNodesInContainerOptions options;
        options = new GetNodesInContainerOptions();
        options.setMaxDepth(depth);
        options.setMaxResults(items);
        return getDocManClient().getNodesInContainer(baseId, options);
    }
    
    class UpdateNodes implements Callable<List<Long>>{
        private final List<Long> partition;
        private final List<Long> processedIds = new ArrayList<>();
        
        public UpdateNodes(List<Long> partition ) {
            this.partition = partition;
            
        }

        @Override
        public List<Long> call() {
            // Create the DocumentManagement service client
            //DocumentManagement docManClient = getDocManClient();
            MemberService msClient = getMsClient();
            long startTime = System.currentTimeMillis();
            DocumentManagement docManClient = getDocManClient(true);
            for(Long node1 : partition)  {
                Node node = docManClient.getNode(node1);
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
                    aclRights.forEach((right) -> {
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
                    });
                } else {
                    logger.error(node.getID() + " has no rights.");
                }    
            }
            //don't
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            logger.info("Partition has updated items in basefolder "+ folderIds.get(0) + " in " + elapsedTime + " milliseconds...");
            return processedIds;
        }
    }
    
}
