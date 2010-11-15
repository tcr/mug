/*
var io = require("io");
var fs = require("fs");

io.log("Called IO module. She didn't call back. :(");
print(fs.read("test.js"));
*/

print(5 > 6 ? "5 is greater than 6" : "6 is greater than 5. duh.");

var f = [5];
print("Should be 1: " + f.length);

var a = [5, 6, 7];
print("Should be 4: " + a.push(8));
print("Should be 8: " + a[3]);

var b = [9, 10];
print("Should be 6: " + a.concat(b).length);
print("Should be 8: " + a.pop());
print("Should be 3: " + a.length);

print("Should be '2-3-4': " + [1,2,3].map(function (x) { return x + 1; }).join('-')); 

var c = /a/;
print("Should be true: " + c.test("bcda"));
print("Should be false: " + c.test("bcd"));
print("Should be WORKS: " + "aaaWORKSaaaaa".substr(3, 5));
print("Should be 'string': " + (typeof "afdsaasdfds"));