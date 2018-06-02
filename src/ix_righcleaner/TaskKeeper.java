/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_righcleaner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Taccu
 */
public class TaskKeeper extends Thread{
    
    private static final ExecutorService EXEC_SVC = Executors.newCachedThreadPool();
    private boolean stopping;
    private static final List<Future<String>> FUTURES = Collections.synchronizedList(new ArrayList<>());
    private final Logger logger;

    /**
     *
     * @param logger
     */
    public TaskKeeper(Logger logger){
        stopping = false;
        this.logger = logger;
    }
    public void addNewTask(Runnable run) {
        EXEC_SVC.submit(run);
    }
    @Override
    public void run(){
        while(!stopping) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                logger.warn(ex.getMessage());
                break;
            }
            FUTURES.forEach((fut) -> {
                try {
                    fut.get(100000L, TimeUnit.SECONDS);
                }catch(InterruptedException | ExecutionException | TimeoutException e) {
                    logger.warn(e.getMessage());
                }
            });
        }
        EXEC_SVC.shutdown();
    }
    
    public void stopTask() {
        stopping = true;
        Thread.currentThread().interrupt();
    }
}
