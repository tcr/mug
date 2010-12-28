var java = require("java")
var TextView = java.import("android.widget.TextView")
var Toast = java.import("android.widget.Toast")

exports.run = function (activity) {
	var view = new TextView(activity);
	activity.setContentView(view);
	view.append("Hello Android!");
	view.setBackgroundColor(0x88FFFF00);
	
	var toast = Toast.makeText(
		activity.getApplicationContext(),
		"Thank you very much!",
		Toast.LENGTH_SHORT
	);
	toast.show();
}