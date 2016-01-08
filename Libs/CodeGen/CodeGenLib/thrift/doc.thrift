namespace java rapture.api.thrift

service RaptureDoc {

	string getDoc(1:string docURI),
	string putDoc(1:string docURI, 2:string content);
}

    
