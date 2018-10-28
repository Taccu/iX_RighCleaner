/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.Metadata;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.logging.Level;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author bho
 */
public class InfoCatMod extends ContentServerTask{
    final String dbServer, dbName;
    final File idFile;
    private final String sql = "SELECT *"
            + "FROM dbo.SCM"
            + "";
    final HashSet<Long> longIds = new HashSet<>();
    public InfoCatMod(Logger logger, String user, String password,String dbServer, String dbName, File idFile,boolean export) {
        super(logger, user, password, export);
        this.dbServer = dbServer;
        this.dbName = dbName;
        this.idFile = idFile;
    }
    
    /**
     *
     * @return
     */
    @Override
    public String getNameOfTask(){
        return "Info-Cat-Mod";
    }
    
    @Override
    public void doWork() {
        try {
            //Read File, filter all lines which aren't a number and than convert and add to HashSet
            Files.readAllLines(idFile.toPath()).stream().filter(line -> NumberUtils.isNumber(line)).forEach(number -> {longIds.add(Long.valueOf(number));});
        } catch (IOException ex) {
            handleError(ex);
        }
        try {
            connectToDatabase(dbServer, dbName);
        } catch (ClassNotFoundException | SQLException ex) {
            handleError(ex);
        }
    }
    
    private void modifyCategory(Long dataId) {
        getCategoryInfo(dataId);
        
    }
    
    private void getCategoryInfo(Long dataId) {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(sql);
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(InfoCatMod.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void setCategoryInfoToNode(Long dataId, Metadata mData){
        
    }
}
