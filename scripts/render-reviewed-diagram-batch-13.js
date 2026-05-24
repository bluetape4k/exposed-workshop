#!/usr/bin/env node

const fs = require("fs");
const { spawnSync } = require("child_process");

const RSVG = process.env.RSVG_CONVERT || "rsvg-convert";
const DOT = process.env.DOT || "dot";
const outDir = "docs/images/readme-diagrams";
const sketchDir = ".omx/artifacts/diagram-sketches";

const style = `
.canvas{fill:#f6f9fc}.frame{fill:#fff;stroke:#d7e2ec;stroke-width:1.5}
.title{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:31px;fill:#1e293b}
.subtitle,.detail{font-family:"Comic Mono","Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;fill:#536476}
.subtitle{font-size:13px}.detail{font-size:12px}.tiny{font-family:"Comic Mono","Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:10px;fill:#64748b}
.label{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:16px;fill:#1e293b}
.panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
.card{stroke-width:1.7;filter:url(#shadow)}.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
.arrow{fill:none;stroke:#2563eb;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}
.mapLine{fill:none;stroke:#16a34a;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
.codecLine{fill:none;stroke:#db2777;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPink)}
.dbUse{fill:none;stroke:#ea580c;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
.runtimeUse{fill:none;stroke:#7c3aed;stroke-width:1.9;stroke-dasharray:6 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.inheritStem{fill:none;stroke:#475569;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}
`;

const colors = [
  ["#dbeafe", "#3b82f6"],
  ["#ede9fe", "#7c3aed"],
  ["#dcfce7", "#16a34a"],
  ["#ffedd5", "#ea580c"],
  ["#fce7f3", "#db2777"],
  ["#ccfbf1", "#0f766e"],
  ["#fef3c7", "#d97706"],
  ["#e2e8f0", "#64748b"],
];

const diagrams = [
  {
    file: `${outDir}/06-advanced-09-exposed-fastjson2-architecture-01.svg`,
    title: "Fastjson2 JSON Query Flow",
    subtitle: "FastjsonColumnTest and FastjsonBColumnTest persist DataHolder models, query nested JSON, and use Fastjson array tables",
    width: 1560,
    height: 940,
    body: () => jsonArchitecture({
      family: "Fastjson2",
      testA: "FastjsonColumnTest",
      testB: "FastjsonBColumnTest",
      schema: "FastjsonSchema",
      serializer: "FastjsonSerializer",
      builders: "fastjson / fastjsonb",
      tableA: "FastjsonTable",
      tableB: "FastjsonBTable",
      arrayTable: "Fastjson arrays",
      columnA: "fastjson_column JSON",
      columnB: "fastjson_b_column JSONB",
      note: "Source check: Fastjson tests cover insert/update, extract, contains, exists, arrays, upsert, defaults, and DAO JSONB usage.",
    }),
  },
  {
    file: `${outDir}/06-advanced-11-exposed-jackson3-architecture-01.svg`,
    title: "Jackson 3 JSON Query Flow",
    subtitle: "JacksonColumnTest and JacksonBColumnTest use Jackson 3 serializers over JSON/JSONB tables and array payloads",
    width: 1560,
    height: 940,
    body: () => jsonArchitecture({
      family: "Jackson 3",
      testA: "JacksonColumnTest",
      testB: "JacksonBColumnTest",
      schema: "JacksonSchema",
      serializer: "DefaultJacksonSerializer",
      builders: "jackson / jacksonb",
      tableA: "JacksonTable",
      tableB: "JacksonBTable",
      arrayTable: "Jackson arrays",
      columnA: "jackson_column JSON",
      columnB: "jackson_b_column JSONB",
      note: "Source check: Jackson 3 tests cover insert/update, extract, contains, exists, arrays, upsert, defaults, and DAO JSONB usage.",
    }),
  },
  {
    file: `${outDir}/06-advanced-12-exposed-tink-architecture-01.svg`,
    title: "Tink Encrypted Column Flow",
    subtitle: "AEAD columns encrypt/decrypt transparently; DAEAD columns use deterministic ciphertext for indexed WHERE searches",
    width: 1560,
    height: 960,
    body: tinkArchitecture,
  },
  {
    file: `${outDir}/06-advanced-12-exposed-tink-class-02.svg`,
    title: "Tink Column Test and DAO Model",
    subtitle: "Tink tests extend AbstractExposedTest; DAO examples pair T1 IntIdTable with E1 IntEntity and DAEAD columns",
    width: 1560,
    height: 980,
    body: tinkClass,
  },
];

writeGraphvizSketches();
for (const diagram of diagrams) {
  fs.writeFileSync(diagram.file, shell(diagram));
  const png = diagram.file.replace(/\.svg$/, ".png");
  const result = spawnSync(RSVG, ["-f", "png", "-o", png, diagram.file], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout);
  console.log(png);
}
validateBatch13Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-13-json-flow": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      tests -> payload -> serializer -> tables -> query_dsl -> db;
      schema -> tables;
    }`,
    "batch-13-tink-flow": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.5, ranksep=0.8, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      tests -> aead_columns -> aead_crypto -> encrypted_rows -> db;
      tests -> daead_columns -> daead_crypto -> searchable_rows -> db;
      dao -> T1 -> E1 -> searchable_rows;
    }`,
    "batch-13-tink-class": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.65, ranksep=0.8, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      AbstractExposedTest -> TinkColumnTypeTest;
      AbstractExposedTest -> TinkColumnTypeDaoTest;
      IntIdTable -> T1;
      IntEntity -> E1;
      T1 -> E1;
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
  }
}

function jsonArchitecture(model) {
  let b = "";
  b += panel(58, 130, 300, 640, "Tests");
  b += card(96, 184, 224, 70, model.testA, 0, "JSON column");
  b += card(96, 352, 224, 70, model.testB, 4, "JSONB column");
  b += card(96, 520, 224, 70, model.schema, 7, "shared fixtures");

  b += panel(430, 130, 318, 640, "Serialization");
  b += card(474, 194, 230, 66, "DataHolder", 2, "User + state");
  b += card(474, 332, 230, 66, model.serializer, 6, `${model.family} codec`);
  b += card(474, 470, 230, 66, model.builders, 1, "ColumnType builders");
  b += card(474, 608, 230, 66, "transform/default", 5, "nullable / generated");

  b += panel(824, 130, 300, 640, "Tables");
  b += erdTable(864, 176, 220, model.tableA, [model.columnA, `${model.tableA.replace("Table", "Entity")}`], 0, 126);
  b += erdTable(864, 346, 220, model.tableB, [model.columnB, `${model.tableB.replace("Table", "Entity")}`], 4, 126);
  b += erdTable(864, 536, 220, model.arrayTable, ["groups JSON/JSONB", "numbers JSON/JSONB"], 2, 132);

  b += panel(1194, 130, 204, 640, "Query DSL");
  b += card(1224, 184, 144, 58, "insert/update", 2, "round trip");
  b += card(1224, 312, 144, 58, "extract", 1, "path slice");
  b += card(1224, 440, 144, 58, "contains", 4, "JSON predicate");
  b += card(1224, 568, 144, 58, "exists", 6, "path exists");
  b += cylinder(1428, 348, 104, 218, "DB", ["Postgres", "MySQL V8", "H2 JSON"], 3);

  b += path("M320,219 L398,219 L398,227 L474,227", "arrow");
  b += path("M320,387 L398,387 L398,365 L474,365", "arrow");
  b += path("M320,555 L398,555 L398,641 L474,641", "runtimeUse");
  b += path("M704,227 L784,227 L784,239 L864,239", "mapLine");
  b += path("M704,365 L784,365 L784,409 L864,409", "mapLine");
  b += path("M704,503 L784,503 L784,602 L864,602", "mapLine");
  b += path("M1084,239 L1160,239 L1160,213 L1224,213", "arrow");
  b += path("M1084,409 L1160,409 L1160,341 L1224,341", "arrow");
  b += path("M1084,409 L1160,409 L1160,469 L1224,469", "codecLine");
  b += path("M1084,602 L1160,602 L1160,597 L1224,597", "runtimeUse");
  b += path("M1368,213 L1404,213 L1404,414 L1428,414", "dbUse");
  b += path("M1368,341 L1404,341 L1404,448 L1428,448", "dbUse");
  b += path("M1368,469 L1404,469 L1404,482 L1428,482", "dbUse");
  b += path("M1368,597 L1404,597 L1404,604 L1480,604 L1480,566", "dbUse");
  b += note(128, 838, 1196, model.note);
  return b;
}

function tinkArchitecture() {
  let b = "";
  b += panel(58, 130, 300, 660, "Tests");
  b += card(96, 190, 224, 70, "TinkColumnTypeTest", 0, "DSL column tests");
  b += card(96, 366, 224, 70, "TinkColumnTypeDaoTest", 4, "DAO DAEAD tests");
  b += card(96, 542, 224, 70, "withTables", 7, "test fixture");

  b += panel(430, 130, 324, 660, "Column families");
  b += card(474, 174, 232, 80, "AEAD columns", 2, "varchar / binary / blob");
  b += card(474, 312, 232, 80, "DAEAD columns", 1, "searchable ciphertext");
  b += card(474, 450, 232, 80, "Nullable/update", 5, "null + changed values");
  b += card(474, 588, 232, 80, "Multi algorithm", 6, "AES / ChaCha20");

  b += panel(824, 130, 312, 660, "Tink crypto");
  b += card(864, 174, 232, 72, "AES256_GCM", 0, "non-deterministic");
  b += card(864, 286, 232, 72, "AES256_SIV", 1, "deterministic");
  b += card(864, 398, 232, 72, "CHACHA20_POLY1305", 6, "AEAD option");
  b += card(864, 548, 232, 72, "Plaintext eq", 4, "DAEAD WHERE search");

  b += panel(1206, 130, 184, 660, "Tables");
  b += erdTable(1228, 170, 140, "AEAD tables", ["secret", "data", "blob"], 2, 126);
  b += erdTable(1228, 356, 140, "DAEAD tables", ["email index", "fingerprint", "blob"], 1, 132);
  b += erdTable(1228, 548, 140, "DAO T1", ["secret index", "data"], 4, 110);
  b += cylinder(1424, 350, 108, 230, "DB", ["encrypted", "ciphertext", "indexed DAEAD"], 3);

  b += path("M320,225 L474,214", "arrow", "M320,225 L398,225 L398,214 L474,214");
  b += path("M320,401 L474,352", "arrow", "M320,401 L398,401 L398,352 L474,352");
  b += path("M320,577 L474,490", "runtimeUse", "M320,577 L398,577 L398,490 L474,490");
  b += path("M706,214 L864,210", "codecLine", "M706,214 L784,214 L784,210 L864,210");
  b += path("M706,352 L864,322", "codecLine", "M706,352 L784,352 L784,322 L864,322");
  b += path("M706,490 L864,434", "runtimeUse", "M706,490 L784,490 L784,434 L864,434");
  b += path("M706,628 L864,434", "runtimeUse", "M706,628 L784,628 L784,434 L864,434");
  b += path("M1096,210 L1228,233", "mapLine", "M1096,210 L1168,210 L1168,233 L1228,233");
  b += path("M1096,322 L1228,422", "mapLine", "M1096,322 L1168,322 L1168,422 L1228,422");
  b += path("M1096,584 L1228,603", "runtimeUse", "M1096,584 L1168,584 L1168,603 L1228,603");
  b += path("M1368,233 L1424,430", "dbUse", "M1368,233 L1402,233 L1402,430 L1424,430");
  b += path("M1368,422 L1424,464", "dbUse", "M1368,422 L1402,422 L1402,464 L1424,464");
  b += path("M1368,603 L1478,580", "dbUse", "M1368,603 L1402,603 L1402,624 L1478,624 L1478,580");
  b += note(124, 850, 1206, "Source check: AEAD uses AES-GCM/ChaCha20 for transparent CRUD; DAEAD uses AES-SIV so indexed equality searches remain possible.");
  return b;
}

function tinkClass() {
  let b = "";
  b += panel(58, 130, 660, 690, "Test classes");
  b += umlBox(252, 158, 240, 96, "AbstractExposedTest", ["ENABLE_DIALECTS_METHOD"], ["withTables(testDB, ...)"], 7);
  b += umlBox(108, 384, 238, 126, "TinkColumnTypeTest", ["AEAD CRUD", "DAEAD WHERE", "nullable/update", "multi algorithm"], [], 0);
  b += umlBox(430, 384, 238, 126, "TinkColumnTypeDaoTest", ["DAO DAEAD save", "DSL/DAO search", "disabled binary DAO search"], [], 4);
  b += generalizationTree(372, 254, 320, 384, [227, 549]);

  b += panel(782, 130, 720, 690, "Tables and DAO");
  b += umlBox(910, 158, 206, 92, "IntIdTable", ["id: EntityID<Int>"], ["table base"], 1);
  b += umlBox(838, 336, 214, 112, "AEAD anonymous tables", ["tink_aead_table", "tink_aead_update_table", "tink_multi_algo_table"], [], 2);
  b += umlBox(1084, 336, 214, 112, "DAEAD anonymous tables", ["tink_daead_table", "tink_nullable_table"], [], 1);
  b += umlBox(1330, 336, 130, 112, "T1", ["secret index", "data"], [], 4);
  b += generalizationTree(1013, 250, 296, 336, [945, 1191, 1395]);
  b += umlBox(1292, 466, 206, 92, "IntEntity", ["EntityID<Int>"], ["DAO base"], 7);
  b += umlBox(1330, 616, 130, 104, "E1", ["secret", "data"], ["toString"], 5);
  b += generalizationTree(1395, 558, 586, 616, [1395]);
  b += path("M1460,392 L1512,392 L1512,668 L1460,668", "mapLine");

  b += panel(108, 842, 1290, 76, "Column extension functions");
  b += card(142, 862, 184, 38, "tinkAeadVarChar", 0);
  b += card(346, 862, 184, 38, "tinkAeadBinary", 2);
  b += card(550, 862, 184, 38, "tinkAeadBlob", 6);
  b += card(782, 862, 184, 38, "tinkDaeadVarChar", 1);
  b += card(986, 862, 184, 38, "tinkDaeadBinary", 4);
  b += card(1190, 862, 184, 38, "tinkDaeadBlob", 5);
  b += path("M227,510 L945,448", "runtimeUse", "M227,510 L227,800 L945,800 L945,448");
  b += path("M549,510 L1395,616", "runtimeUse", "M549,510 L549,780 L1280,780 L1280,668 L1330,668");
  return b;
}

function shell(diagram) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${diagram.width} ${diagram.height}" width="${diagram.width}" height="${diagram.height}" role="img" aria-label="${esc(diagram.title)}">
<defs>
<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
<marker id="arrowBlue" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#2563eb"/></marker>
<marker id="arrowGreen" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#16a34a"/></marker>
<marker id="arrowPink" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#db2777"/></marker>
<marker id="arrowOrange" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#ea580c"/></marker>
<marker id="arrowPurple" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#7c3aed"/></marker>
<style>${style}</style>
</defs>
<rect width="${diagram.width}" height="${diagram.height}" class="canvas"/>
<rect x="20" y="20" width="${diagram.width - 40}" height="${diagram.height - 40}" rx="16" class="frame"/>
<text x="48" y="58" class="title">${esc(diagram.title)}</text>
<text x="48" y="80" class="subtitle">${esc(diagram.subtitle)}</text>
${diagram.body()}</svg>\n`;
}

function panel(x, y, w, h, title) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" class="panel"/><text x="${x + w / 2}" y="${y - 8}" class="panelTitle" text-anchor="middle">${esc(title.toUpperCase())}</text>\n`;
}

function card(x, y, w, h, title, c, detail = "") {
  const [fill, stroke] = colors[c % colors.length];
  const titleLines = splitTitle(title, Math.max(9, Math.floor(w / 10)), 2);
  const shownDetail = titleLines.length > 1 && h < 62 ? "" : detail;
  const titleY = y + (shownDetail ? 25 : h / 2 - (titleLines.length - 1) * 10 + 6);
  let out = `<desc>${esc(title)}</desc><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${titleY + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  if (shownDetail) out += `<text x="${x + w / 2}" y="${y + h - 14}" class="detail" text-anchor="middle">${esc(shownDetail)}</text>\n`;
  return out;
}

function umlBox(x, y, w, h, title, fields, methods, c) {
  const [fill, stroke] = colors[c % colors.length];
  const titleLines = splitTitle(title, Math.max(10, Math.floor(w / 9)), 2);
  const headerH = titleLines.length > 1 ? 56 : 40;
  const split1 = y + headerH;
  const bodyH = h - headerH;
  const hasMethods = methods.length > 0;
  const fieldH = hasMethods ? Math.max(34, Math.floor(bodyH * 0.58)) : bodyH;
  const methodH = hasMethods ? bodyH - fieldH : 0;
  const split2 = split1 + fieldH;
  let out = `<desc>${esc(title)}</desc>
<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${split1} L${x},${y + 4} Q${x},${y} ${x + 4},${y} L${x + w - 4},${y} Q${x + w},${y} ${x + w},${y + 4} L${x + w},${split1} Z" fill="${fill}"/>
<line x1="${x}" y1="${split1}" x2="${x + w}" y2="${split1}" stroke="${stroke}" stroke-width="1.2"/>\n`;
  const firstTitleY = titleLines.length > 1 ? y + 22 : y + 26;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${firstTitleY + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  if (hasMethods) out += `<line x1="${x}" y1="${split2}" x2="${x + w}" y2="${split2}" stroke="${stroke}" stroke-width="1.2"/>\n`;
  fields.forEach((row, i) => (out += `<text x="${x + 16}" y="${centeredRowY(split1, fieldH, fields.length, i)}" class="tiny">${esc(row)}</text>\n`));
  methods.forEach((row, i) => (out += `<text x="${x + 16}" y="${centeredRowY(split2, methodH, methods.length, i)}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function erdTable(x, y, w, title, rows, c, h = 170) {
  const [fill, stroke] = colors[c % colors.length];
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + 42} L${x},${y + 10} Q${x},${y} ${x + 10},${y} L${x + w - 10},${y} Q${x + w},${y} ${x + w},${y + 10} L${x + w},${y + 42} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 28}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 16}" y="${y + 68 + i * 18}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function cylinder(x, y, w, h, title, rows, c) {
  const [fill, stroke] = colors[c % colors.length];
  const capH = 34;
  let out = `<path d="M${x},${y + capH / 2} C${x},${y - 4} ${x + w},${y - 4} ${x + w},${y + capH / 2} L${x + w},${y + h - capH / 2} C${x + w},${y + h + 4} ${x},${y + h + 4} ${x},${y + h - capH / 2} Z" fill="#fff" stroke="${stroke}" stroke-width="1.7" class="card"/>
<ellipse cx="${x + w / 2}" cy="${y + capH / 2}" rx="${w / 2}" ry="${capH / 2}" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<path d="M${x},${y + h - capH / 2} C${x},${y + h + 4} ${x + w},${y + h + 4} ${x + w},${y + h - capH / 2}" fill="none" stroke="${stroke}" stroke-width="1.7"/>
<text x="${x + w / 2}" y="${y + 52}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 14}" y="${y + 86 + i * 18}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function generalizationTree(parentX, parentBottomY, busY, childTopY, childXs) {
  const baseY = parentBottomY + 16;
  let out = `${hollowTriangle(parentX, parentBottomY)}<path d="M${parentX},${baseY} L${parentX},${busY}" class="inheritStem"/>\n`;
  out += `<path d="M${Math.min(...childXs)},${busY} L${Math.max(...childXs)},${busY}" class="inheritStem"/>\n`;
  for (const childX of childXs) out += `<path d="M${childX},${busY} L${childX},${childTopY}" class="inheritStem"/>\n`;
  return out;
}

function hollowTriangle(parentX, parentBottomY) {
  const baseY = parentBottomY + 16;
  return `<polygon points="${parentX},${parentBottomY} ${parentX - 9},${baseY} ${parentX + 9},${baseY}" fill="#fff" stroke="#475569" stroke-width="1.8"/>\n`;
}

function path(defaultD, cls, explicitD = null) {
  return `<path d="${explicitD || defaultD}" class="${cls}"/>\n`;
}

function note(x, y, w, text) {
  return `<rect x="${x}" y="${y}" width="${w}" height="52" rx="10" fill="#ecfdf5" stroke="#86efac" stroke-width="1.3"/>
<text x="${x + w / 2}" y="${y + 31}" class="detail" text-anchor="middle">${esc(text)}</text>\n`;
}

function centeredRowY(top, height, count, index) {
  const step = 18;
  const blockH = count * step;
  return top + (height - blockH) / 2 + 13 + index * step;
}

function splitTitle(value, max, lines) {
  const text = String(value);
  if (text.includes(" ")) return wrap(text, max, lines);
  if (text.length <= max) return [text];
  const parts = text.match(/[A-Z]+(?=[A-Z][a-z]|\d|$)|[A-Z]?[a-z]+|\d+/g) || [text];
  const out = [];
  let line = "";
  for (const part of parts) {
    const next = `${line}${part}`;
    if (next.length > max && line) {
      out.push(line);
      line = part;
    } else {
      line = next;
    }
  }
  if (line) out.push(line);
  return out.slice(0, lines);
}

function wrap(value, max, lines) {
  const words = String(value).split(/\s+/);
  const out = [];
  let line = "";
  for (const word of words) {
    const next = line ? `${line} ${word}` : word;
    if (next.length > max && line) {
      out.push(line);
      line = word;
    } else {
      line = next;
    }
  }
  if (line) out.push(line);
  return out.slice(0, lines);
}

function esc(value) {
  return String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function validateBatch13Semantics() {
  const [fastjson, jackson3, tinkArch, tinkClassSvg] = diagrams.map((d) => fs.readFileSync(d.file, "utf8"));
  const failures = [];
  for (const required of ["FastjsonColumnTest", "FastjsonBColumnTest", "FastjsonSchema", "FastjsonSerializer", "fastjson_column JSON", "Fastjson arrays"]) {
    if (!fastjson.includes(required)) failures.push(`fastjson missing ${required}`);
  }
  for (const required of ["JacksonColumnTest", "JacksonBColumnTest", "JacksonSchema", "DefaultJacksonSerializer", "jackson_column JSON", "Jackson arrays"]) {
    if (!jackson3.includes(required)) failures.push(`jackson3 missing ${required}`);
  }
  for (const required of ["TinkColumnTypeTest", "TinkColumnTypeDaoTest", "AEAD columns", "DAEAD columns", "AES256_GCM", "AES256_SIV", "indexed DAEAD"]) {
    if (!tinkArch.includes(required)) failures.push(`tink architecture missing ${required}`);
  }
  for (const required of ["AbstractExposedTest", "IntIdTable", "IntEntity", "T1", "E1", "tinkAeadVarChar", "tinkDaeadBinary"]) {
    if (!tinkClassSvg.includes(required)) failures.push(`tink class missing ${required}`);
  }
  if (failures.length) throw new Error(`batch13_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch13_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|mapLine|codecLine|dbUse|runtimeUse|inheritStem)";
  const failures = [];
  for (const diagram of diagrams) {
    const svg = fs.readFileSync(diagram.file, "utf8");
    for (const match of svg.matchAll(new RegExp(`<path\\b([^>]*class="${connectorClasses}"[^>]*)`, "g"))) {
      const pathD = (match[1].match(/d="([^"]+)"/) || [])[1];
      if (!pathD) continue;
      const points = [...pathD.matchAll(/[ML]([\d.]+),([\d.]+)/g)].map((m) => ({ x: Number(m[1]), y: Number(m[2]) }));
      if (points.length < 2) continue;
      for (let i = 0; i < points.length - 1; i++) {
        const a = points[i];
        const b = points[i + 1];
        if (a.x !== b.x && a.y !== b.y) failures.push(`${diagram.file}: diagonal connector ${a.x},${a.y}->${b.x},${b.y}`);
      }
    }
  }
  if (failures.length) throw new Error(`batch13_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch13_connectors=ok");
}
