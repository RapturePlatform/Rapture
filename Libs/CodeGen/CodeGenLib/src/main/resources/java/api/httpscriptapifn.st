<if (deprecated)>   /**
    * @deprecated <deprecated>
    */
    @Deprecated<endif>
    @Override
    public <ret> <name>(<params; separator=", ">) {
<if (!isVoid)>
        return <name>(null<if(callParams)>,<endif><callParams; separator=",">);
<else>
        <name>(null<if(callParams)>,<endif><callParams; separator=",">);
<endif>
    }
