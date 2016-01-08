# Doc
## getDoc

```java
HttpDocApi docApi = new HttpDocApi(loginApi);
String doc = docApi.getDoc("document://my/uri");
```

```python
baseAPI.doDoc_GetDoc("document://my/uri");
```

```js
alert("some javascript");
```

```shell
# With shell, you can just pass the correct header with each request
curl "http://localhost:8665/rapture/doc/getDoc"
  -H "Authorization: someAuthToken"
```

This endpoint gets a document.

### HTTP Request

`GET http://example.com:8665/rapture/doc/getDoc`

### Query Parameters

Parameter | Type | Description
--------- | ---- | -----------
documentURI | DocumentURI | The document URI to get

## putDoc

```java
HttpDocApi docApi = new HttpDocApi(loginApi);
/**
@param docURI could be xyz
*/
String doc = docApi.putDoc(docURI, jsonValuew);
```

```python
baseAPI.doDoc_PutDoc("document://my/uri", "{\"field1\": 1}");
```

```js
alert("some javascript");
```

```shell
# With shell, you can just pass the correct header with each request
curl "http://localhost:8665/rapture/doc/putDoc"
  -H "Authorization: someAuthToken"
```

This endpoint writes a document.

### HTTP Request

`PUT http://example.com:8665/rapture/doc/putDoc`

### Query Parameters

Parameter | Type | Description
--------- | ---- | -----------
documentURI | DocumentURI | The document URI to write
content | JSON | The JSON to write
