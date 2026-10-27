(() => {
  "use strict";

  const STRINGS = {
    en: {
      title: "Is this medicine batch safe?",
      lede: "Take a photo of the strip. DawaGuard reads the batch number and checks it against the government's lists of medicine batches that failed quality tests or are suspected fakes.",
      photoLabel: "Medicine photo",
      takePhoto: "Take or choose a photo",
      hint: "Make sure the batch number (B.No.) and expiry date are sharp.",
      samplesLabel: "No strip nearby? Try a sample:",
      sampleFlagged: "Failed batch", sampleSafe: "Clear batch", sampleExpired: "Expired",
      typeLabel: "Or type the batch number", checkBatch: "Check batch",
      progressTitle: "Checking",
      stepRead: "Reading the strip", stepMatch: "Matching government alerts", stepVerify: "Looking for newer alerts",
      stepExplain: "Explaining the result", stepMessage: "Writing a WhatsApp message",
      readTitle: "What DawaGuard read", batchNo: "Batch number",
      fixLabel: "Wrong batch number? Correct it and check again", recheck: "Check again",
      waTitle: "Send to family", copy: "Copy message", copied: "Copied", openWa: "Open WhatsApp",
      sourcesTitle: "Sources", official: "Official CDSCO record",
      traceTitle: "Which model did what", thStep: "Step", thEngine: "Model or tool", thTime: "Time", thTokens: "Tokens",
      traceNote: "The verdict comes from DawaGuard's own check of CDSCO data. The language models only read the photo and explain the result.",
      footData: "Data: CDSCO \"Not of Standard Quality\" and spurious drug alerts, January 2025 onwards, refreshed daily.",
      footSafety: "DawaGuard is a safety check, not medical advice. Don't stop a medicine without talking to your doctor or pharmacist.",
      footCode: "Source code",
      fieldBrand: "Medicine", fieldGeneric: "Contains", fieldExpiry: "Expiry", fieldMaker: "Made by",
      notRead: "Not readable",
      otherProducts: "This batch code also appears in a CDSCO alert for a different product ({p}). Batch codes repeat across companies, so it doesn't apply to your medicine.",
      typedStep: "Typed in",
      running: "Working…", skipped: "Skipped", failed: "Failed",
      errNoBatch: "Type a batch number first.",
      errNetwork: "DawaGuard couldn't reach the server. Check your connection and try again.",
      verdicts: {
        SUSPECTED_FAKE: "Suspected fake medicine",
        FAILED_QUALITY_TEST: "Failed a government quality test",
        POSSIBLE_MATCH: "Check the batch number again",
        EXPIRED: "Expired",
        NEEDS_BATCH_NUMBER: "Batch number not readable",
        UNREADABLE: "No medicine pack found in the photo",
        NOT_FLAGGED: "Not on any government alert",
      },
    },
    hi: {
      title: "क्या यह दवा का बैच सुरक्षित है?",
      lede: "पत्ते (स्ट्रिप) की फ़ोटो लें। DawaGuard बैच नंबर पढ़ता है और उसे सरकार की उन सूचियों से मिलाता है जिनमें गुणवत्ता जाँच में फेल या नकली होने के संदेह वाले बैच दर्ज हैं।",
      photoLabel: "दवा की फ़ोटो",
      takePhoto: "फ़ोटो लें या चुनें",
      hint: "ध्यान रखें कि बैच नंबर (B.No.) और एक्सपायरी तारीख़ साफ़ दिखें।",
      samplesLabel: "पास में स्ट्रिप नहीं है? नमूना आज़माएँ:",
      sampleFlagged: "फेल बैच", sampleSafe: "ठीक बैच", sampleExpired: "एक्सपायर",
      typeLabel: "या बैच नंबर टाइप करें", checkBatch: "बैच जाँचें",
      progressTitle: "जाँच हो रही है",
      stepRead: "स्ट्रिप पढ़ी जा रही है", stepMatch: "सरकारी अलर्ट से मिलान", stepVerify: "नए अलर्ट की खोज",
      stepExplain: "नतीजा समझाया जा रहा है", stepMessage: "WhatsApp संदेश लिखा जा रहा है",
      readTitle: "DawaGuard ने क्या पढ़ा", batchNo: "बैच नंबर",
      fixLabel: "बैच नंबर ग़लत है? सही करके फिर जाँचें", recheck: "फिर जाँचें",
      waTitle: "परिवार को भेजें", copy: "संदेश कॉपी करें", copied: "कॉपी हो गया", openWa: "WhatsApp खोलें",
      sourcesTitle: "स्रोत", official: "CDSCO का आधिकारिक रिकॉर्ड",
      traceTitle: "किस मॉडल ने क्या किया", thStep: "चरण", thEngine: "मॉडल या टूल", thTime: "समय", thTokens: "टोकन",
      traceNote: "फ़ैसला DawaGuard की CDSCO डेटा जाँच से होता है। भाषा मॉडल सिर्फ़ फ़ोटो पढ़ते हैं और नतीजा समझाते हैं।",
      footData: "डेटा: CDSCO की \"मानक गुणवत्ता नहीं\" और नकली दवा अलर्ट, जनवरी 2025 से, रोज़ अपडेट।",
      footSafety: "DawaGuard एक सुरक्षा जाँच है, डॉक्टरी सलाह नहीं। डॉक्टर या फ़ार्मासिस्ट से बात किए बिना कोई दवा बंद न करें।",
      footCode: "सोर्स कोड",
      fieldBrand: "दवा", fieldGeneric: "इसमें है", fieldExpiry: "एक्सपायरी", fieldMaker: "निर्माता",
      notRead: "पढ़ा नहीं जा सका",
      otherProducts: "यही बैच कोड किसी दूसरी दवा ({p}) के CDSCO अलर्ट में भी है। अलग-अलग कंपनियों के बैच कोड एक जैसे हो सकते हैं, इसलिए यह आपकी दवा पर लागू नहीं होता।",
      typedStep: "टाइप किया गया",
      running: "जारी…", skipped: "छोड़ा गया", failed: "विफल",
      errNoBatch: "पहले बैच नंबर टाइप करें।",
      errNetwork: "DawaGuard सर्वर से नहीं जुड़ पाया। इंटरनेट जाँचें और फिर कोशिश करें।",
      verdicts: {
        SUSPECTED_FAKE: "नकली दवा होने का संदेह",
        FAILED_QUALITY_TEST: "सरकारी गुणवत्ता जाँच में फेल",
        POSSIBLE_MATCH: "बैच नंबर दोबारा जाँचें",
        EXPIRED: "एक्सपायर हो चुकी है",
        NEEDS_BATCH_NUMBER: "बैच नंबर पढ़ा नहीं जा सका",
        UNREADABLE: "फ़ोटो में दवा का पैक नहीं मिला",
        NOT_FLAGGED: "किसी सरकारी अलर्ट में नहीं",
      },
    },
  };

  const ENGINE_NAMES = {
    "nvidia/Nemotron-3.5-Super-VL": "Nemotron 3.5 Super VL",
    "nvidia/Nemotron-3-Ultra-550b-a55b": "Nemotron 3 Ultra",
    "nvidia/Nemotron-3_5-Lightning": "Nemotron 3.5 Lightning",
    "cdsco-data": "CDSCO data", "tavily": "Tavily", "manual": "—",
  };

  const TONE = {
    SUSPECTED_FAKE: "bad", FAILED_QUALITY_TEST: "bad",
    POSSIBLE_MATCH: "caution", EXPIRED: "caution", NEEDS_BATCH_NUMBER: "caution", UNREADABLE: "caution",
    NOT_FLAGGED: "good",
  };

  const $ = (id) => document.getElementById(id);
  let lang = "en";
  let busy = false;
  let lastReading = null;

  const t = (key) => STRINGS[lang][key] ?? STRINGS.en[key] ?? key;

  function applyLanguage(next) {
    lang = next;
    document.documentElement.lang = lang;
    document.querySelectorAll("[data-lang]").forEach((b) => b.setAttribute("aria-pressed", String(b.dataset.lang === lang)));
    document.querySelectorAll("[data-i18n]").forEach((el) => { el.textContent = t(el.dataset.i18n); });
    try { localStorage.setItem("dawaguard.lang", lang); } catch (_) { /* storage unavailable */ }
  }

  function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    Object.assign(node, props);
    for (const c of children) node.append(c);
    return node;
  }

  function setBusy(on) {
    busy = on;
    document.querySelectorAll("button, input").forEach((n) => {
      if (!n.closest(".lang")) n.disabled = on;
    });
  }

  function resetProgress(typed) {
    $("progress").hidden = false;
    $("result").hidden = true;
    $("error").hidden = true;
    document.querySelectorAll(".progress li").forEach((li) => {
      li.removeAttribute("data-status");
      li.querySelector(".state").textContent = "";
    });
    if (typed) markStep("read", "done", t("typedStep"));
  }

  function markStep(step, status, text) {
    const li = document.querySelector(`.progress li[data-step="${step}"]`);
    if (!li) return;
    li.dataset.status = status;
    li.querySelector(".state").textContent = text ?? "";
  }

  function onEvent(e) {
    if (e.type === "step") {
      const label = e.status === "done" ? (e.ms != null ? `${(e.ms / 1000).toFixed(1)} s` : "")
        : e.status === "running" ? t("running")
        : e.status === "skipped" ? t("skipped") : t("failed");
      markStep(e.step, e.status, label);
    } else if (e.type === "result") {
      renderResult(e.result);
    } else if (e.type === "error") {
      showError(e.message);
    }
  }

  async function run(request, typed) {
    if (busy) return;
    setBusy(true);
    resetProgress(typed);
    $("progress").scrollIntoView({ behavior: "smooth", block: "start" });
    try {
      const response = await fetch(request.url, request.init);
      if (!response.body) throw new Error("no body");
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";
      for (;;) {
        const { value, done } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        let nl;
        while ((nl = buffer.indexOf("\n")) >= 0) {
          const line = buffer.slice(0, nl).trim();
          buffer = buffer.slice(nl + 1);
          if (line) onEvent(JSON.parse(line));
        }
      }
      if (buffer.trim()) onEvent(JSON.parse(buffer));
    } catch (_) {
      showError(t("errNetwork"));
    } finally {
      setBusy(false);
    }
  }

  function checkPhoto(blob, name) {
    const preview = $("preview");
    preview.src = URL.createObjectURL(blob);
    preview.hidden = false;
    const form = new FormData();
    form.append("image", blob, name || "strip.jpg");
    form.append("lang", lang);
    run({ url: "/api/check", init: { method: "POST", body: form } }, false);
  }

  function checkTyped(batchNo, reading) {
    const body = {
      batchNo,
      brandName: reading?.brand_name || null,
      genericName: reading?.generic_name || null,
      expiryDate: reading?.expiry_date || null,
      lang,
    };
    run({
      url: "/api/check/typed",
      init: { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) },
    }, true);
  }

  function showError(message) {
    const box = $("error");
    box.textContent = message;
    box.hidden = false;
  }

  /** Turns "[1]" markers into links to the matching source; everything else stays plain text. */
  function withCitations(text, sources) {
    const frag = document.createDocumentFragment();
    const parts = String(text || "").split(/(\[\d+\])/g);
    for (const part of parts) {
      const m = part.match(/^\[(\d+)\]$/);
      const source = m && sources[Number(m[1]) - 1];
      if (source) {
        frag.append(el("a", { href: `#source-${m[1]}`, textContent: part, title: source.title }));
      } else {
        frag.append(document.createTextNode(part));
      }
    }
    return frag;
  }

  function renderResult(r) {
    lastReading = r.reading;
    const sources = r.evidence?.sources || [];

    const stamp = $("stamp");
    stamp.className = `stamp ${TONE[r.verdict] || "caution"}`;
    stamp.textContent = STRINGS[lang].verdicts[r.verdict] || r.verdict;
    void stamp.offsetWidth;
    stamp.classList.add("landed");

    $("headline").textContent = r.explanation?.headline || "";
    $("explanation").replaceChildren(withCitations(r.explanation?.explanation, sources));
    $("steps").replaceChildren(...(r.explanation?.next_steps || []).map((s) => el("li", {}, withCitations(s, sources))));

    const reading = r.reading || {};
    $("readBatch").textContent = reading.batch_no || t("notRead");
    $("fixBatch").value = reading.batch_no || "";
    const fields = [
      ["fieldBrand", reading.brand_name],
      ["fieldGeneric", [reading.generic_name, reading.strength].filter(Boolean).join(" ")],
      ["fieldExpiry", reading.expiry_date],
      ["fieldMaker", reading.manufacturer],
    ].filter(([, v]) => v);
    $("readFields").replaceChildren(...fields.flatMap(([k, v]) => [el("dt", { textContent: t(k) }), el("dd", { textContent: v })]));

    const other = r.otherProductMatches || [];
    $("otherProducts").hidden = other.length === 0;
    if (other.length) $("otherProducts").textContent = t("otherProducts").replace("{p}", other.map((b) => b.productName).join(", "));

    const message = r.whatsappMessage || "";
    $("whatsappBox").hidden = !message;
    $("whatsapp").textContent = message;
    $("openWa").href = `https://wa.me/?text=${encodeURIComponent(message)}`;
    $("copyWa").textContent = t("copy");

    $("sources").replaceChildren(...sources.map((s, i) => el("li", { id: `source-${i + 1}` },
      ...(s.official ? [el("span", { className: "tag", textContent: t("official") })] : []),
      el("a", { href: s.url, target: "_blank", rel: "noopener", textContent: s.title }),
      el("span", { className: "snippet", textContent: s.snippet || "" }))));

    $("trace").replaceChildren(...(r.trace || []).map((s) => el("tr", {},
      el("td", { textContent: s.description }),
      el("td", { textContent: ENGINE_NAMES[s.engine] || s.engine, title: s.engine }),
      el("td", { textContent: s.ms ? `${(s.ms / 1000).toFixed(1)} s` : "–" }),
      el("td", { textContent: s.promptTokens + s.completionTokens ? String(s.promptTokens + s.completionTokens) : "–" }))));

    $("result").hidden = false;
    $("result").scrollIntoView({ behavior: "smooth", block: "start" });
  }

  // Wiring
  document.querySelectorAll("[data-lang]").forEach((b) => b.addEventListener("click", () => applyLanguage(b.dataset.lang)));

  $("photo").addEventListener("change", (e) => {
    const file = e.target.files?.[0];
    if (file) checkPhoto(file, file.name);
    e.target.value = "";
  });

  document.querySelectorAll("[data-sample]").forEach((b) => b.addEventListener("click", async () => {
    const res = await fetch(`samples/${b.dataset.sample}`);
    checkPhoto(await res.blob(), b.dataset.sample);
  }));

  $("typed").addEventListener("submit", (e) => {
    e.preventDefault();
    const batch = $("batch").value.trim();
    if (!batch) { showError(t("errNoBatch")); return; }
    lastReading = null;
    $("preview").hidden = true;
    checkTyped(batch, null);
  });

  $("recheck").addEventListener("submit", (e) => {
    e.preventDefault();
    const batch = $("fixBatch").value.trim();
    if (!batch) { showError(t("errNoBatch")); return; }
    checkTyped(batch, lastReading);
  });

  $("copyWa").addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText($("whatsapp").textContent);
      $("copyWa").textContent = t("copied");
    } catch (_) { /* clipboard blocked; the text is still selectable */ }
  });

  let saved = "en";
  try { saved = localStorage.getItem("dawaguard.lang") || "en"; } catch (_) { /* storage unavailable */ }
  applyLanguage(saved === "hi" ? "hi" : "en");
})();
