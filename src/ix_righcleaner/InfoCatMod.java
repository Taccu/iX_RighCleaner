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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

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
    private final boolean debug, fetchAllFirst;
    private AtomicInteger counter = new AtomicInteger();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private ResultSet fetchedResults;
    public InfoCatMod(Logger logger, String user, String password,String dbServer, String dbName, File idFile,String sql, ArrayList<InfoStoreMapping> mapping, String catId, boolean fetchAllFirst, boolean debug, boolean export) {
        super(logger, user, password, export);
        this.dbServer = dbServer;
        this.dbName = dbName;
        this.idFile = idFile;
        this.sql = sql;
        this.mapping = mapping;
        this.catId = catId;
        this.debug = debug;
        this.fetchAllFirst = fetchAllFirst;
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
       // MemberService msClient = getMsClient();
       // if(!msClient.getAuthenticatedUser().getPrivileges().isCanAdministerSystem()) {handleError(new Exception("User is not an admin."));}
            //Read File, filter all lines which aren't a number and than convert and add to HashSet
        Path path = idFile.toPath();
        try {
            Files.readAllLines(path)
                    .stream()
                    //.filter(line -> NumberUtils.isNumber(line))
                    .forEach(number -> {
                        logger.debug("Reading...");
                        longIds.add(Long.valueOf(number));
                    });
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(InfoCatMod.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            connectToDatabase(dbServer, dbName);
        } catch (ClassNotFoundException | SQLException ex) {
            handleError(ex);
        }
        if(fetchAllFirst) {
            logger.info("Fetching data first...");
            fetchedResults = fetchAllFirst();
        }
        ForkJoinPool fPool = new ForkJoinPool(50);
        try {
            fPool.submit(() -> longIds.stream().parallel().forEach(id -> {
                if(fetchAllFirst) {
                    modifyCategory(getDocManClient().getNode(id), fetchedResults);
                }
                else {
                    modifyCategory(getDocManClient().getNode(id));
                }
                logger.info("Processed "+counter.incrementAndGet()+"/"+longIds.size()+" elements.");
            })
            ).get();
            if(counter.get() == longIds.size()) {
                CONNECTION.close(); 
            }
        } catch (InterruptedException | SQLException | ExecutionException ex) {
            logger.error(ex.getLocalizedMessage());
        }
    }
    
    private ResultSet fetchAllFirst() {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(sql);
            //ps.setString(1, String.valueOf(node.getID()));
            return ps.executeQuery();
        } catch (SQLException ex) {
            logger.error(ex.getLocalizedMessage());
            if(debug)ex.printStackTrace();
        }
        return null;
    }
    private void modifyCategory(Node node, ResultSet rs) {
        try {
            ResultSet rsT = rs;
            while(rsT.next()) {
                mapping.stream().forEach(map -> {
                    try {
                        //Map the result from the key to the value
                        Metadata newData = getDocManClient().getNode(node.getID()).getMetadata();
                        switch(map.getSrcType().getSimpleName()) {
                            case "String":
                                String sValue = rsT.getString(map.getSrc());
                                switch(map.getDstType().getSimpleName()) {
                                    case "String":
                                        newData = modifyMData(newData, catId, map.getDst(), sValue);
                                        if(debug)System.out.println("S2S:Setting " + sValue + " at " + map.getDst() + " to " + map.getDst());
                                        break;
                                    case "Integer":
                                    case "Long":
                                        newData = modifyMData(newData, catId, map.getDst(), Long.valueOf(sValue));
                                        if(debug)System.out.println("S2L:Setting " + sValue + " at " + map.getDst() + " to " + map.getDst());
                                        break;
                                    case "Date":
                                        try {
                                            newData = modifyMData(newData, catId, map.getDst(), sdf.parse(sValue));
                                            if(debug)System.out.println("S2D:Setting " + sValue + " at " + map.getDst() + " to " + map.getDst());
                                        } catch (ParseException ex) {
                                            logger.error("Couldn't parse " + sValue +". The error was "+ex);
                                        }
                                        break;

                                    default:
                                        //blabla
                                }
                                break;
                            case "Integer":
                                Integer iValue = rsT.getInt(map.getSrc());
                                switch(map.getDstType().getSimpleName()) {
                                    case "String":
                                        newData = modifyMData(newData, catId, map.getDst(), String.valueOf(iValue));
                                        if(debug)System.out.println("I2S:Setting " + String.valueOf(iValue) + " at " + map.getDst() + " to " + map.getDst());
                                        break;
                                    case "Integer":
                                    case "Long":
                                        newData = modifyMData(newData, catId, map.getDst(), Long.valueOf(iValue));
                                        if(debug)System.out.println("I2L:Setting " + iValue + " at " + map.getDst() + " to " + map.getDst());
                                        break;
                                    case "Date":
                                        try {
                                            newData = modifyMData(newData, catId, map.getDst(), sdf.parse(String.valueOf(iValue)));
                                            if(debug)System.out.println("Setting " + sdf.format(sdf.parse(String.valueOf(iValue))) + " at " + map.getDst() + " to " + map.getDst());
                                        } catch (ParseException ex) {
                                            logger.error("Couldn't parse " + iValue +". The error was "+ex);
                                        }
                                        break;
                                    default:
                                        //blabla
                                }
                                break;
                            case "Long":
                                newData = modifyMData(newData, catId, map.getDst(),rsT.getLong(map.getSrc()));
                                break;
                            case "Date":
                                Date dValue = rsT.getDate(map.getSrc());
                                switch(map.getDstType().getSimpleName()) {
                                    case "String":
                                        newData = modifyMData(newData, catId, map.getDst(), sdf.format(dValue));
                                        break;
                                    case "Integer":
                                    case "Long":
                                        newData = modifyMData(newData, catId, map.getDst(), dValue.getTime());
                                        break;
                                    case "Date":
                                        newData = modifyMData(newData, catId, map.getDst(),dValue);
                                        break;
                                    default:
                                        //blabla
                                }
                                break;
                            default:
                                //do nothing
                        }
                        setCategoryInfoToNode(node, newData);
                    } catch (SQLException ex) {
                        handleError(ex);
                    } 
                });
                break;
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(InfoCatMod.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void modifyCategory(Node node) {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(sql);
            ps.setString(1, String.valueOf(node.getID()));
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                mapping.stream().forEach(map -> {
                    try {
                        //Map the result from the key to the value
                        Metadata newData = getDocManClient().getNode(node.getID()).getMetadata();
                        switch(map.getSrcType().getSimpleName()) {
                            case "String":
                                String sValue = rs.getString(map.getSrc());
                                switch(map.getDstType().getSimpleName()) {
                                    case "String":
                                        newData = modifyMData(newData, catId, map.getDst(), sValue);
                                        if(debug)System.out.println("S2S:Setting " + sValue + " at " + map.getDst() + " to " + map.getDst());
                                        break;
                                    case "Integer":
                                    case "Long":
                                        newData = modifyMData(newData, catId, map.getDst(), Long.valueOf(sValue));
                                        if(debug)System.out.println("S2L:Setting " + sValue + " at " + map.getDst() + " to " + map.getDst());
                                        break;
                                    case "Date":
                                        try {
                                            newData = modifyMData(newData, catId, map.getDst(), sdf.parse(sValue));
                                            if(debug)System.out.println("S2D:Setting " + sValue + " at " + map.getDst() + " to " + map.getDst());
                                        } catch (ParseException ex) {
                                            logger.error("Couldn't parse " + sValue +". The error was "+ex);
                                        }
                                        break;

                                    default:
                                        //blabla
                                }
                                break;
                            case "Integer":
                                Integer iValue = rs.getInt(map.getSrc());
                                switch(map.getDstType().getSimpleName()) {
                                    case "String":
                                        newData = modifyMData(newData, catId, map.getDst(), String.valueOf(iValue));
                                        if(debug)System.out.println("I2S:Setting " + String.valueOf(iValue) + " at " + map.getDst() + " to " + map.getDst());
                                        break;
                                    case "Integer":
                                    case "Long":
                                        newData = modifyMData(newData, catId, map.getDst(), Long.valueOf(iValue));
                                        if(debug)System.out.println("I2L:Setting " + iValue + " at " + map.getDst() + " to " + map.getDst());
                                        break;
                                    case "Date":
                                            try {
                                                newData = modifyMData(newData, catId, map.getDst(), sdf.parse(String.valueOf(iValue)));
                                                if(debug)System.out.println("Setting " + sdf.format(sdf.parse(String.valueOf(iValue))) + " at " + map.getDst() + " to " + map.getDst());
                                            } catch (ParseException ex) {
                                                logger.error("Couldn't parse " + iValue +". The error was "+ex);
                                            }
                                        break;
                                    default:
                                        //blabla
                                }
                                break;
                            case "Long":
                                newData = modifyMData(newData, catId, map.getDst(),rs.getLong(map.getSrc()));
                                break;
                            case "Date":
                                Date dValue = rs.getDate(map.getSrc());
                                switch(map.getDstType().getSimpleName()) {
                                    case "String":
                                        newData = modifyMData(newData, catId, map.getDst(), sdf.format(dValue));
                                        break;
                                    case "Integer":
                                    case "Long":
                                        newData = modifyMData(newData, catId, map.getDst(), dValue.getTime());
                                        break;
                                    case "Date":
                                        newData = modifyMData(newData, catId, map.getDst(),dValue);
                                        break;
                                    default:
                                        //blabla
                                }
                                break;
                            default:
                                //do nothing
                        }
                        setCategoryInfoToNode(node, newData);
                    } catch (SQLException ex) {
                        handleError(ex);
                    } 
                });
                break;
            }
        } catch (SQLException ex) {
            handleError(ex);
        } 
    }
    private Metadata modifyMData(Metadata mData, String catId, String dstType, String value){
        final String trimmedValue = value.trim();
        AttributeGroup attrGrp = getGroup(mData, catId);
        attrGrp.getValues().iterator().forEachRemaining(dValue -> {
            if(dValue.getDescription().equalsIgnoreCase(dstType)) {
                StringValue sValue = (StringValue) dValue;
                if(sValue.getValues().size()<1) sValue.getValues().add(trimmedValue);
                else { sValue.getValues().clear(); sValue.getValues().add(trimmedValue);} 
               if(debug) logger.debug("Setting value " + trimmedValue);
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
                if(debug) logger.debug("Setting value " + value);
            }
        });
        return mData;
    }
   
    private XMLGregorianCalendar makeCalendar(LocalDateTime time) {
        XMLGregorianCalendar cal = null;
        try {
            String date = time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if(debug)System.out.println("Date is "+date);
            cal = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(date.replace("Z", ""));
        } catch (DatatypeConfigurationException ex) {
            logger.warn("Couldn't set " + time.toString() + ". The error was " + ex.getLocalizedMessage());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return cal;
    }
    
    private Metadata modifyMData(Metadata mData, String catId, String dstType, Date value) {
        //LocalDateTime tNow = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        final XMLGregorianCalendar cal = makeCalendar(LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault()));
        AttributeGroup attrGrp = getGroup(mData, catId);
        attrGrp.getValues().iterator().forEachRemaining(dValue -> {
            if(dValue.getDescription().equalsIgnoreCase(dstType)) {
                DateValue sValue = (DateValue) dValue;
                if(sValue.getValues().size()<1) 
                    sValue.getValues().add(cal);
                else { 
                    sValue.getValues().clear();
                    sValue.getValues().add(cal);
                }
                if(debug) logger.debug("Setting value " + value);
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
    
    private void setCategoryInfoToNode(Node node, Metadata mData){
        logger.debug("Setting metadata to Node " +node.getName() + "(id:" +node.getID() + ")...");
        getDocManClient().setNodeMetadata(node.getID(), mData);
    }
}
