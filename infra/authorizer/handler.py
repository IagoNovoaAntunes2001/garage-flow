import base64
import hashlib
import hmac
import json
import os
import time


def decode_part(value):
    return base64.b64decode(value + "=" * (-len(value) % 4), altchars=b"-_", validate=True)


def verify_token(token, secret, issuer, now):
    if len(token) > 8192 or len(secret.encode("utf-8")) < 32:
        return False
    parts = token.split(".")
    if len(parts) != 3:
        return False
    header = json.loads(decode_part(parts[0]))
    claims = json.loads(decode_part(parts[1]))
    if not isinstance(header, dict) or not isinstance(claims, dict) or header.get("alg") != "HS256":
        return False
    signature = hmac.new(secret.encode("utf-8"), (parts[0] + "." + parts[1]).encode("ascii"), hashlib.sha256).digest()
    if not hmac.compare_digest(signature, decode_part(parts[2])):
        return False
    expiration = claims.get("exp")
    not_before = claims.get("nbf", 0)
    roles = claims.get("roles")
    return (
        type(expiration) is int and expiration > now
        and type(not_before) is int and not_before <= now
        and claims.get("iss") == issuer
        and isinstance(claims.get("sub"), str) and bool(claims["sub"])
        and isinstance(roles, list) and "ADMIN" in roles
    )


def handler(event, context):
    try:
        import boto3

        headers = event.get("headers") or {}
        authorization = next((value for key, value in headers.items() if key.lower() == "authorization"), "")
        scheme, token = authorization.split(" ", 1)
        if scheme.lower() != "bearer":
            return {"isAuthorized": False}
        secret = boto3.client("secretsmanager").get_secret_value(SecretId=os.environ["JWT_SECRET_ARN"])["SecretString"]
        return {"isAuthorized": verify_token(token, secret, os.environ["JWT_ISSUER"], int(time.time()))}
    except Exception:
        return {"isAuthorized": False}
