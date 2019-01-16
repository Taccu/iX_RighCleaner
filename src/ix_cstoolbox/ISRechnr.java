/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

import com.opentext.livelink.service.core.StringValue;
import com.opentext.livelink.service.docman.Metadata;
import com.opentext.livelink.service.docman.Node;
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
import static org.apache.commons.collections4.ListUtils.partition;

/**
 *
 * @author bho
 */
public class ISRechnr extends ContentServerTask {
    private final boolean debug;
    private final String dbServer, dbName;
    private Integer partitonSize;
    private ExecutorService executor;
    private final DecimalFormat df = new DecimalFormat("0.00##");
    public ISRechnr(Logger logger, String user, String password, String dbServer, String dbName,boolean debug, boolean export) {
        super(logger,user,password,export);
        this.debug = debug;
        this.dbServer = dbServer;
        this.dbName = dbName;
        partitonSize = 200;
    }
    
    @Override
    public String getNameOfTask() {
        return "ISRechnr";
    }
    
    @Override
    public void doWork() {
        PreparedStatement ps;
        ArrayList<Pair> entries = new ArrayList<>();
        try {
            connectToDatabase(dbServer,dbName);
            ps = CONNECTION.prepareStatement("SELECT TOP 1000000 T1.ID, ABRECHNR\n" +
            "FROM csadmin.LLAttrData T1\n" +
            "LEFT JOIN\n" +
            "(\n" +
            "SELECT CONVERT(bigint, SUBSTRING(IX_ArchiveID,5,10)) AS ID,ABRECHNR\n" +
            "FROM [InfostoreExport].[dbo].[VERKAUF]\n" +
            ") AS T2\n" +
            "ON T1.ID = T2.ID\n" +
            "WHERE T1.DefID = 54743\n" +
            "AND T1.AttrID = 18\n" +
            "AND T1.ValStr =N'0'\n" +
            "AND T2.ABRECHNR IS NOT NULL");
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Pair pair = new Pair();
                pair.setLong(rs.getLong(1));
                pair.setString(rs.getString(2));
                entries.add(pair);
            }
            rs.close();
            ps.close();
        } catch (ClassNotFoundException | SQLException ex) {
            handleError(ex);
        }
        
        
        
        List<List<Pair>> partitions = partition(entries, partitonSize);
            executor = Executors.newFixedThreadPool(50);
            CompletionService<List<Pair>>  completionService =
                    new ExecutorCompletionService<>(executor);
            long startTime = System.currentTimeMillis();
            partitions.stream().parallel().forEach(partition -> {
                logger.info("Creating new parition thread with " +partition.size());
                completionService.submit(new UpdateNodes(partition,debug));
            });
            
            //Help with gc
            int partitionCount = partitions.size();
            entries = null;
            partitions = null;
            
            int received = 0;
            boolean erros = false;
            while(received < partitionCount && !erros) {
                try {
                    Future<List<Pair>> resultFuture = completionService.take();
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
        }
      private double calcTimeLeft(long startTime, int partitionCount, int received) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if(elapsedTime < 1l) elapsedTime = 1l;
        if(partitionCount == 0 || received== 0) return 9999999.00;
        System.out.println(received + ":" + elapsedTime + ":" + partitionCount );
        return 1.00*(partitionCount/(1.00*received/elapsedTime))/1000/60;
    } 
    
    public class UpdateNodes implements Callable<List<Pair>>{
        private final List<Pair> partition;
        private final List<Long> processedIds = new ArrayList<>();
        private final boolean debug;
        public UpdateNodes(List<Pair> partition, boolean debug) {
            this.partition = partition;
            this.debug = debug;
        }

        @Override
        public List<Pair> call() {
            long startTime = System.currentTimeMillis();
            for(Pair entry : partition) {
                logger.debug("Processing " + entry.getLong() + "...");
                Node node = getDocManClient().getNode(entry.getLong());
                getDocManClient().setNodeMetadata(entry.getLong(),fiddleMetadata(node.getMetadata(), entry.getString()));
            }
            //don't
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            logger.info("Partition has updated items in " + elapsedTime + " milliseconds...");
           return null;
        }
         private Metadata fiddleMetadata(Metadata mData, String newValue) {
            Metadata newMData = mData;
            newMData.getAttributeGroups().stream().forEach(attrgrp -> {
                if(attrgrp.getKey().startsWith("54743")) {
                    attrgrp.getValues().stream().forEach(value -> {
                        if(value.getDescription().equalsIgnoreCase("Rechnungs-Nr.") || value.getDescription().equalsIgnoreCase("Rechnung Nr.")) {
                            StringValue sValue = (StringValue) value;
                            logger.debug("Setting sValue to " + newValue);
                            sValue.getValues().set(0, newValue);
                        }
                    });
                }
            });

            return newMData;
        }
    }
    
    public class Pair {
      private long Long;

      private String string;
      
      //accessors

        public long getLong() {
            return Long;
        }

        public void setLong(long Long) {
            this.Long = Long;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }
    }
}
