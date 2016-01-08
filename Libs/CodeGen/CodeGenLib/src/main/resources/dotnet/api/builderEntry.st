    private <fieldType> <fieldName>;

/**
* <if (deprecated)>@deprecated <deprecated><endif>
*/
<if (deprecated)>@Deprecated<endif>
    public <name>PathBuilder <fieldName>(<fieldType> <fieldName>) {
        this.<fieldName> = <fieldName>;
        return this;
    }
    
