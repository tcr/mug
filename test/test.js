/*
var io = require("io");
var fs = require("fs");

io.log("Called IO module. She didn't call back. :(");
print(fs.read("test.js"));
*/

print.apply(null, ["apples"]);

print(5 > 6 ? "5 is greater than 6" : "6 is greater than 5. duh.");
print("Should be '6-7': " + [5,6,7,8,9].slice(1,3).join('-'))
print("Should be '6-7-8': " + [5,6,7,8,9].slice(1,-2).join('-'))

print((63).toString(2));
print((63).toString(8));
print((63).toString(16));
print((63).toString(null));

print("Should be abba: " + "ABBa".toLowerCase());
print("Should be ab$a: " + "a$$a".replace("$", "b"));
print("Should be ab$a: " + "a$$a".replace(/\$/, "b"));
print("Should be abba: " + "a$$a".replace(/\$/g, "b"));
print("Should be asdf3h-3h-h: " + /[a-z]{4}(.(.))/.exec("4je4ojf87asdf3hjo3h34ja").join("-"));

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
print("Should be WORKS: " + "aaaWORKS".substr(3));
print("Should be 'string': " + (typeof "afdsaasdfds"));
print("Should be -1: " + "aaaWORKS".indexOf("b"));
print("Should be 3: " + "aaaWORKS".indexOf("W"));