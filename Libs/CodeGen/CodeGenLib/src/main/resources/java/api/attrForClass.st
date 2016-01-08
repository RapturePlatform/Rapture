<if(isURI)>
// TODO private <if(isURI)>RaptureURI<else><type><endif> <name>;
<endif>

	private <type> <name>;
	
	public void set<name; format="upcase">(<type> <name>) {
             this.<name> = <if(isURI)> new RaptureURI(<name>, Scheme.<scheme>).toString() /* TODO remove toString() */ <else> <name> <endif>;
	}
	
	public <type> get<name; format="upcase">() {
	     return this.<name>;
	}

