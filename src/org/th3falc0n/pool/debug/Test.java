package org.th3falc0n.pool.debug;

import java.io.IOException;
import java.lang.management.MemoryNotificationInfo;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.th3falc0n.pool.ManagedThreadPool;

public class Test {
    volatile int executed = 0;
    volatile int count = 500000;
    
    public static void main(String... args) {
        new Test();
    }
    
    public Test() {
        ManagedThreadPool mpool = new ManagedThreadPool(4);

        for(int i = 0; i < count; i++) {
            mpool.execute(() -> {
               for(int n = 0; n < 1000; n++) {
                   Random rnd = new Random();
                   StringBuilder blub = new StringBuilder();                   
                   blub.append(rnd.nextInt());
               }
               
               synchronized (Test.this) {
                   executed++;
               }
            });
        }
        
        System.out.println("Finished!");
        
        while(true) {
            try {
                Thread.sleep(1000);
                
                System.out.println("Executed " + executed + " of " + count);
                
                if(executed == count) break;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
    }
}
