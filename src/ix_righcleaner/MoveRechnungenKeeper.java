/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import com.opentext.livelink.service.docman.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author bho
 */
public class MoveRechnungenKeeper extends Thread {
    private static final ExecutorService EXEC_SVC = Executors.newFixedThreadPool(30);
    private boolean stopping;
    public static final List<Future<Node>> FUTURES = Collections.synchronizedList(new ArrayList<>());
    private final Logger logger;
    private final ArrayList<Long> exportIds;
    public MoveRechnungenKeeper(ArrayList<Long> exportIds,Logger logger) {
        this.logger = logger;
        this.exportIds = exportIds;
    }
    public void addNewTask(Callable<Node> run) {
        EXEC_SVC.submit(run);
    }
    @Override
    public void run(){
        while(!stopping) {
            try {
                Thread.sleep(1000);
                logger.debug("Currently " + FUTURES.size() + " documents awaiting move...");
            } catch (InterruptedException ex) {
                logger.warn(ex.getMessage());
                break;
            }
            FUTURES.forEach((fut) -> {
                try {
                    Node get = fut.get(10000000L, TimeUnit.SECONDS);
                    
                    if(get!=null)exportIds.add(get.getID());
                }catch(InterruptedException | ExecutionException | TimeoutException e) {
                    logger.warn(e.getMessage());
                }
            });
        }
        EXEC_SVC.shutdown();
    }
    
    public void stopTask() {
        stopping = true;
    }
    
}
