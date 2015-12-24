from base64 import urlsafe_b64encode, urlsafe_b64decode
import hmac
import hashlib
import sys
from datetime import datetime
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

class AuthData():

    def __init__(self, username, domain, email, issued_at):
        self.username = username
        self.domain = domain
        self.email = email
        self.issued_at = issued_at

class RosefireError(Exception):
    pass

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
            sig = _sign(secret, secure_bits)
            if encoded[2] != sig:
                raise RosefireError('Token generated with invalid secret!')
            decoded_claims = _decode_json(encoded[1])['d']
            timestamp = decoded_claims['timestamp']
            issued_at = datetime.strptime(timestamp, '%Y-%m-%dT%H:%M:%S-05:00')
            return AuthData(decoded_claims['uid'], decoded_claims['domain'], decoded_claims['email'], issued_at)
        except:
            raise RosefireError('Error decoding token')



if __name__ == "__main__":
   token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZWJ1ZyI6ZmFsc2UsImQiOnsidWlkIjoicm9ja3dvdGoiLCJkb21haW4iOiJyb3NlLWh1bG1hbi5lZHUiLCJlbWFpbCI6InJvY2t3b3RqQHJvc2UtaHVsbWFuLmVkdSIsInRpbWVzdGFtcCI6IjIwMTUtMTItMjNUMTU6MjU6MTQtMDU6MDAifSwidiI6MCwiYWRtaW4iOnRydWUsImlhdCI6MTQ1MDkwMjMxNH0.P50l5YvcRNO4IQ5WT9sKfaBFBiJHU5yxLnCxfJ5xprI"
   secret = "secret"
   user_info = RosefireTokenVerifier(secret).verify(token)
   print user_info.email
   print user_info.issued_at
