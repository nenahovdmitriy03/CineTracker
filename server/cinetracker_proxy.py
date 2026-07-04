#!/usr/bin/env python3
import base64
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


TMDB_API_ROOT = "https://api.themoviedb.org/3"
TMDB_IMAGE_ROOT = "https://image.tmdb.org/t/p"
POISKKINO_API_ROOT = "https://api.poiskkino.dev"
TMDB_TOKEN = os.environ.get("TMDB_READ_ACCESS_TOKEN", "").strip()
POISKKINO_API_KEY = os.environ.get("POISKKINO_API_KEY", "").strip()
PORT = int(os.environ.get("PORT", "8080"))
CACHE_DIR = os.environ.get("CACHE_DIR", os.path.join(os.path.dirname(__file__), "cache"))


class CineTrackerProxy(BaseHTTPRequestHandler):
    server_version = "CineTrackerProxy/1.1"

    def do_GET(self):
        parsed = urllib.parse.urlsplit(self.path)

        if parsed.path == "/health":
            self.send_json(
                200,
                {
                    "ok": True,
                    "service": "cinetracker-proxy",
                    "tmdb": bool(TMDB_TOKEN),
                    "poiskkino": bool(POISKKINO_API_KEY),
                },
            )
            return

        if parsed.path.startswith("/poiskkino/"):
            if not POISKKINO_API_KEY:
                self.send_json(500, {"error": "POISKKINO_API_KEY is not configured"})
                return
            upstream_path = parsed.path[len("/poiskkino") :]
            upstream = POISKKINO_API_ROOT + upstream_path
            if parsed.query:
                upstream += "?" + parsed.query
            self.forward(
                upstream,
                accept="application/json",
                headers={"X-API-KEY": POISKKINO_API_KEY},
                cache_ttl=poiskkino_cache_ttl(upstream_path),
            )
            return

        if parsed.path.startswith("/3/"):
            if not TMDB_TOKEN:
                self.send_json(500, {"error": "TMDB_READ_ACCESS_TOKEN is not configured"})
                return
            upstream = TMDB_API_ROOT + parsed.path[2:]
            if parsed.query:
                upstream += "?" + parsed.query
            self.forward(
                upstream,
                accept="application/json",
                headers={"Authorization": "Bearer " + TMDB_TOKEN},
                cache_ttl=6 * 60 * 60,
            )
            return

        if is_tmdb_api_path(parsed.path):
            if not TMDB_TOKEN:
                self.send_json(500, {"error": "TMDB_READ_ACCESS_TOKEN is not configured"})
                return
            upstream = TMDB_API_ROOT + parsed.path
            if parsed.query:
                upstream += "?" + parsed.query
            self.forward(
                upstream,
                accept="application/json",
                headers={"Authorization": "Bearer " + TMDB_TOKEN},
                cache_ttl=6 * 60 * 60,
            )
            return

        if parsed.path.startswith("/image/t/p/"):
            if not TMDB_TOKEN:
                self.send_json(500, {"error": "TMDB_READ_ACCESS_TOKEN is not configured"})
                return
            upstream = TMDB_IMAGE_ROOT + parsed.path[len("/image/t/p") :]
            if parsed.query:
                upstream += "?" + parsed.query
            self.forward(
                upstream,
                accept="image/*",
                headers={"Authorization": "Bearer " + TMDB_TOKEN},
                cache_ttl=0,
            )
            return

        self.send_json(404, {"error": "Not found"})

    def forward(self, upstream_url, accept, headers, cache_ttl=0):
        cached = read_cache(upstream_url, cache_ttl)
        if cached:
            self.send_body(
                cached["status"],
                base64.b64decode(cached["body"]),
                cached.get("content_type", "application/json; charset=utf-8"),
                "public, max-age=%s" % cache_ttl,
                {"X-Cache": "HIT"},
            )
            return

        request_headers = {
            "Accept": accept,
            "User-Agent": "CineTrackerProxy/1.1",
        }
        request_headers.update(headers)
        request = urllib.request.Request(upstream_url, headers=request_headers, method="GET")

        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                body = response.read()
                content_type = response.headers.get("Content-Type", "application/octet-stream")
                cache_control = response.headers.get("Cache-Control", "public, max-age=%s" % cache_ttl)

                if cache_ttl > 0 and response.status == 200 and "json" in content_type:
                    write_cache(upstream_url, response.status, content_type, body)

                self.send_body(
                    response.status,
                    body,
                    content_type,
                    cache_control,
                    {"X-Cache": "MISS"},
                )
        except urllib.error.HTTPError as error:
            body = error.read()
            content_type = error.headers.get("Content-Type", "application/json")
            self.send_body(error.code, body, content_type, "no-store", {})
        except Exception as error:
            stale = read_cache(upstream_url, 365 * 24 * 60 * 60, allow_stale=True)
            if stale:
                self.send_body(
                    stale["status"],
                    base64.b64decode(stale["body"]),
                    stale.get("content_type", "application/json; charset=utf-8"),
                    "public, max-age=60",
                    {"X-Cache": "STALE"},
                )
                return
            self.send_json(502, {"error": "Upstream request failed", "detail": str(error)})

    def send_body(self, status, body, content_type, cache_control, extra_headers):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Cache-Control", cache_control)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("X-Proxy-By", "CineTracker")
        for key, value in extra_headers.items():
            self.send_header(key, value)
        self.end_headers()
        self.wfile.write(body)

    def send_json(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_body(status, body, "application/json; charset=utf-8", "no-store", {})

    def log_message(self, fmt, *args):
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))


def poiskkino_cache_ttl(path):
    if path.endswith("/token"):
        return 60
    if "/movie/search" in path:
        return 6 * 60 * 60
    if "/movie/" in path:
        return 7 * 24 * 60 * 60
    if path.endswith("/movie"):
        return 12 * 60 * 60
    return 60 * 60


def is_tmdb_api_path(path):
    return path.startswith(
        (
            "/trending/",
            "/movie/",
            "/tv/",
            "/discover/",
            "/search/",
            "/collection/",
        )
    )


def cache_path(url):
    digest = hashlib.sha256(url.encode("utf-8")).hexdigest()
    return os.path.join(CACHE_DIR, digest + ".json")


def read_cache(url, ttl, allow_stale=False):
    path = cache_path(url)
    if not os.path.exists(path):
        return None
    try:
        with open(path, "r", encoding="utf-8") as file:
            payload = json.load(file)
        age = time.time() - payload.get("created_at", 0)
        if allow_stale or age <= ttl:
            return payload
    except Exception:
        return None
    return None


def write_cache(url, status, content_type, body):
    os.makedirs(CACHE_DIR, exist_ok=True)
    payload = {
        "created_at": time.time(),
        "status": status,
        "content_type": content_type,
        "body": base64.b64encode(body).decode("ascii"),
    }
    tmp_path = cache_path(url) + ".tmp"
    with open(tmp_path, "w", encoding="utf-8") as file:
        json.dump(payload, file)
    os.replace(tmp_path, cache_path(url))


def main():
    os.makedirs(CACHE_DIR, exist_ok=True)
    server = ThreadingHTTPServer(("0.0.0.0", PORT), CineTrackerProxy)
    print("CineTracker proxy listening on port %s" % PORT, flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
