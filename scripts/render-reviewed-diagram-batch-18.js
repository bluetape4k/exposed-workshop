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
.subtitle,.detail,.tiny{font-family:"Comic Mono","Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;fill:#536476}
.subtitle{font-size:13px}.detail{font-size:12px}.tiny{font-size:10px;fill:#64748b}
.label{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:16px;fill:#1e293b}
.className{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:17px;fill:#1e293b}
.panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
.card,.uml,.tableBox,.dbBody{stroke-width:1.7;filter:url(#shadow)}
.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
.arrow{fill:none;stroke:#2563eb;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}
.context{fill:none;stroke:#7c3aed;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.storage{fill:none;stroke:#ea580c;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
.mapLine{fill:none;stroke:#16a34a;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
.fk{fill:none;stroke:#0f766e;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowTeal)}
.returnLine{fill:none;stroke:#64748b;stroke-width:1.7;stroke-dasharray:7 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGray)}
.lifeline{stroke:#cbd5e1;stroke-width:1.4;stroke-dasharray:5 6}
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
    key: "batch-18-schema-layout",
    file: `${outDir}/10-multi-tenant-schema-layout-04.svg`,
    title: "Per-Tenant Schema Layout",
    subtitle: "Shared database uses korean/english schemas; database-per-tenant modules replace schema routing with tenant-owned pools and Database objects",
    width: 1500,
    height: 900,
    body: schemaLayout,
  },
  {
    key: "batch-18-tenant-architecture",
    file: `${outDir}/10-multi-tenant-architecture-01.svg`,
    title: "Multi-Tenant Isolation Architecture",
    subtitle: "Tenant identity is resolved at the edge, propagated through the runtime context, then applied as schema switching or database routing",
    width: 1500,
    height: 900,
    body: tenantArchitecture,
  },
  {
    key: "batch-18-module-comparison",
    file: `${outDir}/10-multi-tenant-class-02.svg`,
    title: "Multi-Tenant Module Implementation Comparison",
    subtitle: "Current source shows three context propagation styles, explicit transaction boundaries, database-per-tenant routing, and security-gated routing",
    width: 1500,
    height: 980,
    body: moduleComparison,
  },
  {
    key: "batch-18-request-flow",
    file: `${outDir}/10-multi-tenant-sequence-03.svg`,
    title: "Common Multi-Tenant Request Flow",
    subtitle: "Calls are solid, returns are dashed, and tenant context is bound before repository and Exposed database access",
    width: 1500,
    height: 940,
    body: requestFlow,
  },
];

let graphvizRoutes = new Map();
let currentDiagram = null;
let currentRects = [];
let currentPaths = [];

graphvizRoutes = writeGraphvizSketches();

for (const diagram of diagrams) {
  currentDiagram = diagram.file;
  currentRects = [];
  currentPaths = [];
  const svg = shell(diagram);
  validateConnectorGeometry(diagram.file, currentRects, currentPaths);
  fs.writeFileSync(diagram.file, svg);
  const png = diagram.file.replace(/\.svg$/, ".png");
  const result = spawnSync(RSVG, ["-f", "png", "-o", png, diagram.file], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout);
  console.log(png);
}

validateBatchSemantics();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-18-schema-layout": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      tenantKorean -> schemaKorean;
      tenantEnglish -> schemaEnglish;
      schemaKorean -> moviesK;
      schemaKorean -> actorsK;
      schemaKorean -> bridgeK;
      schemaEnglish -> moviesE;
      schemaEnglish -> actorsE;
      schemaEnglish -> bridgeE;
      bridgeK -> moviesK;
      bridgeK -> actorsK;
      bridgeE -> moviesE;
      bridgeE -> actorsE;
      dbPerTenant -> koreanPool -> koreanDb;
      dbPerTenant -> englishPool -> englishDb;
    }`,
    "batch-18-tenant-architecture": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      client -> tenantEdge;
      tenantEdge -> mvcContext -> schemaAspect -> repositories -> sharedSchemaDb;
      tenantEdge -> vtContext -> transactionSchemaAspect -> repositories;
      tenantEdge -> webfluxContext -> suspendedTransaction -> repositories;
      tenantEdge -> schemaTenantTransaction -> inventoryRepository -> sharedSchemaDb;
      tenantEdge -> securityFilter -> databaseRegistry -> tenantDatabases;
    }`,
    "batch-18-module-comparison": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.75, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      mvcFilter -> tenantSchemaAspect -> movieRepository -> sharedSchemaDb;
      vtFilter -> transactionSchemaAspect -> movieRepository;
      webfluxFilter -> suspendedTransaction -> movieRepository;
      schemaFilter -> tenantTransaction -> inventoryRepository -> sharedSchemaDb;
      securityFilter -> tenantDatabaseRegistry -> tenantDatabases;
      ktorPlugin -> threadContextElement -> ktorMovieRepository -> sharedSchemaDb;
    }`,
    "batch-18-request-flow": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.5, ranksep=0.8, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      client -> edgeFilter -> contextCarrier -> controller -> service -> transactionBoundary -> repository -> database;
      transactionBoundary -> schemaReset;
      database -> repository -> service -> controller -> client;
    }`,
  };

  const routes = new Map();
  const evidence = {};
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    const plain = spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
    if (plain.status !== 0) throw new Error(plain.stderr || plain.stdout);
    const svg = spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    if (svg.status !== 0) throw new Error(svg.stderr || svg.stdout);
    const parsed = parseGraphvizPlain(fs.readFileSync(`${sketchDir}/${name}.plain`, "utf8"));
    routes.set(name, parsed);
    evidence[name] = parsed.map((route) => ({
      from: route.from,
      to: route.to,
      points: route.points,
      firstLeg: route.firstLeg,
      lastLeg: route.lastLeg,
    }));
  }
  fs.writeFileSync(`${sketchDir}/batch-18-routing-evidence.json`, `${JSON.stringify(evidence, null, 2)}\n`);
  return routes;
}

function schemaLayout() {
  let b = "";
  b += panel(60, 130, 900, 640, "Shared database / separate schema");
  b += card(104, 190, 170, 58, "Tenant korean", 0, "X-TENANT-ID");
  b += card(104, 438, 170, 58, "Tenant english", 1, "X-TENANT-ID");
  b += cylinder(340, 258, 500, 420, "Shared DB instance", ["H2/PostgreSQL compatible", "schemas are the isolation boundary"], 3, false);
  b += schemaBox(390, 312, "korean schema", "Movie domain tables", 2);
  b += schemaBox(610, 504, "english schema", "same table set", 5);
  b += tableMini(402, 418, "movies", ["id PK", "name", "producer_name"], 0);
  b += tableMini(572, 418, "actors_in_movies", ["movie_id FK", "actor_id FK"], 6);
  b += tableMini(744, 418, "actors", ["id PK", "first_name", "last_name"], 4);
  b += tableMini(402, 612, "movies", ["id PK", "name", "release_date"], 0);
  b += tableMini(572, 612, "actors_in_movies", ["movie_id FK", "actor_id FK"], 6);
  b += tableMini(744, 612, "actors", ["id PK", "birthday"], 4);

  b += panel(1020, 130, 420, 640, "Database per tenant contrast");
  b += card(1068, 202, 160, 58, "TenantRegistry", 6, "validates config");
  b += card(1070, 338, 160, 58, "korean pool", 0, "HikariDataSource");
  b += card(1070, 510, 160, 58, "english pool", 1, "HikariDataSource");
  b += cylinder(1280, 292, 118, 150, "korean DB", ["Exposed", "Database"], 0);
  b += cylinder(1280, 476, 118, 150, "english DB", ["Exposed", "Database"], 1);

  b += gpath("batch-18-schema-layout", "tenantKorean", "schemaKorean", "M274,219 L390,352", "context", "M189,248 L189,352 L390,352");
  b += gpath("batch-18-schema-layout", "tenantEnglish", "schemaEnglish", "M274,467 L610,544", "context", "M189,496 L189,544 L610,544");
  b += gpath("batch-18-schema-layout", "schemaKorean", "moviesK", "M470,386 L470,418", "mapLine");
  b += gpath("batch-18-schema-layout", "schemaKorean", "bridgeK", "M540,374 L652,418", "mapLine", "M465,386 L465,402 L652,402 L652,418");
  b += gpath("batch-18-schema-layout", "schemaKorean", "actorsK", "M552,352 L804,418", "mapLine", "M515,386 L515,402 L804,402 L804,418");
  b += gpath("batch-18-schema-layout", "schemaEnglish", "moviesE", "M610,544 L484,612", "mapLine", "M650,578 L650,596 L484,596 L484,612");
  b += gpath("batch-18-schema-layout", "schemaEnglish", "bridgeE", "M685,578 L652,612", "mapLine", "M685,578 L685,600 L652,600 L652,612");
  b += gpath("batch-18-schema-layout", "schemaEnglish", "actorsE", "M760,544 L806,612", "mapLine", "M725,578 L725,596 L806,596 L806,612");
  b += gpath("batch-18-schema-layout", "bridgeK", "moviesK", "M572,458 L522,458", "fk");
  b += gpath("batch-18-schema-layout", "bridgeK", "actorsK", "M692,458 L744,458", "fk");
  b += gpath("batch-18-schema-layout", "bridgeE", "moviesE", "M572,652 L522,652", "fk");
  b += gpath("batch-18-schema-layout", "bridgeE", "actorsE", "M692,652 L744,652", "fk");
  b += gpath("batch-18-schema-layout", "dbPerTenant", "koreanPool", "M1148,260 L1148,338", "context");
  b += gpath("batch-18-schema-layout", "dbPerTenant", "englishPool", "M1148,260 L1148,510", "context", "M1148,260 L1148,278 L1018,278 L1018,539 L1070,539");
  b += gpath("batch-18-schema-layout", "koreanPool", "koreanDb", "M1230,367 L1280,367", "storage");
  b += gpath("batch-18-schema-layout", "englishPool", "englishDb", "M1230,539 L1280,551", "storage", "M1230,539 L1255,539 L1255,551 L1280,551");
  b += note(94, 802, 1290, "Source check: MovieSchema defines movies, actors, actors_in_movies for schema-based modules; TenantDatabaseRegistry creates one HikariDataSource and Exposed Database per configured tenant.");
  return b;
}

function tenantArchitecture() {
  let b = "";
  b += panel(60, 130, 260, 610, "Request edge");
  b += endpoint(104, 198, 170, 72, "HTTP Client", ["X-TENANT-ID", "JWT/API/session"]);
  b += card(104, 372, 170, 70, "Tenant edge", 1, "Filter / Plugin / Auth");
  b += card(104, 548, 170, 70, "OpenAPI docs", 5, "Swagger / SpringDoc");

  b += panel(390, 130, 360, 610, "Runtime propagation");
  b += card(432, 176, 270, 58, "Spring MVC", 0, "TenantContext ThreadLocal");
  b += card(432, 280, 270, 58, "Virtual Threads", 6, "ScopedValue + Tomcat VT");
  b += card(432, 384, 270, 58, "WebFlux", 5, "Reactor Context + TenantId");
  b += card(432, 488, 270, 58, "Ktor", 2, "ThreadContextElement");
  b += card(432, 592, 270, 58, "Security module", 4, "TenantAuthorizationFilter");

  b += panel(820, 130, 300, 610, "Transaction boundary");
  b += card(862, 176, 218, 58, "TenantSchemaAspect", 0, "DataSourceUtils SET SCHEMA");
  b += card(862, 280, 218, 58, "TransactionSchemaAspect", 6, "SchemaUtils in transaction");
  b += card(862, 384, 218, 58, "Suspended tx", 5, "newSuspendedTransaction");
  b += card(862, 488, 218, 58, "TenantTransaction", 2, "set schema + reset");
  b += card(862, 592, 218, 58, "DatabaseRegistry", 4, "databaseFor(tenant)");

  b += panel(1190, 130, 250, 610, "Storage isolation");
  b += cylinder(1238, 222, 150, 150, "Shared DB", ["korean", "english", "schemas"], 3);
  b += cylinder(1238, 504, 150, 150, "Tenant DBs", ["korean pool", "english pool"], 4);

  b += gpath("batch-18-tenant-architecture", "client", "tenantEdge", "M188,270 L188,372", "arrow");
  b += gpath("batch-18-tenant-architecture", "tenantEdge", "mvcContext", "M274,407 L432,205", "context", "M274,407 L348,407 L348,205 L432,205");
  b += gpath("batch-18-tenant-architecture", "tenantEdge", "vtContext", "M274,407 L432,309", "context", "M274,407 L362,407 L362,309 L432,309");
  b += gpath("batch-18-tenant-architecture", "tenantEdge", "webfluxContext", "M274,407 L432,413", "context", "M274,407 L350,407 L350,413 L432,413");
  b += gpath("batch-18-tenant-architecture", "tenantEdge", "schemaTenantTransaction", "M274,407 L432,517", "context", "M274,407 L362,407 L362,517 L432,517");
  b += gpath("batch-18-tenant-architecture", "tenantEdge", "securityFilter", "M274,407 L432,621", "context", "M274,407 L348,407 L348,621 L432,621");
  b += gpath("batch-18-tenant-architecture", "mvcContext", "schemaAspect", "M702,205 L862,205", "arrow");
  b += gpath("batch-18-tenant-architecture", "vtContext", "transactionSchemaAspect", "M702,309 L862,309", "arrow");
  b += gpath("batch-18-tenant-architecture", "webfluxContext", "suspendedTransaction", "M702,413 L862,413", "arrow");
  b += gpath("batch-18-tenant-architecture", "schemaTenantTransaction", "inventoryRepository", "M702,517 L862,517", "arrow");
  b += gpath("batch-18-tenant-architecture", "securityFilter", "databaseRegistry", "M702,621 L862,621", "arrow");
  b += gpath("batch-18-tenant-architecture", "repositories", "sharedSchemaDb", "M1080,309 L1238,297", "storage", "M1080,309 L1160,309 L1160,297 L1238,297");
  b += gpath("batch-18-tenant-architecture", "repositories", "sharedSchemaDb", "M1080,413 L1238,297", "storage", "M1080,413 L1164,413 L1164,297 L1238,297");
  b += gpath("batch-18-tenant-architecture", "inventoryRepository", "sharedSchemaDb", "M1080,517 L1238,297", "storage", "M1080,517 L1172,517 L1172,297 L1238,297");
  b += gpath("batch-18-tenant-architecture", "databaseRegistry", "tenantDatabases", "M1080,621 L1238,579", "storage", "M1080,621 L1168,621 L1168,579 L1238,579");
  b += note(96, 784, 1290, "Source check: MVC uses DataSourceUtils before @Transactional work; WebFlux switches inside newSuspendedTransactionWithTenant; database-per-tenant modules route through TenantDatabaseRegistry.");
  return b;
}

function moduleComparison() {
  let b = "";
  b += panel(55, 126, 300, 720, "Runtime / module");
  b += panel(405, 126, 320, 720, "Tenant boundary");
  b += panel(775, 126, 310, 720, "Repository");
  b += panel(1160, 126, 280, 720, "Storage");

  b += rowCard(92, 166, 225, 72, "01 Spring MVC", ["TenantFilter", "ThreadLocal"]);
  b += rowCard(442, 166, 245, 72, "TenantSchemaAspect", ["DataSourceUtils", "SET SCHEMA"]);
  b += rowCard(812, 166, 235, 72, "MovieExposedRepository", ["MovieTable", "Actor join"]);
  b += cylinder(1220, 148, 150, 126, "Shared DB", ["tenant schemas"], 3);

  b += rowCard(92, 274, 225, 72, "02 Virtual Threads", ["Tomcat VT", "ScopedValue"]);
  b += rowCard(442, 274, 245, 72, "TransactionSchemaAspect", ["transaction { }", "SchemaUtils"]);
  b += rowCard(812, 274, 235, 72, "MovieExposedRepository", ["same movie schema", "@Transactional"]);

  b += rowCard(92, 382, 225, 72, "03 WebFlux", ["WebFilter", "Reactor Context"]);
  b += rowCard(442, 382, 245, 72, "Suspended transaction", ["TenantId element", "set schema in IO"]);
  b += rowCard(812, 382, 235, 72, "Coroutine Repository", ["newSuspendedTransaction", "Movie records"]);

  b += rowCard(92, 490, 225, 72, "04 Schema Tenant", ["Header whitelist", "ThreadLocal"]);
  b += rowCard(442, 490, 245, 72, "TenantTransaction", ["set tenant schema", "reset PUBLIC/evict"]);
  b += rowCard(812, 490, 235, 72, "ExposedInventoryRepository", ["InventoryItems", "execute { }"]);

  b += rowCard(92, 598, 225, 72, "05/06 DB Tenant", ["TenantAuthorizationFilter", "no fallback DB"]);
  b += rowCard(442, 598, 245, 72, "TenantDatabaseRegistry", ["Hikari per tenant", "Database.connect"]);
  b += rowCard(812, 598, 235, 72, "Inventory Repository", ["databaseFor(tenant)", "TenantContext"]);
  b += cylinder(1220, 560, 150, 150, "Tenant DBs", ["korean pool", "english pool"], 4);

  b += rowCard(92, 706, 225, 72, "07 Ktor", ["TenantPlugin", "call attribute"]);
  b += rowCard(442, 706, 245, 72, "ThreadContextElement", ["bind ThreadLocal", "restore old state"]);
  b += rowCard(812, 706, 235, 72, "KtorMovieRepository", ["Database.connect", "TenantBootstrap"]);

  b += gpath("batch-18-module-comparison", "mvcFilter", "tenantSchemaAspect", "M317,202 L442,202", "context");
  b += gpath("batch-18-module-comparison", "tenantSchemaAspect", "movieRepository", "M687,202 L812,202", "arrow");
  b += gpath("batch-18-module-comparison", "movieRepository", "sharedSchemaDb", "M1047,202 L1220,206", "storage", "M1047,202 L1140,202 L1140,206 L1220,206");
  b += gpath("batch-18-module-comparison", "vtFilter", "transactionSchemaAspect", "M317,310 L442,310", "context");
  b += gpath("batch-18-module-comparison", "transactionSchemaAspect", "movieRepository", "M687,310 L812,310", "arrow");
  b += gpath("batch-18-module-comparison", "webfluxFilter", "suspendedTransaction", "M317,418 L442,418", "context");
  b += gpath("batch-18-module-comparison", "suspendedTransaction", "movieRepository", "M687,418 L812,418", "arrow");
  b += gpath("batch-18-module-comparison", "schemaFilter", "tenantTransaction", "M317,526 L442,526", "context");
  b += gpath("batch-18-module-comparison", "tenantTransaction", "inventoryRepository", "M687,526 L812,526", "arrow");
  b += gpath("batch-18-module-comparison", "inventoryRepository", "sharedSchemaDb", "M1047,526 L1220,206", "storage", "M1047,526 L1120,526 L1120,206 L1220,206");
  b += gpath("batch-18-module-comparison", "securityFilter", "tenantDatabaseRegistry", "M317,634 L442,634", "context");
  b += gpath("batch-18-module-comparison", "tenantDatabaseRegistry", "tenantDatabases", "M687,634 L1220,631", "storage", "M687,634 L730,634 L730,688 L1140,688 L1140,631 L1220,631");
  b += gpath("batch-18-module-comparison", "ktorPlugin", "threadContextElement", "M317,742 L442,742", "context");
  b += gpath("batch-18-module-comparison", "threadContextElement", "ktorMovieRepository", "M687,742 L812,742", "arrow");
  b += gpath("batch-18-module-comparison", "ktorMovieRepository", "sharedSchemaDb", "M1047,742 L1220,206", "storage", "M1047,742 L1100,742 L1100,206 L1220,206");
  b += note(98, 890, 1290, "Source check: MovieExposedRepository reads MovieTable, ActorInMovieTable, and ActorTable; InventoryRepository goes through TenantTransaction; registry creates tenant-specific Database objects.");
  return b;
}

function requestFlow() {
  const participants = [
    ["Client", 110],
    ["Tenant Edge", 300],
    ["Context", 500],
    ["Controller", 700],
    ["Service", 900],
    ["Transaction", 1100],
    ["Repository", 1280],
    ["Database", 1410],
  ];
  let b = "";
  b += participants.map(([name, x]) => participant(x, 130, name)).join("");
  for (const [, x] of participants) b += `<line x1="${x}" y1="190" x2="${x}" y2="805" class="lifeline"/>\n`;

  b += band(220, 236, "1", "Resolve tenant", "Filter/Auth reads tenant identity", 0, 70, 430);
  b += msg(110, 300, 260, "client", "edgeFilter", "arrow");
  b += band(300, 332, "2", "Bind context carrier", "ThreadLocal, ScopedValue, Reactor Context, or ThreadContextElement", 1, 250, 500);
  b += msg(300, 500, 356, "edgeFilter", "contextCarrier", "context");
  b += band(380, 405, "3", "Execute application path", "Controller/route delegates to service", 2, 450, 535);
  b += msg(500, 700, 452, "contextCarrier", "controller", "arrow");
  b += msg(700, 900, 452, "controller", "service", "arrow");
  b += band(460, 485, "4", "Apply storage boundary", "Schema switch or tenant Database selection", 3, 850, 380);
  b += msg(900, 1100, 512, "service", "transactionBoundary", "arrow");
  b += band(540, 565, "5", "Read/write through Exposed", "Repository uses tenant-scoped tables", 5, 1050, 390);
  b += msg(1100, 1280, 630, "transactionBoundary", "repository", "mapLine");
  b += msg(1280, 1410, 630, "repository", "database", "storage");
  b += band(660, 685, "6", "Reset or return", "Reset PUBLIC, then dashed returns", 6, 1040, 390);
  b += msg(1100, 1100, 738, "transactionBoundary", "schemaReset", "context", "M1100,738 L1150,738 L1150,772 L1100,772");
  b += msg(1410, 1280, 800, "database", "repository", "returnLine");
  b += msg(1280, 900, 820, "repository", "service", "returnLine", "M1280,820 L1090,820 L1090,820 L900,820");
  b += msg(700, 110, 852, "controller", "client", "returnLine", "M700,852 L430,852 L430,870 L110,870");
  b += note(88, 890, 1290, "Source check: returns are dashed; schema reset is only shown where TenantTransaction owns reset/eviction, while database-per-tenant routing returns through the selected tenant Database.");
  return b;
}

function shell(diagram) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${diagram.width} ${diagram.height}" width="${diagram.width}" height="${diagram.height}" role="img" aria-label="${esc(diagram.title)}">
<defs>
<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
${marker("arrowBlue", "#2563eb")}${marker("arrowPurple", "#7c3aed")}${marker("arrowOrange", "#ea580c")}${marker("arrowGreen", "#16a34a")}${marker("arrowTeal", "#0f766e")}${marker("arrowGray", "#64748b")}
<style>${style}</style>
</defs>
<rect width="${diagram.width}" height="${diagram.height}" class="canvas"/>
<rect x="20" y="20" width="${diagram.width - 40}" height="${diagram.height - 40}" rx="16" class="frame"/>
<text x="48" y="58" class="title">${esc(diagram.title)}</text>
<text x="48" y="80" class="subtitle">${esc(diagram.subtitle)}</text>
${diagram.body()}</svg>\n`;
}

function marker(id, color) {
  return `<marker id="${id}" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="${color}"/></marker>`;
}

function panel(x, y, w, h, title) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" class="panel"/>\n<text x="${x + w / 2}" y="${y - 8}" class="panelTitle" text-anchor="middle">${esc(title.toUpperCase())}</text>\n`;
}

function card(x, y, w, h, title, colorIndex, detail = "") {
  const [fill, stroke] = colors[colorIndex % colors.length];
  currentRects.push({ x, y, w, h, label: title });
  const lines = wrap(title, 20).slice(0, 2);
  const titleStart = y + h / 2 - (detail ? (lines.length > 1 ? 9 : 4) : (lines.length > 1 ? 9 : -5));
  let text = lines.map((line, i) => `<text x="${x + w / 2}" y="${titleStart + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>`).join("\n");
  if (detail) text += `\n<text x="${x + w / 2}" y="${y + h - 14}" class="detail" text-anchor="middle">${esc(detail)}</text>`;
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n${text}\n`;
}

function rowCard(x, y, w, h, title, details) {
  const colorIndex = Math.abs([...title].reduce((acc, ch) => acc + ch.charCodeAt(0), 0)) % colors.length;
  const [fill, stroke] = colors[colorIndex];
  currentRects.push({ x, y, w, h, label: title });
  let b = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  b += `<text x="${x + w / 2}" y="${y + 28}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  details.slice(0, 2).forEach((detail, i) => {
    b += `<text x="${x + w / 2}" y="${y + 48 + i * 15}" class="detail" text-anchor="middle">${esc(detail)}</text>\n`;
  });
  return b;
}

function endpoint(x, y, w, h, title, details) {
  const [fill, stroke] = colors[0];
  currentRects.push({ x, y, w, h, label: title });
  return `<path d="M${x + 18},${y} H${x + w - 8} Q${x + w},${y} ${x + w},${y + 8} V${y + h - 8} Q${x + w},${y + h} ${x + w - 8},${y + h} H${x + 18} L${x},${y + h / 2} Z" fill="${fill}" stroke="${stroke}" class="card"/>\n` +
    `<text x="${x + w / 2 + 4}" y="${y + 29}" class="label" text-anchor="middle">${esc(title)}</text>\n` +
    details.map((d, i) => `<text x="${x + w / 2 + 4}" y="${y + 48 + i * 13}" class="detail" text-anchor="middle">${esc(d)}</text>`).join("\n") + "\n";
}

function schemaBox(x, y, title, detail, colorIndex) {
  const [fill, stroke] = colors[colorIndex];
  currentRects.push({ x, y, w: 150, h: 74, label: title });
  return `<rect x="${x}" y="${y}" width="150" height="74" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n` +
    `<text x="${x + 75}" y="${y + 32}" class="label" text-anchor="middle">${esc(title)}</text>\n` +
    `<text x="${x + 75}" y="${y + 52}" class="detail" text-anchor="middle">${esc(detail)}</text>\n`;
}

function tableMini(x, y, title, rows, colorIndex) {
  const [fill, stroke] = colors[colorIndex];
  currentRects.push({ x, y, w: 120, h: 82, label: title });
  let b = `<rect x="${x}" y="${y}" width="120" height="82" rx="4" fill="#fff" stroke="${stroke}" class="tableBox"/>\n`;
  b += `<rect x="${x}" y="${y}" width="120" height="26" rx="4" fill="${fill}" stroke="${stroke}" class="tableBox"/>\n`;
  b += `<text x="${x + 60}" y="${y + 18}" class="className" text-anchor="middle">${esc(title)}</text>\n`;
  rows.slice(0, 3).forEach((row, i) => {
    b += `<text x="${x + 10}" y="${y + 42 + i * 14}" class="detail">${esc(row)}</text>\n`;
  });
  return b;
}

function uml(x, y, w, h, name, fields, methods, colorIndex) {
  const [fill, stroke] = colors[colorIndex % colors.length];
  currentRects.push({ x, y, w, h, label: name });
  let b = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="#fff" stroke="${stroke}" class="uml"/>\n`;
  b += `<rect x="${x}" y="${y}" width="${w}" height="34" rx="4" fill="${fill}" stroke="${stroke}" class="uml"/>\n`;
  b += `<text x="${x + w / 2}" y="${y + 23}" class="className" text-anchor="middle">${esc(name)}</text>\n`;
  b += `<line x1="${x}" y1="${y + 34}" x2="${x + w}" y2="${y + 34}" stroke="${stroke}" stroke-width="1.1"/>\n`;
  let rowY = y + 54;
  fields.forEach((field) => {
    b += `<text x="${x + 12}" y="${rowY}" class="detail">${esc(field)}</text>\n`;
    rowY += 16;
  });
  if (methods.length) {
    b += `<line x1="${x}" y1="${Math.max(y + 66, rowY - 3)}" x2="${x + w}" y2="${Math.max(y + 66, rowY - 3)}" stroke="${stroke}" stroke-width="1.1"/>\n`;
    rowY = Math.max(y + 82, rowY + 13);
    methods.forEach((method) => {
      b += `<text x="${x + 12}" y="${rowY}" class="detail">${esc(method)}</text>\n`;
      rowY += 16;
    });
  }
  return b;
}

function cylinder(x, y, w, h, title, lines, colorIndex, blocksConnectors = true) {
  const [fill, stroke] = colors[colorIndex % colors.length];
  const cap = 28;
  if (blocksConnectors) currentRects.push({ x, y, w, h, label: title });
  let b = `<path d="M${x},${y + cap / 2} C${x},${y - cap / 2} ${x + w},${y - cap / 2} ${x + w},${y + cap / 2} V${y + h - cap / 2} C${x + w},${y + h + cap / 2} ${x},${y + h + cap / 2} ${x},${y + h - cap / 2} Z" fill="${fill}" stroke="${stroke}" class="dbBody"/>\n`;
  b += `<ellipse cx="${x + w / 2}" cy="${y + cap / 2}" rx="${w / 2}" ry="${cap / 2}" fill="#fff7ed" stroke="${stroke}" stroke-width="1.7"/>\n`;
  b += `<ellipse cx="${x + w / 2}" cy="${y + h - cap / 2}" rx="${w / 2}" ry="${cap / 2}" fill="none" stroke="${stroke}" stroke-width="1.7"/>\n`;
  b += `<text x="${x + w / 2}" y="${y + (blocksConnectors ? 56 : 50)}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  if (blocksConnectors) {
    lines.forEach((line, i) => {
      b += `<text x="${x + w / 2}" y="${y + 84 + i * 17}" class="detail" text-anchor="middle">${esc(line)}</text>\n`;
    });
  }
  return b;
}

function participant(x, y, title) {
  currentRects.push({ x: x - 62, y, w: 124, h: 58, label: title });
  return `<rect x="${x - 62}" y="${y}" width="124" height="58" rx="10" fill="#dbeafe" stroke="#3b82f6" class="card"/>\n` +
    `<text x="${x}" y="${y + 36}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
}

function band(y, textY, num, title, detail, colorIndex, x = 70, w = 1360) {
  const [fill, stroke] = colors[colorIndex % colors.length];
  return `<rect x="${x}" y="${y}" width="${w}" height="58" rx="12" fill="${fill}" stroke="${stroke}" opacity="0.58"/>\n` +
    `<circle cx="${x + 28}" cy="${y + 29}" r="16" fill="#fff" stroke="${stroke}" stroke-width="1.5"/>\n` +
    `<text x="${x + 28}" y="${y + 34}" class="label" text-anchor="middle">${esc(num)}</text>\n` +
    `<text x="${x + 56}" y="${textY}" class="label">${esc(title)}</text>\n` +
    `<text x="${x + 56}" y="${textY + 19}" class="detail">${esc(detail)}</text>\n`;
}

function msg(x1, x2, y, from, to, cls, override = null) {
  const d = override || `M${x1},${y} L${x2},${y}`;
  return gpath("batch-18-request-flow", from, to, d, cls);
}

function gpath(sketch, from, to, d, cls, override = null) {
  assertGraphvizRoute(sketch, from, to);
  const path = override || d;
  currentPaths.push({ d: path, cls, from, to });
  return `<path d="${path}" class="${cls}"/>\n`;
}

function note(x, y, w, text) {
  return `<rect x="${x}" y="${y}" width="${w}" height="44" rx="10" fill="#f8fafc" stroke="#d7e2ec" stroke-width="1.2"/>\n` +
    `<text x="${x + 18}" y="${y + 27}" class="detail">${esc(text)}</text>\n`;
}

function assertGraphvizRoute(sketch, from, to) {
  const routes = graphvizRoutes.get(sketch);
  if (!routes) throw new Error(`${currentDiagram}: missing graphviz sketch ${sketch}`);
  const ok = routes.some((route) => route.from === from && route.to === to);
  if (!ok) throw new Error(`${currentDiagram}: missing Graphviz route ${sketch} ${from}->${to}`);
}

function parseGraphvizPlain(plain) {
  return plain
    .split(/\n/)
    .filter((line) => line.startsWith("edge "))
    .map((line) => {
      const parts = line.trim().split(/\s+/);
      const count = Number(parts[3]);
      const coords = parts.slice(4, 4 + count * 2).map(Number);
      const points = [];
      for (let i = 0; i < coords.length; i += 2) points.push([coords[i], coords[i + 1]]);
      return {
        from: parts[1],
        to: parts[2],
        points,
        firstLeg: classifyLeg(points[0], points[1]),
        lastLeg: classifyLeg(points[points.length - 2], points[points.length - 1]),
      };
    });
}

function classifyLeg(a, b) {
  if (!a || !b) return "unknown";
  const dx = Math.abs(a[0] - b[0]);
  const dy = Math.abs(a[1] - b[1]);
  return dx >= dy ? "horizontal" : "vertical";
}

function validateConnectorGeometry(file, rects, paths) {
  const failures = [];
  for (const path of paths) {
    const segments = pathSegments(path.d);
    for (const [a, b] of segments) {
      if (a.x !== b.x && a.y !== b.y) failures.push(`${file}: diagonal connector ${path.from}->${path.to} ${a.x},${a.y}->${b.x},${b.y}`);
      for (const rect of rects) {
        if (rect.label === path.from || rect.label === path.to) continue;
        if (segmentCrossesInterior(a, b, rect)) {
          failures.push(`${file}: connector ${path.from}->${path.to} crosses ${rect.label}`);
        }
      }
    }
  }
  if (failures.length) throw new Error(failures.join("\n"));
}

function pathSegments(d) {
  const nums = d.match(/-?\d+(?:\.\d+)?/g)?.map(Number) || [];
  const points = [];
  for (let i = 0; i < nums.length; i += 2) points.push({ x: nums[i], y: nums[i + 1] });
  const segments = [];
  for (let i = 1; i < points.length; i += 1) segments.push([points[i - 1], points[i]]);
  return segments;
}

function segmentCrossesInterior(a, b, r) {
  const left = r.x + 3;
  const right = r.x + r.w - 3;
  const top = r.y + 3;
  const bottom = r.y + r.h - 3;
  if (a.y === b.y) {
    const y = a.y;
    if (y <= top || y >= bottom) return false;
    return Math.max(Math.min(a.x, b.x), left) < Math.min(Math.max(a.x, b.x), right);
  }
  if (a.x === b.x) {
    const x = a.x;
    if (x <= left || x >= right) return false;
    return Math.max(Math.min(a.y, b.y), top) < Math.min(Math.max(a.y, b.y), bottom);
  }
  return false;
}

function validateBatchSemantics() {
  const checks = [
    ["10-multi-tenant-schema-layout-04.svg", ["Shared DB instance", "korean schema", "english schema", "TenantRegistry", "actors_in_movies"]],
    ["10-multi-tenant-architecture-01.svg", ["TenantSchemaAspect", "Suspended", "transaction", "DatabaseRegistry", "Tenant DBs"]],
    ["10-multi-tenant-class-02.svg", ["TenantAuthorizationFilter", "TenantDatabaseRegistry", "MovieExposedRepository", "ExposedInventoryRepository"]],
    ["10-multi-tenant-sequence-03.svg", ["Resolve tenant", "Reset or return", "returns are dashed"]],
  ];
  const failures = [];
  for (const [name, needles] of checks) {
    const svg = fs.readFileSync(`${outDir}/${name}`, "utf8");
    for (const needle of needles) {
      if (!svg.includes(needle)) failures.push(`${name}: missing ${needle}`);
    }
    if (!svg.includes("Architects Daughter")) failures.push(`${name}: missing Architects Daughter font`);
    if (!svg.includes("Comic Mono")) failures.push(`${name}: missing Comic Mono font`);
  }
  if (failures.length) throw new Error(failures.join("\n"));
}

function wrap(text, max) {
  const words = text.split(/\s+/);
  const lines = [];
  let line = "";
  for (const word of words) {
    if (!line) {
      line = word;
    } else if ((line.length + word.length + 1) <= max) {
      line += ` ${word}`;
    } else {
      lines.push(line);
      line = word;
    }
  }
  if (line) lines.push(line);
  return lines;
}

function esc(value) {
  return String(value).replace(/[&<>"]/g, (ch) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
  })[ch]);
}
