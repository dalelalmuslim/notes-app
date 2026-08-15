package com.example.notely.data;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Process-wide thread pools for background work.
 *
 * Disk I/O is serialized on a single worker so create/update/delete operations
 * can never interleave and lose updates. Network work (the update checker) uses
 * its own single worker so it can never stall note persistence. Both executors
 * live for the process lifetime, which is deliberate and bounded; they are not
 * recreated per Activity and therefore cannot leak threads.
 *
 * Callbacks must be delivered on the main thread; use {@link #postOnMain(Runnable)}.
 */
public final class AppExecutors {

    private static final ExecutorService DISK_IO = Executors.newSingleThreadExecutor();
    private static final ExecutorService NETWORK_IO = Executors.newSingleThreadExecutor();
    private static Handler sMainHandler;

    private AppExecutors() {
    }

    public static void runOnDiskIo(Runnable task) {
        DISK_IO.execute(task);
    }

    public static void runOnNetworkIo(Runnable task) {
        NETWORK_IO.execute(task);
    }

    public static void postOnMain(Runnable task) {
        Handler handler;
        synchronized (AppExecutors.class) {
            handler = sMainHandler;
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
                sMainHandler = handler;
            }
        }
        handler.post(task);
    }
}
