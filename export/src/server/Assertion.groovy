package server;

class Assertion
{
	Function function;
	String errormessage;

	public Assertion(Function _function, String _errormessage)
	{
		function=_function;
		errormessage=_errormessage;
	}
}