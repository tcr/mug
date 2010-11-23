//=========================================================
// Language
//=========================================================

print("[Language Regression Testbench]\n");

// modules
var rr = require("regression-require");
rr.check();

// structures
print("Should print works:"); do { print("WORKS."); } while(false);
for (var n in {"for": 1, "in": 2, "works": 3}) print(n);
do { break; print("THIS SHOULD NOT PRINT."); } while(false);
for(var n=0;n<10;n++){continue;print("THIS SHOULD NOT PRINT. n="+n);}
var n=0;for(;;){if(n++ > 10)break;}print("Finished infinite loop. Nice.");

// functions
print.apply(null, ["apples"]);

// operators
print("Should be 1: " + (0 || 1))
print("Should be apples: " + ("" || "apples"))
print(true || print("FAIL"));
print(5 > 6 ? "5 is greater than 6" : "6 is greater than 5. duh.");

// numbers
print((63).toString(2));
print((63).toString(8));
print((63).toString(16));
print((63).toString(null));
print((63/3.14159265358979).toFixed(0));
print((63/3.14159265358979).toFixed(1));
print((63/3.14159265358979).toFixed(9));

// strings
print("Should be abba: " + "ABBa".toLowerCase());
print("Should be WORKS: " + "aaaWORKSaaaaa".substr(3, 5));
print("Should be WORKS: " + "aaaWORKS".substr(3));
print("Should be 'string': " + (typeof "afdsaasdfds"));
print("Should be -1: " + "aaaWORKS".indexOf("b"));
print("Should be 3: " + "aaaWORKS".indexOf("W"));

// array
var f = [5];
print("Should be 1: " + f.length);
var a = [5, 6, 7];
print("Should be 4: " + a.push(8));
print("Should be 8: " + a[3]);
var b = [9, 10];
print("Should be 6: " + a.concat(b).length);
print("Should be 8: " + a.pop());
print("Should be 3: " + a.length);
print("Should be '6-7': " + [5,6,7,8,9].slice(1,3).join('-'))
print("Should be '6-7-8': " + [5,6,7,8,9].slice(1,-2).join('-'))
print("Should be '2-3-4': " + [1,2,3].map(function (x) { return x + 1; }).join('-'));
print("Should be 'aaa-bbb-ccc': " + ['ccc', 'aaa', 'bbb'].sort().join('-'));

// regexp
print("Should be ab$a: " + "a$$a".replace("$", "b"));
print("Should be ab$a: " + "a$$a".replace(/\$/, "b"));
print("Should be abba: " + "a$$a".replace(/\$/g, "b"));
print("Should be asdf3h-3h-h: " + /[a-z]{4}(.(.))/.exec("4je4ojf87asdf3hjo3h34ja").join("-"));
var c = /a/;
print("Should be true: " + c.test("bcda"));
print("Should be false: " + c.test("bcd"));