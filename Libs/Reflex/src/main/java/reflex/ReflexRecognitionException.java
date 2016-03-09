package reflex;

import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;

public class ReflexRecognitionException extends RecognitionException {

	private static final long serialVersionUID = 1L;

	private String message;
	private boolean ignorable;
	private IntStream input;
	
	@Override
	public String getMessage() {
		return message;
	}

	public boolean isIgnorable() {
		return ignorable;
	}

	public ReflexRecognitionException(String message, IntStream input, boolean ignorable) {
		super(input);
		this.message = message;
		this.ignorable = ignorable;
		this.input = input;
	}
}
