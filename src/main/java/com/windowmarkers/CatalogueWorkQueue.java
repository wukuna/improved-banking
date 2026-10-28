package com.windowmarkers;

import javax.swing.SwingWorker;

/** Keep one catalogue build running and only the newest waiting bank snapshot. */
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

    // Running work owns detached data and may finish; the revision guard rejects
    // its result. Drop queued work on account/profile changes and shutdown.
    synchronized void clearPending() { waiting = null; }
}
