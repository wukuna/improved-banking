package com.windowmarkers;

import javax.swing.SwingWorker;

final class CatalogueWorkQueue
{
    private SwingWorker<?, ?> running, waiting;

    synchronized void submit(SwingWorker<?, ?> task)
    {
        if (running != null) { waiting = task; return; }
        running = task;
        task.execute();
    }

    synchronized void finished(SwingWorker<?, ?> task)
    {
        if (task != running) return;
        running = waiting;
        waiting = null;
        if (running != null) running.execute();
    }

    synchronized void clearPending() { waiting = null; }
}
