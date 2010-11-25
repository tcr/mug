Mug with Android
================

In order to get this demo working:

* Make your main activity mug.android.MugAndroidActivity
* Include `mug-js.jar` in your Android build path
* Compile (or use the contents of the `out` folder) `test.js` into class files
* Package the class files as a `.jar` (In Eclipse, File -> Export...) and include this .jar in your Android build path

In general, make sure your compiled JavaScript files are in archives so that they undergo the `.class` -> `.dex` conversion.

This is a weak demo that will be improved in the future.