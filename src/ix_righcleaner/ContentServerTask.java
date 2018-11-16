/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.ecm.api.OTAuthentication;
import com.opentext.livelink.service.classifications.Classifications;
import com.opentext.livelink.service.classifications.Classifications_Service;
import com.opentext.livelink.service.core.Authentication;
import com.opentext.livelink.service.core.Authentication_Service;
import com.opentext.livelink.service.core.ChunkedOperationContext;
import com.opentext.livelink.service.docman.CategoryItemsUpgradeInfo;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.DocumentManagement_Service;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRightUpdateInfo;
import com.opentext.livelink.service.docman.RightOperation;
import com.opentext.livelink.service.docman.RightPropagation;
import com.opentext.livelink.service.memberservice.MemberService;
import com.opentext.livelink.service.memberservice.MemberService_Service;
import com.opentext.livelink.service.searchservices.DataBagType;
import com.opentext.livelink.service.searchservices.SGraph;
import com.opentext.livelink.service.searchservices.SResultPage;
import com.opentext.livelink.service.searchservices.SearchService;
import com.opentext.livelink.service.searchservices.SearchService_Service;
import com.opentext.livelink.service.searchservices.SingleSearchRequest;
import com.opentext.livelink.service.searchservices.SingleSearchResponse;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.developer.WSBindingProvider;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;

/**
 *
 * @author Taccu
 */
public abstract class ContentServerTask extends Thread{
    public static final String SEARCH_API = "Livelink Search API V1.1";
    private static final OTAuthentication OT_AUTH = new OTAuthentication();
    public final Logger logger;
    private final Timer timer;
    private final String user, password;
    private int processedItems = 0;
    public final boolean export;
    public final ArrayList<Long> exportIds = new ArrayList<>();
    public static Connection CONNECTION;
    public static String URL;
    private static final Semaphore SEMA = new Semaphore(1);
   
    /**
     *
     * @param logger
     * @param user
     * @param password
     */
    public ContentServerTask(Logger logger, String user, String password, boolean export){
        this.logger = logger;
        this.user = user;
        this.password = password;
        this.export = export;
        timer = new Timer();
    }
        
    public void setProcessedItems(int items) {
        processedItems = items;
    }
    protected void handleError(Exception e) {
        e.printStackTrace();
        logger.error(e.getMessage());
        logger.debug("Interrupting thread...");
        Thread.currentThread().interrupt();
    }
    protected void connectToDatabase(String server, String db) throws ClassNotFoundException, SQLException {
        if(CONNECTION != null && CONNECTION.isValid(10)) return;
        URL = "jdbc:sqlserver://"+server +";databaseName=" +db+";integratedSecurity=true";
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        CONNECTION = DriverManager.getConnection(URL);
    }
    protected void applyRights(DocumentManagement docManClient, Node from, Node to) {
        logger.info("Setting node rights from node " + from.getName() + "(id:" + from.getID() +")" + " to node " + to.getName() + "(id:" + to.getID() + ")");
        docManClient.setNodeRights(to.getID(), docManClient.getNodeRights(from.getID()));
        
    }
    protected void inheritRights(DocumentManagement docManClient, Node from){
        logger.info("Inheriting node right from node "+ from.getName() + "(id:" + from.getID() +")" );
        ChunkedOperationContext updateNodeRightsContext = docManClient.updateNodeRightsContext(from.getID(), RightOperation.ADD_REPLACE, docManClient.getNodeRights(from.getID()).getACLRights(), RightPropagation.TARGET_AND_CHILDREN);
        updateNodeRightsContext.setChunkSize(1);
        try {
            NodeRightUpdateInfo chunkIt = chunkIt(docManClient.updateNodeRights(updateNodeRightsContext),updateNodeRightsContext);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    protected List<Long> getNodesInContainerWithRightId(long baseId, List<Long> subTypes, String dbServer, String dbName, long rightId) throws ClassNotFoundException, SQLException{
        List<Long> dataIds = new ArrayList<>();
        connectToDatabase(dbServer, dbName);
        PreparedStatement ps = CONNECTION.prepareStatement("WITH DCTE AS\n" +
            "(\n" +
            "SELECT DataID,ParentID,Name\n" +
            "FROM GALCS1.csadmin.DTree\n" +
            "WHERE DataID = ?\n" +
            "AND SubType IN (0,144,848)\n" +
            "UNION ALL\n" +
            "SELECT dt.DataID, dt.ParentID, dt.Name\n" +
            "FROM GALCS1.csadmin.DTree dt\n" +
            "INNER JOIN DCTE s ON dt.ParentID = s.DataID\n" +
            "WHERE SubType IN (0,144,848)\n" +
            ")\n" +
            "SELECT T1.DataID\n" +
            "FROM GALCS1.csadmin.DTree T1\n" +
            "RIGHT JOIN\n" +
            "( SELECT * FROM DCTE) AS T2\n" +
            "ON T1.DataID = T2.DataID\n" +
            "LEFT JOIN\n" +
            "(SELECT DataID\n" +
            "FROM GALCS1.csadmin.DTreeACL\n" +
            "WHERE RightID = ?) AS T3\n" +
            "ON T3.DataID = T1.DataID\n" +
            "WHERE T3.DataID IS NOT NULL");
        ps.setLong(1, baseId);
        ps.setLong(2, rightId);
        try {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                dataIds.add(rs.getLong("DataID"));
            }
            
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
        } finally {
            ps.close();
            CONNECTION.close();
        }
        return dataIds;
    }
    protected List<Long> getNodeIdsInContainer(long baseId, String dbServer, String dbName) throws ClassNotFoundException, SQLException {
        List<Long> dataIds = new ArrayList<>();
        connectToDatabase(dbServer, dbName);
        PreparedStatement ps = CONNECTION.prepareStatement("WITH DCTE AS\n" +
                    "(\n" +
                    "SELECT DataID,ParentID,Name\n" +
                    "FROM csadmin.DTree\n" +
                    "WHERE DataID = ?\n" +
                    "AND SubType IN (0,144,848)\n" +
                    "UNION ALL\n" +
                    "SELECT dt.DataID, dt.ParentID, dt.Name\n" +
                    "FROM csadmin.DTree dt\n" +
                    "INNER JOIN DCTE s ON dt.ParentID = s.DataID\n" +
                    "WHERE SubType IN (0,144,848)\n" +
                    ")\n" +
                    "SELECT DataID FROM DCTE;");
        ps.setLong(1, baseId);
        try {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                dataIds.add(rs.getLong("DataID"));
            }
            
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
        } finally {
            ps.close();
            CONNECTION.close();
        }
        return dataIds;
    }
    protected CategoryItemsUpgradeInfo chunkIt(CategoryItemsUpgradeInfo nrui){
        try {
        if(nrui.getUpgradedCount() > 0 ) {
            logger.debug("Updated " + nrui.getUpgradedCount() + " items...");
            DocumentManagement docManClient = getDocManClient();
            ChunkedOperationContext context = nrui.getContext();
            context.setChunkSize(200);
            chunkIt(docManClient.upgradeCategoryItems(context));
        }
        } catch(Exception e) {
            e.printStackTrace();
        }
            return nrui;
    }
    protected NodeRightUpdateInfo chunkIt(NodeRightUpdateInfo nrui, ChunkedOperationContext context){
        if(!context.isFinished()) {
            logger.debug("Updated " + nrui.getTotalNodeCount() + " items...");
            DocumentManagement docManClient = getDocManClient();
            context.setChunkSize(1);
            chunkIt(docManClient.updateNodeRights(context), context);
            
        }
        return nrui;
    }
    public SOAPHeaderElement generateSOAPHeaderElement(OTAuthentication oauth) throws SOAPException {
        // The namespace of the OTAuthentication object
        final String ECM_API_NAMESPACE = "urn:api.ecm.opentext.com";

        // Create a SOAP header
        SOAPHeader header = MessageFactory.newInstance().createMessage().getSOAPPart().getEnvelope().getHeader();

        if(header == null) {throw new SOAPException("Header was null");}
        // Add the OTAuthentication SOAP header element
        SOAPHeaderElement otAuthElement = header.addHeaderElement(new QName(ECM_API_NAMESPACE, "OTAuthentication"));

        // Add the AuthenticationToken SOAP element
        SOAPElement authTokenElement = otAuthElement.addChildElement(new QName(ECM_API_NAMESPACE, "AuthenticationToken"));

        authTokenElement.addTextNode(oauth.getAuthenticationToken());
        return otAuthElement;
    }
    public OTAuthentication loginUserWithPassword(String user, String password) {
        String authToken;
        try {
            if(OT_AUTH.getAuthenticationToken() == null || OT_AUTH.getAuthenticationToken().isEmpty()) {
                Authentication_Service authService = new Authentication_Service();
                Authentication authClient = authService.getBasicHttpBindingAuthentication();
                authToken = authClient.authenticateUser(user, password);
                // Create the OTAuthentication object and set the authentication token
                OT_AUTH.setAuthenticationToken(authToken);
            }
        } catch (Exception e) {
            handleError(e);
        }
        return OT_AUTH;
    } 
    public OTAuthentication loginUserWithPassword(String user, String password, boolean force) {
        String authToken;
        try {
            if(force) {
                Authentication_Service authService = new Authentication_Service();
                Authentication authClient = authService.getBasicHttpBindingAuthentication();
                authToken = authClient.authenticateUser(user , password);
                // Create the OTAuthentication object and set the authentication token
                OT_AUTH.setAuthenticationToken(authToken);
            }
            if(OT_AUTH.getAuthenticationToken() == null || OT_AUTH.getAuthenticationToken().isEmpty()) {
                Authentication_Service authService = new Authentication_Service();
                Authentication authClient = authService.getBasicHttpBindingAuthentication();
                authToken = authClient.authenticateUser(user , password);
                // Create the OTAuthentication object and set the authentication token
                OT_AUTH.setAuthenticationToken(authToken);
            }
        } catch (Exception e) {
            handleError(e);
        }
        return OT_AUTH;
    }  
    public static void writeArrayToPath(List<Long> list, Path path) throws IOException {
        List<String> arrayList = new ArrayList<>(list.size());
        list.stream().forEach((myLong) -> {
            arrayList.add(String.valueOf(myLong));
        });
        Files.write(path,arrayList,Charset.defaultCharset());
    }  
    public DocumentManagement getDocManClient() {
        // Create the DocumentManagement service client
        try {
            DocumentManagement_Service docManService = new DocumentManagement_Service();
            
            DocumentManagement docManClient = docManService.getBasicHttpBindingDocumentManagement();
            SOAPHeaderElement header;
            header = generateSOAPHeaderElement(loginUserWithPassword(user, password));
            ((WSBindingProvider) docManClient).setOutboundHeaders(Headers.create(header));
            //((BindingProvider) docManClient).getRequestContext().put("javax.xml.ws.client.receiveTimeout", "1000000");
            return docManClient;
        }
        catch(Exception e) {
            handleError(e);
        }
        return null;
    }
    public DocumentManagement getDocManClient(boolean force) {
        // Create the DocumentManagement service client
        try {
            DocumentManagement_Service docManService = new DocumentManagement_Service();
            DocumentManagement docManClient = docManService.getBasicHttpBindingDocumentManagement();
            SOAPHeaderElement header;
            header = generateSOAPHeaderElement(loginUserWithPassword(user, password, force));
            ((WSBindingProvider) docManClient).setOutboundHeaders(Headers.create(header));
            return docManClient;
        }
        catch(Exception e) {
            handleError(e);
        }
        return null;
    }
    public MemberService getMsClient() {
        try {
            MemberService_Service memServService = new MemberService_Service();
            MemberService msClient = memServService.getBasicHttpBindingMemberService();
            SOAPHeaderElement header;
            header = generateSOAPHeaderElement(loginUserWithPassword(user, password));
            ((WSBindingProvider) msClient).setOutboundHeaders(Headers.create(header));
            return msClient;
        } catch(Exception e) {
            handleError(e);
        }
        return null;
    }
    public MemberService getMsClient(boolean force) {
        try {
            MemberService_Service memServService = new MemberService_Service();
            MemberService msClient = memServService.getBasicHttpBindingMemberService();
            SOAPHeaderElement header;
            header = generateSOAPHeaderElement(loginUserWithPassword(user, password, force));
            ((WSBindingProvider) msClient).setOutboundHeaders(Headers.create(header));
            return msClient;
        } catch(Exception e) {
            handleError(e);
        }
        return null;
    }
    public Classifications getClassifyClient() {
        // Create the DocumentManagement service client
        try {
            Classifications_Service docManService = new Classifications_Service();
            Classifications docManClient = docManService.getBasicHttpBindingClassifications();
            SOAPHeaderElement header;
            header = generateSOAPHeaderElement(loginUserWithPassword(user, password));
            ((WSBindingProvider) docManClient).setOutboundHeaders(Headers.create(header));
            return docManClient;
        }
        catch(Exception e) {
            handleError(e);
        }
        return null;
    }  
    public SearchService getSearchClient(){
        try {
            SearchService_Service seServService = new SearchService_Service();
            
            SearchService seClient = seServService.getBasicHttpBindingSearchService();
            SOAPHeaderElement header;
            header = generateSOAPHeaderElement(loginUserWithPassword(user, password));
            ((WSBindingProvider) seClient).setOutboundHeaders(Headers.create(header));
            return seClient;
        }
        catch(Exception e){
            handleError(e);
        }
        return null;
    }  
    public List<Long> getNodesBySearch(SearchService sService, String queryString){
        SingleSearchRequest query = new SingleSearchRequest();
        List<String> dataCollections = sService.getDataCollections();
        query.setDataCollectionSpec("'LES Enterprise'");
        query.setQueryLanguage(SEARCH_API);
        query.setFirstResultToRetrieve(1);
        query.setNumResultsToRetrieve(500000);
        query.setResultSetSpec(queryString);
        query.setResultOrderSpec("sortByRegion=OTCreatedBy");
        query.getResultTransformationSpec().add("OTName");
        query.getResultTransformationSpec().add("OTLocation");
        
        SingleSearchResponse results = sService.search(query, "");
        SResultPage srp= results.getResults();
        List<SGraph> sra = results.getResultAnalysis();
        ArrayList<Long> nodes = new ArrayList<>();
        
        if(srp != null) {
            List<SGraph> items = srp.getItem();
            List<DataBagType> types = srp.getType();
            
            if(items != null && types != null && items.size() > 0) {
                for(SGraph item : items) {
                    String extractId = extractId(item.getID());
                    nodes.add(Long.valueOf(extractId));
                }
            }
        }
       return nodes;
    }
    private String extractId(String string){
        return string.replaceAll("(.*)DataId=", "").replaceAll("&(.*)", "");
    }
    @Override
    public void run() {
        logger.info("Starting...");         
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    SEMA.acquire();
                    logger.info("Be patient, we are still updating...");
                    getDocManClient(true);
                    
                    SEMA.release();
                } catch (InterruptedException ex) {
                    java.util.logging.Logger.getLogger(InfoCatMod.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
            }
            }, 1*240*1000, 1*240*1000);
        
        long startTime = System.currentTimeMillis();
        try {
            doWork();
            if(export && exportIds.size() > 0) {
                try{
                    writeArrayToPath(exportIds,Paths.get(getNameOfTask()+".txt"));
                } catch(IOException ex) {
                    logger.error("Couldn't write " + getNameOfTask() + ".txt" );
                    logger.error(ex.getMessage());
                }
            }
        }catch(Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            timer.cancel();
            timer.purge();
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            if(processedItems > 0) {
                logger.info("Finished task " + getNameOfTask() + " with " + processedItems +" items in " + elapsedTime + " milliseconds...");
            } else {
                logger.info("Finished task " + getNameOfTask() + " in " + elapsedTime + " milliseconds...");
            }
        }
    }
    public abstract void doWork() throws InterruptedException;
    public abstract String getNameOfTask();
}
