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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.apache.commons.collections4.ListUtils.partition;
import org.apache.commons.io.FilenameUtils;
/**
 *
 * @author bho
 */
public class CheckAlreadyArchived extends ContentServerTask{
    
    private final String dir, destDir;
    private ExecutorService executor;
    private final ConcurrentHashMap<String, Boolean> archived = new ConcurrentHashMap<>(); 
    public CheckAlreadyArchived(Logger logger,String user, String password, String dbServer, String dbName, String dir, String destDir, boolean export){
        super(logger, user ,password, export);
        this.dir = dir;
        this.destDir = destDir;
    }
    
    @Override
    public void doWork() {
        if(!Files.exists(Paths.get(dir))) handleError(new IOException(dir+ " is not existing..."));
        if(!Files.exists(Paths.get(destDir))) handleError(new IOException(destDir+ " is not existing..."));
        File root = new File(dir);
        if(!root.exists()) return;
        List<File> dirs = Arrays.asList(root.listFiles(entry ->  entry.isDirectory()));
        List<List<File>> partitions = partition(dirs,5);
        executor = Executors.newFixedThreadPool(10);
        CompletionService<List<Path>>  completionService =
        new ExecutorCompletionService<>(executor);
        long startTime = System.currentTimeMillis();
        partitions.stream().parallel().forEach(partition -> {
            logger.info("Creating new parition thread with " +partition.size());
            completionService.submit(new UpdateNodes(partition,archived, destDir));
        });
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
        "FROM InfostoreExport.dbo.VERKAUF_ALL\n" +
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
            partition.stream().parallel().forEach(dir -> {
                List<File> files = Arrays.asList(dir.listFiles(file -> file.getName().startsWith("GX")));
                files.stream().parallel().
                    forEach(file -> {
                        String databaseEntry = FilenameUtils.removeExtension(file.getName()).substring(2);
                        if(archived.containsKey(databaseEntry))try {Files.move(file.toPath(), Paths.get(destDir)); } catch(IOException ex) {logger.error("Couldn't move " + file.getName());}
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
