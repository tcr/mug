package mug.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import mug.js.JSFunction;
import mug.js.JSModule;
import mug.js.JSObject;
import mug.js.JSPrimitive;
import mug.js.JSString;
import mug.js.JSTopLevel;
import mug.js.JSUtils;

public class fs extends JSModule {
	@Override
	public JSObject load() throws Exception {
		final JSTopLevel top = new JSTopLevel();
		
		return new JSObject(top.getObjectPrototype()) { {
			set("open", new JSFunction (top.getFunctionPrototype()) {
				@Override
				public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
				{
					// coerce path
					String path = JSUtils.asString(l0);
					// coerce options
					//[TODO] later
					
					return new JSFile(top.getObjectPrototype(), new File(path));
				}
			});
			
			set("read", new JSFunction (top.getFunctionPrototype()) {
				@Override
				public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
				{
					// coerce path
					String path = JSUtils.asString(l0);
					// coerce options
					//[TODO] later
					
					// create file stream
				    FileInputStream stream = new FileInputStream(new File(path));
				    try {
				    	// read file
				        Reader reader = new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()));
				        StringBuilder builder = new StringBuilder();
				        char[] buffer = new char[8192];
				        int read;
				        while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
				            builder.append(buffer, 0, read);
				        }
				        
				        // return string object
				        return new JSString(builder.toString());
				    } finally {
				        stream.close();
				    }        
				}
			});
		} };
	}
	
	static class JSFile extends JSObject {
		protected File file;

		public File getFile() {
			return file;
		}
		
		public JSFile(JSObject proto, File file) {
			super(proto);
			this.file = file;
		}
	}
}
