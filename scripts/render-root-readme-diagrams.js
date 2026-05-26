#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const DOT = process.env.DOT || "dot";
const RSVG = process.env.RSVG_CONVERT || "rsvg-convert";
const diagramDir = "docs/assets/readme-diagrams";
const chartDir = "docs/assets/readme-charts";
const sketchDir = ".omx/artifacts/diagram-sketches";

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

const groups = [
  ["00-shared", "Shared foundation", "common test fixtures", "00-shared"],
  ["01-spring-boot", "Spring Boot basics", "MVC + WebFlux entrypoints", "01-spring-boot"],
  ["02-alternatives-to-jpa", "JPA alternatives", "Hibernate Reactive, R2DBC, Vert.x", "02-alternatives-to-jpa"],
  ["03-exposed-basic", "Exposed basics", "DAO and SQL DSL", "03-exposed-basic"],
  ["04-exposed-ddl", "DDL", "connection and schema definition", "04-exposed-ddl"],
  ["05-exposed-dml", "DML", "queries, functions, transactions, entities", "05-exposed-dml"],
  ["06-advanced", "Advanced columns", "crypt, time, JSON, money, custom types", "06-advanced"],
  ["07-jpa", "JPA migration", "basic and advanced mapping", "07-jpa"],
  ["08-coroutines", "Async runtime", "coroutines and virtual threads", "08-coroutines"],
  ["09-spring", "Spring integration", "auto-config, transactions, cache, repository", "09-spring"],
  ["10-multi-tenant", "Multi-tenancy", "tenant context, schema, database routing", "10-multi-tenant"],
  ["11-high-performance", "High performance", "cache and routing datasource patterns", "11-high-performance"],
  ["12-production-integration", "Production integration", "Ktor/Spring auth, outbox, observability", "12-production-integration"],
];

const learning = [
  ["Basics", "Spring Boot + Exposed integration"],
  ["Alternatives", "JPA alternatives comparison"],
  ["Exposed Basics", "DSL and DAO patterns"],
  ["DDL / DML", "schema and data manipulation"],
  ["Advanced", "encryption, JSON, custom types"],
  ["JPA Migration", "convert JPA mappings to Exposed"],
  ["Async", "coroutines and virtual threads"],
  ["Spring Integration", "transactions, cache, repository"],
  ["Multi-Tenancy", "tenant-aware architecture"],
  ["High Performance", "cache and routing datasource"],
];

const features = [
  ["Type Safety", "compile-time SQL shape checks"],
  ["DSL and DAO", "SQL-style and ORM-style access"],
  ["Coroutines", "suspend transactions and async examples"],
  ["Lightweight", "small persistence layer compared with JPA"],
  ["Multi-DB Tests", "H2, PostgreSQL, MySQL, MariaDB"],
  ["Spring/Ktor", "MVC, WebFlux, Ktor production examples"],
  ["Advanced Types", "JSON, money, crypt, Tink, custom columns"],
  ["Production Patterns", "multi-tenancy, cache, outbox, readiness"],
];

const routes = writeGraphvizEvidence();

renderAll();

function renderAll() {
  fs.mkdirSync(diagramDir, { recursive: true });
  fs.mkdirSync(chartDir, { recursive: true });

  const diagrams = [
    ["exposed-workshop-mindmap-01.svg", 1460, 860, "Kotlin Exposed Feature Map", "README feature table plus current workshop modules", renderFeatureMap],
    ["exposed-workshop-architecture-01.svg", 1460, 800, "Exposed API Structure", "How Kotlin code uses Exposed DSL/DAO APIs before database access", renderApiStructure],
    ["root-readme-overview-01.svg", 1500, 900, "Exposed Workshop Overview", "Runnable modules progress from foundations to production integration", renderOverview],
    ["exposed-workshop-architecture-02.svg", 1500, 900, "Recommended Learning Path", "The ten README learning-guide steps, rendered as a source-backed journey", renderLearningPath],
    ["exposed-workshop-architecture-03.svg", 1500, 960, "Module Structure", "Actual top-level workshop groups and current submodule counts", renderModuleStructure],
  ];

  for (const [file, width, height, title, subtitle, render] of diagrams) {
    const svgPath = path.join(diagramDir, file);
    const svg = shell(width, height, title, subtitle, render());
    assertSvgContent(svg, title);
    fs.writeFileSync(svgPath, svg);
    renderPng(svgPath);
    console.log(svgPath.replace(/\.svg$/, ".png"));
  }

  const chartSvg = path.join(chartDir, "root-readme-module-chart-01.svg");
  fs.writeFileSync(chartSvg, shell(1500, 820, "Exposed Workshop Module Composition", "Actual submodule counts from the checked-out source tree", renderModuleChart()));
  renderPng(chartSvg);
  console.log(chartSvg.replace(/\.svg$/, ".png"));
}

function renderFeatureMap() {
  let b = "";
  b += `<rect x="555" y="178" width="350" height="584" rx="16" fill="#ccfbf1" stroke="#0f766e" class="card"/>`;
  const positions = [
    [90, 160], [90, 330], [90, 500], [90, 670],
    [1020, 160], [1020, 330], [1020, 500], [1020, 670],
  ];
  features.forEach(([name, detail], index) => {
    const [x, y] = positions[index];
    b += card(x, y, 350, 92, name, [detail], index);
    const from = index < 4 ? `${name.replace(/\W+/g, "")}` : "ExposedWorkshop";
    const to = index < 4 ? "ExposedWorkshop" : `${name.replace(/\W+/g, "")}`;
    const sx = index < 4 ? x + 350 : 905;
    const sy = y + 46;
    const ex = index < 4 ? 555 : x;
    const pathData = `M${sx},${sy} L${ex},${sy}`;
    b += routed("root-feature-map", from, to, pathData, "mapLine");
  });
  b += `<text x="730" y="434" class="title" text-anchor="middle">Exposed Workshop</text>`;
  b += `<text x="730" y="464" class="detail" text-anchor="middle">type-safe SQL learning</text>`;
  b += `<text x="730" y="482" class="detail" text-anchor="middle">runnable, test-backed modules</text>`;
  b += note(470, 110, 520, "This is a feature map, not an architecture pipeline. It mirrors the README key features and current module families.");
  return b;
}

function renderApiStructure() {
  let b = "";
  b += panel(70, 136, 260, 540, "Kotlin application");
  b += card(104, 190, 190, 76, "Application Code", ["controllers, services, tests"], 0);
  b += card(104, 342, 190, 76, "Domain Model", ["records, entities, DTOs"], 1);

  b += panel(430, 118, 440, 576, "Exposed API surface");
  b += card(470, 190, 160, 76, "SQL DSL", ["Table", "select / insert / update"], 2);
  b += card(470, 342, 160, 76, "DAO API", ["Entity", "EntityClass / IdTable"], 3);
  b += card(470, 494, 160, 76, "Column Types", ["JSON, time, money", "custom transforms"], 5);
  b += card(690, 342, 160, 76, "Transactions", ["transaction", "suspend transaction"], 4);

  b += panel(970, 136, 390, 540, "Runtime and databases");
  b += card(1010, 190, 135, 76, "Spring MVC", ["blocking JDBC"], 6);
  b += card(1010, 342, 135, 76, "WebFlux/Ktor", ["coroutines"], 7);
  b += card(1010, 494, 135, 76, "Test Infra", ["H2, PostgreSQL", "MySQL, MariaDB"], 0);
  b += cylinder(1220, 326, 132, 108, "Database", ["JDBC SQL", "execution"], 3);

  b += routed("root-api-structure", "application", "sqlDsl", "M294,228 L470,228", "arrow");
  b += routed("root-api-structure", "application", "daoApi", "M294,228 L382,228 L382,380 L470,380", "arrow");
  b += routed("root-api-structure", "domainModel", "daoApi", "M294,380 L470,380", "mapLine");
  b += routed("root-api-structure", "sqlDsl", "transactions", "M630,228 L670,228 L670,380 L690,380", "arrow");
  b += routed("root-api-structure", "daoApi", "transactions", "M630,380 L690,380", "arrow");
  b += routed("root-api-structure", "columnTypes", "transactions", "M630,532 L770,532 L770,418", "mapLine");
  b += routed("root-api-structure", "transactions", "spring", "M850,380 L920,380 L920,228 L1010,228", "context");
  b += routed("root-api-structure", "transactions", "asyncRuntime", "M850,380 L1010,380", "context");
  b += routed("root-api-structure", "spring", "database", "M1145,228 L1188,228 L1188,380 L1220,380", "storage");
  b += routed("root-api-structure", "asyncRuntime", "database", "M1145,380 L1220,380", "storage");
  b += routed("root-api-structure", "testInfra", "database", "M1145,532 L1188,532 L1188,388 L1220,388", "storage");
  return b;
}

function renderOverview() {
  let b = "";
  b += panel(60, 135, 330, 610, "Foundation");
  b += card(102, 205, 245, 82, "00 Shared", ["common test resources", `${count("00-shared")} submodule`], 0);
  b += card(102, 345, 245, 82, "Multi-DB Tests", ["H2, PostgreSQL", "MySQL, MariaDB"], 1);
  b += cylinder(135, 505, 180, 100, "Databases", ["JDBC-backed examples"], 3);

  b += panel(455, 120, 430, 640, "Learning modules");
  b += card(500, 208, 330, 76, "01-05 Core Path", ["Spring Boot, alternatives, DSL/DAO, DDL, DML"], 2);
  b += card(500, 348, 330, 76, "06-08 Advanced Path", ["column features, JPA migration, async runtimes"], 4);
  b += card(500, 508, 330, 76, "09-12 Production Path", ["Spring, tenant isolation, performance, Ktor/Spring apps"], 5);

  b += panel(950, 135, 430, 610, "Outcome");
  b += card(998, 208, 330, 76, "Runnable Examples", ["each module has README and tests"], 6);
  b += card(998, 348, 330, 76, "Integration Patterns", ["transaction, cache, repository, outbox"], 7);
  b += card(998, 508, 330, 76, "Production Readiness", ["auth/session, observability, tenant onboarding"], 0);

  b += routed("root-overview", "shared", "core", "M347,246 L500,246", "arrow");
  b += routed("root-overview", "shared", "advanced", "M347,246 L500,386", "arrow", "M347,246 L422,246 L422,386 L500,386");
  b += routed("root-overview", "database", "core", "M315,555 L500,246", "storage", "M315,555 L422,555 L422,246 L500,246");
  b += routed("root-overview", "core", "advanced", "M665,284 L665,348", "arrow");
  b += routed("root-overview", "advanced", "production", "M665,424 L665,508", "arrow");
  b += routed("root-overview", "core", "examples", "M830,246 L998,246", "arrow");
  b += routed("root-overview", "advanced", "patterns", "M830,386 L998,386", "arrow");
  b += routed("root-overview", "production", "readiness", "M830,546 L998,546", "arrow");
  b += note(120, 800, 1260, "Root README overview: this diagram summarizes the repository purpose, not one example app. Module families map directly to the README module list.");
  return b;
}

function renderLearningPath() {
  let b = "";
  const positions = [
    [80, 156], [360, 156], [640, 156], [920, 156], [1200, 156],
    [1200, 490], [920, 490], [640, 490], [360, 490], [80, 490],
  ];
  learning.forEach(([name, detail], index) => {
    const [x, y] = positions[index];
    b += stepCard(x, y, 220, 132, index + 1, name, detail, index);
  });
  for (let i = 0; i < 4; i += 1) {
    b += routed("root-learning-path", `step${i + 1}`, `step${i + 2}`, `M${positions[i][0] + 220},${positions[i][1] + 66} L${positions[i + 1][0]},${positions[i + 1][1] + 66}`, "arrow");
  }
  b += routed("root-learning-path", "step5", "step6", "M1310,288 L1310,490", "arrow");
  for (let i = 5; i < 9; i += 1) {
    b += routed("root-learning-path", `step${i + 1}`, `step${i + 2}`, `M${positions[i][0]},${positions[i][1] + 66} L${positions[i + 1][0] + 220},${positions[i + 1][1] + 66}`, "arrow");
  }
  b += note(180, 735, 1140, "The order follows the README Learning Guide exactly: basics first, then Exposed core APIs, advanced topics, integration, tenancy, and performance.");
  return b;
}

function renderModuleStructure() {
  let b = "";
  const rows = [
    ["Foundation", [0], 88],
    ["Core learning path", [1, 2, 3, 4, 5], 242],
    ["Advanced and integration", [6, 7, 8, 9], 452],
    ["Production patterns", [10, 11, 12], 662],
  ];
  rows.forEach(([label, indexes, y]) => {
    b += panel(55, y, 1390, indexes.length === 1 ? 118 : 162, label);
    const gap = 26;
    const w = Math.floor((1290 - gap * (indexes.length - 1)) / indexes.length);
    indexes.forEach((idx, pos) => {
      const [dir, name, detail] = groups[idx];
      const x = 105 + pos * (w + gap);
      const cardY = indexes.length === 1 ? y + 15 : y + 36;
      b += moduleCard(x, cardY, w, 88, dir, name, detail, count(dir), idx);
    });
  });
  b += note(190, 860, 1120, "Counts come from current source directories with build.gradle.kts; numbering follows the checked-out module folders.");
  return b;
}

function renderModuleChart() {
  const max = Math.max(...groups.map(([dir]) => count(dir)));
  let b = "";
  groups.forEach(([dir, name], index) => {
    const y = 126 + index * 46;
    const value = count(dir);
    const barW = Math.round((value / max) * 650);
    const [fill, stroke] = colors[index % colors.length];
    b += `<text x="84" y="${y + 23}" class="smallLabel">${esc(shortDir(dir))}</text>`;
    b += `<text x="365" y="${y + 23}" class="detail">${esc(name)}</text>`;
    b += `<rect x="560" y="${y}" width="650" height="30" rx="8" fill="#f8fafc" stroke="#d7e2ec"/>`;
    b += `<rect x="560" y="${y}" width="${barW}" height="30" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.4"/>`;
    b += `<text x="${1195}" y="${y + 22}" class="label" text-anchor="middle">${value}</text>`;
  });
  b += note(94, 744, 1300, "Source check: counts are current subdirectories that contain build.gradle.kts, excluding buildSrc, .worktrees, and generated build outputs.");
  return b;
}

function writeGraphvizEvidence() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "root-feature-map": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      TypeSafety -> ExposedWorkshop;
      DSLandDAO -> ExposedWorkshop;
      Coroutines -> ExposedWorkshop;
      Lightweight -> ExposedWorkshop;
      ExposedWorkshop -> MultiDBTests;
      ExposedWorkshop -> SpringKtor;
      ExposedWorkshop -> AdvancedTypes;
      ExposedWorkshop -> ProductionPatterns;
    }`,
    "root-api-structure": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.6, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      application -> sqlDsl;
      application -> daoApi;
      domainModel -> daoApi;
      sqlDsl -> transactions;
      daoApi -> transactions;
      columnTypes -> transactions;
      transactions -> spring;
      transactions -> asyncRuntime;
      spring -> database;
      asyncRuntime -> database;
      testInfra -> database;
    }`,
    "root-overview": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      shared -> core;
      shared -> advanced;
      database -> core;
      core -> advanced -> production;
      core -> examples;
      advanced -> patterns;
      production -> readiness;
    }`,
    "root-learning-path": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      step1 -> step2 -> step3 -> step4 -> step5 -> step6 -> step7 -> step8 -> step9 -> step10;
    }`,
    "root-module-structure": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.55, ranksep=0.75, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      shared;
      springBoot; alternatives; exposedBasic; ddl; dml;
      advanced; jpa; coroutines; spring;
      multiTenant; highPerformance; productionIntegration;
      {rank=same; springBoot; alternatives; exposedBasic; ddl; dml;}
      {rank=same; advanced; jpa; coroutines; spring;}
      {rank=same; multiTenant; highPerformance; productionIntegration;}
      shared -> springBoot [style=invis];
      dml -> advanced [style=invis];
      spring -> multiTenant [style=invis];
    }`,
  };
  const parsed = new Map();
  const evidence = {};
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = path.join(sketchDir, `${name}.dot`);
    fs.writeFileSync(dotPath, dot);
    const plain = spawnSync(DOT, ["-Tplain", "-o", path.join(sketchDir, `${name}.plain`), dotPath], { encoding: "utf8" });
    if (plain.status !== 0) throw new Error(plain.stderr || plain.stdout);
    const svg = spawnSync(DOT, ["-Tsvg", "-o", path.join(sketchDir, `${name}.svg`), dotPath], { encoding: "utf8" });
    if (svg.status !== 0) throw new Error(svg.stderr || svg.stdout);
    const routes = parsePlain(fs.readFileSync(path.join(sketchDir, `${name}.plain`), "utf8"));
    parsed.set(name, routes);
    evidence[name] = routes.map((route) => ({ from: route.from, to: route.to, points: route.points }));
  }
  fs.writeFileSync(path.join(sketchDir, "root-readme-routing-evidence.json"), `${JSON.stringify(evidence, null, 2)}\n`);
  return parsed;
}

function parsePlain(text) {
  return text
    .split(/\r?\n/)
    .filter((line) => line.startsWith("edge "))
    .map((line) => {
      const parts = line.split(/\s+/);
      const count = Number(parts[3]);
      const nums = parts.slice(4, 4 + count * 2).map(Number);
      const points = [];
      for (let i = 0; i < nums.length; i += 2) points.push([nums[i], nums[i + 1]]);
      return { from: parts[1], to: parts[2], points };
    });
}

function routed(sketch, from, to, d, klass, fallback) {
  const route = routes.get(sketch)?.some((item) => item.from === from && item.to === to);
  if (!route) throw new Error(`Missing Graphviz route evidence: ${sketch} ${from} -> ${to}`);
  return `<path d="${fallback || d}" class="${klass}"/>`;
}

function shell(width, height, title, subtitle, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" width="${width}" height="${height}" role="img" aria-label="${esc(title)}">
  <defs>
    <filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
    <marker id="arrowBlue" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#2563eb"/></marker>
    <marker id="arrowGreen" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#16a34a"/></marker>
    <marker id="arrowPurple" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#7c3aed"/></marker>
    <marker id="arrowOrange" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#ea580c"/></marker>
    <marker id="arrowGray" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#64748b"/></marker>
    <style>
      .canvas{fill:#f6f9fc}.frame{fill:#fff;stroke:#d7e2ec;stroke-width:1.5}.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
      .card,.db{stroke-width:1.7;filter:url(#shadow)}.title{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:32px;fill:#1e293b}
      .subtitle,.detail,.tiny{font-family:"Comic Mono","Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;fill:#536476}.subtitle{font-size:13px}.detail{font-size:12px}.tiny{font-size:10px;fill:#64748b}
      .label{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:18px;fill:#1e293b}.smallLabel{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:15px;fill:#1e293b}
      .panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
      .arrow{fill:none;stroke:#2563eb;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}
      .mapLine{fill:none;stroke:#16a34a;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
      .context{fill:none;stroke:#7c3aed;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
      .storage{fill:none;stroke:#ea580c;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
      .softArrow{fill:none;stroke:#64748b;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:7 6;marker-end:url(#arrowGray)}
    </style>
  </defs>
  <rect width="${width}" height="${height}" class="canvas"/>
  <rect x="20" y="20" width="${width - 40}" height="${height - 40}" rx="16" class="frame"/>
  <text x="54" y="62" class="title">${esc(title)}</text>
  <text x="54" y="86" class="subtitle">${esc(subtitle)}</text>
  ${body}
</svg>
`;
}

function card(x, y, w, h, title, lines, colorIndex) {
  const [fill, stroke] = colors[colorIndex % colors.length];
  const lineStart = lines.length === 1 ? y + h / 2 + 18 : y + h / 2 + 8;
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="9" fill="${fill}" stroke="${stroke}" class="card"/>
  <text x="${x + w / 2}" y="${y + h / 2 - 8}" class="label" text-anchor="middle">${esc(trim(title, Math.floor(w / 9)))}</text>
  ${lines.map((line, i) => `<text x="${x + w / 2}" y="${lineStart + i * 15}" class="detail" text-anchor="middle">${esc(trim(line, Math.floor(w / 7)))}</text>`).join("\n")}`;
}

function titleCard(x, y, w, h, title, lines, colorIndex) {
  const [fill, stroke] = colors[colorIndex % colors.length];
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="16" fill="${fill}" stroke="${stroke}" class="card"/>
  <text x="${x + w / 2}" y="${y + 54}" class="title" text-anchor="middle">${esc(title)}</text>
  ${lines.map((line, i) => `<text x="${x + w / 2}" y="${y + 84 + i * 18}" class="detail" text-anchor="middle">${esc(line)}</text>`).join("\n")}`;
}

function stepCard(x, y, w, h, number, title, detail, colorIndex) {
  const [fill, stroke] = colors[colorIndex % colors.length];
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>
  <circle cx="${x + 28}" cy="${y + 30}" r="17" fill="#fff" stroke="${stroke}" stroke-width="1.4"/>
  <text x="${x + 28}" y="${y + 36}" class="label" text-anchor="middle">${number}</text>
  <text x="${x + 132}" y="${y + 44}" class="label" text-anchor="middle">${esc(trim(title, 20))}</text>
  ${wrap(detail, 26).map((line, i) => `<text x="${x + w / 2}" y="${y + 82 + i * 16}" class="detail" text-anchor="middle">${esc(line)}</text>`).join("\n")}`;
}

function moduleCard(x, y, w, h, dir, name, detail, countValue, colorIndex) {
  const [fill, stroke] = colors[colorIndex % colors.length];
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="9" fill="${fill}" stroke="${stroke}" class="card"/>
  <text x="${x + 18}" y="${y + 26}" class="tiny">${esc(dir)}</text>
  <text x="${x + w / 2}" y="${y + 44}" class="label" text-anchor="middle">${esc(trim(name, Math.floor(w / 9)))}</text>
  <text x="${x + w / 2}" y="${y + 64}" class="detail" text-anchor="middle">${esc(trim(detail, Math.floor(w / 7)))}</text>
  <text x="${x + w - 18}" y="${y + 28}" class="label" text-anchor="end">${countValue}</text>`;
}

function panel(x, y, w, h, title) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" class="panel"/>
  <text x="${x + w / 2}" y="${y - 8}" class="panelTitle" text-anchor="middle">${esc(title.toUpperCase())}</text>`;
}

function note(x, y, w, text) {
  return `<rect x="${x}" y="${y}" width="${w}" height="54" rx="10" fill="#f8fafc" stroke="#d7e2ec"/>
  ${wrap(text, Math.floor(w / 7)).map((line, i) => `<text x="${x + w / 2}" y="${y + 23 + i * 16}" class="detail" text-anchor="middle">${esc(line)}</text>`).join("\n")}`;
}

function cylinder(x, y, w, h, title, lines, colorIndex) {
  const [fill, stroke] = colors[colorIndex % colors.length];
  return `<path d="M${x},${y + 18} C${x},${y - 6} ${x + w},${y - 6} ${x + w},${y + 18} L${x + w},${y + h - 18} C${x + w},${y + h + 6} ${x},${y + h + 6} ${x},${y + h - 18} Z" fill="${fill}" stroke="${stroke}" class="db"/>
  <ellipse cx="${x + w / 2}" cy="${y + 18}" rx="${w / 2}" ry="20" fill="#fff" fill-opacity="0.38" stroke="${stroke}" stroke-width="1.4"/>
  <ellipse cx="${x + w / 2}" cy="${y + h - 18}" rx="${w / 2}" ry="20" fill="none" stroke="${stroke}" stroke-width="1.4"/>
  <text x="${x + w / 2}" y="${y + 50}" class="label" text-anchor="middle">${esc(title)}</text>
  ${lines.map((line, i) => `<text x="${x + w / 2}" y="${y + 70 + i * 15}" class="detail" text-anchor="middle">${esc(line)}</text>`).join("\n")}`;
}

function count(dir) {
  if (!fs.existsSync(dir)) return 0;
  return fs.readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .filter((entry) => fs.existsSync(path.join(dir, entry.name, "build.gradle.kts")))
    .length;
}

function shortDir(dir) {
  return dir.replace(/^0?/, "");
}

function renderPng(svgPath) {
  const pngPath = svgPath.replace(/\.svg$/, ".png");
  const result = spawnSync(RSVG, ["-f", "png", "-o", pngPath, svgPath], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout);
}

function assertSvgContent(svg, title) {
  if (!svg.includes('font-family:"Architects Daughter"')) throw new Error(`${title}: missing Architects Daughter font`);
  if (!svg.includes('font-family:"Comic Mono"')) throw new Error(`${title}: missing Comic Mono font`);
  if (svg.includes("Source-derived component flow")) throw new Error(`${title}: stale generic diagram text remains`);
}

function wrap(text, width) {
  const words = text.split(/\s+/);
  const lines = [];
  let line = "";
  for (const word of words) {
    const next = line ? `${line} ${word}` : word;
    if (next.length > width && line) {
      lines.push(line);
      line = word;
    } else {
      line = next;
    }
  }
  if (line) lines.push(line);
  return lines.slice(0, 2);
}

function trim(text, length) {
  return text.length > length ? `${text.slice(0, length - 1)}…` : text;
}

function esc(text) {
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
