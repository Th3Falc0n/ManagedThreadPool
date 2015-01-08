package org.th3falc0n.pool.debug;

import java.io.IOException;
import java.lang.management.MemoryNotificationInfo;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.th3falc0n.pool.ManagedThreadPool;

public class Test {
    volatile static int executed = 0;
    volatile static int count = 10000;
    
    public static void main(String... args) {
        ManagedThreadPool mpool = new ManagedThreadPool(8);
        ExecutorService tpool = Executors.newFixedThreadPool(8);
        
        Random rnd = new Random();
        
        System.out.println("Press enter to start");
        try {
            System.in.read();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        long tStart = System.nanoTime();
        
        for(int iouter = 0; iouter < count; iouter++) {
            tpool.execute(new Runnable() {                
                @SuppressWarnings("unused")
                @Override
                public void run() {
                    int cycles;
                    float c = 16;
                    
                    /*while(rnd.nextFloat() < 0.45) {
                        c *= 1.6;
                    }*/
                    
                    cycles = (int)c;
                    
                    
                    for(int y = 0; y < 100; y++) {
                        for(int i = 0; i < cycles; i++) {
                            int a = (i * y) % 128;
                            int b = (i * y) % 127;
                            
                            int x = (a * b) % (i + a + b + 1);
                        }
                    }
                    
                    executed++;
                    
                    //System.out.println("executed " + cycles + " cycles");
                }
            });
        }
        
        long tDif = System.nanoTime() - tStart;
        
        System.out.println("Initialized in " + tDif / 1000000000.0 + " seconds");
        
        while(executed != count) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            System.out.println("TEST: Executed " + executed + " of " + count);
        }
        
        tDif = System.nanoTime() - tStart;
        
        System.out.println("Executed in " + tDif / 1000000000.0 + " seconds");
        
        System.exit(0);
    }
}
