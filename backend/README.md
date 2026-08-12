# Pythagoros Backend

Ktor service for premium AI solving. The Android app can keep local OCR, parser, classifier, and CAS offline; this backend is the paid fallback for tasks the local solver cannot finish.

## Endpoints

- `GET /health` - health check.
- `POST /v1/solve/ai` - solves a math problem with Gemini.

Request:

```json
{
  "expression": "integrate x*ln(x^2+1) dx",
  "problemType": "integral",
  "localSteps": [
    {
      "title": "Substitution",
      "formula": "t = x^2 + 1",
      "explanation": "Local solver reduced the integral."
    }
  ],
  "locale": "ru",
  "requireGraph": true
}
```

Response:

```json
{
  "expression": "integrate x*ln(x^2+1) dx",
  "problemType": "integral",
  "answer": "1/2*((x^2+1)ln(x^2+1)-x^2)+C",
  "steps": [],
  "graph": null,
  "model": "gemini-2.5-pro",
  "source": "gemini"
}
```

## Run Locally

```bash
export GEMINI_API_KEY="your-key"
export PYTHAGOROS_API_TOKEN="long-random-token"
export AI_MODEL="gemini-3.6-flash"
PORT=8080 ./gradlew :backend:run
```

Optional env:

- `HOST` - default `0.0.0.0`.
- `PORT` - default `8080`.
- `PYTHAGOROS_API_TOKEN` - required for `POST /v1/solve/ai` as `Authorization: Bearer ...`.
- `AITUNNEL_API_KEY` or `GEMINI_API_KEY` - AI provider key. `sk-aitunnel-*` keys automatically use AITUNNEL.
- `AI_PROVIDER` - optional: `aitunnel` or `gemini`.
- `AI_MODEL` - default `gemini-3.6-flash`.
- `GEMINI_BASE_URL` - default `https://generativelanguage.googleapis.com/v1beta`.
- `OPENAI_COMPATIBLE_BASE_URL` - default `https://api.aitunnel.ru/v1`.
