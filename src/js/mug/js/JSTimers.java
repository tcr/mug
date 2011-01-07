package mug.js;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class JSTimers {	
	static class JSCallback {
		static long idCounter = 0;
		
		JSObject fn;
		boolean repeating;
		long delay;
		long id;
		boolean enabled = true;
		Object[] args;
		
		public JSCallback(JSObject fn, Object[] args, boolean repeating, long delay) {
			this.fn = fn;
			this.args = args;
			this.repeating = repeating;
			this.delay = delay;
			this.id = idCounter++;
		}
	}
	
	static HashMap<Long, JSCallback> ids = new HashMap();
	static HashMap<Long, ArrayList<JSCallback>> timeouts = new HashMap();
	static ArrayList<Long> timeoutTimes = new ArrayList();
	
	static long setTimeout(JSObject fn, Object[] args, long milliseconds) {
		Long time = System.currentTimeMillis() + milliseconds;
		timeoutTimes.add(time);
		Collections.sort(timeoutTimes);
		if (!timeouts.containsKey(time))
			timeouts.put(time, new ArrayList());
		JSCallback callback = new JSCallback(fn, args, false, milliseconds);
		timeouts.get(time).add(callback);
		ids.put(callback.id, callback);
		return callback.id;
	}
	
	static void clearTimeout(long id) {
		if (ids.containsKey(id))
			ids.get(id).enabled = false;
	}
	
	static long setInterval(JSObject fn, Object[] args, long milliseconds) {
		Long time = System.currentTimeMillis() + milliseconds;
		timeoutTimes.add(time);
		Collections.sort(timeoutTimes);
		if (!timeouts.containsKey(time))
			timeouts.put(time, new ArrayList<JSCallback>());
		JSCallback callback = new JSCallback(fn, args, true, milliseconds);
		timeouts.get(time).add(callback);
		ids.put(callback.id, callback);
		return callback.id;	
	}
	
	static void clearInterval(long id) {
		if (ids.containsKey(id))
			ids.get(id).enabled = false;
	}
	
	static public void yieldForTimers() throws Exception {
		// wait for us to run out of timeouts and intervals
		while (timeoutTimes.size() > 0) {
			Long time = timeoutTimes.get(0);
			long delay = timeoutTimes.get(0) - System.currentTimeMillis();
			if (delay > 0)
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				}
			timeoutTimes.remove(0);
			
			// run callback
			ArrayList<JSCallback> callbacks = timeouts.get(time);
			for (JSCallback callback : callbacks) {
				if (callback.enabled)
					callback.fn.invoke(null, callback.args);
				ids.remove(callback.id);
				if (callback.repeating)
					setInterval(callback.fn, callback.args, callback.delay);
			}
			callbacks.remove(0);
		}
	}
}
