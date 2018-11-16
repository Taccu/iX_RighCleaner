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
import static org.apache.commons.collections4.ListUtils.partition;

/**
 *
 * @author bho
 */
public class MoveVerkauf extends ContentServerTask{
    
    private final boolean debug;
    private final String dbServer, dbName;
    private final Long sourceFolder,dstFolder;
    private final DecimalFormat df = new DecimalFormat("0.00##");
    private ExecutorService executor;
    private final int partitionSize,parallelThreads;
    private Long threadId = 0l;
    public MoveVerkauf(Logger logger,String user, String password,String dbServer, String dbName, Long sourceFolder, Long dstFolder,int partitionSize,int parallelThreads, boolean debug, boolean export) {
        super(logger, user, password, export);
        this.debug = debug;
        this.dbServer = dbServer;
        this.dbName = dbName;
        this.sourceFolder = sourceFolder;
        this.dstFolder = dstFolder;
        this.partitionSize = partitionSize;
        this.parallelThreads = parallelThreads;
    }
        
    @Override
    public String getNameOfTask() {
        return "Move-Verkauf";
    }
    
    @Override
    public void doWork() {
        List<Long> nodeIds = null;
        try {
            nodeIds = getNodeIdsInContainer(sourceFolder, dbServer, dbName);
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
            completionService.submit(new MoveVerkauf.UpdateNodes(partition,debug,dstFolder,threadId++));
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
        public UpdateNodes(List<Long> partition, boolean debug, Long dstId, Long threadId) {
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
                            if(!debug)docManClient.moveNode(nodeId, dstId, node.getName(), mOptions);
                            long stopTime = System.currentTimeMillis();
                            long elapsedTime = stopTime - startTime;
                            logger.debug(threadId+ ": Moved " + node.getName() + "(id:" + node.getID() + ") to " + dstId +" in " + elapsedTime + " milliseconds...");
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
