package org.th3falc0n.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PoolThread extends Thread {
    volatile long stateTime = 0;
    volatile long taskTime = 0;
    volatile boolean isWorking = false;
    
    volatile ConcurrentLinkedQueue<Runnable> scheduledWork = new ConcurrentLinkedQueue<Runnable>();
    volatile boolean closeThread = false;
    
    private ManagedThreadPool threadPool = null;
    
    public PoolThread(ManagedThreadPool pool) {
        threadPool = pool;
    }
    
    public PoolThread(ManagedThreadPool pool, String string) {
        super(string);
        threadPool = pool;
    }

    double getInStateTime() {
        if(stateTime == 0) return 0;
        return (System.nanoTime() - stateTime) / 1000000.0;
    }
    
    double getTaskTime() {
        if(taskTime == 0) return 0;
        return (System.nanoTime() - taskTime) / 1000000.0;
    }
    
    @Override
    public void run() {
        while(true) {
            if(!scheduledWork.isEmpty()) {
                Runnable work = scheduledWork.poll();
                
                if(work != null) {
                    if(!isWorking) stateTime = System.nanoTime();
                    isWorking = true;
                    taskTime = System.nanoTime();
                    
                    work.run();
                }
            }
            else
            {
                if(isWorking) stateTime = System.nanoTime();
                isWorking = false;
                taskTime = 0;
                
                Runnable workSteal = threadPool.attemptWorkSteal(this);
                
                if(workSteal != null) {
                    scheduledWork.add(workSteal);
                    continue;
                }
                
                
                try {
                    synchronized (this) {
                        wait(100);
                    }
                } catch (InterruptedException e) {
                    
                }
                
                if(closeThread) return;
            }
        }
    }
    
    
}
