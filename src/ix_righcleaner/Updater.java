/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.memberservice.Member;
import com.opentext.livelink.service.memberservice.MemberService;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
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
    private final String group, dbServer, dbName;
    private final boolean debug;
    private final Integer partitonSize;
    private final ArrayList<Long> folderIds;
    private final long rightId;
    private ExecutorService executor;
    private final ConcurrentHashMap<Long,Member> members = new ConcurrentHashMap<>();
    private final DecimalFormat df = new DecimalFormat("0.00##");
    
    public Updater(Logger logger,String user, String password, boolean debug,Integer partitonSize,String group, ArrayList<Long> folderIds, String dbServer, String dbName, long rightId, boolean export) {
        super(logger, user , password, export);
        this.logger = logger;
        this.group = group;
        this.folderIds = folderIds;
        this.debug = debug;
        this.partitonSize = partitonSize;
        this.dbServer = dbServer;
        this.dbName = dbName;
        this.rightId = rightId;
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
            List<Long> subTypes = new ArrayList<>();
            subTypes.add(0l);
            subTypes.add(144l);
            subTypes.add(848l);
            List<Long> nodesInContainer;
            if(rightId>0) nodesInContainer = newestWay(baseId, subTypes, dbServer, dbName, rightId);
            else nodesInContainer = newestWay(baseId, subTypes, dbServer, dbName);
            logger.debug("Total nodes:" + nodesInContainer.size());

            List<List<Long>> partitions = partition(nodesInContainer, partitonSize);
            executor = Executors.newFixedThreadPool(10);
            CompletionService<List<Long>>  completionService =
                    new ExecutorCompletionService<>(executor);
            long startTime = System.currentTimeMillis();
            partitions.stream().parallel().forEach(partition -> {
                logger.info("Creating new parition thread with " +partition.size());
                completionService.submit(new UpdateNodes(partition,debug,members));
            });
            
            //Help with gc
            int partitionCount = partitions.size();
            nodesInContainer = null;
            partitions = null;
            
            int received = 0;
            boolean erros = false;
            while(received < partitionCount && !erros) {
                try {
                    Future<List<Long>> resultFuture = completionService.take();
                    exportIds.addAll(resultFuture.get());
                    received ++;
                    logger.info("Document remaining: " + partitonSize*(partitionCount-received) + ". Approx. " + df.format(calcTimeLeft(startTime, partitionCount, received)) + " minutes left");
                    logger.info("Completed " + received + "/" + partitionCount +" partitons: " + df.format((100.00 * received/ partitionCount)) + "%...");
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
    
      private double calcTimeLeft(long startTime, int partitionCount, int received) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if(elapsedTime < 1l) elapsedTime = 1l;
        if(partitionCount == 0 || received== 0) return 9999999.00;
        System.out.println(received + ":" + elapsedTime + ":" + partitionCount );
        return 1.00*(partitionCount/(1.00*received/elapsedTime))/1000/60;
    }
   
    private List<Long> newestWay(long baseId, List<Long> subTypes, String dbServer, String dbName) {
        try {
            return getNodeIdsInContainer(baseId, subTypes, dbServer, dbName);
        } catch (ClassNotFoundException | SQLException ex) {
            logger.error(ex.getMessage());
        }
        return null;
    }
    
    private List<Long> newestWay(long baseId, List<Long> subTypes, String dbServer, String dbName, long rightId) {
        try {
            return getNodesInContainerWithRightId(baseId, subTypes, dbServer, dbName, rightId);
        } catch (ClassNotFoundException | SQLException ex) {
            logger.error(ex.getMessage());
        }
        return null;
    }
    
    class UpdateNodes implements Callable<List<Long>>{
        private final List<Long> partition;
        private final List<Long> processedIds = new ArrayList<>();
        private final boolean debug;
        private final ConcurrentHashMap<Long,Member> members;
        public UpdateNodes(List<Long> partition, boolean debug, ConcurrentHashMap<Long,Member>  members ) {
            this.partition = partition;
            this.debug = debug;
            this.members = members;
        }

        @Override
        public List<Long> call() {
            MemberService msClient = getMsClient();
            long startTime = System.currentTimeMillis();
            DocumentManagement docManClient = getDocManClient(true);
            processIds(docManClient, msClient);
            //don't
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            logger.info("Partition has updated items in basefolder "+ folderIds.get(0) + " in " + elapsedTime + " milliseconds...");
            return processedIds;
        }
        
        private void processIds(DocumentManagement docManClient, MemberService msClient) {
            partition.stream().parallel()
                    .forEach(nodeId -> {
                        Node node = docManClient.getNode(nodeId);
                        if(node==null) return;
                        
                        docManClient.getNodeRights(node.getID()).getACLRights().stream()
                                .forEach(right -> {
                                    //Just not to consume too much ressources, we are saving the 
                                    Member rightOwner = getOrAddMaybe(msClient,right.getRightID());
                                    logger.debug("Processing item " + node.getName() + "(id:" + node.getID() + ")" + ": current right entry:" + rightOwner.getName() + "(id:" +right.getRightID() + ")");
                                    //Removing grp if all grps have to be removed (empty Field) or if the grp name matches the provided grp name
                                    if(group.isEmpty() || group.equals(rightOwner.getName())) {
                                        removeNodeRight(docManClient, rightOwner,node, right,debug);
                                    }
                                });
                    
                    });
        }
        
        private Member getOrAddMaybe(MemberService msClient,Long rightId) {
           if(members.containsKey(rightId)) return members.getOrDefault(rightId, null);
           members.put(rightId, msClient.getMemberById(rightId));
           return getOrAddMaybe(msClient, rightId);
        }
        
        private void removeNodeRight(DocumentManagement docManClient, Member rightOwner, Node node, NodeRight right, boolean debug) {
            logger.debug("Removing "+ rightOwner.getName() +"(id:" + right.getRightID() + ")" + " from " +node.getName() +"(id:" + node.getID() + ")");
            if(!debug)docManClient.removeNodeRight(node.getID(), right);
            processedIds.add(node.getID());
        }
    }
}
