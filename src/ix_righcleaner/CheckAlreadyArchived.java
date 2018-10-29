/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.apache.commons.collections4.ListUtils.partition;
import org.apache.commons.io.FilenameUtils;
/**
 *
 * @author bho
 */
public class CheckAlreadyArchived extends ContentServerTask{
    
    private final String dir, destDir, dbServer, dbName;
    private ExecutorService executor;
    private final boolean debug;
    private final ConcurrentHashMap<String, Boolean> archived = new ConcurrentHashMap<>();
    
    private final DecimalFormat df = new DecimalFormat("0.00##");
    public CheckAlreadyArchived(Logger logger,String user, String password, boolean debug, String dbServer, String dbName, String dir, String destDir, boolean export){
        super(logger, user ,password, export);
        this.dir = dir;
        this.debug = debug;
        this.destDir = destDir;
        this.dbServer = dbServer;
        this.dbName = dbName;
    }
    
    @Override
    public void doWork() {
        int partitionSize = 5;
        if(!Files.exists(Paths.get(dir))) handleError(new IOException(dir+ " is not existing..."));
        if(!Files.exists(Paths.get(destDir))) handleError(new IOException(destDir+ " is not existing..."));
        File root = new File(dir);
        if(!root.exists()) return;
        List<File> dirs = Arrays.asList(root.listFiles(entry ->  entry.isDirectory()));
        List<List<File>> partitions = partition(dirs,partitionSize);
        
        try {
            getAllArchivedDocs(dbServer, dbName);
        } catch (SQLException ex) {
            handleError(ex);
        }
        executor = Executors.newFixedThreadPool(10);
        CompletionService<List<Path>>  completionService =
        new ExecutorCompletionService<>(executor);
        long startTime = System.currentTimeMillis();
        partitions.stream().parallel().forEach(partition -> {
            logger.info("Creating new parition thread with " + partition.size());
            completionService.submit(new UpdateNodes(partition,archived, destDir));
        });
        //Help with gc
        int partitionCount = partitions.size();
        partitions = null;
        int received = 0;
        boolean erros = false;
        while(received < partitionCount && !erros) {
            try {
                Future<List<Path>> resultFuture = completionService.take();
                received ++;
                logger.info("Document remaining: " + partitionSize *(partitionCount-received) + ". Approx. " + df.format(calcTimeLeft(startTime, partitionCount, received)) + " minutes left");
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

    private void getAllArchivedDocs(String dbServer, String dbName) throws SQLException{
        try {
            connectToDatabase(dbServer, dbName);
        } catch (ClassNotFoundException | SQLException ex) {
            handleError(ex);
        }
        boolean valid = false;
        try {
            valid = CONNECTION.isValid(10);
        } catch (SQLException ex) {
            handleError(ex);
        }
        if(!valid) return;
        
        PreparedStatement ps = CONNECTION.prepareStatement("SELECT I_DOCID\n" +
        "FROM dbo.VERKAUF\n" +
        "WHERE IX_Archived IS NOT NULL");
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            String string = rs.getString("I_DOCID");
            archived.put(string, true);
        }
    }
    
    
    @Override
    public String getNameOfTask() {
        return "CheckAlreadyArchived";
    }
    
    class UpdateNodes implements Callable<List<Path>>{
        private final List<File> partition;
        private final ConcurrentHashMap<String, Boolean> archived ;
        private final String destDir;
        public UpdateNodes(List<File> partition, ConcurrentHashMap<String, Boolean> archived, String destDir) {
            this.partition = partition;
            this.archived = archived;
            this.destDir = destDir;
        }
        @Override
        public List<Path> call() {
            long startTime = System.currentTimeMillis();
            partition.stream().parallel().forEach(
                dir -> {
                    List<File> files = Arrays.asList(dir.listFiles(file -> file.getName().startsWith("GX")));
                    files.stream().parallel().
                        forEach(file -> {
                            String databaseEntry = FilenameUtils.removeExtension(file.getName()).substring(2);
                            if(archived.containsKey(databaseEntry)){
                                try {
                                    if(!debug) {
                                        logger.debug("Moving " + file.getName() + " to " + destDir);
                                        Files.move(file.toPath(), Paths.get(destDir));
                                    }
                                    else {
                                        logger.debug("Test:Moving " + file.getName() + " to " + destDir);
                                    }
                                } catch(IOException ex) {
                                logger.error("Couldn't move " + file.getName());
                                }
                            } else {
                                logger.debug(databaseEntry + " was probably archived");
                            }
                    });
            });
            //don't
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            logger.info("Partition has processed items in " + elapsedTime + " milliseconds...");
            return null;
        }
    }
}
