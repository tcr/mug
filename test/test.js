var io = require("io");
var fs = require("fs");

io.log("Called IO module. She didn't call back. :(");
print(fs.read("test.js"));