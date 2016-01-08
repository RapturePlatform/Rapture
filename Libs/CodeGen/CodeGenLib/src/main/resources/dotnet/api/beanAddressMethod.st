//@Override
public RaptureURI getAddressURI() {
    return new RaptureURI(getStoragePath(), Scheme.<scheme>);
	// return new <scheme; format=lower>URI(getStoragePath());
	
}

public static final Scheme scheme = Scheme.<scheme>;
//@Override
public Scheme getScheme() {
	return scheme;
}	

