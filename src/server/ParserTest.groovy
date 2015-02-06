package server;

import java.util.concurrent.*;
import static util.Log.*;

public class ParserTest {
	public static void main(String[] args) {
		info "Full lexer/parser test"
		MirkoGroovyLibrary.execute()

		// Basic stuff		
		test "a=5 \n debug(a,5)"
		test "debug(username,\"subject\")"
		test "debug(role,\"A\")"

		test "a=5 \n debug(sum(a),5)"
		test "a=5 \n debug(mean(a),5)"
		test "a=5 \n debug(median(a),5)"
		test "a=5 \n debug(min(a),5)"
		
		test "debug(5+5,10)"
		test "debug(-5,-5)"
		test "debug(-5.34,-5.34)"
		
		test "debug(5-7,-2)"
		
		test "debug(534.4+7,541.4)"
		test "debug(2*3+4,10)"
		test "debug(2+3*4,14)"
		test "debug(11%3,2)"
		test "debug(11^4,14641)"
		
		test "debug(2*3==6,1)"
		test "debug(2^3!=6,1)"
		
		test "debug(2>3,0)"
		test "debug(3>=2,1)"
		
		test "debug(2&&3,1)"

		test "debug(\"test\"+\"1234\",\"test1234\")"
		test "debug(round(5.4),5)"
		test "debug(round2(2^0.5),1.41)"
		
		test "var=round(15/4+6)"
		test "//test \n  debug(group,1)"
		test "a=5 \n debug(a,5)"
		test "a=2.5 \n b=13 \n c=b*a \n debug(2.5*b,32.5) \n debug(c,32.5)"
		test "debug(round(4/3),1)"
		test "a=randomGauss()"
		
		test "i=1 \n i=1+i \n debug(i,2) "
		test "i=1 \n i=i+1 \n debug(i,2) "
		test "i=1 \n while(i<=5) \n { \n i=i+1 \n 	}"
		test "for (i=1; i<=5; i=i+1) \n { \n 	}"
					
		test "debug(round(4/3)+\"a\",\"1a\")"
		test "a[5]=3\ndebug(a[5],3)"
		test "a[5]=3\ndebug(a[4],0)"
		test "a[2+3]=3\ndebug(a[10/2],3)"
		test "i=3\na[i]=3\ndebug(a[3],3)"
		test "i=3\na[i]=3\ndebug(a[i],3)"
		test "a[5][2]=3\ndebug(a[20/4][1+1],3)"
		test "a[3]=1\nb[a[3]]=3\ndebug(b[a[3]],3)"
		test "a[5]=3==4\ndebug(a[5],0)"
		test "debug(a,0)"
		test "debug(a[5],0)"
		test "a[5==3]=3\ndebug(a[1],0)"
		test "a[5==5]=3\ndebug(a[1],3)"


		test "matchAll(B) \n debug(role,\"B\")"
		test "matchStranger(B) \n debug(role,\"B\")"
		//test "matchPerfectStranger(B) \n debug(role,\"B\")"
		test "matchManual(\"subject\",2,\"C\") \n debug(role,\"C\")"
		test "matchManual(\"subject\",2,\"C\") \n debug(role,\"C\") \n matchDone() \n matchManual(\"subject\",3,\"E\") \n debug(role,\"E\") "

		// Multimatch
		test "\$r=0 \n \$q=0 \n for(i=0; i<3; i=i+1) \n { \n	\$q=\$q+1 \n matchAll(A) \n \$r=\$r+1 \n matchDone() \n } \n debug(\$r,3) \n debug(\$q,3)"

		test "debug(\"test\"+(1+1),\"test2\")"

		test "x=\"test\"+1+1 \n debug(x,\"test11\")"
		test "x=\"test\"+(1+1) \n debug(x,\"test2\")"
		test "x=2+(1+1) \n debug(x,4)"
		test "x=(1+1)+2 \n debug(x,4)"
		System.exit(0)
	}

	private static void test(String string) {
		info "\n"
		
		int i=1
		for (line in string.split("\n"))
			info " "+(i++)+": "+line.trim()
		
		ConcurrentHashMap<String, Object> varspace=new ConcurrentHashMap<String, Object>()
		Group g=new Group(name:"1")
		
		ServerClientThread experimenter=new ServerClientThread(null, null, null);
		experimenter.subinfo.realm="test";
		experimenter.subinfo.username="experimenter";
		ServerClientThread subject=new ServerClientThread(null, null, null);
		subject.subinfo.realm="test";
		subject.subinfo.username="subject";
		ServerClientThread.subjectPool.clear();
		ServerClientThread.subjectPool.add(subject);
		new Session(experimenter,string,varspace,false).run();
		varspace.each{k,v -> info "  $k=[$v]"}
	}
}
