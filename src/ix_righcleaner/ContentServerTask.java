/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.ecm.api.OTAuthentication;
import com.opentext.livelink.service.core.Authentication;
import com.opentext.livelink.service.core.Authentication_Service;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.DocumentManagement_Service;
import com.opentext.livelink.service.memberservice.MemberService;
import com.opentext.livelink.service.memberservice.MemberService_Service;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.developer.WSBindingProvider;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    private static final OTAuthentication OT_AUTH = new OTAuthentication();
    private final Logger logger;
    private final Timer timer;
    private final String user, password;
    private int processedItems = 0;
    /**
     *
     * @param logger
     * @param user
     * @param password
     */
    public ContentServerTask(Logger logger, String user, String password){
        this.logger = logger;
        this.user = user;
        this.password = password;
        timer = new Timer();
    }
        
    public void setProcessedItems(int items) {
        processedItems = items;
    }
    
    protected void handleError(Exception e) {
        logger.error(e.getMessage());
        logger.debug("Interrupting thread...");
        Thread.currentThread().interrupt();
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
        list.forEach((myLong) -> {
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
    
    @Override
    public void run() {
        logger.info("Starting...");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("Be patient, we are still updating...");
            }
            }, 1*30*1000, 1*60*1000);
        
        long startTime = System.currentTimeMillis();
        try {
            doWork();
        }catch(Exception e) {
            logger.error(e.getMessage());
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
