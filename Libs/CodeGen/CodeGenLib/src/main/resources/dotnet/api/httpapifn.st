   
    public <ret> <name>(<params; separator=",  ">)
        {
            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
         new System.Web.Script.Serialization.JavaScriptSerializer();
            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
            <name; format="upcase">Payload requestObj = new <name; format="upcase">Payload();
        <rest(apiparams):apiset(); separator="\n">
            string responseObjectJson;
            responseObjectJson = makeRequest("<name; format="upper">", RaptureSerializer.SerializeJson\<<name; format="upcase">Payload\>(requestObj));
            if (responseObjectJson != null && responseObjectJson.Length > 0) {
              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
              return RaptureSerializer.DeserializeJson\<<ret>\>(resp.response.content);
            }
             throw new Exception("Error in <name>, no response returned");
        }