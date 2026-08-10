#!/usr/bin/env python3
"""Collect no-cost global reference rates and Vietcombank counter rates.

This module intentionally uses only the Python standard library so it can run
next to the Android project without introducing a backend dependency.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import time
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


FRANKFURTER_BASE_URL = "https://api.frankfurter.dev/v2"
VCB_EXCHANGE_RATES_URL = "https://www.vietcombank.com.vn/api/exchangerates"
DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh"
DEFAULT_OUTPUT_DIR = Path("data") / "exchange-rates"
USER_AGENT = "phs-iptv-exchange-rate-collector/1.0"
METAL_CODES = {"XAG", "XAU", "XPD", "XPT"}


class CollectorError(RuntimeError):
    """An actionable collector error that is safe to display."""


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def iso_z(value: datetime) -> str:
    return value.astimezone(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


def timezone_for(name: str) -> timezone:
    # Vietnam has used UTC+07 without DST since 1975. Keeping this common path
    # dependency-free also avoids the missing-IANA-tzdata issue on Windows.
    if name in {DEFAULT_TIMEZONE, "Asia/Saigon", "+07:00"}:
        return timezone(timedelta(hours=7), name=DEFAULT_TIMEZONE)
    if name in {"UTC", "Etc/UTC", "Z"}:
        return timezone.utc
    try:
        from zoneinfo import ZoneInfo

        return ZoneInfo(name)  # type: ignore[return-value]
    except Exception as exc:
        raise CollectorError(
            f"Unknown timezone '{name}'. Install Python tzdata or use {DEFAULT_TIMEZONE}/UTC."
        ) from exc


def parse_local_date(raw: str, timezone_name: str, now: datetime | None = None) -> date:
    tz = timezone_for(timezone_name)
    if raw.lower() == "today":
        return (now or utc_now()).astimezone(tz).date()
    if raw.lower() == "yesterday":
        return (now or utc_now()).astimezone(tz).date() - timedelta(days=1)
    try:
        return date.fromisoformat(raw)
    except ValueError as exc:
        raise CollectorError("Date must be YYYY-MM-DD, 'today', or 'yesterday'.") from exc


class HttpClient:
    def __init__(self, timeout_seconds: int = 30, retries: int = 4) -> None:
        self.timeout_seconds = timeout_seconds
        self.retries = retries

    def get_json(
        self,
        url: str,
        params: dict[str, str] | None = None,
        headers: dict[str, str] | None = None,
    ) -> Any:
        query = f"?{urlencode(params)}" if params else ""
        request_headers = {
            "Accept": "application/json",
            "User-Agent": USER_AGENT,
            **(headers or {}),
        }
        request = Request(f"{url}{query}", headers=request_headers, method="GET")

        for attempt in range(self.retries):
            try:
                with urlopen(request, timeout=self.timeout_seconds) as response:
                    charset = response.headers.get_content_charset() or "utf-8"
                    payload = json.loads(response.read().decode(charset))
                    if not isinstance(payload, (dict, list)):
                        raise CollectorError(f"Unexpected JSON shape returned by {url}.")
                    return payload
            except HTTPError as exc:
                retryable = exc.code == 429 or 500 <= exc.code < 600
                if not retryable or attempt == self.retries - 1:
                    if exc.code in {401, 403}:
                        raise CollectorError(f"Access denied by {url}.") from exc
                    raise CollectorError(f"HTTP {exc.code} returned by {url}.") from exc
                retry_after = exc.headers.get("Retry-After")
                delay = float(retry_after) if retry_after and retry_after.isdigit() else 2**attempt
                time.sleep(min(delay, 30))
            except (URLError, TimeoutError) as exc:
                if attempt == self.retries - 1:
                    raise CollectorError(f"Unable to reach {url}: {exc.reason if isinstance(exc, URLError) else exc}") from exc
                time.sleep(min(2**attempt, 30))
            except json.JSONDecodeError as exc:
                raise CollectorError(f"Invalid JSON returned by {url}.") from exc

        raise CollectorError(f"Unable to reach {url}.")


def atomic_write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f"{path.name}.tmp")
    with temporary.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, ensure_ascii=False, separators=(",", ":"))
        handle.write("\n")
    os.replace(temporary, path)


def validate_frankfurter_catalog(payload: Any, include_metals: bool) -> dict[str, dict[str, Any]]:
    if not isinstance(payload, list) or not payload:
        raise CollectorError("Frankfurter returned an empty currency catalog.")
    catalog: dict[str, dict[str, Any]] = {}
    for row in payload:
        if not isinstance(row, dict) or not isinstance(row.get("iso_code"), str):
            raise CollectorError("Frankfurter returned a malformed currency catalog row.")
        code = row["iso_code"].upper()
        if not include_metals and code in METAL_CODES:
            continue
        if code in catalog:
            raise CollectorError(f"Frankfurter returned duplicate currency code {code}.")
        catalog[code] = row
    return catalog


def normalize_frankfurter_rates(
    payload: Any,
    base: str,
    catalog_codes: set[str],
    include_metals: bool,
    last_known_codes: set[str] | None = None,
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    if not isinstance(payload, list) or not payload:
        raise CollectorError("Frankfurter returned an empty rates list.")
    normalized: dict[str, dict[str, Any]] = {}
    provider_dates: list[str] = []
    for row in payload:
        if not isinstance(row, dict):
            raise CollectorError("Frankfurter returned a malformed rate row.")
        quote = row.get("quote")
        if not isinstance(quote, str) or not isinstance(row.get("date"), str):
            raise CollectorError("Frankfurter rate row is missing quote or date.")
        quote = quote.upper()
        if not include_metals and quote in METAL_CODES:
            continue
        try:
            value = float(row["rate"])
        except (KeyError, TypeError, ValueError) as exc:
            raise CollectorError(f"Frankfurter returned an invalid rate for {quote}.") from exc
        if value <= 0 or row.get("base") != base:
            raise CollectorError(f"Frankfurter returned an invalid base/rate for {quote}.")
        normalized[quote] = {
            "currency_code": quote,
            "value": value,
            "rate_date": row["date"],
            "is_last_known": quote in (last_known_codes or set()),
        }
        provider_dates.append(row["date"])

    normalized[base] = {
        "currency_code": base,
        "value": 1.0,
        "rate_date": None,
        "is_last_known": False,
    }
    actual_codes = set(normalized)
    missing_codes = sorted(catalog_codes - actual_codes)
    unexpected_codes = sorted(actual_codes - catalog_codes)
    validation = {
        "catalog_currency_count": len(catalog_codes),
        "actual_currency_count": len(actual_codes),
        "missing_currency_codes": missing_codes,
        "unexpected_currency_codes": unexpected_codes,
        "provider_rate_date_min": min(provider_dates) if provider_dates else None,
        "provider_rate_date_max": max(provider_dates) if provider_dates else None,
        "last_known_currency_codes": sorted(last_known_codes or set()),
        "is_complete": not missing_codes and not unexpected_codes,
    }
    return [normalized[code] for code in sorted(normalized)], validation


def collect_frankfurter_once(
    args: argparse.Namespace,
    client: HttpClient | None = None,
    now: datetime | None = None,
) -> dict[str, Any]:
    local_date = parse_local_date(args.date, args.timezone, now)
    current_local_date = (now or utc_now()).astimezone(timezone_for(args.timezone)).date()
    if local_date > current_local_date:
        raise CollectorError(f"Cannot collect future date {local_date.isoformat()}.")
    http = client or HttpClient(args.timeout, args.retries)
    catalog_payload = http.get_json(f"{FRANKFURTER_BASE_URL}/currencies")
    catalog = validate_frankfurter_catalog(catalog_payload, args.include_metals)
    if args.base not in catalog:
        raise CollectorError(f"Base currency {args.base} is not in the Frankfurter catalog.")
    rate_params = {"base": args.base}
    if local_date != current_local_date:
        rate_params["date"] = local_date.isoformat()
    rates_payload = http.get_json(f"{FRANKFURTER_BASE_URL}/rates", params=rate_params)
    rates, validation = normalize_frankfurter_rates(
        rates_payload,
        args.base,
        set(catalog),
        args.include_metals,
    )
    request_count = 2
    last_known_codes: set[str] = set()
    if local_date == current_local_date and validation["missing_currency_codes"]:
        if not isinstance(rates_payload, list):
            raise CollectorError("Frankfurter returned an invalid rates payload.")
        missing_by_date: dict[str, set[str]] = {}
        for code in validation["missing_currency_codes"]:
            end_date = catalog[code].get("end_date")
            if isinstance(end_date, str) and end_date <= local_date.isoformat():
                missing_by_date.setdefault(end_date, set()).add(code)

        augmented_payload = list(rates_payload)
        for last_date, missing_codes in sorted(missing_by_date.items()):
            historical_payload = http.get_json(
                f"{FRANKFURTER_BASE_URL}/rates",
                params={"base": args.base, "date": last_date},
            )
            request_count += 1
            if not isinstance(historical_payload, list):
                continue
            for row in historical_payload:
                if isinstance(row, dict) and row.get("quote") in missing_codes:
                    augmented_payload.append(row)
                    last_known_codes.add(row["quote"])

        rates, validation = normalize_frankfurter_rates(
            augmented_payload,
            args.base,
            set(catalog),
            args.include_metals,
            last_known_codes,
        )
    if not validation["is_complete"] and not args.allow_incomplete:
        raise CollectorError(
            "Frankfurter result failed completeness validation: "
            f"{validation['actual_currency_count']}/{validation['catalog_currency_count']} currencies. "
            "Use --allow-incomplete only when you intentionally want a partial file."
        )

    digest = snapshot_hash(rates)
    output_root = Path(args.output_dir)
    output_path = output_root / "global" / local_date.isoformat() / f"frankfurter-{args.base}.jsonl"
    catalog_path = output_root / "global" / "currencies-frankfurter.json"
    atomic_write_json(
        catalog_path,
        {
            "schema_version": 1,
            "source": "frankfurter.dev/v2/currencies",
            "fetched_at": iso_z(utc_now()),
            "count": len(catalog),
            "include_metals": args.include_metals,
            "data": [catalog[code] for code in sorted(catalog)],
        },
    )
    if digest in existing_snapshot_hashes(output_path):
        return {
            "output": str(output_path),
            "catalog": str(catalog_path),
            "status": "unchanged",
            "data_sha256": digest,
            "request_count": request_count,
            **validation,
        }

    record = {
        "schema_version": 1,
        "source": "frankfurter.dev/v2/rates",
        "fetched_at": iso_z(utc_now()),
        "local_date": local_date.isoformat(),
        "timezone": args.timezone,
        "base_currency": args.base,
        "include_metals": args.include_metals,
        "data_sha256": digest,
        "request_count": request_count,
        "validation": validation,
        "data": rates,
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("a", encoding="utf-8", newline="\n") as handle:
        handle.write(json.dumps(record, ensure_ascii=False, separators=(",", ":")))
        handle.write("\n")
        handle.flush()
        os.fsync(handle.fileno())
    return {
        "output": str(output_path),
        "catalog": str(catalog_path),
        "status": "saved",
        "data_sha256": digest,
        "request_count": request_count,
        **validation,
    }


def validate_vcb_response(payload: dict[str, Any]) -> list[dict[str, Any]]:
    data = payload.get("Data")
    if not isinstance(data, list) or not data:
        raise CollectorError("Vietcombank response is missing the Data array.")
    if payload.get("Count") != len(data):
        raise CollectorError("Vietcombank Count does not match the number of rows.")

    codes: set[str] = set()
    for row in data:
        if not isinstance(row, dict) or not isinstance(row.get("currencyCode"), str):
            raise CollectorError("Vietcombank returned a malformed currency row.")
        code = row["currencyCode"]
        if code in codes:
            raise CollectorError(f"Vietcombank returned duplicate currency code {code}.")
        codes.add(code)
        for field in ("cash", "transfer", "sell"):
            try:
                float(row[field])
            except (KeyError, TypeError, ValueError) as exc:
                raise CollectorError(f"Invalid Vietcombank {field} value for {code}.") from exc
    return data


def snapshot_hash(data: list[dict[str, Any]]) -> str:
    encoded = json.dumps(data, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def existing_snapshot_hashes(path: Path) -> set[str]:
    if not path.exists():
        return set()
    hashes: set[str] = set()
    with path.open("r", encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, start=1):
            if not line.strip():
                continue
            try:
                record = json.loads(line)
            except json.JSONDecodeError as exc:
                raise CollectorError(f"Invalid JSONL at {path}:{line_number}.") from exc
            value = record.get("data_sha256")
            if isinstance(value, str):
                hashes.add(value)
    return hashes


def collect_vcb_once(
    args: argparse.Namespace,
    client: HttpClient | None = None,
    now: datetime | None = None,
) -> dict[str, Any]:
    local_date = parse_local_date(args.date, args.timezone, now)
    http = client or HttpClient(args.timeout, args.retries)
    payload = http.get_json(
        VCB_EXCHANGE_RATES_URL,
        params={"date": local_date.isoformat()},
        headers={"Accept-Language": "vi-VN,vi;q=0.9"},
    )
    data = validate_vcb_response(payload)
    digest = snapshot_hash(data)
    path = Path(args.output_dir) / "vietcombank" / f"{local_date.isoformat()}.jsonl"
    if digest in existing_snapshot_hashes(path):
        return {
            "output": str(path),
            "status": "unchanged",
            "provider_updated_at": payload.get("UpdatedDate"),
            "currency_count": len(data),
            "data_sha256": digest,
        }

    record = {
        "schema_version": 1,
        "source": "vietcombank.com.vn/api/exchangerates",
        "requested_date": local_date.isoformat(),
        "fetched_at": iso_z(utc_now()),
        "provider_date": payload.get("Date"),
        "provider_updated_at": payload.get("UpdatedDate"),
        "currency_count": len(data),
        "data_sha256": digest,
        "data": data,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8", newline="\n") as handle:
        handle.write(json.dumps(record, ensure_ascii=False, separators=(",", ":")))
        handle.write("\n")
        handle.flush()
        os.fsync(handle.fileno())
    return {
        "output": str(path),
        "status": "saved",
        "provider_updated_at": payload.get("UpdatedDate"),
        "currency_count": len(data),
        "data_sha256": digest,
    }


def watch_vcb(args: argparse.Namespace) -> None:
    if args.interval < 300:
        raise CollectorError("Vietcombank interval must be at least 300 seconds.")
    print(json.dumps({"status": "started", "interval_seconds": args.interval}), flush=True)
    while True:
        try:
            result = collect_vcb_once(args)
            print(json.dumps(result, ensure_ascii=False), flush=True)
        except CollectorError as exc:
            print(json.dumps({"status": "error", "message": str(exc)}), file=sys.stderr, flush=True)
        try:
            time.sleep(args.interval)
        except KeyboardInterrupt:
            print(json.dumps({"status": "stopped"}), flush=True)
            return


def watch_frankfurter(args: argparse.Namespace) -> None:
    if args.interval < 900:
        raise CollectorError("Frankfurter interval must be at least 900 seconds.")
    print(
        json.dumps(
            {"status": "started", "source": "frankfurter", "interval_seconds": args.interval}
        ),
        flush=True,
    )
    while True:
        try:
            result = collect_frankfurter_once(args)
            print(json.dumps(result, ensure_ascii=False), flush=True)
        except CollectorError as exc:
            print(json.dumps({"status": "error", "message": str(exc)}), file=sys.stderr, flush=True)
        try:
            time.sleep(args.interval)
        except KeyboardInterrupt:
            print(json.dumps({"status": "stopped"}), flush=True)
            return


def jsonl_summary(path: Path) -> tuple[int, dict[str, Any] | None]:
    count = 0
    latest: dict[str, Any] | None = None
    if not path.exists():
        return count, latest
    with path.open("r", encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, start=1):
            if not line.strip():
                continue
            try:
                record = json.loads(line)
            except json.JSONDecodeError as exc:
                raise CollectorError(f"Invalid JSONL at {path}:{line_number}.") from exc
            if not isinstance(record, dict):
                raise CollectorError(f"Unexpected JSONL record at {path}:{line_number}.")
            count += 1
            latest = record
    return count, latest


def status_for_day(args: argparse.Namespace) -> dict[str, Any]:
    local_date = parse_local_date(args.date, args.timezone)
    output_root = Path(args.output_dir)
    global_path = output_root / "global" / local_date.isoformat() / f"frankfurter-{args.base}.jsonl"
    vcb_path = output_root / "vietcombank" / f"{local_date.isoformat()}.jsonl"

    global_snapshots, latest_global = jsonl_summary(global_path)
    global_status: dict[str, Any] = {
        "exists": global_path.exists(),
        "path": str(global_path),
        "unique_snapshots": global_snapshots,
        "latest_fetched_at": latest_global.get("fetched_at") if latest_global else None,
        "latest_validation": latest_global.get("validation") if latest_global else None,
    }

    snapshots, _ = jsonl_summary(vcb_path)
    updates: set[str] = set()
    if vcb_path.exists():
        with vcb_path.open("r", encoding="utf-8") as handle:
            for line in handle:
                if line.strip():
                    record = json.loads(line)
                    if record.get("provider_updated_at"):
                        updates.add(record["provider_updated_at"])
    return {
        "local_date": local_date.isoformat(),
        "timezone": args.timezone,
        "global": global_status,
        "vietcombank": {
            "exists": vcb_path.exists(),
            "path": str(vcb_path),
            "unique_snapshots": snapshots,
            "provider_updates": len(updates),
        },
    }


def add_common_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--date", default="today", help="Local date: YYYY-MM-DD, today, or yesterday")
    parser.add_argument("--timezone", default=DEFAULT_TIMEZONE)
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--timeout", type=int, default=30)
    parser.add_argument("--retries", type=int, default=4)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    global_parser = subparsers.add_parser(
        "global-once", help="Save one free Frankfurter global-rate snapshot"
    )
    add_common_arguments(global_parser)
    global_parser.add_argument("--base", default="VND")
    global_parser.add_argument("--include-metals", action="store_true")
    global_parser.add_argument("--allow-incomplete", action="store_true")

    global_watch_parser = subparsers.add_parser(
        "global-watch", help="Poll free Frankfurter rates and save changes"
    )
    add_common_arguments(global_watch_parser)
    global_watch_parser.add_argument("--base", default="VND")
    global_watch_parser.add_argument("--include-metals", action="store_true")
    global_watch_parser.add_argument("--allow-incomplete", action="store_true")
    global_watch_parser.add_argument("--interval", type=int, default=3600)

    vcb_once_parser = subparsers.add_parser("vcb-once", help="Save one Vietcombank snapshot")
    add_common_arguments(vcb_once_parser)

    vcb_watch_parser = subparsers.add_parser("vcb-watch", help="Continuously save changed Vietcombank snapshots")
    add_common_arguments(vcb_watch_parser)
    vcb_watch_parser.add_argument("--interval", type=int, default=310)

    status_parser = subparsers.add_parser("status", help="Check saved coverage for a day")
    add_common_arguments(status_parser)
    status_parser.add_argument("--base", default="VND")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        if args.command == "global-once":
            result = collect_frankfurter_once(args)
        elif args.command == "global-watch":
            watch_frankfurter(args)
            return 0
        elif args.command == "vcb-once":
            result = collect_vcb_once(args)
        elif args.command == "vcb-watch":
            watch_vcb(args)
            return 0
        elif args.command == "status":
            result = status_for_day(args)
        else:
            raise CollectorError(f"Unknown command {args.command}.")
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except CollectorError as exc:
        print(json.dumps({"status": "error", "message": str(exc)}, ensure_ascii=False), file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print(json.dumps({"status": "stopped"}), file=sys.stderr)
        return 130


if __name__ == "__main__":
    raise SystemExit(main())
