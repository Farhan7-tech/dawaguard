# DawaGuard

**A WhatsApp medicine guardian for Indian families.** Send a photo of a medicine strip; DawaGuard reads the batch number and checks it against India's official lists of drug batches that failed quality tests or are suspected fakes.

> Built for the [Nebius x NVIDIA Global AI Hackathon](https://nebiusglobalaihackathon.devpost.com/) with NVIDIA Nemotron models on Nebius Token Factory. Work in progress.

## The problem

Every month India's drug regulator, CDSCO, publishes the medicine batches that failed lab tests ("Not of Standard Quality") or were found spurious. Between January 2025 and September 2026 that was **3,788 failed batches and 49 suspected fakes**, including **355 batches of paracetamol**. More than half of the failed batches don't expire until 2027 or later, so they may still be on shelves.

The lists are public, but nobody checks them before taking a tablet.

## How it works

| Step | What happens | Powered by |
|---|---|---|
| 1. Read | Extract brand, ingredients, batch number, manufacturer and expiry from a strip photo | NVIDIA Nemotron 3.5 Super VL on Nebius Token Factory |
| 2. Match | Look up the batch in CDSCO's flagged-batch data, tolerant of OCR mistakes | Local database synced from CDSCO |
| 3. Verify | Search for alerts and recalls newer than the monthly data | Tavily |
| 4. Explain | Plain-language verdict in English or Hindi, with the official source and safe next steps, plus a short message to forward to family | NVIDIA Nemotron 3 Ultra (explanation, Hindi messages), Nemotron 3.5 Lightning (English messages) |
| 5. Watch | Keep each family's checked medicines in a cabinet and re-check them whenever CDSCO publishes a new list | Code, alerts sent over WhatsApp |

**The verdict is decided by code, not by a language model.** DawaGuard matches the batch number (tolerating common misreads such as O/0 and S/5), checks that the product on the strip is the product CDSCO flagged (batch codes repeat across companies), and parses the expiry date. Nemotron only reads the photo and explains the decided result, and its prompts forbid changing the verdict or adding facts. Every answer cites the official CDSCO record first.

The web page streams each step as it happens and shows which model did what, with timings and token counts. Three synthetic sample strips (failed batch, clear batch, expired) let anyone try it without a real strip.

## On WhatsApp

Families already live on WhatsApp, so DawaGuard works there too (Meta WhatsApp Cloud API):

- Send a photo of a strip, or type a batch number such as `TG25-1814A Pansid-40`. The reply is the same check as the web page. A photo caption ("Papa's BP tablet") becomes the medicine's label.
- Every checked medicine with a batch number is saved to the family's **medicine cabinet**. Reply `cabinet` to see it with a fresh verdict for each, `remove 2` to take one out, `हिंदी` or `English` to switch language, and `stop` to delete everything saved.
- When the daily CDSCO sync finds new flagged batches, DawaGuard re-checks every cabinet against just those rows and messages the family about any match, once per finding. Alerts are worded by code, not a model, because they must be exact. More than 24 hours after the family last wrote, WhatsApp only allows approved templates, so the alert goes as the `dawaguard_batch_alert` template and the full details follow when they reply.
- Webhooks are accepted only with a valid Meta signature (`X-Hub-Signature-256`), and repeated deliveries are answered once.

CDSCO publishes alert lists about once a month. To show the alert path in a demo, `POST /api/demo/cdsco-alert` with header `X-Demo-Token` (set `DEMO_TOKEN`) and `{"batchNo": "..."}` simulates a new list containing a saved batch. The alert says it is simulated, and the simulated row is never stored.

## Run locally

Requires Java 21.

```bash
cp .env.example .env   # then add NEBIUS_API_KEY and TAVILY_API_KEY
./mvnw spring-boot:run
```

Then open `http://localhost:8080` and try a sample strip, or use the API:

- `POST /api/check` (multipart `image`, optional `lang=en|hi`): photo check, streamed as newline-delimited JSON
- `POST /api/check/typed` (JSON `{"batchNo": "...", "brandName": "...", "lang": "en"}`): check a typed batch number
- `http://localhost:8080/api/status`: loaded CDSCO data and freshness
- `http://localhost:8080/api/batches/{batchNo}`: look up a batch number

With `DIAGNOSTICS_ENABLED=true` in `.env` (local only, these spend API credits):

- `http://localhost:8080/api/diagnostics/models`: one test call to each Nemotron model
- `http://localhost:8080/api/diagnostics/search`: one Tavily search

## Deploy

`deploy/` runs DawaGuard behind Caddy, which obtains the HTTPS certificate WhatsApp webhooks require. On a Linux server with Docker:

```bash
git clone https://github.com/Farhan7-tech/dawaguard && cd dawaguard/deploy
cp ../.env.example .env   # add keys, a strong DB_PASSWORD, and DAWAGUARD_DOMAIN=your.domain
docker compose up -d --build
```

Then set the Meta app's webhook to `https://your.domain/webhook/whatsapp` with your `WHATSAPP_VERIFY_TOKEN`, and subscribe to `messages`. The WhatsApp Business Account must also have the app subscribed (`POST /<WABA_ID>/subscribed_apps`), or Meta sends incoming messages nowhere. With a Meta Sandbox test account, only the developer's own (long-lived) user token can send from the sandbox account; a system user token gets error #10. The database (CDSCO copy and cabinets) lives in the `dawaguard-data` volume.

## Data source

Flagged batches come from CDSCO's public NSQ and spurious drug alerts ([cdscoonline.gov.in/CDSCO/viewPublicNSQDrug](https://cdscoonline.gov.in/CDSCO/viewPublicNSQDrug)). A snapshot (January 2025 to September 2026) ships in `src/main/resources/seed/` so the app works if the CDSCO site is unreachable; the app refreshes from CDSCO daily.

The sample strips in `src/main/resources/static/samples/` are synthetic images rendered by `tools/MakeSampleStrips.java` and watermarked as such. The "failed batch" sample uses a real batch number from CDSCO's January 2026 alert so the match can be demonstrated.

## Limitations

- A failed test applies to that specific batch, not to the brand or manufacturer as a whole.
- CDSCO publishes each month's alert a few weeks later, so the newest failures may not be listed yet.
- Reading batch numbers from curved or shiny strips can go wrong; DawaGuard shows what it read so you can correct it.
- DawaGuard is a safety check, not medical advice. Don't stop a prescribed medicine without talking to a doctor or pharmacist.

## License

[MIT](LICENSE)
