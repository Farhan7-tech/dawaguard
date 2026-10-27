(() => {
  "use strict";

  const STRINGS = {
    en: {
      skip: "Skip to content",
      checkTitle: "Check a medicine",
      checkText: "Find out if a medicine batch failed a government quality test or is a suspected fake.",
      tabPhoto: "Photo", tabBatch: "Batch number",
      dropTitle: "Take or upload a photo",
      dropHint: "The batch number (B.No.) and expiry date should be sharp and readable.",
      samplesLabel: "No strip with you? Try a sample",
      sampleFlaggedTitle: "Pansid-40", sampleFlagged: "Batch on a CDSCO alert",
      sampleSafeTitle: "Demopar 500", sampleSafe: "Batch not on any alert",
      sampleExpiredTitle: "Democet 10", sampleExpired: "Past its expiry date",
      batchLabel: "Batch number", batchHelp: "Printed after \"B.No.\" or \"Batch\" on the strip.",
      brandLabel: "Medicine name", optional: "Optional", brandHelp: "Helps confirm the batch belongs to your medicine.",
      checkBatch: "Check batch",
      panelNote: "DawaGuard is a safety check, not medical advice. Don't stop a medicine without talking to your doctor or pharmacist.",
      emptyTitle: "How the check works",
      how1Title: "Read the strip", how1Text: "NVIDIA Nemotron reads the medicine name, batch number and expiry date from your photo.",
      how2Title: "Match government alerts", how2Text: "The batch is matched against every batch CDSCO has reported as failing a lab test or as spurious, and checked for newer alerts online.",
      how3Title: "Explain what to do", how3Text: "You get a clear answer with the official source, safe next steps, and a message you can send to family on WhatsApp.",
      statFailed: "Failed batches", statFake: "Suspected fakes", statUpdated: "Latest alert",
      freshness: "CDSCO data through {m}",
      progressTitle: "Checking your medicine", inProgress: "In progress", completed: "Completed", stopped: "Stopped",
      stepRead: "Read strip", stepMatch: "Match alerts", stepVerify: "Search news", stepExplain: "Explain", stepMessage: "WhatsApp text",
      stepsTitle: "What to do next",
      readTitle: "Medicine details", readSubtitle: "As read from your photo", readSubtitleTyped: "As you typed it",
      fixLabel: "Batch number wrong? Correct it", recheck: "Check again",
      recordTitle: "CDSCO record", officialBadge: "Official", recordLink: "View CDSCO's public list",
      recordSubtitleNsq: "Not of Standard Quality alert, {m}", recordSubtitleFake: "Spurious drug alert, {m}",
      recordSubtitlePossible: "Closest match, batch {b}",
      waTitle: "Message for family", waSubtitle: "Ready to send on WhatsApp", copy: "Copy", copied: "Copied", openWa: "Send on WhatsApp",
      sourcesTitle: "Sources", official: "Official CDSCO record", mentionsBatch: "Mentions this batch", web: "Web",
      traceTitle: "How this answer was made", thStep: "Step", thEngine: "Model or tool", thTime: "Time", thTokens: "Tokens",
      traceTotal: "{s} s total",
      traceNote: "The verdict is decided by DawaGuard's own check of CDSCO data. The language models only read the photo and explain the result.",
      newCheck: "Check another medicine",
      footData: "Data from CDSCO's public \"Not of Standard Quality\" and spurious drug alerts, January 2025 onwards, refreshed daily.",
      footBuilt: "Built with NVIDIA Nemotron on Nebius Token Factory and Tavily.", footCode: "Source code",
      fBrand: "Medicine", fGeneric: "Contains", fBatch: "Batch number", fMfg: "Manufactured", fExpiry: "Expiry", fMaker: "Manufacturer",
      fProduct: "Product", fResult: "Test result", fLab: "Reported by",
      notRead: "Not readable",
      otherProducts: "This batch code also appears in a CDSCO alert for a different product ({p}). Batch codes repeat across companies, so it doesn't apply to your medicine.",
      typed: "Typed in",
      running: "Working", skipped: "Skipped", failed: "Failed",
      errNoBatch: "Enter the batch number printed on the strip.",
      errNetwork: "DawaGuard couldn't reach the server. Check your connection and try again.",
      verdicts: {
        SUSPECTED_FAKE: "Suspected fake medicine",
        FAILED_QUALITY_TEST: "Failed a government quality test",
        POSSIBLE_MATCH: "Possible match: check the batch number",
        EXPIRED: "Expired",
        NEEDS_BATCH_NUMBER: "Batch number not readable",
        UNREADABLE: "No medicine pack found",
        NOT_FLAGGED: "Not on any government alert",
      },
    },
    hi: {
      skip: "सामग्री पर जाएँ",
      checkTitle: "दवा जाँचें",
      checkText: "जानें कि दवा का बैच सरकारी गुणवत्ता जाँच में फेल हुआ है या नकली होने का संदेह है।",
      tabPhoto: "फ़ोटो", tabBatch: "बैच नंबर",
      dropTitle: "फ़ोटो लें या अपलोड करें",
      dropHint: "बैच नंबर (B.No.) और एक्सपायरी तारीख़ साफ़ दिखनी चाहिए।",
      samplesLabel: "स्ट्रिप पास में नहीं है? नमूना आज़माएँ",
      sampleFlaggedTitle: "Pansid-40", sampleFlagged: "CDSCO अलर्ट में दर्ज बैच",
      sampleSafeTitle: "Demopar 500", sampleSafe: "किसी अलर्ट में नहीं",
      sampleExpiredTitle: "Democet 10", sampleExpired: "एक्सपायर हो चुकी",
      batchLabel: "बैच नंबर", batchHelp: "स्ट्रिप पर \"B.No.\" या \"Batch\" के बाद छपा होता है।",
      brandLabel: "दवा का नाम", optional: "वैकल्पिक", brandHelp: "इससे पुष्टि होती है कि बैच आपकी दवा का ही है।",
      checkBatch: "बैच जाँचें",
      panelNote: "DawaGuard एक सुरक्षा जाँच है, डॉक्टरी सलाह नहीं। डॉक्टर या फ़ार्मासिस्ट से बात किए बिना कोई दवा बंद न करें।",
      emptyTitle: "जाँच कैसे होती है",
      how1Title: "स्ट्रिप पढ़ना", how1Text: "NVIDIA Nemotron आपकी फ़ोटो से दवा का नाम, बैच नंबर और एक्सपायरी तारीख़ पढ़ता है।",
      how2Title: "सरकारी अलर्ट से मिलान", how2Text: "बैच को CDSCO के हर उस बैच से मिलाया जाता है जो लैब जाँच में फेल हुआ या नकली पाया गया, और नए अलर्ट ऑनलाइन खोजे जाते हैं।",
      how3Title: "आगे क्या करें", how3Text: "आपको आधिकारिक स्रोत के साथ साफ़ जवाब, सुरक्षित अगले कदम और परिवार को WhatsApp पर भेजने लायक संदेश मिलता है।",
      statFailed: "फेल बैच", statFake: "नकली होने का संदेह", statUpdated: "नवीनतम अलर्ट",
      freshness: "CDSCO डेटा {m} तक",
      progressTitle: "आपकी दवा की जाँच हो रही है", inProgress: "जारी है", completed: "पूरा हुआ", stopped: "रुक गया",
      stepRead: "स्ट्रिप पढ़ना", stepMatch: "अलर्ट मिलान", stepVerify: "खबरें खोजना", stepExplain: "समझाना", stepMessage: "WhatsApp संदेश",
      stepsTitle: "आगे क्या करें",
      readTitle: "दवा की जानकारी", readSubtitle: "आपकी फ़ोटो से पढ़ी गई", readSubtitleTyped: "जैसा आपने टाइप किया",
      fixLabel: "बैच नंबर ग़लत है? सही करें", recheck: "फिर जाँचें",
      recordTitle: "CDSCO रिकॉर्ड", officialBadge: "आधिकारिक", recordLink: "CDSCO की सार्वजनिक सूची देखें",
      recordSubtitleNsq: "मानक गुणवत्ता नहीं (NSQ) अलर्ट, {m}", recordSubtitleFake: "नकली दवा अलर्ट, {m}",
      recordSubtitlePossible: "सबसे मिलता-जुलता, बैच {b}",
      waTitle: "परिवार के लिए संदेश", waSubtitle: "WhatsApp पर भेजने के लिए तैयार", copy: "कॉपी करें", copied: "कॉपी हो गया", openWa: "WhatsApp पर भेजें",
      sourcesTitle: "स्रोत", official: "CDSCO का आधिकारिक रिकॉर्ड", mentionsBatch: "इस बैच का ज़िक्र", web: "वेब",
      traceTitle: "यह जवाब कैसे बना", thStep: "चरण", thEngine: "मॉडल या टूल", thTime: "समय", thTokens: "टोकन",
      traceTotal: "कुल {s} सेकंड",
      traceNote: "फ़ैसला DawaGuard की CDSCO डेटा जाँच से होता है। भाषा मॉडल सिर्फ़ फ़ोटो पढ़ते हैं और नतीजा समझाते हैं।",
      newCheck: "दूसरी दवा जाँचें",
      footData: "डेटा: CDSCO के सार्वजनिक \"मानक गुणवत्ता नहीं\" और नकली दवा अलर्ट, जनवरी 2025 से, रोज़ अपडेट।",
      footBuilt: "NVIDIA Nemotron (Nebius Token Factory) और Tavily से बना।", footCode: "सोर्स कोड",
      fBrand: "दवा", fGeneric: "इसमें है", fBatch: "बैच नंबर", fMfg: "निर्माण", fExpiry: "एक्सपायरी", fMaker: "निर्माता",
      fProduct: "उत्पाद", fResult: "जाँच का नतीजा", fLab: "रिपोर्ट करने वाला",
      notRead: "पढ़ा नहीं जा सका",
      otherProducts: "यही बैच कोड किसी दूसरी दवा ({p}) के CDSCO अलर्ट में भी है। अलग-अलग कंपनियों के बैच कोड एक जैसे हो सकते हैं, इसलिए यह आपकी दवा पर लागू नहीं होता।",
      typed: "टाइप किया गया",
      running: "जारी", skipped: "छोड़ा गया", failed: "विफल",
      errNoBatch: "स्ट्रिप पर छपा बैच नंबर लिखें।",
      errNetwork: "DawaGuard सर्वर से नहीं जुड़ पाया। इंटरनेट जाँचें और फिर कोशिश करें।",
      verdicts: {
        SUSPECTED_FAKE: "नकली दवा होने का संदेह",
        FAILED_QUALITY_TEST: "सरकारी गुणवत्ता जाँच में फेल",
        POSSIBLE_MATCH: "संभावित मिलान: बैच नंबर जाँचें",
        EXPIRED: "एक्सपायर हो चुकी है",
        NEEDS_BATCH_NUMBER: "बैच नंबर पढ़ा नहीं जा सका",
        UNREADABLE: "दवा का पैक नहीं मिला",
        NOT_FLAGGED: "किसी सरकारी अलर्ट में नहीं",
      },
    },
  };

  const TONE = {
    SUSPECTED_FAKE: "danger", FAILED_QUALITY_TEST: "danger",
    POSSIBLE_MATCH: "warning", EXPIRED: "warning", NEEDS_BATCH_NUMBER: "warning", UNREADABLE: "warning",
    NOT_FLAGGED: "success",
  };

  const ICONS = {
    danger: '<svg class="icon" viewBox="0 0 24 24"><path d="M8.5 3h7L21 8.5v7L15.5 21h-7L3 15.5v-7z"/><path d="M12 8v4.5"/><path d="M12 16v.01"/></svg>',
    warning: '<svg class="icon" viewBox="0 0 24 24"><path d="M12 4l9 16H3z"/><path d="M12 10v4"/><path d="M12 17v.01"/></svg>',
    success: '<svg class="icon" viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>',
  };

  const ENGINE_NAMES = {
    "nvidia/Nemotron-3.5-Super-VL": "NVIDIA Nemotron 3.5 Super VL",
    "nvidia/Nemotron-3-Ultra-550b-a55b": "NVIDIA Nemotron 3 Ultra",
    "nvidia/Nemotron-3_5-Lightning": "NVIDIA Nemotron 3.5 Lightning",
    "cdsco-data": "CDSCO flagged-batch data", "tavily": "Tavily search", "manual": "—",
  };

  const MONTHS = { en: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"] };

  const $ = (id) => document.getElementById(id);
  let lang = "en";
  let busy = false;
  let lastReading = null;
  let lastResult = null;
  let status = null;

  const t = (key) => STRINGS[lang][key] ?? STRINGS.en[key] ?? key;
  const fill = (s, vars) => s.replace(/\{(\w)\}/g, (_, k) => vars[k] ?? "");

  function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    Object.assign(node, props);
    for (const c of children) if (c != null) node.append(c);
    return node;
  }

  /** "2026-09" -> "Sep 2026" */
  function period(p) {
    const m = /^(\d{4})-(\d{2})$/.exec(p || "");
    return m ? `${MONTHS.en[Number(m[2]) - 1]} ${m[1]}` : p || "";
  }

  // ---------- Language ----------
  function applyLanguage(next) {
    lang = next;
    document.documentElement.lang = lang;
    document.querySelectorAll("[data-lang]").forEach((b) => b.setAttribute("aria-pressed", String(b.dataset.lang === lang)));
    document.querySelectorAll("[data-i18n]").forEach((node) => { node.textContent = t(node.dataset.i18n); });
    renderStatus();
    if (lastResult) renderResult(lastResult, false);
    try { localStorage.setItem("dawaguard.lang", lang); } catch (_) { /* storage unavailable */ }
  }

  // ---------- Data freshness ----------
  async function loadStatus() {
    try {
      const res = await fetch("/api/status");
      if (res.ok) { status = await res.json(); renderStatus(); }
    } catch (_) { /* status is optional */ }
  }

  function renderStatus() {
    if (!status || !status.latestReportingPeriod) return;
    $("freshnessText").textContent = fill(t("freshness"), { m: period(status.latestReportingPeriod) });
    $("freshness").hidden = false;
    $("statFailed").textContent = Number(status.nsqBatches).toLocaleString("en-IN");
    $("statFake").textContent = Number(status.spuriousBatches).toLocaleString("en-IN");
    $("statUpdated").textContent = period(status.latestReportingPeriod);
    $("stats").hidden = false;
  }

  // ---------- Tabs ----------
  function selectTab(name) {
    for (const tab of ["photo", "batch"]) {
      const on = tab === name;
      $(`tab-${tab}`).setAttribute("aria-selected", String(on));
      $(`tab-${tab}`).tabIndex = on ? 0 : -1;
      $(`pane-${tab}`).hidden = !on;
    }
  }

  // ---------- Running a check ----------
  function setBusy(on) {
    busy = on;
    document.querySelectorAll(".panel button, .panel input, #recheck button, #recheck input, #newCheck")
      .forEach((n) => { n.disabled = on; });
  }

  function startProgress(typed) {
    $("empty").hidden = true;
    $("result").hidden = true;
    $("error").hidden = true;
    $("progress").hidden = false;
    $("progressBadge").textContent = t("inProgress");
    document.querySelectorAll(".stepper__step").forEach((li) => {
      li.removeAttribute("data-status");
      li.querySelector(".stepper__time").textContent = "";
    });
    if (typed) markStep("read", "done", t("typed"));
    if (window.matchMedia("(max-width: 959px)").matches) {
      $("progress").scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }

  function markStep(step, state, text) {
    const li = document.querySelector(`.stepper__step[data-step="${step}"]`);
    if (!li) return;
    li.dataset.status = state;
    li.querySelector(".stepper__time").textContent = text ?? "";
  }

  function onEvent(e) {
    if (e.type === "step") {
      const label = e.status === "done" ? (e.ms != null ? `${(e.ms / 1000).toFixed(1)} s` : "")
        : e.status === "running" ? t("running")
        : e.status === "skipped" ? t("skipped") : t("failed");
      markStep(e.step, e.status, label);
    } else if (e.type === "result") {
      $("progressBadge").textContent = t("completed");
      renderResult(e.result, true);
    } else if (e.type === "error") {
      $("progressBadge").textContent = t("stopped");
      showError(e.message);
    }
  }

  async function run(url, init, typed) {
    if (busy) return;
    setBusy(true);
    startProgress(typed);
    try {
      const response = await fetch(url, init);
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
      $("progressBadge").textContent = t("stopped");
      showError(t("errNetwork"));
    } finally {
      setBusy(false);
    }
  }

  function showPreview(blob) {
    const preview = $("preview");
    if (preview.src) URL.revokeObjectURL(preview.src);
    preview.src = URL.createObjectURL(blob);
    preview.hidden = false;
    $("dropzoneEmpty").hidden = true;
    $("dropzone").classList.add("has-preview");
  }

  function checkPhoto(blob, name) {
    showPreview(blob);
    const form = new FormData();
    form.append("image", blob, name || "strip.jpg");
    form.append("lang", lang);
    run("/api/check", { method: "POST", body: form }, false);
  }

  function checkTyped(batchNo, brandName, reading) {
    const body = {
      batchNo,
      brandName: brandName || reading?.brand_name || null,
      genericName: reading?.generic_name || null,
      expiryDate: reading?.expiry_date || null,
      lang,
    };
    run("/api/check/typed", {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
    }, true);
  }

  function showError(message) {
    $("errorText").textContent = message;
    $("error").hidden = false;
  }

  // ---------- Rendering ----------
  /** Turns "[1]" markers into links to the matching source; everything else stays plain text. */
  function withCitations(text, sources) {
    const frag = document.createDocumentFragment();
    for (const part of String(text || "").split(/(\[\d+\])/g)) {
      const m = part.match(/^\[(\d+)\]$/);
      const source = m && sources[Number(m[1]) - 1];
      frag.append(source
        ? el("a", { href: `#source-${m[1]}`, textContent: part, title: source.title })
        : document.createTextNode(part));
    }
    return frag;
  }

  /** Packs often print the strength inside the name ("... IP 40 mg"); don't repeat it. */
  function withStrength(name, strength) {
    if (!strength) return name || "";
    if (!name) return strength;
    const compact = (s) => s.toLowerCase().replace(/\s+/g, "");
    return compact(name).includes(compact(strength)) ? name : `${name} ${strength}`;
  }

  function detailRows(rows) {
    return rows.filter(([, v]) => v).flatMap(([key, value, mono]) => [
      el("dt", { textContent: t(key) }),
      el("dd", { textContent: value, className: mono ? "mono" : "" }),
    ]);
  }

  function renderResult(r, fresh) {
    lastResult = r;
    lastReading = r.reading;
    const sources = r.evidence?.sources || [];
    const tone = TONE[r.verdict] || "warning";

    const verdict = $("verdict");
    verdict.className = `verdict verdict--${tone}`;
    $("verdictIcon").innerHTML = ICONS[tone];
    $("verdictStatus").textContent = STRINGS[lang].verdicts[r.verdict] || r.verdict;
    $("headline").textContent = r.explanation?.headline || "";
    $("explanation").replaceChildren(withCitations(r.explanation?.explanation, sources));

    const steps = r.explanation?.next_steps || [];
    $("stepsCard").hidden = steps.length === 0;
    $("steps").replaceChildren(...steps.map((s) => el("li", {}, el("span", {}, withCitations(s, sources)))));

    // Medicine details
    const reading = r.reading || {};
    const typed = reading.legibility === "manual";
    document.querySelector("#readTitle + .card__subtitle").textContent = t(typed ? "readSubtitleTyped" : "readSubtitle");
    $("readFields").replaceChildren(...detailRows([
      ["fBrand", reading.brand_name],
      ["fGeneric", withStrength(reading.generic_name, reading.strength)],
      ["fBatch", reading.batch_no || t("notRead"), true],
      ["fMfg", reading.mfg_date],
      ["fExpiry", reading.expiry_date],
      ["fMaker", reading.manufacturer],
    ]));
    if (fresh) $("fixBatch").value = reading.batch_no || "";

    const other = r.otherProductMatches || [];
    $("otherProducts").hidden = other.length === 0;
    if (other.length) $("otherProducts").textContent = fill(t("otherProducts"), { p: other.map((b) => b.productName).join(", ") });

    // Official record
    const confirmed = r.confirmedMatches || [];
    const record = confirmed[0] || (r.possibleMatches || [])[0];
    $("recordCard").hidden = !record;
    if (record) {
      const month = record.reportingMonth || "";
      $("recordSubtitle").textContent = confirmed.length
        ? fill(t(record.listType === "SPURIOUS" ? "recordSubtitleFake" : "recordSubtitleNsq"), { m: month })
        : fill(t("recordSubtitlePossible"), { b: record.batchNo });
      $("recordFields").replaceChildren(...detailRows([
        ["fProduct", record.productName],
        ["fBrand", record.brandName && record.brandName !== "NA" ? record.brandName : null],
        ["fBatch", record.batchNo, true],
        ["fMfg", record.mfgDate],
        ["fExpiry", record.expiryDate],
        ["fMaker", record.manufacturer],
        ["fResult", record.result],
        ["fLab", record.reportedBy],
      ]));
    }

    // WhatsApp
    const message = r.whatsappMessage || "";
    $("waCard").hidden = !message;
    // With the record card shown, the message card would sit alone in its row; let it span the grid.
    $("waCard").classList.toggle("card--wide", Boolean(record));
    $("whatsapp").textContent = message;
    $("openWa").href = `https://wa.me/?text=${encodeURIComponent(message)}`;
    $("copyWaLabel").textContent = t("copy");

    // Sources
    $("sources").replaceChildren(...sources.map((s, i) => {
      let host = "";
      try { host = new URL(s.url).hostname.replace(/^www\./, ""); } catch (_) { /* keep empty */ }
      const meta = el("span", { className: "sources__meta" },
        s.official ? el("span", { className: "badge badge--official", textContent: t("official") }) : null,
        !s.official && s.mentionsBatch ? el("span", { className: "badge badge--neutral", textContent: t("mentionsBatch") }) : null,
        el("span", { textContent: host }));
      return el("li", { id: `source-${i + 1}`, className: s.official ? "is-official" : "" },
        el("div", {},
          el("a", { className: "sources__title", href: s.url, target: "_blank", rel: "noopener", textContent: s.title }),
          meta,
          s.snippet ? el("p", { className: "sources__snippet", textContent: s.snippet }) : null));
    }));

    // Trace
    const trace = r.trace || [];
    const totalMs = trace.reduce((sum, s) => sum + (s.ms || 0), 0);
    $("traceTotal").textContent = fill(t("traceTotal"), { s: (totalMs / 1000).toFixed(1) });
    $("trace").replaceChildren(...trace.map((s) => el("tr", {},
      el("td", { textContent: s.description }),
      el("td", { textContent: ENGINE_NAMES[s.engine] || s.engine, title: s.engine }),
      el("td", { className: "num", textContent: s.ms ? `${(s.ms / 1000).toFixed(1)} s` : "—" }),
      el("td", { className: "num", textContent: s.promptTokens + s.completionTokens ? (s.promptTokens + s.completionTokens).toLocaleString("en-IN") : "—" }))));

    $("result").hidden = false;
    if (fresh) {
      verdict.style.animation = "none";
      void verdict.offsetWidth;
      verdict.style.animation = "";
      verdict.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }
  }

  function resetForNewCheck() {
    lastResult = null;
    $("result").hidden = true;
    $("progress").hidden = true;
    $("error").hidden = true;
    $("empty").hidden = false;
    $("preview").hidden = true;
    $("dropzoneEmpty").hidden = false;
    $("dropzone").classList.remove("has-preview");
    window.scrollTo({ top: 0, behavior: "smooth" });
    $("tab-photo").focus();
  }

  // ---------- Wiring ----------
  document.querySelectorAll("[data-lang]").forEach((b) => b.addEventListener("click", () => applyLanguage(b.dataset.lang)));

  $("tab-photo").addEventListener("click", () => selectTab("photo"));
  $("tab-batch").addEventListener("click", () => selectTab("batch"));
  document.querySelector(".tabs").addEventListener("keydown", (e) => {
    if (e.key !== "ArrowLeft" && e.key !== "ArrowRight") return;
    const next = document.activeElement === $("tab-photo") ? "batch" : "photo";
    selectTab(next);
    $(`tab-${next}`).focus();
  });

  $("photo").addEventListener("change", (e) => {
    const file = e.target.files?.[0];
    if (file) checkPhoto(file, file.name);
    e.target.value = "";
  });

  const dz = $("dropzone");
  ["dragenter", "dragover"].forEach((ev) => dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.add("is-dragging"); }));
  ["dragleave", "drop"].forEach((ev) => dz.addEventListener(ev, () => dz.classList.remove("is-dragging")));
  dz.addEventListener("drop", (e) => {
    e.preventDefault();
    const file = e.dataTransfer?.files?.[0];
    if (file && file.type.startsWith("image/") && !busy) checkPhoto(file, file.name);
  });

  document.querySelectorAll("[data-sample]").forEach((b) => b.addEventListener("click", async () => {
    const res = await fetch(`samples/${b.dataset.sample}`);
    checkPhoto(await res.blob(), b.dataset.sample);
  }));

  function validateBatch(input) {
    const ok = input.value.trim().length > 0;
    input.setAttribute("aria-invalid", String(!ok));
    if (!ok) { showError(t("errNoBatch")); input.focus(); }
    return ok;
  }

  $("pane-batch").addEventListener("submit", (e) => {
    e.preventDefault();
    if (!validateBatch($("batch"))) return;
    lastReading = null;
    $("preview").hidden = true;
    $("dropzoneEmpty").hidden = false;
    $("dropzone").classList.remove("has-preview");
    checkTyped($("batch").value.trim(), $("brand").value.trim(), null);
  });
  $("batch").addEventListener("input", () => $("batch").removeAttribute("aria-invalid"));

  $("recheck").addEventListener("submit", (e) => {
    e.preventDefault();
    if (!validateBatch($("fixBatch"))) return;
    checkTyped($("fixBatch").value.trim(), null, lastReading);
  });

  $("copyWa").addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText($("whatsapp").textContent);
      $("copyWaLabel").textContent = t("copied");
    } catch (_) { /* clipboard blocked; the text is still selectable */ }
  });

  $("newCheck").addEventListener("click", resetForNewCheck);

  let saved = "en";
  try { saved = localStorage.getItem("dawaguard.lang") || "en"; } catch (_) { /* storage unavailable */ }
  applyLanguage(saved === "hi" ? "hi" : "en");
  loadStatus();
})();
