/*
var io = require("io");
var fs = require("fs");

io.log("Called IO module. She didn't call back. :(");
print(fs.read("test.js"));
*/

var a = [5, 6, 7];
print(a.push(8));
print(a[3]);

var b = [9, 10];
print(a.concat(b).length);
print(a.pop());
print(a.length);