package rapture.operation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;
import rapture.script.reflex.ReflexHandler;
import reflex.ReflexExecutor;

/**
 * This class is used to manage the invocation of an operation on a document
 * 
 * @author alanmoore
 *
 */
public class OperationManager {
	private static String INTERFACE = "$interface";
	private static String BASE = "$parent";
	private static int MAXFOLLOW = 10;

	public Map<String, Object> invoke(CallingContext ctx, String docUri,
			String function, Map<String, Object> params) {
		// Step 1 - find the function to execute
		String reflexScript = findFunction(ctx, docUri, function, null, true, 0);
		if (reflexScript == null) {
			throw RaptureExceptionFactory.create("Could not locate " + function
					+ " on " + docUri + " or parents/interfaces");
		}
		Object x= invokeScript(ctx, reflexScript, docUri, function, params, false);
		if (x instanceof Map<?, ?>) {
			return (Map<String, Object>) x;
		}
		throw RaptureExceptionFactory.create("Incorrect return type from script - " + x.getClass().toString());
	}

	public Map<String, Object> invokeAlt(CallingContext ctx, String docUri,
			String function, Map<String, Object> params, String altInterface) {
		String reflexScript = findFunction(ctx, docUri, function, altInterface, true, 0);
		if (reflexScript == null) {
			throw RaptureExceptionFactory.create("Could not locate " + function
					+ " on " + docUri + " or parents/interfaces");
		}
		Object x= invokeScript(ctx, reflexScript, docUri, function, params, false);
		if (x instanceof Map<?, ?>) {
			return (Map<String, Object>) x;
		}
		throw RaptureExceptionFactory.create("Incorrect return type from script - " + x.getClass().toString());
	}
	
	public Map<String, Object> invokeSave(CallingContext ctx, String docUri,
			String function, Map<String, Object> params) {
		String reflexScript = findFunction(ctx, docUri, function, null, true, 0);
		if (reflexScript == null) {
			throw RaptureExceptionFactory.create("Could not locate " + function
					+ " on " + docUri + " or parents/interfaces");
		}
		Object x= invokeScript(ctx, reflexScript, docUri, function, params, true);
		if (x instanceof Map<?, ?>) {
			return (Map<String, Object>) x;
		}
		throw RaptureExceptionFactory.create("Incorrect return type from script - " + x.getClass().toString());
	}
	
	public Map<String, Object> invokeSaveAlt(CallingContext ctx, String docUri,
			String function, Map<String, Object> params, String altInterface) {
		String reflexScript = findFunction(ctx, docUri, function, altInterface, true, 0);
		if (reflexScript == null) {
			throw RaptureExceptionFactory.create("Could not locate " + function
					+ " on " + docUri + " or parents/interfaces");
		}
		Object x= invokeScript(ctx, reflexScript, docUri, function, params, true);
		if (x instanceof Map<?, ?>) {
			return (Map<String, Object>) x;
		}
		throw RaptureExceptionFactory.create("Incorrect return type from script - " + x.getClass().toString());
	}
	
	private String findFunction(CallingContext ctx, String docUri,
			String function, String altInterface, boolean followParent, int followCount) {
		// First load this document
		String content = Kernel.getDoc().getDoc(ctx, docUri);
		if (content == null) {
			throw RaptureExceptionFactory.create("Could not load document - "
					+ docUri);
		}
		// Convert to a map
		Map<String, Object> docMap = JacksonUtil.getMapFromJson(content);
		if (docMap == null) {
			throw RaptureExceptionFactory.create("Document is not json - "
					+ docUri);
		}
		// Now look for the "function" key
		if (docMap.containsKey(function)) {
			return loadScript(ctx, docMap.get(function).toString());
		}
		// Now look for the $interface key
		if (docMap.containsKey(INTERFACE)) {
			Object in = docMap.get(INTERFACE);
			String script = null;
			if (in instanceof String) {
				script = findFunction(ctx, in.toString(), function, altInterface, false,
						followCount + 1);
			} else if (in instanceof List) {
				List<?> l = (List<?>) in;
				for (Object v : l) {
					script = findFunction(ctx, v.toString(), function, altInterface, false,
							followCount + 1);
					if (script != null) {
						break;
					}
				}
			}
			if (script != null) {
				return script;
			}
		}
		if (followParent && followCount < MAXFOLLOW) {
			if (docMap.containsKey(BASE)) {
				return findFunction(ctx, docMap.get(BASE).toString(), function, altInterface,
						true, followCount + 1);
			}
		}
		if (altInterface != null) {
			return findFunction(ctx, altInterface, function, null, false, followCount+1);
		}
		return null;
	}

	private String loadScript(CallingContext ctx, String script) {
		if (script.startsWith("script://")) {
			RaptureScript scr = Kernel.getScript().getScript(ctx, script);
			return scr.getScript();
		}
		return script;
	}

	private Object invokeScript(CallingContext ctx, String script,
			String docUri, String function, Map<String, Object> params, boolean saveThis) {
		Map<String, Object> masterParameterMap = new HashMap<String, Object>();
		String doc = Kernel.getDoc().getDoc(ctx, docUri);
		masterParameterMap.put("this", JacksonUtil.getMapFromJson(doc));
		masterParameterMap.put("function", function);
		masterParameterMap.put("params", params);
		masterParameterMap.put("ctx", ctx);

		// Now run script
		ReflexHandler handler = new ReflexHandler(ctx);
		Object ret = ReflexExecutor.runReflexProgram(script, handler,
				masterParameterMap);
		
		if (saveThis) {
			// Save the this parameter back
			 String modifiedThis = JacksonUtil.jsonFromObject(masterParameterMap.get("this"));
			 Kernel.getDoc().putDoc(ctx, docUri, modifiedThis);
		}
		return ret;
	}

	
}
