function array_to_hash(a) {
        var ret = {};
        for (var i = 0; i < a.length; i++)
                ret[a[i]] = true;
        return ret;
};

var KEYWORDS = array_to_hash([
        "break",
        "case",
        "catch",
        "const",
        "continue",
        "default",
        "delete",
        "do",
        "else",
        "finally",
        "for",
        "function",
        "if",
        "in",
        "instanceof",
        "new",
        "return",
        "switch",
        "throw",
        "try",
        "typeof",
        "var",
        "void",
        "while",
        "with",
        "NaN"
]);

print(KEYWORDS.NaN);