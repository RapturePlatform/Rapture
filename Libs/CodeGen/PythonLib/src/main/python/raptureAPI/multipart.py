import requests, httplib, mimetypes, hashlib, json, gzip, StringIO

def post_multipart(session, host, selector, fields, files):
    """
    Post fields and files to an http host as multipart/form-data.
    fields is a sequence of (name, value) elements for regular form fields.
    files is a sequence of (name, filename, value) elements for data to be uploaded as files
    Return the server's response page.
    """
    return new_post_multipart(session, host, selector, fields, files)

def has_tricky_encoding(fields):
    if isinstance(fields, list):
        for i in range (0, len(fields)):
            if is_tricky_field(fields[i]):
                return True
        return False
    return True

def is_tricky_field(field):
    (key, val) = field
    return ('\\' in val) or ('?' in val) or ('=' in val) or ('#' in val) or ('%' in val) or ('&' in val)

def old_post_multipart(host, selector, fields, files):
    """
    This is a slow version of post_multipart with a robust encoder
    """
    content_type, body = encode_multipart_formdata(fields, files)
    h = httplib.HTTPConnection(host)
    h.putrequest('POST', selector)
    h.putheader('content-type', content_type)
    h.putheader('content-length', str(len(body)))
    h.endheaders()
    h.send(body)
    resp = h.getresponse()
    return resp.read()

def new_post_multipart(session, host, selector, fields, files):
    """
    This is a fast version of post_multipart with some problems with embedded JSON. It should be
    used only for simple fields
    """
    if (not host.startswith('http://') and not host.startswith('https://')):
        host = 'http://' + host
    fields_and_files = []
    for k, v in fields:
        if k.upper() == 'PARAMS':
            string_io = StringIO.StringIO()
            # Compress the content while we're at it...
            with gzip.GzipFile(fileobj=string_io, mode='w') as gzip_file:
                gzip_file.write(v)

            # Setting "Content-Type: application/octet-stream" bypasses the webserver's file size limit
            fields_and_files.append((k, ('params.txt', string_io.getvalue(), 'application/octet-stream')))
        else:
            fields_and_files.append((k, ('', v)))

    fields_and_files.extend(files)
    resp = session.post(host + selector, files=fields_and_files)
    return resp.text

def encode_multipart_formdata(fields, files):
    """
    fields is a sequence of (name, value) elements for regular form fields.
    files is a sequence of (name, filename, value) elements for data to be uploaded as files
    Return (content_type, body) ready for httplib.HTTP instance
    """
    BOUNDARY = '----------ThIs_Is_tHe_bouNdaRY_$'
    CRLF = '\r\n'
    L = []
    for (key, value) in fields:
        L.append('--' + BOUNDARY)
        L.append('Content-Disposition: form-data; name="%s"' % key)
        L.append('')
        L.append(value)
    for (key, filename, value) in files:
        L.append('--' + BOUNDARY)
        L.append('Content-Disposition: form-data; name="%s"; filename="%s"' % (key, filename))
        L.append('Content-Type: %s' % get_content_type(filename))
        L.append('')
        L.append(value)
    L.append('--' + BOUNDARY + '--')
    L.append('')
    body = CRLF.join(L)
    content_type = 'multipart/form-data; boundary=%s' % BOUNDARY
    return content_type, body

def get_content_type(filename):
    return mimetypes.guess_type(filename)[0] or 'application/octet-stream'

def get_salt(jsonstr):
    json_object = json.loads(jsonstr)
    return json_object['response']['salt']

def get_context(jsonstr):
    json_object = json.loads(jsonstr)
    return json_object['response']

def get_value_by_key(jsonstr,key):
    json_object = json.loads(jsonstr)
    return json_object['response'][key]

def MD5(password):
    d = hashlib.md5()
    d.update(password)
    return d.hexdigest()

def get_hmxcontext(jsonstr):
    json_object = json.loads(jsonstr)
    hmxcontextobj = json_object['response']
    return json.dumps(hmxcontextobj)
