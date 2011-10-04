import java.util.ArrayList;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.Perl5Substitution;
import org.apache.oro.text.regex.Util;


public class RegexTest {
	public static void main(String[] args) throws Exception {
		Perl5Compiler compiler = new Perl5Compiler();
		Perl5Matcher matcher = new Perl5Matcher();
		
		// matching
		
		Pattern regexpr = compiler.compile("\\b(\\w+)(\\s+\\1)+\\b");
		boolean matches = matcher.matches("same same same", regexpr);
		System.out.println(matches);
		
		// substitution
		
		regexpr = compiler.compile("\\b(\\w+)\\b");
		Perl5Substitution sub = new Perl5Substitution("apples");
		System.out.println(Util.substitute(matcher,regexpr,sub,"same same same",Util.SUBSTITUTE_ALL));
		
		// strings

		regexpr = compiler.compile(Perl5Compiler.quotemeta("b"));
		ArrayList<String> list = new ArrayList<String>();
		Util.split(list, matcher, regexpr, "abbb");
		System.out.println(list.size());
		
		regexpr = compiler.compile(Perl5Compiler.quotemeta(""));
		list = new ArrayList<String>();
		Util.split(list, matcher, regexpr, "abbc");
		System.out.println(list.size());
	}
}
