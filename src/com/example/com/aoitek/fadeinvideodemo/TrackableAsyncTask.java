/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.com.aoitek.fadeinvideodemo;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * {@link AsyncTask} substitution for the email app.
 *
 * Modeled after {@link AsyncTask}; the basic usage is the same, with extra features:
 * - Bulk cancellation of multiple tasks.  This is mainly used by UI to cancel pending tasks
 *   in onDestroy() or similar places.
 * - Instead of {@link AsyncTask#onPostExecute}, it has {@link #onSuccess(Object)}, as the
 *   regular {@link AsyncTask#onPostExecute} is a bit hard to predict when it'll be called and
 *   whel it won't.
 *
 * Note this class is missing some of the {@link AsyncTask} features, e.g. it lacks
 * {@link AsyncTask#onProgressUpdate}.  Add these when necessary.
 */
public abstract class TrackableAsyncTask<Params, Progress, Result> {
    private static final Executor SERIAL_EXECUTOR = AsyncTask.SERIAL_EXECUTOR;
    private static final Executor PARALLEL_EXECUTOR = AsyncTask.THREAD_POOL_EXECUTOR;

    /**
     * Tracks {@link TrackableAsyncTask}.
     *
     * Call {@link #cancellAllInterrupt()} to cancel all tasks registered.
     */
    public static class Tracker {
        private final LinkedList<TrackableAsyncTask<?, ?, ?>> mTasks =
                new LinkedList<TrackableAsyncTask<?, ?, ?>>();

        private void add(TrackableAsyncTask<?, ?, ?> task) {
            synchronized (mTasks) {
                mTasks.add(task);
            }
        }

        private void remove(TrackableAsyncTask<?, ?, ?> task) {
            synchronized (mTasks) {
                mTasks.remove(task);
            }
        }

        /**
         * Cancel all registered tasks.
         */
        public void cancellAllInterrupt() {
            synchronized (mTasks) {
                for (TrackableAsyncTask<?, ?, ?> task : mTasks) {
                    task.cancel(true);
                }
                mTasks.clear();
            }
        }

        /**
         * Cancel all instances of the same class as {@code current} other than
         * {@code current} itself.
         */
        /* package */ void cancelOthers(TrackableAsyncTask<?, ?, ?> current) {
            final Class<?> clazz = current.getClass();
            synchronized (mTasks) {
                final ArrayList<TrackableAsyncTask<?, ?, ?>> toRemove =
                        new ArrayList<TrackableAsyncTask<?, ?, ?>>();
                for (TrackableAsyncTask<?, ?, ?> task : mTasks) {
                    if ((task != current) && task.getClass().equals(clazz)) {
                        task.cancel(true);
                        toRemove.add(task);
                    }
                }
                for (TrackableAsyncTask<?, ?, ?> task : toRemove) {
                    mTasks.remove(task);
                }
            }
        }

        /* package */ int getTaskCountForTest() {
            return mTasks.size();
        }

        /* package */ boolean containsTaskForTest(TrackableAsyncTask<?, ?, ?> task) {
            return mTasks.contains(task);
        }
    }

    private final Tracker mTracker;

    private static class InnerTask<Params2, Progress2, Result2>
            extends AsyncTask<Params2, Progress2, Result2> {
        private final TrackableAsyncTask<Params2, Progress2, Result2> mOwner;

        public InnerTask(TrackableAsyncTask<Params2, Progress2, Result2> owner) {
            mOwner = owner;
        }

        @Override
        protected Result2 doInBackground(Params2... params) {
            return mOwner.doInBackground(params);
        }

        @Override
        public void onCancelled(Result2 result) {
            mOwner.unregisterSelf();
            mOwner.onCancelled(result);
        }

        @Override
        public void onPostExecute(Result2 result) {
            mOwner.unregisterSelf();
            if (mOwner.mCancelled) {
                mOwner.onCancelled(result);
            } else {
                mOwner.onSuccess(result);
            }
        }
    }

    private final InnerTask<Params, Progress, Result> mInnerTask;
    private volatile boolean mCancelled;

    public TrackableAsyncTask(Tracker tracker) {
        mTracker = tracker;
        if (mTracker != null) {
            mTracker.add(this);
        }
        mInnerTask = new InnerTask<Params, Progress, Result>(this);
    }

    /* package */ final void unregisterSelf() {
        if (mTracker != null) {
            mTracker.remove(this);
        }
    }

    /** @see AsyncTask#doInBackground */
    protected abstract Result doInBackground(Params... params);


    /** @see AsyncTask#cancel(boolean) */
    public final void cancel(boolean mayInterruptIfRunning) {
        mCancelled = true;
        mInnerTask.cancel(mayInterruptIfRunning);
    }

    /** @see AsyncTask#onCancelled */
    protected void onCancelled(Result result) {
    }

    /**
     * Similar to {@link AsyncTask#onPostExecute}, but this will never be executed if
     * {@link #cancel(boolean)} has been called before its execution, even if
     * {@link #doInBackground(Object...)} has completed when cancelled.
     *
     * @see AsyncTask#onPostExecute
     */
    protected void onSuccess(Result result) {
    }

    /**
     * execute on {@link #PARALLEL_EXECUTOR}
     *
     * @see AsyncTask#execute
     */
    public final TrackableAsyncTask<Params, Progress, Result> executeParallel(Params... params) {
        return executeInternal(PARALLEL_EXECUTOR, false, params);
    }

    /**
     * execute on {@link #SERIAL_EXECUTOR}
     *
     * @see AsyncTask#execute
     */
    public final TrackableAsyncTask<Params, Progress, Result> executeSerial(Params... params) {
        return executeInternal(SERIAL_EXECUTOR, false, params);
    }

    /**
     * Cancel all previously created instances of the same class tracked by the same
     * {@link Tracker}, and then {@link #executeParallel}.
     */
    public final TrackableAsyncTask<Params, Progress, Result> cancelPreviousAndExecuteParallel(
            Params... params) {
        return executeInternal(PARALLEL_EXECUTOR, true, params);
    }

    /**
     * Cancel all previously created instances of the same class tracked by the same
     * {@link Tracker}, and then {@link #executeSerial}.
     */
    public final TrackableAsyncTask<Params, Progress, Result> cancelPreviousAndExecuteSerial(
            Params... params) {
        return executeInternal(SERIAL_EXECUTOR, true, params);
    }

    private final TrackableAsyncTask<Params, Progress, Result> executeInternal(Executor executor,
            boolean cancelPrevious, Params... params) {
        if (cancelPrevious) {
            if (mTracker == null) {
                throw new IllegalStateException();
            } else {
                mTracker.cancelOthers(this);
            }
        }
        mInnerTask.executeOnExecutor(executor, params);
        return this;
    }

    /**
     * Runs a {@link Runnable} in a bg thread, using {@link #PARALLEL_EXECUTOR}.
     */
    public static TrackableAsyncTask<Void, Void, Void> runAsyncParallel(Runnable runnable) {
        return runAsyncInternal(PARALLEL_EXECUTOR, runnable);
    }

    /**
     * Runs a {@link Runnable} in a bg thread, using {@link #SERIAL_EXECUTOR}.
     */
    public static TrackableAsyncTask<Void, Void, Void> runAsyncSerial(Runnable runnable) {
        return runAsyncInternal(SERIAL_EXECUTOR, runnable);
    }

    private static TrackableAsyncTask<Void, Void, Void> runAsyncInternal(Executor executor,
            final Runnable runnable) {
        TrackableAsyncTask<Void, Void, Void> task = new TrackableAsyncTask<Void, Void, Void>(null) {
            @Override
            protected Void doInBackground(Void... params) {
                runnable.run();
                return null;
            }
        };
        return task.executeInternal(executor, false, (Void[]) null);
    }

    /**
     * Wait until {@link #doInBackground} finishes and returns the results of the computation.
     *
     * @see AsyncTask#get
     */
    public final Result get() throws InterruptedException, ExecutionException {
        return mInnerTask.get();
    }

    /* package */ final Result callDoInBackgroundForTest(Params... params) {
        return mInnerTask.doInBackground(params);
    }

    /* package */ final void callOnCancelledForTest(Result result) {
        mInnerTask.onCancelled(result);
    }

    /* package */ final void callOnPostExecuteForTest(Result result) {
        mInnerTask.onPostExecute(result);
    }
}
