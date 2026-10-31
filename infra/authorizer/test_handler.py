import base64
import hashlib
import hmac
import json
import sys
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from handler import handler, verify_token

SECRET = "0123456789abcdef0123456789abcdef"


def token_for(claims, secret=SECRET, algorithm="HS256"):
    def encode(value):
        return base64.urlsafe_b64encode(json.dumps(value, separators=(",", ":")).encode()).rstrip(b"=").decode()

    header = encode({"alg": algorithm})
    payload = encode(claims)
    signature = hmac.new(secret.encode(), (header + "." + payload).encode(), hashlib.sha256).digest()
    return header + "." + payload + "." + base64.urlsafe_b64encode(signature).rstrip(b"=").decode()


class AuthorizerTest(unittest.TestCase):
    def test_requires_valid_signature_issuer_expiry_and_admin_role(self):
        claims = {"iss": "garage-flow", "sub": "admin", "exp": 200, "roles": ["ADMIN"]}
        self.assertTrue(verify_token(token_for(claims), SECRET, "garage-flow", 100))
        self.assertFalse(verify_token(token_for(claims, secret="a" * 32), SECRET, "garage-flow", 100))
        self.assertFalse(verify_token(token_for(claims), SECRET, "other", 100))
        self.assertFalse(verify_token(token_for({**claims, "exp": 100}), SECRET, "garage-flow", 100))
        self.assertFalse(verify_token(token_for({**claims, "nbf": 101}), SECRET, "garage-flow", 100))
        self.assertFalse(verify_token(token_for({**claims, "roles": ["CUSTOMER"]}), SECRET, "garage-flow", 100))
        self.assertFalse(verify_token(token_for(claims, algorithm="none"), SECRET, "garage-flow", 100))

    def test_handler_fails_closed_and_accepts_signed_admin(self):
        claims = {"iss": "garage-flow", "sub": "admin", "exp": 4102444800, "roles": ["ADMIN"]}
        client = SimpleNamespace(get_secret_value=lambda **kwargs: {"SecretString": SECRET})
        sdk = SimpleNamespace(client=lambda service: client)
        with patch.dict(sys.modules, {"boto3": sdk}), patch.dict("os.environ", {"JWT_SECRET_ARN": "secret", "JWT_ISSUER": "garage-flow"}):
            self.assertTrue(handler({"headers": {"Authorization": "Bearer " + token_for(claims)}}, None)["isAuthorized"])
            self.assertFalse(handler({"headers": {"authorization": "Bearer invalid"}}, None)["isAuthorized"])
            self.assertFalse(handler({"headers": {}}, None)["isAuthorized"])
            self.assertFalse(handler({"headers": {"authorization": "Basic value"}}, None)["isAuthorized"])

    def test_handler_denies_when_secret_store_fails(self):
        def fail(**kwargs):
            raise RuntimeError("unavailable")

        sdk = SimpleNamespace(client=lambda service: SimpleNamespace(get_secret_value=fail))
        with patch.dict(sys.modules, {"boto3": sdk}), patch.dict("os.environ", {"JWT_SECRET_ARN": "secret", "JWT_ISSUER": "garage-flow"}):
            self.assertFalse(handler({"headers": {"authorization": "Bearer a.b.c"}}, None)["isAuthorized"])


if __name__ == "__main__":
    unittest.main()
