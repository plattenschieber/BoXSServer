package util;

public class Log
{
	public static void error(String s)
	{
		System.out.println("\033[31m"+s+"\033[0m");
	}

	public static void error(Throwable s)
	{
		System.err.println("\033[31m");
		s.printStackTrace();
		System.err.println("\033[0m");
	}

	public static void okay(String s)
	{
		System.out.println("\033[32m"+s+"\033[0m");
	}	

	public static void warning(String s)
	{
		System.out.println("\033[33m"+s+"\033[0m");
	}

	public static void info(String s)
	{
		System.out.println("\033[37m"+s+"\033[0m");
	}

	public static void log(String s)
	{
		System.out.println("\033[36m"+s+"\033[0m");
	}


}

