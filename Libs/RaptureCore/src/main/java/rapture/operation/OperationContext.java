package rapture.operation;

import java.util.HashMap;
import java.util.Map;

import rapture.common.CallingContext;

public class OperationContext {
	public Map<String, Object> getThisPtr() {
		return thisPtr;
	}
	public void setThisPtr(Map<String, Object> thisPtr) {
		this.thisPtr = thisPtr;
	}
	public Map<String, Object> getParams() {
		return params;
	}
	public void setParams(Map<String, Object> params) {
		this.params = params;
	}
	public String getFunction() {
		return function;
	}
	public void setFunction(String function) {
		this.function = function;
	}
	public CallingContext getCtx() {
		return ctx;
	}
	public void setCtx(CallingContext ctx) {
		this.ctx = ctx;
	}
	public String getReflexScript() {
		return reflexScript;
	}
	public void setReflexScript(String reflexScript) {
		this.reflexScript = reflexScript;
	}
	public Map<String, Object> getModifiedThisPtr() {
		return modifiedThisPtr;
	}
	public void setModifiedThisPtr(Map<String, Object> modifiedThisPtr) {
		this.modifiedThisPtr = modifiedThisPtr;
	}
	public Map<String, Object> getRet() {
		return ret;
	}
	public void setRet(Map<String, Object> ret) {
		this.ret = ret;
	}
	private Map<String, Object> thisPtr;
	private Map<String, Object> modifiedThisPtr;
	private Map<String, Object> params;
	private String function;
	private CallingContext ctx;
	private String reflexScript;
	private Map<String, Object> ret;
	
	public OperationContext(OperationContext other) {
		setCtx(other.getCtx());
		setFunction(other.getFunction());
		setReflexScript(other.getReflexScript());
		setThisPtr(new HashMap<String, Object>(other.getThisPtr()));
		setParams(other.getParams());
	}
	
	public OperationContext() {
		
	}
}
