import argparse
import tempfile
import unittest
from datetime import date, datetime, timezone
from pathlib import Path

from collector import (
    CollectorError,
    collect_frankfurter_once,
    collect_vcb_once,
    normalize_frankfurter_rates,
    parse_local_date,
    validate_frankfurter_catalog,
)


class FakeSingleClient:
    def __init__(self, payload):
        self.payload = payload

    def get_json(self, *_args, **_kwargs):
        return self.payload


class FakeFrankfurterClient:
    def __init__(self):
        self.catalog = [
            {"iso_code": "USD", "name": "United States Dollar"},
            {"iso_code": "VND", "name": "Vietnamese Đồng"},
            {"iso_code": "XAU", "name": "Gold"},
        ]
        self.rates = [
            {"date": "2026-08-07", "base": "VND", "quote": "USD", "rate": 0.000038},
            {"date": "2026-08-07", "base": "VND", "quote": "XAU", "rate": 0.00000001},
        ]

    def get_json(self, url, **_kwargs):
        return self.catalog if url.endswith("/currencies") else self.rates


class CollectorTests(unittest.TestCase):
    def test_frankfurter_catalog_excludes_metals_by_default(self):
        catalog = validate_frankfurter_catalog(FakeFrankfurterClient().catalog, False)
        self.assertEqual({"USD", "VND"}, set(catalog))

    def test_frankfurter_rates_include_base_and_are_complete(self):
        rates, validation = normalize_frankfurter_rates(
            FakeFrankfurterClient().rates,
            "VND",
            {"USD", "VND"},
            False,
        )
        self.assertEqual(["USD", "VND"], [row["currency_code"] for row in rates])
        self.assertTrue(validation["is_complete"])
        self.assertEqual(2, validation["actual_currency_count"])

    def test_frankfurter_validation_detects_missing_currency(self):
        _, validation = normalize_frankfurter_rates(
            FakeFrankfurterClient().rates,
            "VND",
            {"EUR", "USD", "VND"},
            False,
        )
        self.assertFalse(validation["is_complete"])
        self.assertEqual(["EUR"], validation["missing_currency_codes"])

    def test_frankfurter_marks_last_known_currency(self):
        rates, validation = normalize_frankfurter_rates(
            FakeFrankfurterClient().rates,
            "VND",
            {"USD", "VND"},
            False,
            {"USD"},
        )
        usd = next(row for row in rates if row["currency_code"] == "USD")
        self.assertTrue(usd["is_last_known"])
        self.assertEqual(["USD"], validation["last_known_currency_codes"])

    def test_frankfurter_snapshot_is_saved_once_when_unchanged(self):
        with tempfile.TemporaryDirectory() as directory:
            args = argparse.Namespace(
                date="2026-08-07",
                timezone="Asia/Ho_Chi_Minh",
                output_dir=directory,
                timeout=30,
                retries=1,
                base="VND",
                include_metals=False,
                allow_incomplete=False,
            )
            now = datetime(2026, 8, 7, 8, tzinfo=timezone.utc)
            client = FakeFrankfurterClient()
            first = collect_frankfurter_once(args, client, now)
            second = collect_frankfurter_once(args, client, now)
            self.assertEqual("saved", first["status"])
            self.assertEqual("unchanged", second["status"])
            lines = Path(first["output"]).read_text(encoding="utf-8").splitlines()
            self.assertEqual(1, len(lines))

    def test_vcb_snapshot_is_saved_once_when_unchanged(self):
        payload = {
            "Count": 1,
            "Date": "2026-08-07T00:00:00",
            "UpdatedDate": "2026-08-07T14:46:38+07:00",
            "Data": [{
                "currencyName": "US DOLLAR",
                "currencyCode": "USD",
                "cash": "26000.00",
                "transfer": "26030.00",
                "sell": "26410.00",
                "icon": "/flag.svg",
            }],
        }
        with tempfile.TemporaryDirectory() as directory:
            args = argparse.Namespace(
                date="2026-08-07",
                timezone="Asia/Ho_Chi_Minh",
                output_dir=directory,
                timeout=30,
                retries=1,
            )
            first = collect_vcb_once(args, FakeSingleClient(payload))
            second = collect_vcb_once(args, FakeSingleClient(payload))
            self.assertEqual("saved", first["status"])
            self.assertEqual("unchanged", second["status"])
            lines = Path(first["output"]).read_text(encoding="utf-8").splitlines()
            self.assertEqual(1, len(lines))

    def test_future_date_is_rejected(self):
        args = argparse.Namespace(
            date="2026-08-08",
            timezone="Asia/Ho_Chi_Minh",
            output_dir="unused",
            timeout=30,
            retries=1,
            base="VND",
            include_metals=False,
            allow_incomplete=False,
        )
        with self.assertRaises(CollectorError):
            collect_frankfurter_once(
                args,
                FakeFrankfurterClient(),
                datetime(2026, 8, 7, tzinfo=timezone.utc),
            )

    def test_yesterday_uses_vietnam_local_date(self):
        value = parse_local_date(
            "yesterday",
            "Asia/Ho_Chi_Minh",
            now=datetime(2026, 8, 7, 17, 30, tzinfo=timezone.utc),
        )
        self.assertEqual(date(2026, 8, 7), value)


if __name__ == "__main__":
    unittest.main()
