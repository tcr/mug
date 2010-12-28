package mug.modules;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import mug.js.JSFunction;
import mug.js.JSModule;
import mug.js.JSNumber;
import mug.js.JSObject;
import mug.js.JSString;
import mug.js.JSTopLevel;
import mug.js.JSUtils;

public class fs extends JSModule {
	final JSTopLevel top = new JSTopLevel();
	
	// path prototype extends string prototype
	final JSObject pathPrototype = new JSObject(top.getStringPrototype()) { {
		set("read", new JSFunction(top.getFunctionPrototype()) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return _read.invoke(ths, JSUtils.asString(ths));
			}
		});
	} };
	
	// open method
	JSFunction _open = new JSFunction (top.getFunctionPrototype()) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
		{
			// coerce path
			String path = JSUtils.asString(l0);
			// coerce options
			//[TODO] later
			
			// path object extends string object
			return new JSString(pathPrototype, path);
		}
	};
	
	// read method
	JSFunction _read = new JSFunction (top.getFunctionPrototype()) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
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
		        return builder.toString();
		    } finally {
		        stream.close();
		    }        
		}
	};
	
	// rename method
	JSFunction _rename = new JSFunction (top.getFunctionPrototype()) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
		{
			final String path1 = JSUtils.asString(l0);
			final String path2 = JSUtils.asString(l1);
			final JSObject callback = l2 instanceof JSObject ? (JSObject) l2 : null;
			
			(new Thread() {
				public void run() {
					try {
						(new File(path1)).renameTo(new File(path2));
					} catch (Exception e1) {
						// pass exception to callback
						if (callback != null)
							//[TODO]
							try { callback.invoke(e1.getMessage()); } catch (Exception e2) { }
						return;
					}
					
					// callback
					if (callback != null)
						try { callback.invoke(null); } catch (Exception e) { }
				}				
			}).start();
			return null;
		}
	};
	
	// renameSync method
	JSFunction _renameSync = new JSFunction (top.getFunctionPrototype()) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
		{
			final String path1 = JSUtils.asString(l0);
			final String path2 = JSUtils.asString(l1);
			
			(new File(path1)).renameTo(new File(path2));
			return null;
		}
	};
	
	// exports library
	final JSObject exports = new JSObject(top.getObjectPrototype()) { {
		set("open", _open);
		set("read", _read);
	} };
	
	@Override
	public JSObject load() throws Exception {
		// running module returns exports object
		return exports;
	}
}
