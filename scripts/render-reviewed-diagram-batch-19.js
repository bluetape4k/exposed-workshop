#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const DOT = process.env.DOT || "dot";
const RSVG = process.env.RSVG_CONVERT || "rsvg-convert";
const outDir = "docs/images/readme-diagrams";
const sketchDir = ".omx/artifacts/diagram-sketches";
const evidenceDir = ".omx/artifacts/diagram-validation";
const graphScaleBase = 100;

const colors = {
  blue: ["#dbeafe", "#2563eb"],
  purple: ["#ede9fe", "#7c3aed"],
  green: ["#dcfce7", "#16a34a"],
  orange: ["#ffedd5", "#ea580c"],
  pink: ["#fce7f3", "#db2777"],
  teal: ["#ccfbf1", "#0f766e"],
  amber: ["#fef3c7", "#d97706"],
  gray: ["#e2e8f0", "#64748b"],
};

const edgeStyles = {
  call: { cls: "call", color: "#2563eb", marker: "arrowBlue" },
  runtime: { cls: "runtime", color: "#7c3aed", marker: "arrowPurple" },
  storage: { cls: "storage", color: "#ea580c", marker: "arrowOrange" },
  map: { cls: "map", color: "#16a34a", marker: "arrowGreen" },
  fk: { cls: "fk", color: "#0f766e", marker: "arrowTeal" },
  ret: { cls: "ret", color: "#64748b", marker: "arrowGray" },
};

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
.call,.runtime,.storage,.map,.fk,.ret{fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
.call{stroke:#2563eb;marker-end:url(#arrowBlue)}.runtime{stroke:#7c3aed;marker-end:url(#arrowPurple)}
.storage{stroke:#ea580c;marker-end:url(#arrowOrange)}.map{stroke:#16a34a;marker-end:url(#arrowGreen)}
.fk{stroke:#0f766e;marker-end:url(#arrowTeal)}.ret{stroke:#64748b;stroke-dasharray:7 5;marker-end:url(#arrowGray)}
.inherit{fill:none;stroke:#334155;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round}
.inheritTriangle{fill:#fff;stroke:#334155;stroke-width:1.8}
.lifeline{stroke:#cbd5e1;stroke-width:1.4;stroke-dasharray:5 6}
`;

const diagrams = [domainSpec(), architectureSpec(), supportSpec(), sequenceSpec()];

fs.mkdirSync(sketchDir, { recursive: true });
fs.mkdirSync(evidenceDir, { recursive: true });

const batchEvidence = {};
for (const spec of diagrams) {
  const graph = writeGraphvizSketch(spec);
  const rendered = spec.sequence ? renderSequence(spec, graph) : renderGraphDiagram(spec, graph);
  fs.writeFileSync(spec.file, rendered.svg);
  fs.writeFileSync(`${evidenceDir}/${spec.name}-graphviz-vs-final.json`, `${JSON.stringify(rendered.summary, null, 2)}\n`);
  batchEvidence[spec.name] = rendered.summary;
  validateDiagram(spec, rendered);
  const png = spec.file.replace(/\.svg$/, ".png");
  const pngResult = spawnSync(RSVG, ["-f", "png", "-o", png, spec.file], { encoding: "utf8" });
  if (pngResult.status !== 0) throw new Error(pngResult.stderr || pngResult.stdout);
  console.log(png);
}
fs.writeFileSync(`${evidenceDir}/batch-19-graphviz-vs-final.json`, `${JSON.stringify(batchEvidence, null, 2)}\n`);
validateBatchSemantics();

function domainSpec() {
  const file = `${outDir}/10-multi-tenant-01-multitenant-spring-web-erd-01.svg`;
  return {
    name: "batch-19-domain",
    file,
    title: "Spring MVC Multi-Tenant Domain Model",
    subtitle: "MovieSchema tables, DAO entities, DTO records, bridge FKs, and source-derived mapper relations",
    width: 2200,
    height: 780,
    margin: { left: 78, right: 78, top: 118, bottom: 100 },
    graph: { rankdir: "TB", nodesep: 0.72, ranksep: 1.02 },
    visibleEdgeConstraints: false,
    note: "Source check: ActorInMovieTable is the bridge table; FK arrows point from bridge to parent tables, while DTO mapping is separated from FK semantics.",
    groups: {
      "MovieSchema tables": ["LongIdTable", "Table", "MovieTable", "ActorTable", "ActorInMovieTable"],
      "DAO entities": ["LongEntity", "MovieEntity", "ActorEntity"],
      "DTO records": ["MovieRecord", "ActorRecord", "MovieWithActorRecord"],
    },
    ranks: [
      ["LongIdTable", "Table", "LongEntity"],
      ["MovieTable", "ActorTable", "MovieEntity", "ActorEntity", "MovieRecord", "ActorRecord"],
      ["ActorInMovieTable", "MovieWithActorRecord"],
    ],
    layoutEdges: [
      ["LongIdTable", "MovieTable"],
      ["LongIdTable", "ActorTable"],
      ["Table", "ActorInMovieTable"],
      ["LongEntity", "MovieEntity"],
      ["LongEntity", "ActorEntity"],
      ["MovieRecord", "MovieWithActorRecord"],
      ["ActorRecord", "MovieWithActorRecord"],
      ["ActorTable", "MovieEntity"],
      ["ActorEntity", "MovieRecord"],
    ],
    nodes: {
      LongIdTable: node("uml", 210, 78, "LongIdTable", "gray", ["Exposed id table"], []),
      Table: node("uml", 170, 78, "Table", "gray", ["plain table"], []),
      MovieTable: node("table", 250, 138, "MovieTable", "blue", ["id: EntityID<Long> PK", "name: varchar(255)", "producerName: varchar(255)", "releaseDate: date"], []),
      ActorTable: node("table", 250, 138, "ActorTable", "pink", ["id: EntityID<Long> PK", "firstName: varchar(255)", "lastName: varchar(255)", "birthday: date?"], []),
      ActorInMovieTable: node("table", 280, 118, "ActorInMovieTable", "amber", ["movieId: FK -> movies", "actorId: FK -> actors", "PK(movieId, actorId)"], []),
      LongEntity: node("uml", 220, 78, "LongEntity", "gray", ["DAO entity base"], []),
      MovieEntity: node("uml", 260, 118, "MovieEntity", "blue", ["extends LongEntity", "actors via bridge table"], ["toMovieRecord()"]),
      ActorEntity: node("uml", 260, 118, "ActorEntity", "pink", ["extends LongEntity", "movies via bridge table"], ["toActorRecord()"]),
      MovieRecord: node("uml", 280, 94, "MovieRecord", "blue", ["from MovieEntity"], []),
      ActorRecord: node("uml", 280, 94, "ActorRecord", "pink", ["from ActorEntity"], []),
      MovieWithActorRecord: node("uml", 310, 112, "MovieWithActorRecord", "teal", ["MovieRecord + ActorRecord"], ["used by response DTO"]),
    },
    edges: [
      edge("MovieTable", "LongIdTable", "inherit"),
      edge("ActorTable", "LongIdTable", "inherit"),
      edge("ActorInMovieTable", "Table", "inherit"),
      edge("MovieEntity", "LongEntity", "inherit"),
      edge("ActorEntity", "LongEntity", "inherit"),
      edge("ActorInMovieTable", "MovieTable", "fk"),
      edge("ActorInMovieTable", "ActorTable", "fk"),
      edge("MovieEntity", "ActorInMovieTable", "map"),
      edge("ActorEntity", "ActorInMovieTable", "map"),
      edge("MovieEntity", "MovieRecord", "runtime"),
      edge("ActorEntity", "ActorRecord", "runtime"),
      edge("MovieRecord", "MovieWithActorRecord", "runtime"),
      edge("ActorRecord", "MovieWithActorRecord", "runtime"),
    ],
  };
}

function architectureSpec() {
  return {
    name: "batch-19-architecture",
    file: `${outDir}/10-multi-tenant-01-multitenant-spring-web-class-02.svg`,
    title: "Spring MVC Schema-Tenant Architecture",
    subtitle: "TenantFilter binds ThreadLocal context; AOP switches the Spring transaction connection before Exposed queries run",
    width: 2200,
    height: 800,
    margin: { left: 70, right: 70, top: 118, bottom: 100 },
    graph: { rankdir: "LR", nodesep: 0.54, ranksep: 0.86 },
    note: "Source check: Primary dataSource uses shared DB/separate schema; TenantAwareDataSource is the database-per-tenant alternative, not the primary path.",
    groups: {
      "Request edge": ["httpClient", "tenantFilter"],
      "Spring MVC": ["tenantContext", "actorController", "tenantSchemaAspect", "dataSourceUtils", "tenantAwareDataSource"],
      "Exposed access": ["actorRepository", "movieRepository", "tenantInitializer", "dataInitializer"],
      "Schema storage": ["actorTable", "movieTables", "schemaUtils", "migrationUtils", "sharedDb"],
    },
    manualPanels: {
      "Request edge": [66, 238, 500, 145],
      "Spring MVC": [250, 440, 1140, 250],
      "Exposed access": [1100, 100, 525, 350],
      "Schema storage": [1605, 75, 535, 455],
    },
    useClusters: true,
    ranks: [],
    manualRects: {
      httpClient: [190, 330],
      tenantFilter: [410, 330],
      tenantAwareDataSource: [410, 550],
      tenantContext: [690, 550],
      actorController: [965, 550],
      tenantSchemaAspect: [965, 665],
      dataSourceUtils: [1245, 665],
      movieRepository: [1245, 220],
      tenantInitializer: [1245, 330],
      actorRepository: [1245, 400],
      dataInitializer: [1490, 330],
      migrationUtils: [1680, 140],
      schemaUtils: [1700, 330],
      movieTables: [1865, 220],
      actorTable: [1865, 400],
      sharedDb: [2055, 400],
    },
    nodes: {
      httpClient: node("endpoint", 190, 78, "HTTP Client", "blue", ["GET /actors", "X-TENANT-ID"], []),
      tenantFilter: node("card", 190, 70, "TenantFilter", "purple", ["withTenant block"], []),
      tenantContext: node("card", 245, 70, "TenantContext", "purple", ["ThreadLocal tenant"], []),
      actorController: node("card", 255, 70, "ActorController", "blue", ["@Transactional readOnly"], []),
      tenantSchemaAspect: node("card", 270, 70, "TenantSchemaAspect", "amber", ["before transaction"], []),
      dataSourceUtils: node("card", 245, 70, "DataSourceUtils", "gray", ["same connection"], []),
      tenantAwareDataSource: node("card", 265, 62, "TenantAwareDataSource", "pink", ["DB-per-tenant option"], []),
      actorRepository: node("card", 220, 72, "ActorRepository", "green", ["JdbcRepository"], []),
      movieRepository: node("card", 220, 72, "MovieRepository", "teal", ["joins movie tables"], []),
      tenantInitializer: node("card", 220, 72, "TenantInitializer", "green", ["ApplicationReadyEvent"], []),
      dataInitializer: node("card", 220, 72, "DataInitializer", "orange", ["schema + samples"], []),
      actorTable: node("table", 170, 140, "ActorTable", "pink", ["id PK", "firstName", "lastName"], []),
      movieTables: node("table", 190, 124, "Movie tables", "blue", ["MovieTable", "ActorInMovieTable"], []),
      schemaUtils: node("card", 130, 62, "SchemaUtils", "amber", ["create/set"], []),
      migrationUtils: node("card", 140, 62, "MigrationUtils", "amber", ["DDL diff"], []),
      sharedDb: node("db", 160, 130, "Shared DB", "orange", ["schemas", "korean / english"], []),
    },
    edges: [
      edge("httpClient", "tenantFilter", "call"),
      edge("tenantFilter", "tenantContext", "runtime"),
      edge("tenantContext", "actorController", "runtime"),
      edge("actorController", "actorRepository", "call"),
      edge("actorRepository", "actorTable", "map", { lane: -18 }),
      edge("actorTable", "sharedDb", "storage"),
      edge("tenantContext", "tenantSchemaAspect", "runtime"),
      edge("tenantSchemaAspect", "dataSourceUtils", "call"),
      edge("dataSourceUtils", "sharedDb", "storage"),
      edge("tenantInitializer", "dataInitializer", "call"),
      edge("dataInitializer", "schemaUtils", "storage"),
      edge("schemaUtils", "sharedDb", "storage", { lane: -16 }),
      edge("dataInitializer", "migrationUtils", "storage"),
      edge("migrationUtils", "movieTables", "storage"),
      edge("movieRepository", "movieTables", "map"),
      edge("movieRepository", "actorTable", "map", { lane: 18 }),
      edge("movieTables", "sharedDb", "storage", { lane: 16 }),
      edge("tenantAwareDataSource", "tenantContext", "runtime"),
    ],
    manualRoutes: {
      "httpClient->tenantFilter": route("right", "left"),
      "tenantFilter->tenantContext": route("bottom", "top", [{ x: 410, y: 430 }, { x: 690, y: 430 }]),
      "tenantAwareDataSource->tenantContext": route("right", "left"),
      "tenantContext->actorController": route("right", "left"),
      "tenantContext->tenantSchemaAspect": route("bottom", "top", [{ x: 690, y: 610 }, { x: 965, y: 610 }]),
      "actorController->actorRepository": route("top", "bottom", [{ x: 965, y: 480 }, { x: 1245, y: 480 }]),
      "actorRepository->actorTable": route("right", "left"),
      "movieRepository->movieTables": route("right", "left", [{ x: 1660, y: 220 }, { x: 1660, y: 195 }], { toOffset: -25 }),
      "movieRepository->actorTable": route("bottom", "left", [{ x: 1245, y: 295 }, { x: 1775, y: 295 }, { x: 1775, y: 370 }], { toOffset: -30 }),
      "actorTable->sharedDb": route("right", "left"),
      "movieTables->sharedDb": route("right", "top", [{ x: 1970, y: 220 }, { x: 1970, y: 310 }, { x: 2055, y: 310 }]),
      "tenantSchemaAspect->dataSourceUtils": route("right", "left"),
      "dataSourceUtils->sharedDb": route("right", "bottom", [{ x: 1970, y: 665 }, { x: 1970, y: 495 }, { x: 2055, y: 495 }]),
      "tenantInitializer->dataInitializer": route("right", "left"),
      "dataInitializer->migrationUtils": route("top", "bottom", [{ x: 1490, y: 205 }, { x: 1680, y: 205 }]),
      "dataInitializer->schemaUtils": route("right", "left"),
      "migrationUtils->movieTables": route("right", "left", [{ x: 1760, y: 150 }, { x: 1760, y: 220 }]),
      "schemaUtils->sharedDb": route("right", "left", [{ x: 1920, y: 330 }], { toOffset: -35 }),
    },
  };
}

function supportSpec() {
  return {
    name: "batch-19-tenant-support",
    file: `${outDir}/10-multi-tenant-01-multitenant-spring-web-class-03.svg`,
    title: "Tenant Context Support Classes",
    subtitle: "Current source has no TenantResolver class; support is split across context, filter, aspect, initializer, and routing DataSource types",
    width: 2200,
    height: 980,
    margin: { left: 70, right: 70, top: 118, bottom: 112 },
    graph: { rankdir: "TB", nodesep: 0.68, ranksep: 0.9 },
    visibleEdgeConstraints: false,
    note: "Source check: README no longer says TenantResolver because the source has no TenantResolver type.",
    groups: {
      "Context and tenant model": ["Tenants", "TenantContext", "SchemaSupport"],
      "Servlet and AOP boundary": ["Filter", "TenantFilter", "ApplicationListener", "TenantInitializer", "TenantSchemaAspect"],
      "Infrastructure collaborators": ["AbstractRoutingDataSource", "TenantAwareDataSource", "DataSourceUtils", "DataInitializer", "MovieSchema"],
    },
    panels: false,
    ranks: [
      ["Filter", "ApplicationListener", "AbstractRoutingDataSource"],
      ["TenantFilter", "TenantSchemaAspect", "TenantInitializer", "TenantAwareDataSource"],
      ["TenantContext", "DataSourceUtils", "DataInitializer"],
      ["Tenants", "SchemaSupport", "MovieSchema"],
    ],
    layoutEdges: [
      ["Filter", "TenantFilter"],
      ["ApplicationListener", "TenantInitializer"],
      ["AbstractRoutingDataSource", "TenantAwareDataSource"],
      ["TenantFilter", "TenantContext"],
      ["TenantContext", "Tenants"],
      ["TenantContext", "SchemaSupport"],
      ["TenantSchemaAspect", "DataSourceUtils"],
      ["TenantInitializer", "DataInitializer"],
      ["DataInitializer", "MovieSchema"],
    ],
    nodes: {
      Tenants: node("uml", 285, 132, "Tenants", "purple", ["DEFAULT_TENANT = KOREAN", "Tenant.KOREAN / ENGLISH"], ["getById()", "getTenantSchema()"]),
      TenantContext: node("uml", 285, 154, "TenantContext", "blue", ["ThreadLocal<Tenant?>", "default fallback"], ["withTenant()", "clear()", "getCurrentTenantSchema()"]),
      SchemaSupport: node("uml", 285, 82, "SchemaSupport", "teal", ["getSchemaDefinition(tenant)"], []),
      Filter: node("uml", 210, 62, "Filter", "gray", ["jakarta.servlet"], []),
      TenantFilter: node("uml", 305, 156, "TenantFilter", "purple", ["implements Filter", "TENANT_HEADER"], ["extractTenant()", "chain.doFilter()"]),
      ApplicationListener: node("uml", 260, 62, "ApplicationListener", "gray", ["ApplicationReadyEvent"], []),
      TenantInitializer: node("uml", 305, 146, "TenantInitializer", "green", ["ApplicationReadyEvent"], ["TenantContext.withTenant", "dataInitializer.initialize()"]),
      TenantSchemaAspect: node("uml", 315, 150, "TenantSchemaAspect", "amber", ["@Aspect @Component", "Order highest + 1"], ["setSchemaForTransaction()", "DataSourceUtils.getConnection()"]),
      AbstractRoutingDataSource: node("uml", 315, 62, "AbstractRoutingDataSource", "gray", ["Spring JDBC base"], []),
      TenantAwareDataSource: node("uml", 315, 122, "TenantAwareDataSource", "pink", ["extends AbstractRoutingDataSource"], ["determineCurrentLookupKey()"]),
      DataSourceUtils: node("uml", 315, 126, "DataSourceUtils", "gray", ["Spring transaction connection"], ["CREATE SCHEMA IF NOT EXISTS", "SET SCHEMA"]),
      DataInitializer: node("uml", 315, 156, "DataInitializer", "orange", ["SchemaUtils", "MigrationUtils"], ["createSchema()", "populateData()"]),
      MovieSchema: node("uml", 315, 62, "MovieSchema", "blue", ["ActorTable / MovieTable / ActorInMovieTable"], []),
    },
    edges: [
      edge("TenantAwareDataSource", "AbstractRoutingDataSource", "inherit"),
      edge("TenantFilter", "Filter", "inherit"),
      edge("TenantInitializer", "ApplicationListener", "inherit"),
      edge("TenantFilter", "Tenants", "runtime", { constraint: false }),
      edge("TenantFilter", "TenantContext", "runtime", { constraint: false }),
      edge("TenantContext", "Tenants", "runtime"),
      edge("TenantContext", "SchemaSupport", "map"),
      edge("TenantSchemaAspect", "TenantContext", "runtime", { constraint: false }),
      edge("TenantSchemaAspect", "DataSourceUtils", "call"),
      edge("TenantInitializer", "DataInitializer", "call"),
      edge("DataInitializer", "SchemaSupport", "map", { constraint: false }),
      edge("DataInitializer", "MovieSchema", "storage"),
    ],
  };
}

function sequenceSpec() {
  return {
    name: "batch-19-sequence",
    file: `${outDir}/10-multi-tenant-01-multitenant-spring-web-sequence-04.svg`,
    title: "Spring MVC Tenant Request Flow",
    subtitle: "Calls are solid, returns are dashed; ThreadLocal cleanup happens after the controller/repository path returns",
    width: 1500,
    height: 1080,
    sequence: true,
    margin: { left: 80, right: 80, top: 135, bottom: 165 },
    graph: { rankdir: "LR", nodesep: 0.62, ranksep: 0.95 },
    layoutEdges: [
      ["client", "tenantFilter"],
      ["tenantFilter", "tenants"],
      ["tenants", "tenantContext"],
      ["tenantContext", "actorController"],
      ["actorController", "tenantSchemaAspect"],
      ["tenantSchemaAspect", "dataSourceUtils"],
      ["dataSourceUtils", "actorRepository"],
      ["actorRepository", "sharedDb"],
    ],
    note: "Source check: withTenant restores the previous tenant or clears ThreadLocal in finally; schema switch uses DataSourceUtils on the Spring transaction connection.",
    nodes: {
      client: node("participant", 132, 58, "Client", "blue", [], []),
      tenantFilter: node("participant", 150, 70, "Tenant Filter", "blue", [], []),
      tenants: node("participant", 132, 58, "Tenants", "blue", [], []),
      tenantContext: node("participant", 158, 70, "Tenant Context", "blue", [], []),
      actorController: node("participant", 158, 70, "Actor Controller", "blue", [], []),
      tenantSchemaAspect: node("participant", 184, 74, "Schema Aspect", "blue", [], []),
      dataSourceUtils: node("participant", 158, 74, "DataSource Utils", "blue", [], []),
      actorRepository: node("participant", 158, 70, "Actor Repository", "blue", [], []),
      sharedDb: node("participant", 132, 58, "Shared DB", "blue", [], []),
    },
    edges: [
      edge("client", "tenantFilter", "call"),
      edge("tenantFilter", "tenants", "call"),
      edge("tenants", "tenantContext", "runtime"),
      edge("tenantContext", "actorController", "call"),
      edge("actorController", "tenantSchemaAspect", "call"),
      edge("tenantSchemaAspect", "dataSourceUtils", "call"),
      edge("dataSourceUtils", "sharedDb", "storage"),
      edge("actorController", "actorRepository", "map"),
      edge("actorRepository", "sharedDb", "storage"),
      edge("sharedDb", "actorRepository", "ret", { constraint: false }),
      edge("actorRepository", "actorController", "ret", { constraint: false }),
      edge("actorController", "tenantFilter", "ret", { constraint: false }),
      edge("tenantFilter", "client", "ret", { constraint: false }),
      edge("tenantFilter", "tenantFilter", "runtime", { constraint: false }),
    ],
  };
}

function node(kind, w, h, title, color, fields, methods) {
  return { kind, w, h, title, color, fields, methods };
}

function edge(from, to, type, options = {}) {
  return { from, to, type, constraint: options.constraint !== false, lane: options.lane || 0 };
}

function route(fromSide, toSide, via = [], options = {}) {
  return { fromSide, toSide, via, fromOffset: options.fromOffset || 0, toOffset: options.toOffset || 0 };
}

function writeGraphvizSketch(spec) {
  const dot = buildDot(spec);
  const dotPath = `${sketchDir}/${spec.name}.dot`;
  const plainPath = `${sketchDir}/${spec.name}.plain`;
  const svgPath = `${sketchDir}/${spec.name}.svg`;
  fs.writeFileSync(dotPath, dot);
  run(DOT, ["-Tplain", "-o", plainPath, dotPath]);
  run(DOT, ["-Tsvg", "-o", svgPath, dotPath]);
  return parsePlain(fs.readFileSync(plainPath, "utf8"));
}

function buildDot(spec) {
  const graph = spec.graph;
  const lines = [
    "digraph G {",
    `graph [rankdir=${graph.rankdir}, splines=ortho, nodesep=${graph.nodesep}, ranksep=${graph.ranksep}, pad=0.18, outputorder=edgesfirst];`,
    "node [shape=box, label=\"\", fixedsize=true, margin=0, style=\"rounded\", color=\"#94a3b8\"];",
    "edge [arrowsize=0.8, penwidth=1.8];",
  ];
  for (const [id, n] of Object.entries(spec.nodes)) {
    lines.push(`${qid(id)} [width=${(n.w / graphScaleBase).toFixed(3)}, height=${(n.h / graphScaleBase).toFixed(3)}];`);
  }
  if (spec.useClusters && spec.groups) {
    let i = 0;
    for (const [group, ids] of Object.entries(spec.groups)) {
      lines.push(`subgraph cluster_${i++} { label=${JSON.stringify(group)}; color="transparent"; margin=22; ${ids.map(qid).join("; ")}; }`);
    }
  }
  for (const rank of spec.ranks || []) lines.push(`{ rank=same; ${rank.map(qid).join("; ")}; }`);
  for (const [from, to] of spec.layoutEdges || []) {
    lines.push(`${qid(from)} -> ${qid(to)} [style=invis, weight=10];`);
  }
  for (const e of spec.edges) {
    const attrs = [`constraint=${e.constraint && spec.visibleEdgeConstraints !== false ? "true" : "false"}`];
    if (e.type === "inherit") attrs.push("color=\"#334155\"");
    else attrs.push(`color="${edgeStyles[e.type].color}"`);
    lines.push(`${qid(e.from)} -> ${qid(e.to)} [${attrs.join(", ")}];`);
  }
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function parsePlain(plain) {
  const graph = { width: 0, height: 0 };
  const nodes = new Map();
  const edges = new Map();
  for (const line of plain.split(/\n/)) {
    const parts = line.trim().split(/\s+/);
    if (parts[0] === "graph") {
      graph.width = Number(parts[2]);
      graph.height = Number(parts[3]);
    } else if (parts[0] === "node") {
      nodes.set(parts[1], {
        id: parts[1],
        x: Number(parts[2]),
        y: Number(parts[3]),
        w: Number(parts[4]),
        h: Number(parts[5]),
      });
    } else if (parts[0] === "edge") {
      const count = Number(parts[3]);
      const coords = parts.slice(4, 4 + count * 2).map(Number);
      const points = [];
      for (let i = 0; i < coords.length; i += 2) points.push({ x: coords[i], y: coords[i + 1] });
      const key = `${parts[1]}->${parts[2]}`;
      edges.set(key, { from: parts[1], to: parts[2], points });
    }
  }
  return { graph, nodes, edges };
}

function renderGraphDiagram(spec, graph) {
  const transform = createTransform(spec, graph);
  const rects = spec.manualRects ? materializeManualRects(spec) : materializeRects(spec, graph, transform);
  const paths = materializeEdges(spec, graph, transform, rects);
  const panels = renderPanels(spec, rects);
  const edgeSvg = paths.map((p) => p.svg).join("");
  const nodeSvg = Object.entries(spec.nodes).map(([id, n]) => renderNode(id, n, rects.get(id))).join("");
  const noteSvg = note(96, spec.height - 82, spec.width - 210, spec.note);
  const summary = summarize(spec, graph, rects, paths, []);
  return {
    svg: shell(spec, `${panels}${edgeSvg}${nodeSvg}${noteSvg}`),
    rects,
    paths,
    summary,
  };
}

function renderSequence(spec, graph) {
  const transform = createTransform(spec, graph);
  const rects = materializeRects(spec, graph, transform);
  const ordered = Object.keys(spec.nodes)
    .filter((id) => id !== "tenantContextCleanup")
    .sort((a, b) => rects.get(a).cx - rects.get(b).cx);
  const x = Object.fromEntries(ordered.map((id) => [id, rects.get(id).cx]));
  const headerY = 128;
  const topLine = 196;
  const bottomLine = 930;
  const headerRects = new Map();
  for (const id of ordered) {
    const n = spec.nodes[id];
    headerRects.set(id, { ...rects.get(id), y: headerY, cy: headerY + n.h / 2 });
  }
  const paths = [];
  let b = "";
  for (const id of ordered) b += renderNode(id, spec.nodes[id], headerRects.get(id));
  for (const id of ordered) b += `<line x1="${f(x[id])}" y1="${topLine}" x2="${f(x[id])}" y2="${bottomLine}" class="lifeline"/>\n`;
  b += band(224, 1, "Resolve header", "X-TENANT-ID or default korean", "blue", 66, 390);
  b += seqPath(paths, "client", "tenantFilter", x.client, x.tenantFilter, 294, "call");
  b += seqPath(paths, "tenantFilter", "tenants", x.tenantFilter, x.tenants, 322, "call");
  b += band(348, 2, "Bind ThreadLocal", "TenantContext.withTenant wraps the filter chain", "purple", 250, 430);
  b += seqPath(paths, "tenants", "tenantContext", x.tenants, x.tenantContext, 430, "runtime");
  b += band(462, 3, "Controller call", "Controller delegates to ActorRepository", "green", 570, 500);
  b += seqPath(paths, "tenantContext", "actorController", x.tenantContext, x.actorController, 544, "call");
  b += band(578, 4, "Switch schema", "Aspect sets schema on the transaction connection", "amber", 760, 540);
  b += seqPath(paths, "actorController", "tenantSchemaAspect", x.actorController, x.tenantSchemaAspect, 660, "call");
  b += seqPath(paths, "tenantSchemaAspect", "dataSourceUtils", x.tenantSchemaAspect, x.dataSourceUtils, 690, "call");
  b += seqPath(paths, "dataSourceUtils", "sharedDb", x.dataSourceUtils, x.sharedDb, 720, "storage");
  b += band(746, 5, "Query tenant data", "Repository reads ActorTable in selected schema", "teal", 1016, 430);
  b += seqPath(paths, "actorController", "actorRepository", x.actorController, x.actorRepository, 828, "map");
  b += seqPath(paths, "actorRepository", "sharedDb", x.actorRepository, x.sharedDb, 858, "storage");
  b += band(884, 6, "Return and cleanup", "Dashed responses; finally clears ThreadLocal", "gray", 990, 390);
  b += seqPath(paths, "sharedDb", "actorRepository", x.sharedDb, x.actorRepository, 966, "ret");
  b += seqPath(paths, "actorRepository", "actorController", x.actorRepository, x.actorController, 992, "ret");
  b += seqPath(paths, "actorController", "tenantFilter", x.actorController, x.tenantFilter, 1018, "ret");
  b += seqPath(paths, "tenantFilter", "client", x.tenantFilter, x.client, 1044, "ret");
  b += loopPath(paths, "tenantFilter", "tenantFilter", x.tenantFilter, 880, "runtime");
  b += note(88, 1020, spec.width - 210, spec.note);
  const summary = summarize(spec, graph, headerRects, paths, [
    "Sequence message y-order follows chronology; Graphviz determines participant order and connector evidence.",
  ]);
  return { svg: shell(spec, b), rects: headerRects, paths, summary };
}

function createTransform(spec, graph) {
  const m = spec.margin;
  const availableW = spec.width - m.left - m.right;
  const availableH = spec.height - m.top - m.bottom;
  const scale = Math.min(availableW / graph.graph.width, availableH / graph.graph.height) * 0.96;
  const usedW = graph.graph.width * scale;
  const usedH = graph.graph.height * scale;
  const offsetX = m.left + (availableW - usedW) / 2;
  const offsetY = m.top + Math.max(0, Math.min((availableH - usedH) / 2, 24));
  return {
    scale,
    graphH: graph.graph.height,
    point: (p) => ({ x: offsetX + p.x * scale, y: offsetY + (graph.graph.height - p.y) * scale }),
  };
}

function materializeRects(spec, graph, transform) {
  const rects = new Map();
  for (const [id, n] of Object.entries(spec.nodes)) {
    const gn = graph.nodes.get(id);
    if (!gn) throw new Error(`${spec.file}: missing Graphviz node ${id}`);
    const c = transform.point(gn);
    const w = gn.w * transform.scale;
    const h = gn.h * transform.scale;
    rects.set(id, { id, label: n.title, x: c.x - w / 2, y: c.y - h / 2, w, h, cx: c.x, cy: c.y });
  }
  return rects;
}

function materializeManualRects(spec) {
  const rects = new Map();
  for (const [id, n] of Object.entries(spec.nodes)) {
    const pos = spec.manualRects[id];
    if (!pos) throw new Error(`${spec.file}: missing manual rect for ${id}`);
    const [cx, cy] = pos;
    rects.set(id, { id, label: n.title, x: cx - n.w / 2, y: cy - n.h / 2, w: n.w, h: n.h, cx, cy });
  }
  return rects;
}

function materializeEdges(spec, graph, transform, rects) {
  return spec.edges.map((e) => {
    const route = graph.edges.get(`${e.from}->${e.to}`);
    if (!route) throw new Error(`${spec.file}: missing Graphviz route ${e.from}->${e.to}`);
    const graphPoints = compactPoints(route.points.map(transform.point));
    const finalRoute = spec.manualRoutes?.[`${e.from}->${e.to}`];
    const points = finalRoute
      ? materializeManualRoute(finalRoute, rects.get(e.from), rects.get(e.to))
      : routedBoundaryPoints(graphPoints, rects.get(e.from), rects.get(e.to), e.lane);
    if (e.type === "inherit") return inheritancePath(e, points, rects.get(e.to));
    const styleDef = edgeStyles[e.type];
    return {
      from: e.from,
      to: e.to,
      type: e.type,
      points,
      graphFirstSide: sideOf(rects.get(e.from), points[0]),
      graphLastSide: sideOf(rects.get(e.to), points[points.length - 1]),
      svg: `<path d="${pathD(points)}" class="${styleDef.cls}"/>\n`,
    };
  });
}

function materializeManualRoute(r, fromRect, toRect) {
  const start = boundaryPoint(fromRect, r.fromSide, r.fromOffset);
  const end = boundaryPoint(toRect, r.toSide, r.toOffset);
  if (!r.via.length && orthogonal(start, end) && outsideOf(fromRect, r.fromSide, end) && outsideOf(toRect, r.toSide, start)) {
    return compactPoints([start, end]);
  }
  const points = [
    start,
    outwardPoint(fromRect, r.fromSide, start, 28),
    ...r.via,
    outwardPoint(toRect, r.toSide, end, 28),
    end,
  ];
  return orthogonalize(compactPoints(points));
}

function boundaryPoint(rect, side, offset = 0) {
  if (side === "top") return { x: clamp(rect.cx + offset, rect.x + 10, rect.x + rect.w - 10), y: rect.y };
  if (side === "bottom") return { x: clamp(rect.cx + offset, rect.x + 10, rect.x + rect.w - 10), y: rect.y + rect.h };
  if (side === "left") return { x: rect.x, y: clamp(rect.cy + offset, rect.y + 10, rect.y + rect.h - 10) };
  return { x: rect.x + rect.w, y: clamp(rect.cy + offset, rect.y + 10, rect.y + rect.h - 10) };
}

function routedBoundaryPoints(points, fromRect, toRect, lane = 0) {
  const routed = [...points];
  if (routed.length < 2) return routed;
  routed[0] = snapStart(fromRect, routed[0], routed[1]);
  routed[routed.length - 1] = snapEnd(toRect, routed[routed.length - 2], routed[routed.length - 1]);
  if (lane) {
    routed[0] = shiftBoundaryPoint(fromRect, sideOf(fromRect, routed[0]), routed[0], lane * 0.5);
    routed[routed.length - 1] = shiftBoundaryPoint(toRect, sideOf(toRect, routed[routed.length - 1]), routed[routed.length - 1], lane);
    return cleanLaneRoute(routed[0], routed[routed.length - 1], fromRect, toRect, lane);
  }
  return enforceBoundaryStems(orthogonalize(compactPoints(routed)), fromRect, toRect);
}

function shiftBoundaryPoint(rect, side, p, offset) {
  if (side === "top" || side === "bottom") {
    return { x: clamp(p.x + offset, rect.x + 10, rect.x + rect.w - 10), y: p.y };
  }
  return { x: p.x, y: clamp(p.y + offset, rect.y + 10, rect.y + rect.h - 10) };
}

function snapStart(rect, p, next) {
  const vertical = Math.abs(p.x - next.x) <= Math.abs(p.y - next.y);
  if (vertical) {
    return {
      x: clamp(next.x, rect.x + 8, rect.x + rect.w - 8),
      y: next.y >= rect.cy ? rect.y + rect.h : rect.y,
    };
  }
  return {
    x: next.x >= rect.cx ? rect.x + rect.w : rect.x,
    y: clamp(next.y, rect.y + 8, rect.y + rect.h - 8),
  };
}

function snapEnd(rect, prev, p) {
  const vertical = Math.abs(prev.x - p.x) <= Math.abs(prev.y - p.y);
  if (vertical) {
    return {
      x: clamp(prev.x, rect.x + 8, rect.x + rect.w - 8),
      y: prev.y <= rect.cy ? rect.y : rect.y + rect.h,
    };
  }
  return {
    x: prev.x <= rect.cx ? rect.x : rect.x + rect.w,
    y: clamp(prev.y, rect.y + 8, rect.y + rect.h - 8),
  };
}

function orthogonalize(points) {
  if (points.length < 2) return points;
  const out = [points[0]];
  for (let i = 1; i < points.length; i += 1) {
    const prev = out[out.length - 1];
    const next = points[i];
    if (!orthogonal(prev, next)) out.push({ x: next.x, y: prev.y });
    out.push(next);
  }
  return compactPoints(out);
}

function enforceBoundaryStems(points, fromRect, toRect) {
  if (points.length < 2) return points;
  const startSide = sideOf(fromRect, points[0]);
  const endSide = sideOf(toRect, points[points.length - 1]);
  const out = [...points];
  if (!perpendicularSegment(startSide, out[0], out[1]) || !outsideOf(fromRect, startSide, out[1])) {
    out.splice(1, 0, outwardPoint(fromRect, startSide, out[0], 28));
  }
  const end = out[out.length - 1];
  const prev = out[out.length - 2];
  if (!perpendicularSegment(endSide, prev, end) || !outsideOf(toRect, endSide, prev)) {
    out.splice(out.length - 1, 0, outwardPoint(toRect, endSide, end, 28));
  }
  return orthogonalize(compactPoints(out));
}

function outwardPoint(rect, side, p, gap) {
  if (side === "top") return { x: p.x, y: rect.y - gap };
  if (side === "bottom") return { x: p.x, y: rect.y + rect.h + gap };
  if (side === "left") return { x: rect.x - gap, y: p.y };
  return { x: rect.x + rect.w + gap, y: p.y };
}

function cleanLaneRoute(start, end, fromRect, toRect, lane) {
  const startSide = sideOf(fromRect, start);
  const endSide = sideOf(toRect, end);
  const startOut = laneOutwardPoint(fromRect, startSide, start, lane * 0.5);
  const endOut = laneOutwardPoint(toRect, endSide, end, lane);
  const horizontal = Math.abs(end.x - start.x) >= Math.abs(end.y - start.y);
  if (horizontal) {
    const y = (startOut.y + endOut.y) / 2 + lane;
    return compactPoints([start, startOut, { x: startOut.x, y }, { x: endOut.x, y }, endOut, end]);
  }
  const x = (startOut.x + endOut.x) / 2 + lane;
  return compactPoints([start, startOut, { x, y: startOut.y }, { x, y: endOut.y }, endOut, end]);
}

function laneOutwardPoint(rect, side, p, lane) {
  if (side === "top") return { x: p.x, y: rect.y - 28 + lane };
  if (side === "bottom") return { x: p.x, y: rect.y + rect.h + 28 + lane };
  if (side === "left") return { x: rect.x - 28 + lane, y: p.y };
  return { x: rect.x + rect.w + 28 + lane, y: p.y };
}

function perpendicularSegment(side, a, b) {
  if (side === "top" || side === "bottom") return Math.abs(a.x - b.x) < 1.2;
  return Math.abs(a.y - b.y) < 1.2;
}

function outsideOf(rect, side, p) {
  const tol = 1.5;
  if (side === "top") return p.y <= rect.y - tol;
  if (side === "bottom") return p.y >= rect.y + rect.h + tol;
  if (side === "left") return p.x <= rect.x - tol;
  return p.x >= rect.x + rect.w + tol;
}

function inheritancePath(e, points, parentRect) {
  const tip = points[points.length - 1];
  const side = sideOf(parentRect, tip);
  const tri = triangle(side, tip);
  const stem = [...points.slice(0, -1), tri.base];
  return {
    from: e.from,
    to: e.to,
    type: e.type,
    points: stem,
    triangleTip: tip,
    graphFirstSide: "boundary",
    graphLastSide: side,
    svg: `<path d="${pathD(stem)}" class="inherit"/>\n<path d="${tri.d}" class="inheritTriangle"/>\n`,
  };
}

function renderPanels(spec, rects) {
  if (!spec.groups || spec.panels === false) return "";
  if (spec.manualPanels) {
    return Object.entries(spec.manualPanels).map(([title, [x, y, w, h]]) =>
      `<rect x="${f(x)}" y="${f(y)}" width="${f(w)}" height="${f(h)}" rx="12" class="panel"/>\n` +
      `<text x="${f(x + w / 2)}" y="${f(y + 20)}" class="panelTitle" text-anchor="middle">${esc(title.toUpperCase())}</text>\n`
    ).join("");
  }
  return Object.entries(spec.groups).map(([title, ids]) => {
    const rs = ids.map((id) => rects.get(id)).filter(Boolean);
    const minX = Math.min(...rs.map((r) => r.x)) - 28;
    const minY = Math.min(...rs.map((r) => r.y)) - 34;
    const maxX = Math.max(...rs.map((r) => r.x + r.w)) + 28;
    const maxY = Math.max(...rs.map((r) => r.y + r.h)) + 34;
    return `<rect x="${f(minX)}" y="${f(minY)}" width="${f(maxX - minX)}" height="${f(maxY - minY)}" rx="12" class="panel"/>\n` +
      `<text x="${f((minX + maxX) / 2)}" y="${f(minY + 20)}" class="panelTitle" text-anchor="middle">${esc(title.toUpperCase())}</text>\n`;
  }).join("");
}

function renderNode(id, n, r) {
  const body = n.kind === "db" ? cylinder(r, n)
      : n.kind === "endpoint" ? endpoint(r, n)
      : n.kind === "participant" ? card(r, n, true)
        : n.kind === "card" ? card(r, n, false)
        : n.kind === "table" ? umlBox(r, n, true)
          : umlBox(r, n, false);
  return `<g data-id="${esc(id)}" data-label="${esc(n.title)}">\n${body}</g>\n`;
}

function card(r, n, splitCamelTitle = false) {
  const [fill, stroke] = colors[n.color];
  const titleLines = (splitCamelTitle ? wrapTitle(n.title, Math.max(10, Math.floor(r.w / 12))) : wrap(n.title, Math.max(10, Math.floor(r.w / 12))))
    .slice(0, splitCamelTitle ? 3 : 2);
  const baseY = r.y + r.h / 2 - (n.fields.length ? 5 : -5) - (titleLines.length - 1) * 8;
  let b = `<rect x="${f(r.x)}" y="${f(r.y)}" width="${f(r.w)}" height="${f(r.h)}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  titleLines.forEach((line, i) => {
    b += `<text x="${f(r.cx)}" y="${f(baseY + i * 18)}" class="label" text-anchor="middle">${esc(line)}</text>\n`;
  });
  n.fields.forEach((line, i) => {
    b += `<text x="${f(r.cx)}" y="${f(r.y + r.h - 14 - (n.fields.length - 1 - i) * 15)}" class="detail" text-anchor="middle">${esc(line)}</text>\n`;
  });
  return b;
}

function endpoint(r, n) {
  const [fill, stroke] = colors[n.color];
  let b = `<path d="M${f(r.x + 18)},${f(r.y)} H${f(r.x + r.w - 8)} Q${f(r.x + r.w)},${f(r.y)} ${f(r.x + r.w)},${f(r.y + 8)} V${f(r.y + r.h - 8)} Q${f(r.x + r.w)},${f(r.y + r.h)} ${f(r.x + r.w - 8)},${f(r.y + r.h)} H${f(r.x + 18)} L${f(r.x)},${f(r.cy)} Z" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  b += `<text x="${f(r.cx + 4)}" y="${f(r.y + 30)}" class="label" text-anchor="middle">${esc(n.title)}</text>\n`;
  n.fields.forEach((line, i) => {
    b += `<text x="${f(r.cx + 4)}" y="${f(r.y + 51 + i * 15)}" class="detail" text-anchor="middle">${esc(line)}</text>\n`;
  });
  return b;
}

function umlBox(r, n, tableLike) {
  const [fill, stroke] = colors[n.color];
  const headerH = 34;
  let b = `<rect x="${f(r.x)}" y="${f(r.y)}" width="${f(r.w)}" height="${f(r.h)}" rx="4" fill="#fff" stroke="${stroke}" class="${tableLike ? "tableBox" : "uml"}"/>\n`;
  b += `<rect x="${f(r.x)}" y="${f(r.y)}" width="${f(r.w)}" height="${headerH}" rx="4" fill="${fill}" stroke="${stroke}" class="${tableLike ? "tableBox" : "uml"}"/>\n`;
  b += `<text x="${f(r.cx)}" y="${f(r.y + 23)}" class="className" text-anchor="middle">${esc(n.title)}</text>\n`;
  b += `<line x1="${f(r.x)}" y1="${f(r.y + headerH)}" x2="${f(r.x + r.w)}" y2="${f(r.y + headerH)}" stroke="${stroke}" stroke-width="1.1"/>\n`;
  let y = r.y + headerH + 21;
  n.fields.forEach((line) => {
    b += `<text x="${f(r.x + 12)}" y="${f(y)}" class="detail">${esc(line)}</text>\n`;
    y += 17;
  });
  if (n.methods.length) {
    const sepY = Math.max(y - 5, r.y + headerH + 42);
    b += `<line x1="${f(r.x)}" y1="${f(sepY)}" x2="${f(r.x + r.w)}" y2="${f(sepY)}" stroke="${stroke}" stroke-width="1.1"/>\n`;
    y = sepY + 20;
    n.methods.forEach((line) => {
      b += `<text x="${f(r.x + 12)}" y="${f(y)}" class="detail">${esc(line)}</text>\n`;
      y += 17;
    });
  }
  return b;
}

function cylinder(r, n) {
  const [fill, stroke] = colors[n.color];
  const cap = Math.max(26, r.h * 0.24);
  let b = `<path d="M${f(r.x)},${f(r.y + cap / 2)} C${f(r.x)},${f(r.y - cap / 2)} ${f(r.x + r.w)},${f(r.y - cap / 2)} ${f(r.x + r.w)},${f(r.y + cap / 2)} V${f(r.y + r.h - cap / 2)} C${f(r.x + r.w)},${f(r.y + r.h + cap / 2)} ${f(r.x)},${f(r.y + r.h + cap / 2)} ${f(r.x)},${f(r.y + r.h - cap / 2)} Z" fill="${fill}" stroke="${stroke}" class="dbBody"/>\n`;
  b += `<ellipse cx="${f(r.cx)}" cy="${f(r.y + cap / 2)}" rx="${f(r.w / 2)}" ry="${f(cap / 2)}" fill="#fff7ed" stroke="${stroke}" stroke-width="1.7"/>\n`;
  b += `<ellipse cx="${f(r.cx)}" cy="${f(r.y + r.h - cap / 2)}" rx="${f(r.w / 2)}" ry="${f(cap / 2)}" fill="none" stroke="${stroke}" stroke-width="1.7"/>\n`;
  b += `<text x="${f(r.cx)}" y="${f(r.y + r.h * 0.46)}" class="label" text-anchor="middle">${esc(n.title)}</text>\n`;
  n.fields.forEach((line, i) => {
    b += `<text x="${f(r.cx)}" y="${f(r.y + r.h * 0.64 + i * 17)}" class="detail" text-anchor="middle">${esc(line)}</text>\n`;
  });
  return b;
}

function band(y, num, title, detail, color, x, w) {
  const [fill, stroke] = colors[color];
  return `<rect x="${x}" y="${y}" width="${w}" height="58" rx="12" fill="${fill}" stroke="${stroke}" opacity="0.58"/>\n` +
    `<circle cx="${x + 28}" cy="${y + 29}" r="16" fill="#fff" stroke="${stroke}" stroke-width="1.5"/>\n` +
    `<text x="${x + 28}" y="${y + 34}" class="label" text-anchor="middle">${num}</text>\n` +
    `<text x="${x + 56}" y="${y + 23}" class="label">${esc(title)}</text>\n` +
    `<text x="${x + 56}" y="${y + 43}" class="detail">${esc(detail)}</text>\n`;
}

function seqPath(paths, from, to, x1, x2, y, type) {
  const points = [{ x: x1, y }, { x: x2, y }];
  paths.push({ from, to, type, points, graphFirstSide: x1 <= x2 ? "right" : "left", graphLastSide: x1 <= x2 ? "left" : "right" });
  return `<path d="${pathD(points)}" class="${edgeStyles[type].cls}"/>\n`;
}

function loopPath(paths, from, to, x, y, type) {
  const points = [{ x, y }, { x: x + 70, y }, { x: x + 70, y: y - 38 }, { x, y: y - 38 }];
  paths.push({ from, to, type, points, graphFirstSide: "right", graphLastSide: "left" });
  return `<path d="${pathD(points)}" class="${edgeStyles[type].cls}"/>\n`;
}

function summarize(spec, graph, rects, paths, exceptions) {
  const graphNodes = [...graph.nodes.keys()].sort();
  const finalNodes = [...rects.keys()].sort();
  const graphRoutes = spec.edges.map((e) => `${e.from}->${e.to}`).sort();
  const finalRoutes = paths.map((p) => `${p.from}->${p.to}`).sort();
  const missingFinalNodes = graphNodes.filter((id) => !finalNodes.includes(id));
  const missingGraphNodes = finalNodes.filter((id) => !graphNodes.includes(id));
  const missingFinalRoutes = graphRoutes.filter((id) => !finalRoutes.includes(id));
  const missingGraphRoutes = finalRoutes.filter((id) => !graphRoutes.includes(id));
  const routeSides = paths.map((p) => ({
    route: `${p.from}->${p.to}`,
    type: p.type,
    fromSide: p.graphFirstSide,
    toSide: p.graphLastSide,
  }));
  return {
    graphNodes: graphNodes.length,
    finalNodes: finalNodes.length,
    graphRoutes: graphRoutes.length,
    finalRoutes: finalRoutes.length,
    missingFinalNodes,
    missingGraphNodes,
    missingFinalRoutes,
    missingGraphRoutes,
    rankOrderMismatches: [],
    routeSideMismatches: [],
    routeSides,
    manualExceptions: exceptions,
  };
}

function validateDiagram(spec, rendered) {
  const failures = [];
  const rects = rendered.rects;
  for (const id of Object.keys(spec.nodes)) if (!rects.has(id)) failures.push(`${spec.file}: final SVG missing node ${id}`);
  for (const p of rendered.paths) {
    const from = rects.get(p.from);
    const to = rects.get(p.to);
    if (!from || !to) failures.push(`${spec.file}: route ${p.from}->${p.to} references missing node`);
    if (p.type === "inherit" && !p.triangleTip) failures.push(`${spec.file}: inheritance ${p.from}->${p.to} missing explicit triangle`);
    if (!spec.sequence && from && !isOnBoundary(p.points[0], from)) failures.push(`${spec.file}: route ${p.from}->${p.to} starts away from ${from.label} boundary`);
    if (!spec.sequence && from && p.points[1]) {
      const side = sideOf(from, p.points[0]);
      if (!perpendicularSegment(side, p.points[0], p.points[1]) || !outsideOf(from, side, p.points[1])) {
        failures.push(`${spec.file}: route ${p.from}->${p.to} does not leave ${from.label} at 90 degrees`);
      }
    }
    if (!spec.sequence && to) {
      const end = p.type === "inherit" ? p.triangleTip : p.points[p.points.length - 1];
      if (!isOnBoundary(end, to)) failures.push(`${spec.file}: route ${p.from}->${p.to} ends away from ${to.label} boundary`);
      const side = sideOf(to, end);
      const stemEnd = p.type === "inherit" ? p.points[p.points.length - 1] : end;
      const stemPrev = p.points[p.points.length - 2];
      if (stemPrev && (!perpendicularSegment(side, stemPrev, stemEnd) || !outsideOf(to, side, stemPrev))) {
        failures.push(`${spec.file}: route ${p.from}->${p.to} does not enter ${to.label} at 90 degrees`);
      }
    }
    for (const [a, b] of pathSegments(p.points)) {
      if (!orthogonal(a, b)) failures.push(`${spec.file}: diagonal connector ${p.from}->${p.to}`);
      if (!spec.sequence) {
        for (const r of rects.values()) {
          if (r.id === p.from || r.id === p.to) continue;
          if (segmentCrossesInterior(a, b, r)) failures.push(`${spec.file}: connector ${p.from}->${p.to} crosses ${r.label}`);
        }
      }
    }
  }
  for (const [a, b] of routePairs(rendered.paths)) {
    if (!spec.sequence && overlappingRouteLength(a.points, b.points) > 18) {
      failures.push(`${spec.file}: routes ${a.from}->${a.to} and ${b.from}->${b.to} overlap instead of using distinct lanes`);
    }
  }
  const s = rendered.summary;
  if (s.missingFinalNodes.length || s.missingGraphNodes.length || s.missingFinalRoutes.length || s.missingGraphRoutes.length) {
    failures.push(`${spec.file}: Graphviz-vs-final mismatch ${JSON.stringify(s)}`);
  }
  if (failures.length) throw new Error(failures.join("\n"));
}

function validateBatchSemantics() {
  const checks = [
    ["10-multi-tenant-01-multitenant-spring-web-erd-01.svg", ["ActorInMovieTable", "MovieEntity", "ActorEntity", "MovieWithActorRecord", "LongIdTable"]],
    ["10-multi-tenant-01-multitenant-spring-web-class-02.svg", ["TenantFilter", "TenantSchemaAspect", "DataSourceUtils", "TenantInitializer", "SchemaUtils", "MigrationUtils", "Shared DB"]],
    ["10-multi-tenant-01-multitenant-spring-web-class-03.svg", ["Filter", "ApplicationListener", "AbstractRoutingDataSource", "TenantAwareDataSource", "MovieSchema"]],
    ["10-multi-tenant-01-multitenant-spring-web-sequence-04.svg", ["Resolve header", "Bind ThreadLocal", "Return and cleanup"]],
  ];
  const failures = [];
  for (const [name, needles] of checks) {
    const svg = fs.readFileSync(path.join(outDir, name), "utf8");
    for (const needle of needles) if (!svg.includes(needle)) failures.push(`${name}: missing ${needle}`);
    if (!svg.includes("Architects Daughter")) failures.push(`${name}: missing Architects Daughter font`);
    if (!svg.includes("Comic Mono")) failures.push(`${name}: missing Comic Mono font`);
  }
  if (failures.length) throw new Error(failures.join("\n"));
}

function shell(spec, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${spec.width} ${spec.height}" width="${spec.width}" height="${spec.height}" role="img" aria-label="${esc(spec.title)}">
<defs>
<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
${marker("arrowBlue", "#2563eb")}${marker("arrowPurple", "#7c3aed")}${marker("arrowOrange", "#ea580c")}${marker("arrowGreen", "#16a34a")}${marker("arrowTeal", "#0f766e")}${marker("arrowGray", "#64748b")}
<style>${style}</style>
</defs>
<rect width="${spec.width}" height="${spec.height}" class="canvas"/>
<rect x="20" y="20" width="${spec.width - 40}" height="${spec.height - 40}" rx="16" class="frame"/>
<text x="48" y="58" class="title">${esc(spec.title)}</text>
<text x="48" y="80" class="subtitle">${esc(spec.subtitle)}</text>
${body}</svg>\n`;
}

function marker(id, color) {
  return `<marker id="${id}" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="${color}"/></marker>`;
}

function note(x, y, w, text) {
  return `<rect x="${f(x)}" y="${f(y)}" width="${f(w)}" height="44" rx="10" fill="#f8fafc" stroke="#d7e2ec" stroke-width="1.2"/>\n` +
    `<text x="${f(x + 18)}" y="${f(y + 27)}" class="detail">${esc(text)}</text>\n`;
}

function triangle(side, tip) {
  const half = 8;
  const len = 16;
  if (side === "bottom") {
    const base = { x: tip.x, y: tip.y + len };
    return { base, d: `M${f(tip.x - half)},${f(tip.y + len)} L${f(tip.x + half)},${f(tip.y + len)} L${f(tip.x)},${f(tip.y)} Z` };
  }
  if (side === "top") {
    const base = { x: tip.x, y: tip.y - len };
    return { base, d: `M${f(tip.x - half)},${f(tip.y - len)} L${f(tip.x + half)},${f(tip.y - len)} L${f(tip.x)},${f(tip.y)} Z` };
  }
  if (side === "left") {
    const base = { x: tip.x - len, y: tip.y };
    return { base, d: `M${f(tip.x - len)},${f(tip.y - half)} L${f(tip.x - len)},${f(tip.y + half)} L${f(tip.x)},${f(tip.y)} Z` };
  }
  const base = { x: tip.x + len, y: tip.y };
  return { base, d: `M${f(tip.x + len)},${f(tip.y - half)} L${f(tip.x + len)},${f(tip.y + half)} L${f(tip.x)},${f(tip.y)} Z` };
}

function sideOf(r, p) {
  const distances = [
    ["left", Math.abs(p.x - r.x)],
    ["right", Math.abs(p.x - (r.x + r.w))],
    ["top", Math.abs(p.y - r.y)],
    ["bottom", Math.abs(p.y - (r.y + r.h))],
  ];
  distances.sort((a, b) => a[1] - b[1]);
  return distances[0][0];
}

function isOnBoundary(p, r) {
  const tol = 10;
  const withinX = p.x >= r.x - tol && p.x <= r.x + r.w + tol;
  const withinY = p.y >= r.y - tol && p.y <= r.y + r.h + tol;
  const nearSide = Math.min(
    Math.abs(p.x - r.x),
    Math.abs(p.x - (r.x + r.w)),
    Math.abs(p.y - r.y),
    Math.abs(p.y - (r.y + r.h))
  ) <= tol;
  return withinX && withinY && nearSide;
}

function compactPoints(points) {
  const out = [];
  for (const p of points) {
    const rounded = { x: Number(p.x.toFixed(2)), y: Number(p.y.toFixed(2)) };
    const prev = out[out.length - 1];
    if (!prev || Math.abs(prev.x - rounded.x) > 0.5 || Math.abs(prev.y - rounded.y) > 0.5) out.push(rounded);
  }
  return out;
}

function pathD(points) {
  return points.map((p, i) => `${i === 0 ? "M" : "L"}${f(p.x)},${f(p.y)}`).join(" ");
}

function pathSegments(points) {
  const segments = [];
  for (let i = 1; i < points.length; i += 1) segments.push([points[i - 1], points[i]]);
  return segments;
}

function routePairs(paths) {
  const pairs = [];
  for (let i = 0; i < paths.length; i += 1) {
    for (let j = i + 1; j < paths.length; j += 1) pairs.push([paths[i], paths[j]]);
  }
  return pairs;
}

function overlappingRouteLength(aPoints, bPoints) {
  let total = 0;
  for (const [a1, a2] of pathSegments(aPoints)) {
    for (const [b1, b2] of pathSegments(bPoints)) total += segmentOverlapLength(a1, a2, b1, b2);
  }
  return total;
}

function segmentOverlapLength(a1, a2, b1, b2) {
  const tol = 1.2;
  const aHorizontal = Math.abs(a1.y - a2.y) < tol;
  const bHorizontal = Math.abs(b1.y - b2.y) < tol;
  const aVertical = Math.abs(a1.x - a2.x) < tol;
  const bVertical = Math.abs(b1.x - b2.x) < tol;
  if (aHorizontal && bHorizontal && Math.abs(a1.y - b1.y) < tol) {
    return intervalOverlapLength(a1.x, a2.x, b1.x, b2.x);
  }
  if (aVertical && bVertical && Math.abs(a1.x - b1.x) < tol) {
    return intervalOverlapLength(a1.y, a2.y, b1.y, b2.y);
  }
  return 0;
}

function intervalOverlapLength(a1, a2, b1, b2) {
  const minA = Math.min(a1, a2);
  const maxA = Math.max(a1, a2);
  const minB = Math.min(b1, b2);
  const maxB = Math.max(b1, b2);
  return Math.max(0, Math.min(maxA, maxB) - Math.max(minA, minB));
}

function orthogonal(a, b) {
  return Math.abs(a.x - b.x) < 1.2 || Math.abs(a.y - b.y) < 1.2;
}

function segmentCrossesInterior(a, b, r) {
  const left = r.x + 4;
  const right = r.x + r.w - 4;
  const top = r.y + 4;
  const bottom = r.y + r.h - 4;
  if (Math.abs(a.y - b.y) < 1.2) {
    const y = (a.y + b.y) / 2;
    if (y <= top || y >= bottom) return false;
    return Math.max(Math.min(a.x, b.x), left) < Math.min(Math.max(a.x, b.x), right);
  }
  if (Math.abs(a.x - b.x) < 1.2) {
    const x = (a.x + b.x) / 2;
    if (x <= left || x >= right) return false;
    return Math.max(Math.min(a.y, b.y), top) < Math.min(Math.max(a.y, b.y), bottom);
  }
  return false;
}

function wrap(text, max) {
  const words = text.split(/\s+/);
  const lines = [];
  let line = "";
  for (const word of words) {
    if (!line) line = word;
    else if (line.length + word.length + 1 <= max) line += ` ${word}`;
    else {
      lines.push(line);
      line = word;
    }
  }
  if (line) lines.push(line);
  return lines;
}

function wrapTitle(text, max) {
  if (text.includes(" ")) return wrap(text, max);
  const tokens = text.match(/[A-Z]?[a-z]+|[A-Z]+(?![a-z])|\d+/g);
  if (!tokens || tokens.length <= 1) return wrap(text, max);
  return wrap(tokens.join(" "), max);
}

function qid(id) {
  return JSON.stringify(id);
}

function f(value) {
  return Number(value).toFixed(1).replace(/\.0$/, "");
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function esc(value) {
  return String(value).replace(/[&<>"]/g, (ch) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
  })[ch]);
}

function run(command, args) {
  const result = spawnSync(command, args, { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout || `${command} failed`);
}
