import unittest
from check_database_plan import validate


def change(address, actions, after=None):
    return {"mode": "managed", "address": address, "change": {"actions": actions, "after": after}}


class DatabasePlanTest(unittest.TestCase):
    def setUp(self):
        self.plan = {"errored": False, "complete": True, "resource_changes": []}
        self.database = {"publicly_accessible": False, "deletion_protection": True,
                         "manage_master_user_password": True, "storage_encrypted": True, "multi_az": False,
                         "identifier": "garage-flow-postgres", "engine": "postgres",
                         "instance_class": "db.t4g.micro", "allocated_storage": 20,
                         "backup_retention_period": 1}

    def test_noop_and_approved_creation(self):
        self.plan["resource_changes"] = [change("module.eks.cluster", ["no-op"]),
                                          change("aws_db_instance.database", ["create"], self.database),
                                          change("aws_db_subnet_group.database", ["update"], {})]
        validate(self.plan)

    def test_blocks_delete_and_replacement(self):
        for actions in (["delete"], ["delete", "create"], ["create", "delete"]):
            with self.subTest(actions=actions), self.assertRaises(ValueError):
                self.plan["resource_changes"] = [change("aws_db_instance.database", actions, self.database)]
                validate(self.plan)

    def test_blocks_unrelated_change_and_drift(self):
        self.plan["resource_changes"] = [change("module.eks.cluster", ["update"], {})]
        with self.assertRaises(ValueError): validate(self.plan)
        self.plan["resource_changes"] = []
        self.plan["resource_drift"] = [change("module.eks.cluster", ["update"], {})]
        with self.assertRaises(ValueError): validate(self.plan)

    def test_blocks_public_or_expensive_database(self):
        for key, value in (("publicly_accessible", True), ("deletion_protection", False),
                           ("instance_class", "db.t3.large"), ("multi_az", True),
                           ("manage_master_user_password", False), ("backup_retention_period", 0)):
            with self.subTest(key=key), self.assertRaises(ValueError):
                database = dict(self.database, **{key: value})
                self.plan["resource_changes"] = [change("aws_db_instance.database", ["update"], database)]
                validate(self.plan)

    def test_rejects_incomplete_or_missing_plan(self):
        for plan in ({}, {"errored": True, "complete": True, "resource_changes": []},
                     {"errored": False, "complete": False, "resource_changes": []}):
            with self.subTest(plan=plan), self.assertRaises(ValueError): validate(plan)


if __name__ == "__main__":
    unittest.main()
