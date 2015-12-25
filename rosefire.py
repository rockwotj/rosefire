from base64 import urlsafe_b64encode, urlsafe_b64decode
import hmac
import hashlib
import sys
from datetime import datetime
import urllib2
try:
    import json
except ImportError:
    import simplejson as json

if sys.version_info < (2, 7):
    def _encode(bytes_data):
        # Python 2.6 has problems with bytearrays in b64
        encoded = urlsafe_b64encode(bytes(bytes_data))
        return encoded.decode('utf-8').replace('=', '')
else:
    def _encode(bytes):
        encoded = urlsafe_b64encode(bytes)
        return encoded.decode('utf-8').replace('=', '')

if sys.version_info < (2, 7):
    def _decode(bytes_data):
        # Python 2.6 has problems with bytearrays in b64
        encoded = urlsafe_b64decode(bytes(bytes_data))
        return encoded.decode('utf-8')
else:
    def _decode(bytes):
        encoded = urlsafe_b64decode(bytes)
        return encoded.decode('utf-8')


def _sign(secret, to_sign):
    def portable_bytes(s):
        try:
            return bytes(s, 'utf-8')
        except TypeError:
            return bytes(s)
    return _encode(hmac.new(portable_bytes(secret), portable_bytes(to_sign), hashlib.sha256).digest())

def _decode_json(obj_str):
    # http://stackoverflow.com/a/9956217
    obj_str += '=' * (4 - len(obj_str) % 4)
    return json.loads(_decode(bytearray(obj_str, 'utf-8')))

class RosefireError(Exception):
    pass

def get_token(registry_token, email, password, options=None):
    req = urllib2.Request('https://rosefire.csse.rose-hulman.edu/api/auth/')
    req.add_header('Content-Type', 'application/json')

    data = {
        'registryToken': registry_token,
        'email': email,
        'password': password
    }
    if options is not None:
        data['options'] = {}
        for key in ['admin', 'expires', 'notBefore']:
            if key in options:
                data['options'][key] = options[key]
    response = None
    error = False
    try:
        response = urllib2.urlopen(req, json.dumps(data))
    except urllib2.HTTPError as e:
        error = True
        response = e
    payload = json.loads(response.read())
    if error:
        raise RosefireError(payload['error'] or 'Error getting token')
    return payload['token']

class AuthData():

    def __init__(self, username, domain, email, issued_at):
        self.username = username
        self.domain = domain
        self.email = email
        self.issued_at = issued_at

class RosefireTokenVerifier():

    def __init__(self, secret):
        self.secret = secret

    def verify(self, token):
        try:
            encoded = token.split('.')
            decoded_header = _decode_json(encoded[0])
            if decoded_header['typ'] != 'JWT':
                raise RosefireError('Incorrect JWT')
            if decoded_header['alg'] != 'HS256':
                raise RosefireError('Wrong algorithm!')
            secure_bits = '.'.join(encoded[:-1])
            sig = _sign(self.secret, secure_bits)
            if encoded[2] != sig:
                raise RosefireError('Token generated with invalid secret!')
            decoded_claims = _decode_json(encoded[1])
            issued_at = datetime.fromtimestamp(decoded_claims['iat'])
            payload = decoded_claims['d']
            return AuthData(payload['uid'], payload['domain'], payload['email'], issued_at)
        except:
            raise RosefireError('Error decoding token')



if __name__ == "__main__":
   registry_token = "59c300ed7fa9d438198293e9cb675290fcf40988c93103b10b997dc0329c6aa58d7d0f1c244ffd41a0a24e8e04d089238oVFqR4JfVWV/+sohxs6u2He6uOd6ZpFovwiRNam8OUb6kyk6BLktxRGT4/sq6jtYHS7Q/cDH4MmUml8n89i9HL/AIQhzU3HjuIKJS96JBA="
   token = get_token(registry_token, "rockwotj@rose-hulman.edu", "Pa$sW0rd")
   secret = "secret"
   user_info = RosefireTokenVerifier(secret).verify(token)
   print user_info.email
   print user_info.issued_at
