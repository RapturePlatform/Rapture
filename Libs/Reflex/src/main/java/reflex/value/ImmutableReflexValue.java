package reflex.value;

import java.util.List;

import reflex.ReflexException;

public class ImmutableReflexValue extends ReflexValue {

    public static final ImmutableReflexValue TRUE = new ImmutableReflexValue(Boolean.TRUE);
    public static final ImmutableReflexValue FALSE = new ImmutableReflexValue(Boolean.FALSE);

	boolean valueSet = false;
	public ImmutableReflexValue(int lineNumber, List<ReflexValue> v) {
		super(lineNumber, v);
		valueSet = true;
	}
	public ImmutableReflexValue(int lineNumber, Object v) {
		super(lineNumber, v);
		valueSet = true;
	}
	public ImmutableReflexValue(List<ReflexValue> v) {
		super(v);
		valueSet = true;
	}
	public ImmutableReflexValue(Object v) {
		super(v);
		valueSet = true;
	}
	
	@Override
	public void setValue(Object value) {
		if (valueSet) throw new ReflexException(-1, "Value is immutable, Cannot be changed");
		super.setValue(value);
		valueSet = true;
	}

}
