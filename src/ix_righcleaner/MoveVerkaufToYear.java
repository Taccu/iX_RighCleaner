/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.AttributeSourceType;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.MoveOptions;
import com.opentext.livelink.service.docman.Node;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
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
public class MoveVerkaufToYear extends ContentServerTask {
    private final boolean debug;
    private final String dbServer, dbName;
    private final long sourceFolderId, destFolderId;
    private final int partitionSize, parallelThreads;
    private final DecimalFormat df = new DecimalFormat("0.00##");
    private ExecutorService executor;
    private Long threadId = 0l;
    public MoveVerkaufToYear(Logger logger, String user, String password,  String dbServer, String dbName, long sourceFolderId, long destFolderId,int partitionSize,int parallelThreads, boolean debug, boolean export) {
        super(logger, user, password, export);
        this.debug = debug;
        this.dbServer = dbServer;
        this.dbName = dbName;
        this.sourceFolderId = sourceFolderId;
        this.destFolderId = destFolderId;
        this.partitionSize = partitionSize;
        this.parallelThreads = parallelThreads;
    }
    
    public String getNameOfTask(){
        return "Move-Verkauf-To-Year";
    }
    
    public void doWork() {
        List<Long> nodeIds = null;
        try {
            nodeIds = getNodeIdsInContainer(sourceFolderId, dbServer, dbName);
        } catch (ClassNotFoundException | SQLException ex) {
            handleError(ex);
        }
        if(nodeIds == null) {
            handleError(new Exception("Couldn't find any nodes"));
            return;
        }
        logger.debug("Total nodes:" + nodeIds.size());
        List<List<Long>> partitions = partition(nodeIds, partitionSize);
        executor = Executors.newFixedThreadPool(parallelThreads);
        CompletionService<List<Long>>  completionService =
                new ExecutorCompletionService<>(executor);
        long startTime = System.currentTimeMillis();
        partitions.stream().forEach(partition -> {
            logger.info("Creating new parition thread with " +partition.size());
            completionService.submit(new UpdateNodes(partition,debug,destFolderId,((threadId++)%parallelThreads)));
        });
        //Help with gc
        int partitionCount = partitions.size();
        nodeIds = null;
        partitions = null;

        int received = 0;
        boolean erros = false;
        while(received < partitionCount && !erros) {
            try {
                Future<List<Long>> resultFuture = completionService.take();
                exportIds.addAll(resultFuture.get());
                received ++;
                logger.info("Document remaining: " + partitionSize*(partitionCount-received) + ". Approx. " + df.format(calcTimeLeft(startTime, partitionCount, received)) + " minutes left");
                logger.info("Completed " + received + "/" + partitionCount +" partitons: " + df.format((100.00 * received/ partitionCount)) + "%...");
            } catch(Exception e) {
                logger.error(e.getMessage());
                erros = true;
                logger.debug("Interrupting thread");
                Thread.currentThread().interrupt();
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
    class UpdateNodes implements Callable<List<Long>>{
        private final List<Long> partition;
        private final List<Long> processedIds = new ArrayList<>();
        private final boolean debug;
        private final Long dstId,threadId;
        private DocumentManagement docManClient;
        private PreparedStatement ps = null;
        public UpdateNodes(List<Long> partition, boolean debug, Long dstId, Long threadId ) {
            this.partition = partition;
            this.debug = debug;
            this.dstId = dstId;
            this.threadId = threadId;
        }

        @Override
        public List<Long> call() {
            long startTime = System.currentTimeMillis();
            docManClient = getDocManClient();
            processIds();
            //don't
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            logger.info("Partition has moved items "+ partition.size() + " in " + elapsedTime + " milliseconds...");
            return processedIds;
        }
        
        private Long getDestination(Long nodeId, int tried) {
            logger.debug("Getting destination for " + nodeId + ". That's the " + tried +" time.");
            ResultSet rs = null;
            long destId = 0l;
            try {
                connectToDatabase(dbServer, dbName);
                if(ps==null)ps = CONNECTION.prepareStatement("SELECT DataID\n" +
                    "FROM csadmin.DTree\n" +
                    "WHERE ParentID = 54483\n" +
                    "AND SubType = 0\n" +
                    "AND Name = CONVERT(NVARCHAR(255),(\n" +
                    "SELECT TOP 1 YEAR(ValDate) AS YFolder\n" +
                    "FROM csadmin.LLAttrData\n" +
                    "WHERE  DefID = 54743\n" +
                    "AND AttrID = 23\n" +
                    "AND ID = ?\n" +
                    "ORDER BY VerNum DESC\n" +
                    "))");
                ps.setLong(1, nodeId);
                rs = ps.executeQuery();
                while(rs.next()) {
                    destId = rs.getLong("DataID");
                }
            } catch (ClassNotFoundException | SQLException ex) {
                logger.error(ex.getMessage());
                ex.printStackTrace();
            }
            if(destId == 0l) {
                logger.warn("Couldn't find the folder for " + nodeId);
                createFolder(nodeId);
                if(tried>2){
                    logger.error("Tried too many times to create folder for " + nodeId);
                    return 0l;
                }
                getDestination(nodeId,tried++);
            }
            return destId;
        }
        
        private void createFolder(Long nodeId){
            String name = "";
            try {
                connectToDatabase(dbServer, dbName);
                PreparedStatement ps = CONNECTION.prepareStatement("SELECT TOP 1 YEAR(ValDate) AS YFolder\n" +
                    "FROM csadmin.LLAttrData\n" +
                    "WHERE  DefID = 54743\n" +
                    "AND AttrID = 23\n" +
                    "AND ID = ?\n" +
                    "ORDER BY VerNum DESC");
                ps.setLong(1, nodeId);
                ResultSet rs = ps.executeQuery();
                if(rs.next()) {
                    name = rs.getString("YFolder");
                }
            }
            catch (ClassNotFoundException | SQLException ex) {
                logger.error(ex.getMessage());
                ex.printStackTrace();
            }
            if(name.isEmpty()) return;
            //Ordner existierte nicht
            Node dstNode = getDocManClient().getNode(dstId);
            if(!debug)getDocManClient().createFolder(dstId, name, "", dstNode.getMetadata());
            logger.warn("Created folder " + name + " in " + dstNode.getName() + "(id:" + dstId + ")");
        }
        
        private void processIds() {
            final MoveOptions mOptions = new MoveOptions();
            mOptions.setForceInheritPermissions(true);
            mOptions.setAddVersion(false);
            mOptions.setAttrSourceType(AttributeSourceType.ORIGINAL);
            partition.stream()
                .forEach(nodeId -> {
                    int count = 0;
                    int maxTries = 3;
                    while(true) {
                        try {
                            long startTime = System.currentTimeMillis();
                            Node node = docManClient.getNode(nodeId);
                            if(node==null) return;
                            if(node.getID()==dstId) return;
                            if(node.isIsContainer()) return;
                            Long destination = getDestination(nodeId,0);
                            if(!debug)docManClient.moveNode(nodeId, destination, node.getName(), mOptions);
                            long stopTime = System.currentTimeMillis();
                            long elapsedTime = stopTime - startTime;
                            logger.debug(threadId+ ": Moved " + node.getName() + "(id:" + node.getID() + ") to " + destination +" in " + elapsedTime + " milliseconds...");
                            break;
                        } catch(ServerSOAPFaultException ex) {
                            logger.warn("Session timed out. " + ex.getMessage());
                            ex.printStackTrace();
                            if(++count == maxTries) {
                                logger.error("Couldn't move " + nodeId);
                                break;
                            }
                            docManClient = getDocManClient(true);
                        }
                    }
            });
        }
    }
}
