package reflex;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;

public class ErrorHandler {

    public static String displayError(IntStream stream, int line, int position, int length) {
        String[] lines = null;
        StringBuilder sb = new StringBuilder();
        if (stream instanceof CharStream) {
            lines = ((CharStream) stream).substring(0, stream.size() - 1).split("\n");
        } else if (stream instanceof TokenStream) {
            lines = ((TokenStream) stream).toString(0, stream.size() - 1).split("\n");
        }

        sb.append(" at line ").append(line).append("\n");

        if (lines != null) {
            int start = Math.max(0, line - 5);
            int end = Math.min(lines.length, line + 5);
            int badline = line - 1;

            for (int i = start; i < end; i++) {
                sb.append(String.format("%5d: %s\n", i + 1, lines[i]));
                if (i == badline) {
                    for (int j = 0; j < position + 7; j++)
                        sb.append("-");
                    for (int j = 0; j <= length; j++)
                        sb.append("^");
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    static RecognitionException duplicate = null;

    public static String getParserExceptionDetails(RecognitionException e) {
        StringBuilder sb = new StringBuilder();
        if ((duplicate == null) || !duplicate.equals(e)) {
            IntStream stream;
            int line, position, length;
            CommonToken token = (CommonToken) e.token;

            String message = e.getMessage();
            if (message != null) {
                sb.append(message);
            } else {
                sb.append("Exception of type ").append(e.getClass().getCanonicalName());
            }

            if (token == null) {
                stream = e.input;
                line = e.line;
                position = e.charPositionInLine;
                length = 1;
            } else {
                sb.append(" at token ").append(token.getText());
                stream = token.getInputStream();
                line = token.getLine();
                position = token.getCharPositionInLine();
                length = token.getStopIndex() - token.getStartIndex();
            }

            String error = displayError(stream, line, position, length);
            sb.append(error);

            Throwable cause = e.getCause();
            if (cause != null) {
                sb.append("Caused by ").append(cause.getMessage()).append("\n");
            }
        }
        duplicate = e;
        return sb.toString();
    }
}
