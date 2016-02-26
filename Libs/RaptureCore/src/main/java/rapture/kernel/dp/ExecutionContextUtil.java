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
package rapture.kernel.dp;

import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.ContextValueType;
import rapture.common.dp.ExecutionContext;
import rapture.common.dp.ExecutionContextField;
import rapture.common.dp.ExecutionContextFieldStorage;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

public class ExecutionContextUtil {
    private static final Logger log = Logger.getLogger(ExecutionContextUtil.class);
    public static final char VAR_MARKER = '$';
    public static final char TEMPLATE_MARKER = '%';
    public static final char LINK_MARKER = '!';
    public static final char LITERAL_MARKER = '#';

    // getValue with ExecutionContextField
    public static String getValueECF(CallingContext callingContext, String workOrderUri, String varAlias, Map<String, String> view) {
        String realId = addContextMarkerAndLookupInView(view, varAlias, VAR_MARKER);

        if (realId == null || realId.length() <= 1) {
            throw RaptureExceptionFactory.create(String.format("Cannot get real name for alias %s", varAlias));
        }

        char controlChar = realId.charAt(0);
        if (controlChar == VAR_MARKER || isExpressionChar(controlChar)) {
            String toEval;
            /**
             * If something is stored as the value of a context variable, or if it is an expression coming from the View, then we need to evaluate it
             */
            if (controlChar == VAR_MARKER) {
                /*
                 * If a variable, first retrieve the val we need to evaluate
                 */
                String idNoMarker = realId.substring(1);
                ExecutionContextField ecf = ExecutionContextFieldStorage.readByFields(workOrderUri, idNoMarker);
                if (ecf != null) {
                    toEval = ecf.getValue();
                } else {
                    toEval = null;
                }
            } else {
                /*
                 * If an expression, the "realId" is what we got from the mapping inside the View, so that's what we need to evaluate
                 */
                toEval = realId;
            }

            ContextValueType valueType = getValueType(toEval);
            if (valueType == ContextValueType.LINK) {
                return evalLinkExpression(callingContext, toEval.substring(1));
            } else if (valueType == ContextValueType.LITERAL) {
                return evalLiteral(toEval);
            } else {
                log.error(String.format("id='%s', value='%s' is not a literal nor expression!", varAlias, toEval));
                return null;
            }

        } else if (controlChar == LITERAL_MARKER) {
            return realId.substring(1);
        } else if (controlChar == TEMPLATE_MARKER) {
            return evalTemplateECF(callingContext, workOrderUri, realId.substring(1), view);
        } else {
            throw RaptureExceptionFactory.create(String.format("Error! Unrecognized mapping: %s -> %s", varAlias, realId));
        }
    }


    /**
     * Does this char denote an expression?
     * 
     * @param controlChar
     * @return
     */
    private static boolean isExpressionChar(char controlChar) {
        return controlChar == LINK_MARKER;
    }

    private static String evalLiteral(String literalVal) {
        if (literalVal == null || literalVal.length() < 1) {
            return literalVal;
        } else {
            return literalVal.substring(1);
        }
    }

    private static ContextValueType getValueType(String retVal) {
        if (retVal == null || retVal.isEmpty()) {
            return ContextValueType.LITERAL;
        } else if (retVal.charAt(0) == LINK_MARKER) {
            return ContextValueType.LINK;
        } else if (retVal.charAt(0) == LITERAL_MARKER) {
            return ContextValueType.LITERAL;
        } else {
            return null;
        }

    }

    private static String evalLinkExpression(CallingContext ctx, String uri) {
        // The content here is a uri to content, with an attribute that
        // describes the field we want
        RaptureURI innerUri = new RaptureURI(uri, Scheme.DOCUMENT);
        String fieldName = innerUri.getElement();
        if (fieldName == null) {
            log.error("Error, 'element' must be set in the ping uri " + uri);
            return null;
        } else {
            String content = Kernel.getDoc().getDoc(ctx, "//" + innerUri.getShortPath());
            if (content == null) {
                return "";
            }
            Map<String, Object> parsedDoc = JacksonUtil.getMapFromJson(content);
            String[] parts = fieldName.split("\\.");
            for (int i = 0; i < parts.length - 1; i++) {
                // the extra declaration is so I can mark the cast as safe (or at least known ugly)
                // it compiles down to a single assignment so there is no runtime cost
                @SuppressWarnings("unchecked")
                Map<String, Object> unchecked = (Map<String, Object>) parsedDoc.get(parts[i]);
                parsedDoc = unchecked;
            }
            return parsedDoc.get(parts[parts.length - 1]).toString();
        }
    }

    public static String evalTemplateECF(CallingContext ctx, String workOrderUri, String template, Map<String, String> view) {
        int nut = template.indexOf("$");
        if (nut < 0) return template;
        StringBuilder sb = new StringBuilder();
        int bolt = 0;
        while (nut >= 0) {
            sb.append(template.substring(bolt, nut));
            try {
                switch (template.charAt(nut + 1)) {
                    case '$':
                        sb.append('$');
                        bolt = nut + 2;
                        break;
                    case '{':
                        int startVar = nut + 2;
                        int endVar = template.indexOf('}', nut);
                        if (endVar < 0) {
                            throw RaptureExceptionFactory.create("'${' has no matching '}' in " + template);
                        }
                        String varName = template.substring(startVar, endVar);
                        String val = getValueECF(ctx, workOrderUri, varName, view);
                        if (val == null) {
                            throw RaptureExceptionFactory.create("Variable ${" + varName + "} required but missing");
                        }
                        sb.append(val);
                        bolt = endVar + 1;
                        break;
                    default:
                        throw RaptureExceptionFactory.create("Unescaped $ with no { varName } in " + template);
                }
            } catch (IndexOutOfBoundsException ex) {
                throw RaptureExceptionFactory.create("$ with no variable in " + template);
            }
            nut = template.indexOf("$", bolt);
        }
        sb.append(template.substring(bolt));
        return sb.toString();
    }

    public static void setValue(ExecutionContext context, Map<String, String> view, String varAlias, ContextValueType type, String value) {
        switch (type) {
            case LINK:
                setValue(context, view, varAlias, LINK_MARKER + value);
                break;
            case LITERAL:
                setValue(context, view, varAlias, LITERAL_MARKER + value);
                break;
            default:
                log.error("Unsupported type " + type);
                break;
        }
    }

    private static void setValue(ExecutionContext context, Map<String, String> view, String varAlias, String value) {
        String realId = addContextMarkerAndLookupInView(view, varAlias, VAR_MARKER);
        if (realId != null && realId.length() > 1 && realId.charAt(0) == VAR_MARKER) {
            String idNoMarker = realId.substring(1);
            context.getData().put(idNoMarker, value);
        } else {
            throw RaptureExceptionFactory.create(String.format(
                    "Error! Can only set context values on variables, which start with '%s'. This is not the case: varAlias='%s', realId='%s'!", VAR_MARKER,
                    varAlias, realId));
        }
    }

    public static void setValueECF(CallingContext context, String workOrderUri, Map<String, String> view, String varAlias, ContextValueType type, String value) {
        switch (type) {
            case LINK:
                setValueECF(context, workOrderUri, view, varAlias, LINK_MARKER + value);
                break;
            case LITERAL:
                setValueECF(context, workOrderUri, view, varAlias, LITERAL_MARKER + value);
                break;
            default:
                log.error("Unsupported type " + type);
                break;
        }
    }

    private static void setValueECF(CallingContext context, String workOrderUri, Map<String, String> view, String varAlias, String value) {
        String realId = addContextMarkerAndLookupInView(view, varAlias, VAR_MARKER);
        if (realId != null && realId.length() > 1 && realId.charAt(0) == VAR_MARKER) {
            String idNoMarker = realId.substring(1);
            ExecutionContextField ecf = new ExecutionContextField();
            ecf.setWorkOrderURI(workOrderUri);
            ecf.setVarName(idNoMarker);
            ecf.setValue(value);
            ExecutionContextFieldStorage.add(ecf, context.getUser(), "Set context field value");
        } else {
            throw RaptureExceptionFactory.create(String.format(
                    "Error! Can only set context values on variables, which start with '%s'. This is not the case: varAlias='%s', realId='%s'!", VAR_MARKER,
                    varAlias, realId));
        }
    }

    private static String addContextMarkerAndLookupInView(Map<String, String> view, String alias, char defaultMarker) {
        // Look in view for alias.
        String viewValue = lookupInView(view, alias);
        if (viewValue != null) {
            return viewValue;
        }
        // If not there, try prepending the default marker.
        alias = addDefaultMarker(alias, defaultMarker);
        return alias;
    }

    // If alias is a variable in the view, return the value from the view, otherwise return alias.
    private static String lookupInView(Map<String, String> view, String alias) {
        if (alias == null || alias.length() == 0) {
            // If null or empty, just leave it alone.
            return null;
        }
        if (view != null && view.containsKey(alias)) {
            String realId = view.get(alias);
            if (realId.length() < 2) {
                log.warn(String.format("Bad view definition: %s -> %s", alias, realId));
                return VAR_MARKER + alias;
            } else {
                return realId;
            }
        }
        // Nothing found in view, just return null;
        return null;
    }

    // If the alias is naked, prepend the defaultMarker.
    private static String addDefaultMarker(String alias, char defaultMarker) {
        if (alias == null || alias.length() == 0) {
            // If null or empty, just leave it alone.
            return alias;
        }
        switch (alias.charAt(0)) {
            case LINK_MARKER:
            case LITERAL_MARKER:
            case TEMPLATE_MARKER:
                // Don't add if we have a known context marker.
                return alias;
        }
        return defaultMarker + alias;
    }

    public static void saveContext(ExecutionContext executionContext, String user, String comment) {
        Map<String, String> data = executionContext.getData();
        for (String varName : data.keySet()) {
            String value = data.get(varName);

            ExecutionContextField ecf = new ExecutionContextField();
            ecf.setWorkOrderURI(executionContext.getWorkOrderURI());
            ecf.setVarName(varName);
            ecf.setValue(value);
            ExecutionContextFieldStorage.add(ecf, user, comment);
        }
    }

    public static String treatValueAsDefaultLiteral(String value) {
        // prepend a LITERAL_MARKER if it was a naked value, strip the $ if it was a variable.
        if (value != null && value.length() > 0) {
            switch (value.charAt(0)) {
                case ExecutionContextUtil.LINK_MARKER:
                case ExecutionContextUtil.LITERAL_MARKER:
                case ExecutionContextUtil.TEMPLATE_MARKER:
                    break;

                case ExecutionContextUtil.VAR_MARKER:
                    value = value.substring(1);
                    break;

                default:
                    value = ExecutionContextUtil.LITERAL_MARKER + value;
            }
        }
        return value;
    }
}
