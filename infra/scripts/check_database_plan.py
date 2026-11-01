"""Reject Terraform plans that exceed the database delivery scope."""
import json
import sys

ALLOWED = {"aws_db_instance.database", "aws_db_subnet_group.database"}
SAFE_ACTIONS = {("no-op",), ("create",), ("update",)}


def validate(plan):
    if not isinstance(plan, dict) or plan.get("errored") is not False or plan.get("complete") is not True:
        raise ValueError("Terraform plan is incomplete or errored")
    changes = plan.get("resource_changes")
    if not isinstance(changes, list):
        raise ValueError("Terraform plan has no resource change list")
    for item in changes:
        if item.get("mode") != "managed":
            continue
        address = item.get("address")
        change = item.get("change", {})
        actions = tuple(change.get("actions", ()))
        if actions == ("no-op",):
            continue
        if address not in ALLOWED or actions not in SAFE_ACTIONS:
            raise ValueError(f"Disallowed change: {address} {actions}")
        if address == "aws_db_instance.database":
            after = change.get("after") or {}
            required = {"publicly_accessible": False, "deletion_protection": True,
                        "manage_master_user_password": True, "storage_encrypted": True, "multi_az": False,
                        "identifier": "garage-flow-postgres", "engine": "postgres",
                        "instance_class": "db.t4g.micro", "allocated_storage": 20,
                        "backup_retention_period": 1}
            if any(after.get(key) != value for key, value in required.items()):
                raise ValueError("Database plan violates the approved environment limits")
    for item in plan.get("resource_drift", []):
        if item.get("mode") == "managed" and item.get("address") not in ALLOWED:
            raise ValueError(f"Unrelated infrastructure drift: {item.get('address')}")


def main():
    try:
        validate(json.load(sys.stdin))
    except (ValueError, TypeError, json.JSONDecodeError) as exc:
        print(f"Database plan rejected: {exc}", file=sys.stderr)
        return 1
    print("Database plan accepted: only approved non-destructive changes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
