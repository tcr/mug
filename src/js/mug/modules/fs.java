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
import mug.js.JSUtils;

public class fs extends JSModule {
	@Override
	public JSObject load() throws Exception {
		return new JSObject() { {
			set("open", new JSFunction () {
				@Override
				public JSPrimitive invoke(JSObject ths, int argc,
						JSPrimitive l0, JSPrimitive l1, JSPrimitive l2,
						JSPrimitive l3, JSPrimitive l4, JSPrimitive l5,
						JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
						throws Exception {
					// coerce path
					String path = JSUtils.asString(l0);
					// coerce options
					//[TODO] later
					
					return new JSFile(new File(path));
				}
			});
			
			set("read", new JSFunction () {
				@Override
				public JSPrimitive invoke(JSObject ths, int argc,
						JSPrimitive l0, JSPrimitive l1, JSPrimitive l2,
						JSPrimitive l3, JSPrimitive l4, JSPrimitive l5,
						JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
						throws Exception {
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
		
		public JSFile(File file) {
			this.file = file;
		}
	}
}
