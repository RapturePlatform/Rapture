package rapture.operation;

import java.util.Map;

public class OperationMergeResult {
	private Map<String, Object> thisMap = null;
	private Map<String, Object> retMap = null;
	
	public void merge(OperationContext ctx) {
		if (thisMap == null) {
			thisMap = ctx.getModifiedThisPtr();
		} else {
			merge(thisMap, ctx.getModifiedThisPtr());
		}
		if (retMap == null) {
			retMap = ctx.getRet();
		} else {
			merge(retMap, ctx.getRet());
		}
	}
	
	private void merge(Map<String, Object> into, Map<String, Object> ret) {
		for(Map.Entry<String, Object> e : ret.entrySet()) {
			if(into.containsKey(e.getKey())) {
				if (e.getValue() instanceof Map<?, ?>) {
					Object v = into.get(e.getKey());
					if (v instanceof Map<?, ?>) {
						merge((Map<String, Object>)v, (Map<String, Object>)e.getValue());
					} else {
						into.put(e.getKey(), e.getValue());
					}
				} else {
					into.put(e.getKey(), e.getValue());
				}
			} else {
				into.put(e.getKey(), e.getValue());				
			}
		}
	}

	public Map<String, Object> mergedThis() {
		return thisMap;
	}
	
	public Map<String, Object> mergedRet() {
		return retMap;
	}
}
