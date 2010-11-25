package mug.android;

import mug.Modules;
import mug.js.JSFunction;
import mug.js.JSObject;
import mug.modules.java.JSJavaObject;
import android.app.Activity;
import android.os.Bundle;

public class MugAndroidActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
        	JSObject test = Modules.getModule("test").load();
			((JSFunction) test.get("run")).invoke(null, new JSJavaObject(test.getProto(), this));
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}