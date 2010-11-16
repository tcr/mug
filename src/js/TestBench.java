import mug.Modules;
import mug.js.JSObject;
import mug.js.JSFunction;
import mug.js.JSString;

public class TestBench {
	public static void main(String[] args) throws Exception {
		long start = System.nanoTime();
		System.out.println("###START");
		JSObject test = Modules.getModule("test").load();
//		((JSFunction) test.get("cool")).invoke(null, 1, new JSString("Hi!"), null, null, null, null, null, null, null, null);
		System.out.println("###END (time: " + ((System.nanoTime() - start) / 1000000.0) + " milliseconds)");
	}
}