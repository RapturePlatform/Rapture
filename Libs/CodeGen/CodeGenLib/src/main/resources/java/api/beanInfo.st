/*
*  Start of field '<varName>' (<varType>) 
*/
private <varType> <varName><if(constructor)> = <constructor><endif>;

@JsonProperty("<varName>")
public <varType> get<varName; format="upcase">() {
   return <varName>;
}

@JsonProperty("<varName>")
public void set<varName; format="upcase">(<varType> <varName>) {
   this.<varName> = <if(isURI)> new RaptureURI(<varName><if (scheme)>, Scheme.<scheme><endif>).toString() /* TODO remove toString */  <else> <varName> <endif>;
}
/*
*  end of field '<varName>' (<varType>) 
*/

