var java = require("java")
var TextView = java.import("android.widget.TextView")

exports.run = function (activity) {
	var view = new TextView(activity);
	activity.setContentView(view);
	view.append("Hello Android!");
}