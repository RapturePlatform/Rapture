package reflex;

import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

public class ReflexRecognitionException extends RecognitionException {

	private static final long serialVersionUID = 1L;

	private String message;
	
	@Override
	public String getMessage() {
		return message;
	}

	public ReflexRecognitionException(String message, IntStream input, Token token) {
		super(input);
		this.message = message;
		this.token = token;
	}
}
