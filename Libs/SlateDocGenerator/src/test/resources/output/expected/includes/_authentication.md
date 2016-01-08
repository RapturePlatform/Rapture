# Authentication

> To log in and get authenticated, use this code:

```java
CredentialsProvider provider = new SimpleCredentialsProvider("rapture", "password");
HttpLoginApi loginApi = new HttpLoginApi("http://localhost:8665/rapture", provider);
loginApi.login();
```

```python
import raptureAPI,multipart,json,pprint,idpAPI,time
uri = "http://localhost:8665/rapture" # change this to real uri
user = "rapture"  # change this to real user 
password = "password" # change this to real password

# connect to Rapture and log in 
baseApi = raptureAPI.raptureAPI(uri, user, password)
if 'valid' in baseApi.context and baseApi.context['valid']:
    print 'logged in successfully'
else:
    print 'oops. there was an error logging in.'
```

```js
var rapture=require('./rapture.js');
//umm just kidding, this isn't really client-side
```

```shell
# With shell, you can just pass the correct header with each request
curl "http://localhost:8665/rapture"
  -H "Authorization: someAuthToken"
```

> You must replace <code>localhost</code> with your URI.

Rapture uses username and password to log in. Look at the code samples on the right for a how-to.

`Host: localhost`
`Username: rapture`
`Password: password`

<aside class="notice">
You must replace <code>localhost</code> as well as username and password with your own values.
</aside>
