package mug.parse;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class Parser {
	static public String parse(String input) throws Exception {
		// evaluate parser
		Context cx = Context.enter();
		try {
			Scriptable scope = cx.initStandardObjects();
			cx.evaluateString(scope, loadScript("parse-js.js"), "parse-js.js", 1, null);
			cx.evaluateString(scope, loadScript("gen-clojure.js"), "gen-clojure.js", 1, null);

			Object wrappedString = Context.javaToJS(input, scope);
			ScriptableObject.putProperty(scope, "inputSource", wrappedString);
			
			Object result = null;
			try {
				Object res = cx.evaluateString(scope, "parse(inputSource).toSource()", "<parse-script>", 1, null);
				System.out.println(res);
				result = cx.evaluateString(scope, "gen_clojure(parse(inputSource))", "<parse-script>", 1, null);
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
			
			return Context.toString(result);
		} finally {
		    Context.exit();
		}
	}
	
	static public String loadScript(String filename) {
		InputStream is = Parser.class.getClassLoader().getResourceAsStream(filename);
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in;
		try {
			in = new InputStreamReader(is, "UTF-8");
			int read;
			do {
			  read = in.read(buffer, 0, buffer.length);
			  if (read>0) {
			    out.append(buffer, 0, read);
			  }
			} while (read>=0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out.toString();
	}
}
