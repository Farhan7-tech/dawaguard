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
| 4. Explain | Plain-language verdict in English or Hindi, with the official source and safe next steps | NVIDIA Nemotron 3 Ultra |
| 5. Watch | Remember the family's medicines and re-check them whenever CDSCO publishes a new list | NVIDIA Nemotron 3.5 Lightning |

## Run locally

Requires Java 21.

```bash
cp .env.example .env   # then add NEBIUS_API_KEY and TAVILY_API_KEY
./mvnw spring-boot:run
```

Then open:

- `http://localhost:8080/api/status`: loaded CDSCO data and freshness
- `http://localhost:8080/api/batches/{batchNo}`: look up a batch number

With `DIAGNOSTICS_ENABLED=true` in `.env` (local only, these spend API credits):

- `http://localhost:8080/api/diagnostics/models`: one test call to each Nemotron model
- `http://localhost:8080/api/diagnostics/search`: one Tavily search

## Data source

Flagged batches come from CDSCO's public NSQ and spurious drug alerts ([cdscoonline.gov.in/CDSCO/viewPublicNSQDrug](https://cdscoonline.gov.in/CDSCO/viewPublicNSQDrug)). A snapshot (January 2025 to September 2026) ships in `src/main/resources/seed/` so the app works if the CDSCO site is unreachable; the app refreshes from CDSCO daily.

## Limitations

- A failed test applies to that specific batch, not to the brand or manufacturer as a whole.
- CDSCO publishes each month's alert a few weeks later, so the newest failures may not be listed yet.
- Reading batch numbers from curved or shiny strips can go wrong; DawaGuard shows what it read so you can correct it.
- DawaGuard is a safety check, not medical advice. Don't stop a prescribed medicine without talking to a doctor or pharmacist.

## License

[MIT](LICENSE)
