/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.common;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureExceptionFactory;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@JsonSerialize(using = RaptureURIJacksonSerializer.class)
@JsonDeserialize(using = RaptureURIJasksonDeserializer.class)
public class RaptureURI implements Cloneable {

    private Scheme scheme;
    private String authority;
    private String docPath;
    private String version;
    private String asOfTime;
    private String attribute;
    private String element;

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(RaptureURI.class);

    private RaptureURI() {
    }

    public RaptureURI(String documentURI) {
        this(documentURI, null);
    }

    public RaptureURI(String documentURI, Scheme defaultScheme) {
        if ("//".equals(documentURI) || "/".equals(documentURI) || "".equals(documentURI) || documentURI == null) {
            this.scheme = defaultScheme;
            this.authority="";
        } else try {
            Parser parser = new Parser(documentURI, defaultScheme);
            parser.parse();
            this.scheme = parser.getScheme();
            this.authority = parser.getAuthority();
            this.docPath = parser.getDocPath();
            this.version = parser.getVersion();
            this.asOfTime = parser.getAsOfTime();
            this.element = parser.getElement();
            this.attribute = parser.getAttribute();
        } catch (URISyntaxException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    String.format("Unable to parse URI %s: %s", documentURI, e.getMessage()), e);
        }

    }

    // getDocPath should return the Doc Path. If we need helper methods, let's
    // add them.

    public String getDocPath() {
        return docPath == null ? "" : docPath;
    }

    public String getDocPathWithAttributes() {
        return attribute == null ? getDocPath() : getDocPath() + Parser.SEPARATOR_CHAR + Parser.ATTRIBUTE_CHAR + attribute;
    }

    public String getDocPathWithElement() {
        return element == null ? getDocPath() : getDocPath() + Parser.ELEMENT_CHAR + element;
    }

    public String getShortPath() {
        StringBuilder sb = new StringBuilder();
        sb.append(getAuthority()).append(Parser.SEPARATOR_CHAR).append(getDocPath());
        return sb.toString();
    }

    public String getFullPath() {
        StringBuilder sb = new StringBuilder();
        sb.append(getShortPath());
        if (version != null) sb.append(Parser.VERSION_CHAR).append(version);
        if (asOfTime != null) sb.append(Parser.VERSION_CHAR).append(asOfTime);
        if (attribute != null) sb.append(Parser.SEPARATOR_CHAR).append(Parser.ATTRIBUTE_CHAR).append(attribute);
        if (element != null) sb.append(Parser.ELEMENT_CHAR).append(element);
        return sb.toString();
    }

    public String getParent() {
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://");
        if (docPath != null) {
            sb.append(getAuthority());
            if (docPath.contains("/")) {
                sb.append(Parser.SEPARATOR_CHAR).append(docPath.substring(0, docPath.lastIndexOf('/')));
            }
        }
        return sb.toString();
    }

    public Scheme getScheme() {
        return scheme;
    }

    public String getAuthority() {
        return authority;
    }

    public String getVersion() {
        return version;
    }

    public String getAsOfTime() {
        return asOfTime;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getAttributeName() {
        if (attribute != null) {
            return attribute.split(Parser.SEPARATOR_CHAR.toString())[0];
        } else {
            return null;
        }
    }

    public String getAttributeKey() {
        if (attribute != null) {
            String[] retVal = attribute.split(Parser.SEPARATOR_CHAR.toString(), 2);
            return (retVal.length > 1) ? retVal[1] : null;
        } else {
            return null;
        }
    }

    public String getElement() {
        return element;
    }

    @Override
    public String toString() {
        return prependScheme(getFullPath());
    }

    public String toShortString() {
        return prependScheme(getShortPath());
    }

    public String toAuthString() {
        return prependScheme(getAuthority());
    }

    private String prependScheme(String uri) {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) sb.append(scheme).append(":");
        sb.append(uri.startsWith("/") ? "/" : "//");
        sb.append(uri);
        return sb.toString();
    }

    public boolean hasDocPath() {
        return docPath != null && docPath.length() > 0;
    }

    public boolean hasAttribute() {
        return attribute != null;
    }

    public boolean hasElement() {
        return element != null;
    }

    public boolean hasScheme() {
        return scheme != null;
    }

    public boolean hasVersion() {
        return version != null;
    }

    public boolean hasAsOfTime() {
        return asOfTime != null;
    }

    public String getDisplayName() {
        return Strings.isNullOrEmpty(docPath) ? "" : docPath;
    }

    public static Builder builder(Scheme scheme, String authority) {
        return new Builder(scheme, authority);
    }

    public static Builder builder(RaptureURI uri) {
        return new Builder(uri);
    }

    public RaptureURI withoutAttribute() {
        return RaptureURI.builder(this).attribute(null).build();
    }

    public RaptureURI withoutDecoration() {
        return RaptureURI.builder(this).attribute(null).element(null).version(null).asOfTime(null).build();
    }

    public static class Builder {
        private final RaptureURI result;

        public Builder(Scheme scheme, String authority) {
            if (authority == null) {
                throw new IllegalArgumentException("Authority cannot be null");
            }
            result = new RaptureURI();
            result.scheme = scheme;
            result.authority = authority;
        }

        public Builder(RaptureURI uri) {
            result = new RaptureURI();
            result.scheme = uri.scheme;
            result.authority = uri.authority;
            result.docPath = uri.docPath;
            result.version = uri.version;
            result.asOfTime = uri.asOfTime;
            result.attribute = uri.attribute;
            result.element = uri.element;
        }

        public Builder docPath(String docPath) {
            result.docPath = docPath;
            return this;
        }

        public Builder attribute(String attribute) {
            result.attribute = attribute;
            return this;
        }

        public Builder version(String version) {
            result.version = version;
            return this;
        }

        public Builder asOfTime(String asOfTime) {
            result.asOfTime = asOfTime;
            return this;
        }

        public Builder element(String element) {
            result.element = element;
            return this;
        }

        public RaptureURI build() {
            return result;
        }

        public String asString() {
            return build().toString();
        }
    }

    public static RaptureURI createFromFullPath(String fullPath, Scheme scheme) {
        int sliceCount = 2;
        String[] parts = fullPath.split(Parser.SEPARATOR_CHAR.toString(), sliceCount);
        Preconditions.checkArgument(parts.length == sliceCount, "Not a valid path: " + fullPath);
        return RaptureURI.builder(scheme, parts[0]).docPath(parts[1]).build();
    }

    public static RaptureURI createFromFullPathWithAttribute(String fullPath, String attribute, Scheme scheme) {
        int sliceCount = 2;
        String[] parts = fullPath.split(Parser.SEPARATOR_CHAR.toString(), sliceCount);
        Preconditions.checkArgument(parts.length == sliceCount, "Not a valid path: " + fullPath);
        String docPath = parts[1];
        // do not allow the empty string
        if (StringUtils.isBlank(docPath)) {
            docPath = null;
        }
        return RaptureURI.builder(scheme, parts[0]).docPath(docPath).attribute(attribute).build();
    }

    public int getDocPathDepth() {
        int count = 1;
        if (!Strings.isNullOrEmpty(docPath)) {
            count++;
            for (int i = 0; i < docPath.length(); i++) {
                if (docPath.charAt(i) == Parser.SEPARATOR) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
        result = prime * result + ((docPath == null) ? 0 : docPath.hashCode());
        result = prime * result + ((element == null) ? 0 : element.hashCode());
        result = prime * result + ((authority == null) ? 0 : authority.hashCode());
        result = prime * result + ((scheme == null) ? 0 : scheme.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((asOfTime == null) ? 0 : asOfTime.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RaptureURI other = (RaptureURI) obj;
        if (attribute == null) {
            if (other.attribute != null) return false;
        } else if (!attribute.equals(other.attribute)) return false;
        if (docPath == null) {
            if (other.docPath != null) return false;
        } else if (!docPath.equals(other.docPath)) return false;
        if (element == null) {
            if (other.element != null) return false;
        } else if (!element.equals(other.element)) return false;
        if (authority == null) {
            if (other.authority != null) return false;
        } else if (!authority.equals(other.authority)) return false;
        if (scheme != other.scheme) return false;
        if (version == null) {
            if (other.version != null) return false;
        } else if (!version.equals(other.version)) return false;
        if (asOfTime == null) {
            if (other.asOfTime != null) return false;
        } else if (!asOfTime.equals(other.asOfTime)) return false;
        return true;
    }

    public static final class Parser {

        public static boolean isValidDocPathChar(int ch) {
            return Character.isUnicodeIdentifierPart(ch) || DOC_PATH_REPEAT.contains(ch);
        }

        public static final Character VERSION_CHAR = '@';
        public static final Character ELEMENT_CHAR = '#';
        public static final Character ATTRIBUTE_CHAR = '$';
        public static final Character SEPARATOR_CHAR = '/';
        public static final Character COLON_CHAR = ':';

        static final Integer VERSION = new Integer(VERSION_CHAR);
        static final Integer ELEMENT = new Integer(ELEMENT_CHAR);
        static final Integer ATTRIBUTE = new Integer(ATTRIBUTE_CHAR);
        static final Integer SEPARATOR = new Integer(SEPARATOR_CHAR);
        static final Integer COLON = new Integer(COLON_CHAR);

        private static final Set<Integer> OPTIONAL_CONTROL_CHARS = ImmutableSet.<Integer> of(Parser.ATTRIBUTE, Parser.VERSION, Parser.ELEMENT);

/**
         * These are the 'special' characters that we accept. Loosely based on unreserved, as defined in RFC-2396 (Appendix A)
         * Note that we do not currently allow space or plus
         * 
         * reserved    = ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" | "$" | ","
         * unreserved  = alphanum | mark
         * mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
         * delims      = "<" | ">" | "#" | "%" | <">
         * unwise      = "{" | "}" | "|" | "\" | "^" | "[" | "]" | "`"
         *
         * Given that many of the encoders rely on RFC-2396 based algorithms - eg java.net.URLEncoder - we should certainly allow 
         * all of unreserved - In fact thete's an argument to be made for allowing anything that's not explicitly defined as a Character above:
         *      VERSION_CHAR, ELEMENT_CHAR, ATTRIBUTE_CHAR, SEPARATOR_CHAR, COLON_CHAR
         */

        private static final Set<Integer> WHITELIST = ImmutableSet.of(new Integer('-'), new Integer('.'), new Integer('_'), new Integer('~'),
                new Integer('!'), new Integer('*'), new Integer('"'), new Integer('('), new Integer(')'), new Integer('|'), new Integer('%'));

        /**
         * Contains any characters that we allow for authority
         */
        private static final Set<Integer> AUTHORITY;

        static {
            com.google.common.collect.ImmutableSet.Builder<Integer> builder = ImmutableSet.<Integer> builder();
            builder.add(Parser.VERSION).add(COLON).addAll(WHITELIST);
            AUTHORITY = builder.build();
        }

        private boolean isValidAuthorityChar(int ch) {
            return Character.isUnicodeIdentifierPart(ch) || AUTHORITY.contains(ch);
        }

        /**
         * Contains any characters we allow for the doc path, which can be repeated as many times as necessary
         */
        private static final Set<Integer> DOC_PATH_REPEAT;

        static {
            com.google.common.collect.ImmutableSet.Builder<Integer> builder = ImmutableSet.<Integer> builder();
            builder.addAll(WHITELIST).add(Parser.SEPARATOR).add(COLON).addAll(OPTIONAL_CONTROL_CHARS);
            DOC_PATH_REPEAT = builder.build();
        }

        /**
         * We populate a map, mapping string representations of {@link Scheme} to their object equivalents. This allows faster conversion of text to string,
         * rather than doing {@link Scheme#valueOf(String)}
         */
        private static final Map<String, Scheme> valToScheme;

        static {
            com.google.common.collect.ImmutableMap.Builder<String, Scheme> builder = ImmutableMap.<String, Scheme> builder();
            for (Scheme scheme : Scheme.values()) {
                builder.put(scheme.toString(), scheme);
            }
            valToScheme = builder.build();
        }

        private int i;

        private int inputLength;
        private String input;
        private int[] chars; // Got to support Un

        Map<Integer, Integer> badChars;

        private Scheme defaultScheme;

        Set<Integer> alreadySeen;

        private Scheme scheme;

        private String docPath;

        private String attribute;

        private String element;

        private String version;

        private String asOfTime;

        private String authority;

        public Parser(String input, Scheme defaultScheme) {
            this.input = input;
            this.defaultScheme = defaultScheme;
            alreadySeen = new HashSet<Integer>();
            badChars = new HashMap<Integer, Integer>();
            inputLength = input.length();
            chars = new int[inputLength];
        }

        /**
         * Attempt to parse an input {@link String}. This method should be used to parse the input and then retrieve the resulting parts using methods like
         * {@link #getAuthority()}, {@link #getScheme()}, etc. {@link Parser} is a stateful object. For example:
         * <p>
         * 
         * <pre>
         * URIParser parser = new URIParser(input, defaultScheme);
         * parser.parse();
         * this.scheme = parser.getScheme();
         * this.authority = parser.getAuthority();
         * </pre>
         * 
         * @return A {@link RaptureURI} for this string
         * @throws URISyntaxException
         *             If parsing fails due to bad syntax in the input
         */
        void parse() throws URISyntaxException {
            if (inputLength < 3) {
                throw new URISyntaxException(input, "Invalid URI, length must be at least 3");
            }
            for (int counter = 0; counter < inputLength; counter++)
                chars[counter] = input.codePointAt(counter);

            i = 0;
            // parse scheme
            int endSchemeIndex = input.indexOf("://");
            if (endSchemeIndex == -1) {
                if (defaultScheme != null) {
                    scheme = defaultScheme;
                } else {
                    throw new URISyntaxException(input, "URI does not contain a scheme and no default scheme is specified");
                }
                if (chars[0] == Parser.SEPARATOR && chars[1] == Parser.SEPARATOR) i += 2;

            } else {
                String schemeString = new String(chars, 0, endSchemeIndex);
                i = endSchemeIndex + 3;
                scheme = valToScheme.get(schemeString);
                if (scheme == null) {
                    throw new URISyntaxException(input, "Invalid scheme " + schemeString);
                }
            }

            // You can't have a null authority and a doc path: foo:////bar/baz --> foo://bar/baz 
            while ((i < inputLength) && (chars[i] == '/')) i++; // skip over slashes
            
            // parse authority
            boolean isInvalidAuthority = false;
            int authorityStartIndex = i;
            while (i < inputLength) {
                int currChar = chars[i];
                if (currChar == Parser.SEPARATOR) {
                    break;
                } else {
                    if (!isValidAuthorityChar(currChar)) {
                        badChars.put(i, currChar);
                        isInvalidAuthority = true;
                    }
                    i++;
                }
            }

            authority = new String(chars, authorityStartIndex, i - authorityStartIndex);
            if (isInvalidAuthority) {
                throw new URISyntaxException(input, String.format("Invalid authority '%s': bad characters are %s", authority, badCharsToString()));
            }

            boolean isInvalidDocPathChar = false;
            do {
                i++; // skip over slashes
                if (i >= inputLength) {
                    return;
                }
            } while (chars[i] == '/');
            
            int docPathStartIndex = i;
            while (i < inputLength && !OPTIONAL_CONTROL_CHARS.contains(chars[i])) {
                if (OPTIONAL_CONTROL_CHARS.contains(chars[i])) {
                    break;
                } else {
                    if (!isValidDocPathChar(chars[i])) {
                        isInvalidDocPathChar = true;
                        badChars.put(i, chars[i]);
                    }
                    i++;
                }
            }

            if (i > docPathStartIndex && chars[i - 1] == Parser.SEPARATOR) {
                docPath = new String(chars, docPathStartIndex, i - 1 - docPathStartIndex);
            } else {
                docPath = new String(chars, docPathStartIndex, i - docPathStartIndex);
            }
            docPath = docPath.replaceAll("//+", "/");
            if (isInvalidDocPathChar) {
                throw new URISyntaxException(input, String.format("Invalid doc path '%s': bad characters are %s ", docPath, badCharsToString()));
            }

            readOptionalPart();
            readOptionalPart();
            readOptionalPart();
        }

        private String badCharsToString() {
            StringBuilder sb = new StringBuilder();
            for (Entry<Integer, Integer> entry : badChars.entrySet()) {
                sb.append(Character.toChars(entry.getValue())).append(" (pos ").append(entry.getKey()).append("); ");
            }
            return sb.toString();
        }

        private void readOptionalPart() throws URISyntaxException {
            if (i < inputLength) {
                int controlChar = chars[i];
                alreadySeen.add(controlChar);
                i++;
                int startIndex = i;
                while (i < inputLength) {
                    int currChar = chars[i];
                    if (OPTIONAL_CONTROL_CHARS.contains(currChar)) {
                        if (alreadySeen.contains(currChar)) {
                            throw new URISyntaxException(input, String.format("Error, '%s' cannot appear twice because it implies redefining the same entity",
                                    currChar), i);
                        } else {
                            break;
                        }
                    } else {
                        i++;
                    }
                }
                int end = i - startIndex;
                // remove trailing separators
                while (chars[end + startIndex - 1] == Parser.SEPARATOR) {
                    end--;
                }
                String text = new String(chars, startIndex, end);
                if (text.length() > 0) {
                    if (controlChar == VERSION) {
                        if (text.matches("\\d+")) {
                            version = text;
                        }
                        else {
                            asOfTime = text;
                        }
                    } else if (controlChar == ELEMENT) {
                        element = text;

                    } else if (controlChar == ATTRIBUTE) {
                        attribute = text;
                    }
                }
            }
        }

        public Scheme getScheme() {
            return scheme;
        }

        public String getDocPath() {
            return docPath;
        }

        public String getAttribute() {
            return attribute;
        }

        public String getElement() {
            return element;
        }

        public String getVersion() {
            return version;
        }

        public String getAsOfTime() {
            return asOfTime;
        }

        public String getAuthority() {
            return authority;
        }
    }

    public String debug() {
        StringBuilder sb = new StringBuilder();
        sb.append(" Scheme: ").append(scheme);
        sb.append(" Authority: ").append(authority);
        sb.append(" Doc Path: ").append(docPath).append(" Depth: ").append(this.getDocPathDepth());
        sb.append(" Version: ").append(version);
        sb.append(" As Of Time: ").append(asOfTime);
        sb.append(" Attribute: ").append(attribute);
        sb.append(" Element: ").append(element).append("\n");
        sb.append(" Attribute Key-Name: ").append(this.getAttributeKey()).append("-").append(this.getAttributeName()).append("\n");

        return sb.toString();
    }

    public RaptureURI withoutElement() {
        return builder(this).element(null).build();
    }
    
    public static RaptureURI newScheme(String uri, Scheme newScheme) {
        RaptureURI retVal = new RaptureURI(uri);
        retVal.scheme = newScheme;
        return retVal;
    }
    
    public static RaptureURI newScheme(RaptureURI uri, Scheme newScheme) {
        RaptureURI retVal = null;
		try {
			retVal = (RaptureURI) uri.clone();
	        retVal.scheme = newScheme;
		} catch (CloneNotSupportedException e) {
			log.info("This can't happen", e);
		}
        return retVal;
    }
}
