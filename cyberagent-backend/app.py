import os
from flask import Flask, jsonify

app = Flask(__name__)

# Minimal safe backend for CyberAgent Android.
# It provides health/status and a versioned, operator-controlled IOC feed.
# It does not receive APKs, execute uploaded code, or perform destructive actions.
IOC_FEED = {
    "version": 1,
    "indicators": []
}

@app.get("/")
def root():
    return jsonify({"service": "CyberAgent Backend", "version": "0.1.0", "status": "ok"})

@app.get("/health")
def health():
    return jsonify({"status": "healthy"})

@app.get("/api/v1/threat-feed")
def threat_feed():
    return jsonify(IOC_FEED)

if __name__ == "__main__":
    port = int(os.environ.get("PORT", "10000"))
    app.run(host="0.0.0.0", port=port)
