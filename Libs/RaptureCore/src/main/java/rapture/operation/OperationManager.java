package rapture.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;
import rapture.kernel.OperationApiImpl;
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
	   private static Logger logger = Logger.getLogger(OperationManager.class);

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

	private void invokeContext(OperationContext context) {
		logger.info("Invoke on context - " + context.getFunction());
		// Given a context, run the script and then save the output back into the context
		Map<String, Object> masterParameterMap = new HashMap<String, Object>();
		masterParameterMap.put("this", context.getThisPtr());
		masterParameterMap.put("function", context.getFunction());
		masterParameterMap.put("params", context.getParams());
		masterParameterMap.put("ctx", context.getCtx());

		// Now run script
		ReflexHandler handler = new ReflexHandler(context.getCtx());
		Object ret = ReflexExecutor.runReflexProgram(context.getReflexScript(), handler,
				masterParameterMap);
		context.setModifiedThisPtr((Map<String, Object>) masterParameterMap.get("this"));
		context.setRet((Map<String, Object>) ret);
		logger.info("Finshed invoke on context " + context.getFunction());
	}
	
	public Map<String, Object> invokeParallel(CallingContext context, String docUri, List<String> methods,
			Map<String, Object> params) {
		List<OperationContext> contextList = new ArrayList<OperationContext>();
		OperationContext first = new OperationContext();
		first.setCtx(context);
		first.setParams(params);
		String doc = Kernel.getDoc().getDoc(context, docUri);
		first.setThisPtr(JacksonUtil.getMapFromJson(doc));
		for(String method : methods) {
			first.setFunction(method);
			String funcRef = findFunction(context, docUri, method, null, true, 0);
			first.setReflexScript(loadScript(context, funcRef));
			contextList.add(first);
			first = new OperationContext(first);
		}
		
		// Now we run all of these scripts (ideally in parallel)
		ExecutorService executor = Executors.newCachedThreadPool();
		for(OperationContext oc : contextList) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					invokeContext(oc);
				}
				
			});
		}
		executor.shutdown();
		while(!executor.isTerminated()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		// Now we have finished calling everything, we merge the results
		OperationMergeResult res = new OperationMergeResult();
		for(OperationContext oc : contextList) {
			res.merge(oc);
		}
		return res.mergedRet();
	}

	
}
