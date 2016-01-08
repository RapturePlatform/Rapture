    private <fieldType> <fieldName>;

/**
* <if (deprecated)>@deprecated <deprecated><endif>
*/
<if (deprecated)>@Deprecated<endif>
    public <name>PathBuilder <fieldName>(<fieldType> <fieldName>) {
        <if (deprecated)>log.warn("Deprecated method <name>PathBuilder.<fieldName> called by "+
		new Throwable().getStackTrace()[1].getFileName());<endif>
        this.<fieldName> = <fieldName>;
        return this;
    }
    
