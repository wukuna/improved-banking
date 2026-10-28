package com.windowmarkers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingWorker;
import org.junit.Test;
import static org.junit.Assert.*;

public class CatalogueWorkQueueTest
{
    private SwingWorker<Void,Void> work(CatalogueWorkQueue queue, int id, List<Integer> built,
        CountDownLatch started, CountDownLatch release, CountDownLatch finished, boolean fail)
    {
        return new SwingWorker<Void,Void>() {
            protected Void doInBackground() throws Exception {
                built.add(id); started.countDown();
                if(!release.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("test worker timed out");
                if(fail) throw new IllegalStateException("failed catalogue");
                return null;
            }
            protected void done() { try { queue.finished(this); } finally { finished.countDown(); } }
        };
    }

    @Test public void rapidChangesBuildOnlyTheRunningAndNewestSnapshot() throws Exception
    {
        CatalogueWorkQueue queue=new CatalogueWorkQueue();
        List<Integer> built=Collections.synchronizedList(new ArrayList<>());
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(2);
        queue.submit(work(queue,0,built,started,release,finished,false));
        try {
            assertTrue(started.await(5,TimeUnit.SECONDS));
            for(int i=1;i<=30;i++) queue.submit(work(queue,i,built,new CountDownLatch(0),new CountDownLatch(0),finished,false));
            assertEquals(Collections.singletonList(0),built);
        } finally { release.countDown(); }
        assertTrue(finished.await(5,TimeUnit.SECONDS));
        assertEquals(java.util.Arrays.asList(0,30),built);
    }

    @Test public void stoppedContextDropsQueuedWorkAndFailureAllowsNewContext() throws Exception
    {
        CatalogueWorkQueue queue=new CatalogueWorkQueue();
        List<Integer> built=Collections.synchronizedList(new ArrayList<>());
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(2);
        queue.submit(work(queue,1,built,started,release,finished,true));
        try {
            assertTrue(started.await(5,TimeUnit.SECONDS));
            queue.submit(work(queue,2,built,new CountDownLatch(0),new CountDownLatch(0),finished,false));
            queue.clearPending();
            queue.submit(work(queue,3,built,new CountDownLatch(0),new CountDownLatch(0),finished,false));
        } finally { release.countDown(); }
        assertTrue(finished.await(5,TimeUnit.SECONDS));
        assertEquals(java.util.Arrays.asList(1,3),built);
        CountDownLatch again=new CountDownLatch(1);
        queue.submit(work(queue,4,built,new CountDownLatch(0),new CountDownLatch(0),again,false));
        assertTrue(again.await(5,TimeUnit.SECONDS));
        assertEquals(java.util.Arrays.asList(1,3,4),built);
    }
}
