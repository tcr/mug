import mug.js.JSFunction;
import mug.js.JSObject;
import binarytrees.JSScript;
import binarytrees.JSScriptScope;

public class Container {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// create global scope
		JSScriptScope global = new JSScriptScope();
		JSScript script = new JSScript();
		
		// time and execute
		long start = System.nanoTime();
		System.out.println("###START");
		script.execute(global);
		System.out.println("###END (time: " + ((System.nanoTime() - start) / 1000000.0) + " milliseconds)");
	}
}