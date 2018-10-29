/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.core.DateValue;
import com.opentext.livelink.service.core.IntegerValue;
import com.opentext.livelink.service.core.StringValue;
import com.opentext.livelink.service.docman.AttributeGroup;
import com.opentext.livelink.service.docman.Metadata;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.memberservice.MemberService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author bho
 */
public class InfoCatMod extends ContentServerTask{
    final String dbServer, dbName,sql, catId;
    final File idFile;
    final HashSet<Long> longIds = new HashSet<>();
    private final ArrayList<InfoStoreMapping> mapping;
    private SimpleDateFormat dFormat = new SimpleDateFormat();
    private final Timer timer = new Timer();
    private static final Semaphore SEMA = new Semaphore(1);
    private AtomicInteger counter = new AtomicInteger();
    private final boolean debug;
    public InfoCatMod(Logger logger, String user, String password,String dbServer, String dbName, File idFile,String sql, ArrayList<InfoStoreMapping> mapping, String catId, boolean debug, boolean export) {
        super(logger, user, password, export);
        this.dbServer = dbServer;
        this.dbName = dbName;
        this.idFile = idFile;
        this.sql = sql;
        this.mapping = mapping;
        this.catId = catId;
        this.debug = debug;
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
        //MemberService msClient = getMsClient();
        //if(!msClient.getAuthenticatedUser().getPrivileges().isCanAdministerSystem()) {handleError(new Exception("User is not an admin."));}
            //Read File, filter all lines which aren't a number and than convert and add to HashSet
        /*Path path = idFile.toPath();
        Files.readAllLines(path)
                .stream()
                .filter(line -> NumberUtils.isNumber(line))
                .forEach(number -> {
                    logger.debug("Reading...");
                    longIds.add(Long.valueOf(number));
                });*/
        longIds.add(103829l);
        try {
            connectToDatabase(dbServer, dbName);
        } catch (ClassNotFoundException | SQLException ex) {
            handleError(ex);
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run(){
                try {
                    SEMA.acquire();
                    getDocManClient(true);
                    SEMA.release();
                } catch (InterruptedException ex) {
                    java.util.logging.Logger.getLogger(InfoCatMod.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, new Date(), 30000l);
        
        longIds.stream().parallel().forEach(id -> {
            modifyCategory(id);
            logger.info("Processed "+counter.incrementAndGet()+"/"+longIds.size()+" elements.");
        });
        timer.cancel();
    }
    
    private void modifyCategory(Long dataId) {
        Metadata mData = getCategoryInfo(dataId);
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(sql);
            ps.setLong(1, dataId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                mapping.stream().forEach(map -> {
                    try {
                        //Map the result from the key to the value
                        System.out.println(map.getSrcType().getName());
                        switch(map.getSrcType().getSimpleName()) {
                            case "String":
                                setCategoryInfoToNode(dataId,modifyMData(mData, catId, map.getDst(),rs.getString(map.getSrc())));
                                break;
                            case "Integer":
                                setCategoryInfoToNode(dataId,modifyMData(mData, catId, map.getDst(),Long.valueOf(rs.getInt(map.getSrc()))));
                                break;
                            case "Date":
                                setCategoryInfoToNode(dataId,modifyMData(mData, catId, map.getDst(),rs.getDate(map.getSrc())));
                                break;
                            case "Long":
                                setCategoryInfoToNode(dataId,modifyMData(mData, catId, map.getDst(),rs.getLong(map.getSrc())));
                                break;
                            default:
                                //do nothing
                        }
                    } catch (SQLException ex) {
                        handleError(ex);
                    }
                });
            }
        } catch (SQLException ex) {
            handleError(ex);
        } finally{
            try {
                CONNECTION.close();
            } catch (SQLException ex) {
                handleError(ex);
            }
        }
    }
    
    private Metadata getCategoryInfo(Long dataId) {
        Node node = getDocManClient().getNode(dataId);
        Metadata mData = node.getMetadata();
        return mData;
    }
    
    private Metadata modifyMData(Metadata mData, String catId, String dstType, String value){
        AttributeGroup attrGrp = getGroup(mData, catId);
        attrGrp.getValues().iterator().forEachRemaining(dValue -> {
            if(dValue.getDescription().equalsIgnoreCase(dstType)) {
                StringValue sValue = (StringValue) dValue;
                if(sValue.getValues().size()<1) sValue.getValues().add(value);
                else { sValue.getValues().clear(); sValue.getValues().add(value);}
            }
        });
        return mData;
    }
    
    private Metadata modifyMData(Metadata mData, String catId, String dstType, Long value){
        AttributeGroup attrGrp = getGroup(mData, catId);
        attrGrp.getValues().iterator().forEachRemaining(dValue -> {
            if(dValue.getDescription().equalsIgnoreCase(dstType)) {
                IntegerValue sValue = (IntegerValue) dValue;
                if(sValue.getValues().size()<1) sValue.getValues().add(value);
                else { sValue.getValues().clear(); sValue.getValues().add(value);}
            }
        });
        return mData;
    }
   
    
    private Metadata modifyMData(Metadata mData, String catId, String dstType, Date value) {
        AttributeGroup attrGrp = getGroup(mData, catId);
        attrGrp.getValues().iterator().forEachRemaining(dValue -> {
            if(dValue.getDescription().equalsIgnoreCase(dstType)) {
                DateValue sValue = (DateValue) dValue;
                if(sValue.getValues().size()<1) try {
                    sValue.getValues().add(DatatypeFactory.newInstance().newXMLGregorianCalendar(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value.toInstant())));
                } catch (DatatypeConfigurationException ex) {
                    java.util.logging.Logger.getLogger(InfoCatMod.class.getName()).log(Level.SEVERE, null, ex);
                }
                else { sValue.getValues().clear();
                    try {
                        sValue.getValues().add(DatatypeFactory.newInstance().newXMLGregorianCalendar(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value.toInstant())));
                    } catch (DatatypeConfigurationException ex) {
                        java.util.logging.Logger.getLogger(InfoCatMod.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        return mData;
    }
        
    private AttributeGroup getGroup(Metadata mData, String catId) {
        for(AttributeGroup group : mData.getAttributeGroups()){
            if(group.getKey().startsWith(catId)) return group;
        }
        return null;
    }
    
    private void setCategoryInfoToNode(Long dataId, Metadata mData){
        logger.debug("Setting metadata to Node " +dataId);
        if(!debug)getDocManClient().setNodeMetadata(dataId, mData);
    }
}
