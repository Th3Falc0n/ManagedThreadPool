package org.th3falc0n.pool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

public class ManagedThreadPool {
    int maxLiveThreads = 0;
    int maxPersistentThreads = 256;
    
    int timeTillNew = 50; //time block causes a new thread to join the active pool
    int timeTillPers = 375; //time till block causes a single task to join the persistent pool
    int timeTillClose = 1000; //idle time till a existing thread will close;
    
    int liveThreadNumber = 0;
    int persThreadNumber = 0;
    
    Deque<PoolThread> liveThreads = new LinkedBlockingDeque<>();
    
    Deque<PoolThread> persistentThreads = new LinkedBlockingDeque<>();
    
    ManagerThread managerThread;
    
    public ManagedThreadPool(int maxThreads) {
        maxLiveThreads = maxThreads;
        
        (managerThread = new ManagerThread()).start();
    }
    
    public void execute(Runnable r) {
        synchronized (managerThread) {
            managerThread.notify();
        }
        
        PoolThread thread = getIdealThread();
        
        thread.scheduledWork.add(r);
        
        synchronized (thread) {
            thread.notify();
        }
    }
    
    private PoolThread getIdealThread() {
        double minBlockTime = Double.MAX_VALUE;
        PoolThread mbtThread = null;
        
        for(PoolThread t : liveThreads) {            
            if(!t.isWorking) return t;
            
            if(minBlockTime > t.getInStateTime()) {
                minBlockTime = t.getInStateTime();
                mbtThread = t;
            }
        }
        
        if(mbtThread != null) return mbtThread;
        
        System.out.println("MTP: Started new thread on execute");
        
        return createNewLiveThread();
    }
    
    private PoolThread createNewLiveThread() {
        PoolThread pthread = new PoolThread(ManagedThreadPool.this, "MTP Live Thread " + (liveThreadNumber++));
        pthread.start();
        
        liveThreads.add(pthread);
        
        return pthread;
    }
    
    private class ManagerThread extends Thread {
        public ManagerThread() {
            this.setDaemon(true);;
            this.setPriority(MAX_PRIORITY);
            this.setName("MTP Manager Thread");
        }
        
        @Override
        public void run() {
            while(true) {                
                double blockedTime = Double.MAX_VALUE;

                Iterator<PoolThread> iterator = liveThreads.iterator();
                
                while(iterator.hasNext()) {
                    PoolThread t = iterator.next();
                    
                    if(!t.isWorking && t.getInStateTime() > timeTillClose) {
                        System.out.println("MTP: Idle thread closed (" + liveThreads.size() + "/" + maxLiveThreads + " active)");
                        
                        t.closeThread = true;
                        liveThreads.remove(t);
                      
                        synchronized(t) {
                            t.notify();
                        }
                    }
                    
                    if(!t.isWorking) blockedTime = 0;
                    
                    if(t.isWorking) {
                        blockedTime = Math.min(blockedTime, t.getInStateTime());
                        
                        if(t.getTaskTime() > timeTillPers) {
                            liveThreads.remove(t);
                            
                            synchronized (persistentThreads) {
                                persistentThreads.add(t);
                                t.closeThread = true;
                                
                                t.setName("MTP Persistent Thread " + (persThreadNumber++));
                                
                                PoolThread ptr = createNewLiveThread();
                                ptr.setName(ptr.getName() + " (rep)");
                                
                                while(!t.scheduledWork.isEmpty()) {
                                    ptr.scheduledWork.add(t.scheduledWork.poll());
                                }

                                synchronized (ptr) {
                                    ptr.notify();
                                }
                                
                                System.out.println("MTP: Moved thread to persistent pool (" + persistentThreads.size() + " active)");
                            }
                        }
                    }
                }
                
                if(blockedTime > timeTillNew && liveThreads.size() < maxLiveThreads && blockedTime != Double.MAX_VALUE) {
                    createNewLiveThread();
                    
                    System.out.println("MTP: Started new thread on block time (" + liveThreads.size() + "/" + maxLiveThreads + " active)");
                }

                iterator = persistentThreads.iterator();

                while(iterator.hasNext()) {
                    PoolThread t = iterator.next();
                    if(t.getState() == State.TERMINATED) {
                        persistentThreads.remove(t);
                        
                        System.out.println("MTP: Thread finished in persistent pool (" + persistentThreads.size() + " active)");
                    }
                } 
                
                try {
                    synchronized (this) {
                        wait(1);
                    }
                } catch (InterruptedException e) {
                    
                }
            }
        }
    }

    public Runnable attemptWorkSteal(PoolThread poolThread) {
        synchronized (managerThread) {
            managerThread.notify();
        }
        
        PoolThread maxTaskTimeThread = null;
        double maxTaskTime = 0;
        
        Iterator<PoolThread> iterator = liveThreads.iterator();
        
        while(iterator.hasNext()) {
            PoolThread t = iterator.next();
            if(t == poolThread) continue;
            
            if(t.getTaskTime() > maxTaskTime && !t.scheduledWork.isEmpty()) {
                maxTaskTime = t.getTaskTime();
                maxTaskTimeThread = t;
            }
        }
        
        if(maxTaskTimeThread == null) {
            return null;
        }
        
        return maxTaskTimeThread.scheduledWork.poll();
    }
}
