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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import java.util.logging.Level;
import static org.apache.commons.collections4.ListUtils.partition;

/**
 *
 * @author bho
 */
public class RemovePermFromObj extends ContentServerTask {
    
    private final boolean debug;
    private final String dbServer, dbName, sql, group;
    private ArrayList<Long> folderIds;
    private ExecutorService executor;
    private final Integer partitonSize;
    private final ConcurrentHashMap<Long,Member> members = new ConcurrentHashMap<>();
    private final DecimalFormat df = new DecimalFormat("0.00##");
    public RemovePermFromObj(Logger logger, String user, String password, String dbServer, String dbName, String sql, String group, boolean debug, boolean export) {
        super(logger,user,password,export);
        this.debug = debug;
        this.dbServer =dbServer;
        this.dbName = dbName;
        this.sql = sql;
        this.group = group;
        partitonSize = 200;
    }
    
    public String getNameOfTask() {
        return "Remove-Perm";
    }
    
    public void doWork() {
        folderIds = getNodesFromSql();
        List<List<Long>> partitions = partition(folderIds, partitonSize);
        executor = Executors.newFixedThreadPool(10);
        CompletionService<List<Long>>  completionService =
                new ExecutorCompletionService<>(executor);
        long startTime = System.currentTimeMillis();
        partitions.stream().parallel().forEach(partition -> {
            logger.info("Creating new partition thread with " +partition.size());
            completionService.submit(new UpdateNodes(partition,debug,members));
        });

        //Help with gc
        int partitionCount = partitions.size();
        folderIds = null;
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
        
        executor.shutdown();
        setProcessedItems(exportIds.size());
        try {
            CONNECTION.close();
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(RemovePermFromObj.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
  
    private double calcTimeLeft(long startTime, int partitionCount, int received) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if(elapsedTime < 1l) elapsedTime = 1l;
        if(partitionCount == 0 || received== 0) return 9999999.00;
        System.out.println(received + ":" + elapsedTime + ":" + partitionCount );
        return 1.00*(partitionCount/(1.00*received/elapsedTime))/1000/60;
    }
    public ArrayList<Long> getNodesFromSql() {
        ArrayList<Long> dataIds = new ArrayList<>();
        try {
            this.connectToDatabase(dbServer, dbName);
            PreparedStatement ps = CONNECTION.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Long id = rs.getLong(1);
                dataIds.add(id);
            }
        } catch (ClassNotFoundException | SQLException ex) {
            handleError(ex);
        }
        return dataIds;
    }
    
    class UpdateNodes implements Callable<List<Long>>{
        private final List<Long> partition;
        private final List<Long> processedIds = new ArrayList<>();
        private final boolean debug;
        private final ConcurrentHashMap<Long,Member> members;
        //private PreparedStatement ps = null;
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
            logger.info("Partition has updated items  "+ partition.size() + " in " + elapsedTime + " milliseconds...");
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
                                    //Member rightOwner = getOrAddMaybe(msClient,right.getRightID());
                                    String rightOwner = getNameOfUser(right.getRightID());
                                    logger.debug("Processing item " + node.getName() + "(id:" + node.getID() + ")" + ": current right entry:" + rightOwner + "(id:" +right.getRightID() + ")");
                                    //Removing grp if all grps have to be removed (empty Field) or if the grp name matches the provided grp name
                                    if(group.isEmpty() || group.equals(rightOwner)) {
                                        removeNodeRight(docManClient, rightOwner,node, right,debug);
                                    }
                                });
                    
                    });
        }
        private String getNameOfUser(Long rightID) {
            try {
                PreparedStatement ps = null;
                if(ps==null){
                    connectToDatabase(dbServer,dbName);
                    ps = CONNECTION.prepareStatement("SELECT Name FROM csadmin.KUAF WHERE ID = ?;");
                }
                ps.setLong(1, rightID);
                ResultSet rs = ps.executeQuery();
                if(rs.next()) return rs.getString(1);
            } catch (ClassNotFoundException | SQLException ex) {
                ex.printStackTrace();
                handleError(ex);
            }
            return "";
        }
        private void removeNodeRight(DocumentManagement docManClient, String rightOwner, Node node, NodeRight right, boolean debug) {
            logger.info("Removing "+ rightOwner+"(id:" + right.getRightID() + ")" + " from " +node.getName() +"(id:" + node.getID() + ")");
            if(!debug)docManClient.removeNodeRight(node.getID(), right);
            processedIds.add(node.getID());
        }
    }
}
