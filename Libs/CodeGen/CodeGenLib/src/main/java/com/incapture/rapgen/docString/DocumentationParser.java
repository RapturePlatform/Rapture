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
package com.incapture.rapgen.docString;

import rapture.common.exception.RaptureExceptionFactory;

import java.util.LinkedHashMap;

/**
 * Created by seanchen on 6/4/15.
 */
public class DocumentationParser {

    private static final int CUT_OFF_LENGTH = 80;
    protected static final int PARAM_TAG_LENGTH = 7;
    private static final String PARAM = "param";
    private static final String SINCE = "since";
    private static final String RETURN = "return";

    public static LinkedHashMap<String, String> retrieveParams(String raw) {
        if (tagsIncorrect(raw)) {
            throw RaptureExceptionFactory.create("Incorrect tag!");
        }
        raw = trimRaw(raw);

        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        int startParams = raw.indexOf("@");

        if (startParams != -1) {
            String tagString = raw.substring(startParams);
            try {
                int i = 0;
                do {
                    if (tagString.substring(i + 1, i + 6).equals(PARAM)) {
                        int nextAtIndex = tagString.indexOf("@", i + 1);
                        String paramString;
                        if (nextAtIndex != -1) {
                            paramString = tagString.substring(i + PARAM_TAG_LENGTH, nextAtIndex);
                        } else {
                            paramString = tagString.substring(i + PARAM_TAG_LENGTH);
                        }
                        paramString = cutOffLine(paramString.trim());
                        int paramNameEnd = paramString.indexOf(" ");
                        params.put(paramString.substring(0, paramNameEnd), paramString.substring(paramNameEnd + 1).trim());

                    } else {
                        break;
                    }
                } while ((i = tagString.indexOf("@", i + 1)) != -1);
            } catch (IndexOutOfBoundsException e) {
                throw RaptureExceptionFactory.create("Param string not formatted correctly! \nCorrect format: @param $paramName $paramDescription");
            }
        }

        return params;
    }

    public static String retrieveDescription(String raw) {
        if (tagsIncorrect(raw)) {
            throw RaptureExceptionFactory.create("Incorrect tag!");
        }

        raw = raw.replaceAll("    ", "").replaceAll("\n", "");

        int endDescription = raw.indexOf("@");
        if (endDescription == -1) {
            return cutOffLine(raw.trim());
        } else {
            return cutOffLine(raw.substring(0, endDescription).trim());
        }

    }

    public static String retrieveSince(String raw) {
        if (tagsIncorrect(raw)) {
            throw RaptureExceptionFactory.create("Incorrect tag!");
        }

        raw = trimRaw(raw);
        int startSince = raw.indexOf("@since");
        if (startSince != -1) {
            int spaceIndex = raw.indexOf(" ", startSince);
            int endSince = raw.indexOf("@", startSince + 1);
            String since;
            if (endSince != -1) {
                since = raw.substring(spaceIndex, endSince);
            } else {
                since = raw.substring(spaceIndex);
            }

            return since.trim();
        }

        return "";
    }

    public static String retrieveReturn(String raw) {
        if (tagsIncorrect(raw)) {
            throw RaptureExceptionFactory.create("Incorrect tag!");
        }

        raw = trimRaw(raw);
        int startReturn = raw.indexOf("@return");
        if (startReturn != -1) {
            int spaceIndex = raw.indexOf(" ", startReturn);
            int endReturn = raw.indexOf("@", startReturn + 1);
            String returnString;
            if (endReturn != -1) {
                returnString = raw.substring(spaceIndex, endReturn);
            } else {
                returnString = raw.substring(spaceIndex);
            }

            return returnString.trim();
        }

        return "";
    }

    private static String trimRaw(String raw) {
        raw = raw.replaceAll("^\\s", "").replaceAll("\n", " ").replaceAll(" +", " ");
        return raw;
    }

    private static String cutOffLine(String raw) {
        StringBuilder sb = new StringBuilder(raw);
        int i = 0;
        while ((i = sb.indexOf(" ", i + CUT_OFF_LENGTH)) != -1) {
            sb.replace(i, i + 1, "\n");
        }
        String retString = sb.toString();

        if (retString.charAt(retString.length() - 1) != '\n') {
            return retString + "\n";
        } else {
            return retString;
        }
    }

    private static Boolean tagsIncorrect(String raw) {
        int i = raw.indexOf("@");
        Boolean paramsEnded = false;
        Boolean returnSeen = false;
        if (i != -1) {
            do {
                if (!PARAM.equals(raw.substring(i + 1, i + 6)) && !SINCE.equals(raw.substring(i + 1, i + 6)) && !RETURN.equals(raw.substring(i + 1, i + 7))) {
                    return true;
                }

                if (PARAM.equals(raw.substring(i + 1, i + 6))) {
                    if (paramsEnded || returnSeen) {
                        return true;
                    }
                } else {
                    paramsEnded = true;
                }
                if (SINCE.equals(raw.substring(i + 1, i + 6))) {
                    if (returnSeen) {
                        return true;
                    }
                }

                if (RETURN.equals(raw.substring(i + 1, i + 7))) {
                    returnSeen = true;
                }

            } while ((i = raw.indexOf("@", i + 1)) != -1);
        }
        return false;
    }

}
