import mug.Modules;
import mug.js.JSException;
import mug.js.JSObject;
import mug.js.JSBoolean;

public class RegressionTest {
	public static void main(String[] args) throws Exception {
		long start = System.nanoTime();
		System.out.println("###START");
		JSObject test = Modules.getModule("regression").load();
		System.out.println("###END (time: " + ((System.nanoTime() - start) / 1000000.0) + " milliseconds)");
	}
}