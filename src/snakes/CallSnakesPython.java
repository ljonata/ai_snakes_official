package snakes;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

public class CallSnakesPython {

	public static void main(String[] args) {
		
	}

	void setExamples1() {
		/**
		 * Source:
		 * https://stackoverflow.com/questions/9381906/how-to-call-a-python-method-from-a-java-class
		 */

		// Example 1

		PythonInterpreter interpreter = new PythonInterpreter();
		interpreter.execfile("./python/_file.py");
		// PyObject result = interpreter.eval("myPythonClass().abc()");
		PyObject str = interpreter.eval("repr(myPythonClass().abc())");
		System.out.println(str.toString());

		// Example 2
		interpreter.set("myvariable", new Integer(21));
		PyObject answer = interpreter.eval("'the answer is: %s' % (2*myvariable)");
		System.out.println(answer.toString());

		// Example 3
		interpreter.execfile("./python/somme_x_y.py");
		PyObject str2 = interpreter.eval("repr(somme(4,5))");
		System.out.println(str2.toString());
	}

}
