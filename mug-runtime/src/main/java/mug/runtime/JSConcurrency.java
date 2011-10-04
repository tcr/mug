package mug.runtime;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class JSConcurrency
{
	static final int NTHREADS = 10;
	static final ExecutorService executor = Executors.newCachedThreadPool();
	static final ExecutorCompletionService<Runnable> completion = new ExecutorCompletionService<Runnable>(executor);
	
	static int tasks = 0;
	static public Future<Runnable> submitTask(Callable<Runnable> cb) {
		tasks++;
		return completion.submit(cb);
	}
	
	static public abstract class JSAsyncFunction extends JSFunction
	{
		public JSAsyncFunction(JSEnvironment env) {
			super(env);
		}

		@Override
		public Object invoke(final Object ths, final int argc, final Object l0, final Object l1, final Object l2, final Object l3, final Object l4, final Object l5, final Object l6, final Object l7, final Object[] rest) throws Exception {
			submitTask(new Callable<Runnable>() {
				public Runnable call() throws Exception {
					return invokeAsync(ths, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				}
			});
			return null;
		}
		
		/**
		 * Abstract async class. Returns runnable to evaluate on main thread.
		 */
		
		protected abstract Runnable invokeAsync(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception;
		
		protected Runnable invokeSync(final JSObject callback, final Object ths, final int argc, final Object l0, final Object l1, final Object l2, final Object l3, final Object l4, final Object l5, final Object l6, final Object l7, final Object[] rest) {
			return new Runnable() {
				public void run() {
					try {
						callback.invoke(ths, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
					} catch (Throwable e) { }
				}
			};
		}
		
		/* convenience methods */
		
		final protected Runnable invokeSync(JSObject callback, Object ths) throws Exception {
			return invokeSync(callback, ths, 0, null, null, null, null, null, null, null, null, null);
		}
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object l0) throws Exception {
			return invokeSync(callback, ths, 1, l0, null, null, null, null, null, null, null, null);
		}
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object l0, Object l1) throws Exception {
			return invokeSync(callback, ths, 2, l0, l1, null, null, null, null, null, null, null);
		}
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object l0, Object l1, Object l2) throws Exception {
			return invokeSync(callback, ths, 3, l0, l1, l2, null, null, null, null, null, null);
		}
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object l0, Object l1, Object l2, Object l3) throws Exception {
			return invokeSync(callback, ths, 4, l0, l1, l2, l3, null, null, null, null, null);
		}
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object l0, Object l1, Object l2, Object l3, Object l4) throws Exception {
			return invokeSync(callback, ths, 5, l0, l1, l2, l3, l4, null, null, null, null);
		}
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5) throws Exception {
			return invokeSync(callback, ths, 6, l0, l1, l2, l3, l4, l5, null, null, null);
		}
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6) throws Exception {
			return invokeSync(callback, ths, 7, l0, l1, l2, l3, l4, l5, l6, null, null);
		}
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7) throws Exception {
			return invokeSync(callback, ths, 8, l0, l1, l2, l3, l4, l5, l6, l7, null);
		}	
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return invokeSync(callback, ths, rest.length + 8, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		}
		
		final protected Runnable invokeSync(JSObject callback, Object ths, Object[] args) throws Exception {
			Object l0 = null, l1 = null, l2 = null, l3 = null, l4 = null, l5 = null, l6 = null, l7 = null;
			switch (args.length > 8 ? 8 : args.length) {
			case 8: l7 = args[7];
			case 7: l6 = args[6];
			case 6: l5 = args[5];
			case 5: l4 = args[4];
			case 4: l3 = args[3];
			case 3: l2 = args[2];
			case 2: l1 = args[1];
			case 1: l0 = args[0];
			}
			Object[] rest = null;
			if (args.length > 8) {
				rest = new Object[Math.max(args.length - 8, 0)];
				System.arraycopy(args, 8, rest, 0, rest.length);
			}
			return invokeSync(callback, ths, args.length, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		}
	}
	
	/**
	 * Timers
	 */
	
	static long TIMEOUTID = 0;
	
	static HashMap<Long, Future<Runnable>> timers = new HashMap();
	
	static long setTimeout(final JSObject fn, final Object[] args, final long milliseconds) {
		long id = TIMEOUTID;
		TIMEOUTID += 2;
		Callable<Runnable> task = new Callable<Runnable>() {
			public Runnable call() throws Exception {
				Thread.sleep(milliseconds);
				return new Runnable() {
					public void run() {
						try {
							fn.invoke(null, args);
						} catch (Exception e) {
						}
					}
				};
			}
		};
		timers.put(id, submitTask(task));
		return id;
	}
	
	static void clearTimeout(long id) {
		if ((id & 1) == 1)
			return;
		if (timers.containsKey(id))
			timers.get(id).cancel(false);
		timers.remove(id);
	}
	
	static long setInterval(final JSObject fn, final Object[] args, final long milliseconds) {
		long id = TIMEOUTID + 1;
		TIMEOUTID += 2;
		final Callable<Runnable> task = new Callable<Runnable>() {
			final Callable<Runnable> _task = this;
			public Runnable call() throws Exception {
				Thread.sleep(milliseconds);
				return new Runnable() {
					public void run() {
						submitTask(_task); // resubmit task
						try {
							fn.invoke(null, args);
						} catch (Exception e) {
						}
					}
				};
			}
		};
		timers.put(id, submitTask(task));
		return id;
	}
	
	static void clearInterval(long id) {
		if ((id & 1) == 0)
			return;
		if (timers.containsKey(id))
			timers.get(id).cancel(false);
		timers.remove(id);
	}
	
	static public void awaitTaskPool() {
		while (tasks > 0) {
			try {
				Runnable task = completion.take().get();
				task.run();
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
			}
			tasks--;
		}
		executor.shutdown();
	}
}
