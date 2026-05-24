#!/usr/bin/env node

const fs = require("fs");
const { spawnSync } = require("child_process");

const RSVG = process.env.RSVG_CONVERT || "rsvg-convert";

const style = `
.canvas{fill:#f6f9fc}.frame{fill:#fff;stroke:#d7e2ec;stroke-width:1.5}
.title{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:32px;fill:#1e293b}
.subtitle,.detail{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;fill:#536476}
.subtitle{font-size:13px}.detail{font-size:12px}.tiny{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:10px;fill:#64748b}
.label{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:17px;fill:#1e293b}
.panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
.card{stroke-width:1.7;filter:url(#shadow)}.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
.arrow{fill:none;stroke:#64748b;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
.softArrow{fill:none;stroke:#94a3b8;stroke-width:1.35;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
.movieMap{fill:none;stroke:#16a34a;stroke-width:1.7;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
.actorMap{fill:none;stroke:#7c3aed;stroke-width:1.7;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.lifeline{fill:none;stroke:#94a3b8;stroke-width:1.5;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round}
.inherit{fill:none;stroke:#475569;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round}
.inheritHead{fill:#fff;stroke:#475569;stroke-width:1.8;stroke-linejoin:round}
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
    file: "docs/images/readme-diagrams/01-spring-boot-class-01.svg",
    title: "Spring Boot Module Comparison",
    subtitle: "Same Movie/Actor domain implemented with two Spring concurrency models",
    width: 1260,
    height: 690,
    body: moduleComparison,
  },
  {
    file: "docs/images/readme-diagrams/01-spring-boot-erd-02.svg",
    title: "Shared Movie/Actor Schema",
    subtitle: "Exposed table objects shared by Spring MVC and WebFlux examples",
    width: 1040,
    height: 610,
    body: sharedErd,
  },
  {
    file: "docs/images/readme-diagrams/01-spring-boot-spring-mvc-exposed-class-01.svg",
    title: "Spring MVC With Exposed Architecture",
    subtitle: "Controllers delegate to repositories that run Exposed work in Spring transactions",
    width: 1320,
    height: 720,
    body: springMvcArchitecture,
  },
  {
    file: "docs/images/readme-diagrams/01-spring-boot-spring-mvc-exposed-sequence-02.svg",
    title: "Spring MVC Request Processing Flow",
    subtitle: "Representative Actor API request through controller, repository, transaction, and response mapping",
    width: 1280,
    height: 760,
    body: springMvcSequence,
  },
  {
    file: "docs/images/readme-diagrams/01-spring-boot-spring-mvc-exposed-erd-03.svg",
    title: "Spring MVC Domain Model",
    subtitle: "Tables, DAO entities, and DTO records used by Movie and Actor APIs",
    width: 1460,
    height: 860,
    body: springMvcDomain,
  },
];

function moduleComparison() {
  let b = panel(56, 120, 520, 330, "Spring MVC module");
  b += card(86, 152, 210, 72, "Actor Controller / Movie Controller", 0, "Servlet endpoints");
  b += card(336, 152, 210, 72, "Tomcat Virtual Threads", 6, "blocking JDBC is acceptable");
  b += card(86, 260, 210, 72, "@Transactional boundary", 2, "Spring AOP transaction");
  b += card(336, 260, 210, 72, "Exposed JDBC DSL / DAO", 5, "repository methods");
  b += card(206, 368, 250, 54, "HikariCP + Profiles", 7, "H2 / MySQL / PostgreSQL");

  b += panel(684, 120, 520, 330, "Spring WebFlux module");
  b += card(714, 152, 210, 72, "Suspend Controllers", 1, "non-blocking HTTP layer");
  b += card(964, 152, 210, 72, "Netty EventLoop", 4, "kept off JDBC blocking work");
  b += card(714, 260, 210, 72, "Dispatchers.IO bridge", 2, "moves JDBC work");
  b += card(964, 260, 210, 72, "new Suspended Transaction", 5, "coroutine transaction");
  b += card(834, 368, 250, 54, "Same schema and API surface", 7);

  b += panel(120, 510, 1020, 96, "Shared learning target");
  b += card(160, 536, 210, 44, "Movie / Actor domain", 0);
  b += card(410, 536, 210, 44, "Repository search patterns", 2);
  b += card(660, 536, 210, 44, "Swagger verified APIs", 6);
  b += card(910, 536, 190, 44, "Profile switching", 5);
  b += arrow(576, 284, 684, 284);
  b += softArrow(290, 450, 290, 510) + softArrow(970, 450, 970, 510);
  return b;
}

function sharedErd() {
  let b = erdTable(96, 154, 278, "MovieTable", ["id BIGSERIAL PK", "name varchar(255)", "producer_name varchar(255)", "release_date timestamp"], 0);
  b += erdTable(666, 154, 278, "ActorTable", ["id BIGSERIAL PK", "first_name varchar(255)", "last_name varchar(255)", "birthday date nullable"], 1);
  b += erdTable(382, 374, 278, "ActorInMovieTable", ["movie_id FK -> MovieTable", "actor_id FK -> ActorTable", "primary key(movie_id, actor_id)", "ON DELETE CASCADE"], 2);
  b += relation(521, 374, 235, 308, "N:1 movie_id");
  b += relation(521, 374, 805, 308, "N:1 actor_id");
  b += note(156, 514, 728, "Bridge table models the many-to-many cast relation; FK arrows point from child table to parent table.");
  return b;
}

function springMvcArchitecture() {
  let b = panel(42, 118, 286, 410, "HTTP entry");
  b += card(66, 144, 226, 54, "Client / Swagger UI", 0, "REST caller");
  b += apiDocument(88, 218, 182, 62, "OpenAPI", "Swagger config", 0);
  b += card(66, 304, 226, 54, "Actor Controller", 1, "/actors");
  b += card(66, 374, 226, 54, "Movie Controller", 2, "/movies");
  b += card(66, 444, 226, 54, "Movie Actors Controller", 3);

  b += panel(390, 118, 290, 410, "Application logic");
  b += card(420, 144, 230, 58, "@Transactional AOP", 6, "readOnly/write boundary");
  b += card(420, 258, 230, 66, "Actor Repository", 4, "find/search/create/delete");
  b += card(420, 398, 230, 66, "Movie Repository", 5, "join + DAO eager load");

  b += panel(760, 118, 270, 410, "Exposed model");
  b += card(796, 144, 198, 58, "Movie Entity / Actor Entity", 5);
  b += card(796, 252, 198, 58, "Actor Table", 1, "actors");
  b += card(796, 342, 198, 58, "Movie Table", 0, "movies");
  b += card(796, 432, 198, 58, "Actor In Movie Table", 2, "bridge");

  b += panel(1090, 118, 188, 410, "Runtime");
  b += card(1108, 150, 152, 58, "Tomcat VT", 6, "per task executor");
  b += card(1108, 288, 152, 58, "HikariCP", 7, "connection pool");
  b += dbCylinder(1112, 414, 144, 74, "H2 / MySQL / PostgreSQL", 3);

  b += orthArrow(270, 249, 292, 171, 314, "softArrow");
  b += orthArrow(292, 331, 420, 291, 360);
  b += orthArrow(292, 401, 420, 431, 360);
  b += orthArrow(292, 471, 420, 431, 360);
  b += orthArrow(650, 291, 796, 281, 720);
  b += orthArrow(650, 431, 796, 371, 720);
  b += orthArrow(650, 431, 796, 461, 720);
  b += orthArrow(994, 281, 1108, 317, 1054, "softArrow");
  b += orthArrow(994, 371, 1108, 317, 1054, "softArrow");
  b += orthArrow(994, 461, 1108, 317, 1054, "softArrow");
  b += `<path d="M1184,346 L1184,414" class="arrow"/>\n`;
  b += note(92, 582, 1136, "The MVC layer stays synchronous; Spring owns transaction demarcation while Exposed repositories own SQL and DAO mapping.");
  return b;
}

function springMvcSequence() {
  const xs = [126, 330, 534, 738, 942, 1146];
  const names = ["Client", "ActorController", "ActorRepository", "Spring Transaction", "ActorTable", "ActorRecord"];
  let b = "";
  xs.forEach((x, i) => {
    b += card(x - 72, 122, 144, 52, names[i], i, "");
    b += `<path d="M${x},184 L${x},682" class="lifeline"/>\n`;
  });
  [
    [0, 1, "GET /actors/{id}", 224],
    [1, 2, "getActorById() delegates", 286],
    [2, 3, "enter readOnly transaction", 348],
    [3, 4, "select ActorTable by id", 410],
    [4, 5, "map ResultRow to ActorRecord", 472],
    [5, 2, "return nullable record", 534],
    [2, 1, "controller wraps ResponseEntity", 596],
    [1, 0, "200 OK or 404 Not Found", 658],
  ].forEach(([from, to, label, y], i) => {
    b += band(xs[from], xs[to], y, `${i + 1}. ${label}`, to < from);
  });
  b += note(72, 686, 1136, "Search/create/delete follow the same boundary: controller validates request shape, repository composes Exposed DSL, transaction returns DTOs.");
  return b;
}

function springMvcDomain() {
  let b = panel(58, 120, 580, 250, "Persistence tables");
  b += erdTable(86, 160, 230, "MovieTable", ["id PK", "name", "producerName", "releaseDate"], 0, 130);
  b += erdTable(370, 160, 230, "ActorTable", ["id PK", "firstName", "lastName", "birthday"], 1, 130);
  b += erdTable(240, 390, 250, "ActorInMovieTable", ["movieId FK", "actorId FK", "composite PK"], 2, 128);

  b += panel(720, 120, 660, 250, "DAO entities");
  b += umlClass(934, 146, 230, 90, "LongEntity", ["id: EntityID<Long>"], 7, "superclass");
  b += umlClass(780, 280, 240, 90, "MovieEntity", ["name", "producerName", "actors"], 3, "DAO");
  b += umlClass(1080, 280, 240, 90, "ActorEntity", ["firstName", "lastName", "movies"], 4, "DAO");
  b += inheritArrow(900, 280, 1000, 236, 264);
  b += inheritArrow(1200, 280, 1100, 236, 264);

  b += panel(90, 550, 1280, 160, "DTO records");
  b += umlClass(130, 580, 190, 108, "MovieRecord", ["id", "name", "producerName"], 0, "record");
  b += umlClass(350, 580, 230, 108, "MovieActorCountRecord", ["movieName", "actorCount"], 6, "record");
  b += umlClass(610, 580, 220, 108, "MovieWithProducingActorRecord", ["movieName", "producerActorName"], 5, "record");
  b += umlClass(860, 580, 260, 108, "MovieWithActorRecord", ["movie", "actors: List<ActorRecord>"], 2, "record");
  b += umlClass(1150, 580, 190, 108, "ActorRecord", ["id", "firstName", "lastName"], 1, "record");
  b += relation(365, 390, 201, 290, "movieId");
  b += relation(365, 390, 485, 290, "actorId");
  b += `<path d="M900,370 L900,532 L990,532 L990,580" class="movieMap"/>\n`;
  b += `<path d="M1200,370 L1200,532 L1245,532 L1245,580" class="actorMap"/>\n`;
  b += note(180, 760, 1100, "DAO entities model relationships for eager loading; controllers return immutable record DTOs, not Exposed entities.");
  return b;
}

function shell(diagram) {
  const body = diagram.body();
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${diagram.width} ${diagram.height}" width="${diagram.width}" height="${diagram.height}" role="img" aria-label="${esc(diagram.title)}">
<defs>
<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
<marker id="arrow" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#64748b"/></marker>
<marker id="arrowGreen" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#16a34a"/></marker>
<marker id="arrowPurple" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#7c3aed"/></marker>
<marker id="inherit" markerWidth="14" markerHeight="12" refX="12" refY="6" orient="auto"><path d="M1,1 L12,6 L1,11 Z" fill="#fff" stroke="#475569" stroke-width="1.8"/></marker>
<style>${style}</style>
</defs>
<rect width="${diagram.width}" height="${diagram.height}" class="canvas"/>
<rect x="20" y="20" width="${diagram.width - 40}" height="${diagram.height - 40}" rx="16" class="frame"/>
<text x="48" y="58" class="title">${esc(diagram.title)}</text>
<text x="48" y="80" class="subtitle">${esc(diagram.subtitle)}</text>
${body}</svg>
`;
}

function panel(x, y, w, h, title) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" class="panel"/><text x="${x + w / 2}" y="${y - 8}" class="panelTitle" text-anchor="middle">${esc(title.toUpperCase())}</text>\n`;
}

function card(x, y, w, h, title, c, detail = "") {
  const [fill, stroke] = colors[c % colors.length];
  const titleLines = wrap(title, Math.floor(w / 10), detail ? 2 : 2);
  const shownDetail = titleLines.length > 1 && h < 70 ? "" : detail;
  const titleY = y + (shownDetail ? 24 : h / 2 - (titleLines.length - 1) * 10 + 6);
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${titleY + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  if (shownDetail) out += `<text x="${x + w / 2}" y="${y + h - 14}" class="detail" text-anchor="middle">${esc(shownDetail)}</text>\n`;
  return out;
}

function erdTable(x, y, w, title, rows, c, h = 170) {
  const [fill, stroke] = colors[c % colors.length];
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + 40} L${x},${y + 12} Q${x},${y} ${x + 12},${y} L${x + w - 12},${y} Q${x + w},${y} ${x + w},${y + 12} L${x + w},${y + 40} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 27}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((r, i) => (out += `<text x="${x + 18}" y="${y + 66 + i * 17}" class="tiny">${esc(r)}</text>\n`));
  return out;
}

function umlClass(x, y, w, h, title, rows, c, stereotype = "") {
  const [fill, stroke] = colors[c % colors.length];
  const titleLines = wrapIdentifier(title, Math.floor(w / 10), 2);
  const headerH = titleLines.length > 1 ? 60 : 46;
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + headerH} L${x},${y + 4} Q${x},${y} ${x + 4},${y} L${x + w - 4},${y} Q${x + w},${y} ${x + w},${y + 4} L${x + w},${y + headerH} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 18}" class="tiny" text-anchor="middle">&lt;&lt;${esc(stereotype)}&gt;&gt;</text>
`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${y + 36 + i * 17}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  out += `<path d="M${x},${y + headerH} H${x + w}" stroke="${stroke}" stroke-width="1.1"/>\n`;
  rows.slice(0, 3).forEach((r, i) => (out += `<text x="${x + 14}" y="${y + headerH + 18 + i * 15}" class="tiny">${esc(r)}</text>\n`));
  return out;
}

function wrapIdentifier(value, max, lines) {
  const text = String(value);
  if (text.length <= max) return [text];
  const parts = text.split(/(?=[A-Z][a-z])/);
  const out = [];
  let line = "";
  for (const part of parts) {
    const next = line + part;
    if (next.length > max && line) {
      out.push(line);
      line = part;
    } else line = next;
  }
  if (line) out.push(line);
  return out.slice(0, lines);
}

function dbCylinder(x, y, w, h, title, c) {
  const [fill, stroke] = colors[c % colors.length];
  return `<path d="M${x},${y + 14} C${x},${y - 2} ${x + w},${y - 2} ${x + w},${y + 14} L${x + w},${y + h - 14} C${x + w},${y + h + 2} ${x},${y + h + 2} ${x},${y + h - 14} Z" fill="${fill}" stroke="${stroke}" class="card"/>
<path d="M${x},${y + 14} C${x},${y + 30} ${x + w},${y + 30} ${x + w},${y + 14}" fill="none" stroke="${stroke}" stroke-width="1.5"/>
<text x="${x + w / 2}" y="${y + 35}" class="label" text-anchor="middle">Database</text>
<text x="${x + w / 2}" y="${y + 54}" class="detail" text-anchor="middle">${esc(title)}</text>\n`;
}

function apiDocument(x, y, w, h, title, detail, c) {
  const [fill, stroke] = colors[c % colors.length];
  return `<path d="M${x},${y} H${x + w - 22} L${x + w},${y + 22} V${y + h} H${x} Z" fill="${fill}" stroke="${stroke}" class="card"/>
<path d="M${x + w - 22},${y} V${y + 22} H${x + w}" fill="#fff" stroke="${stroke}" stroke-width="1.2"/>
<text x="${x + w / 2}" y="${y + 30}" class="label" text-anchor="middle">${esc(title)}</text>
<text x="${x + w / 2}" y="${y + 49}" class="detail" text-anchor="middle">${esc(detail)}</text>\n`;
}

function arrow(x1, y1, x2, y2) {
  return `<path d="M${x1},${y1} C${(x1 + x2) / 2},${y1} ${(x1 + x2) / 2},${y2} ${x2},${y2}" class="arrow"/>\n`;
}

function orthArrow(x1, y1, x2, y2, laneX, cls = "arrow") {
  return `<path d="M${x1},${y1} L${laneX},${y1} L${laneX},${y2} L${x2},${y2}" class="${cls}"/>\n`;
}

function softArrow(x1, y1, x2, y2) {
  return `<path d="M${x1},${y1} C${(x1 + x2) / 2},${y1} ${(x1 + x2) / 2},${y2} ${x2},${y2}" class="softArrow"/>\n`;
}

function dashedOrthDown(x1, y1, x2, y2, laneY) {
  return `<path d="M${x1},${y1} L${x1},${laneY} L${x2},${laneY} L${x2},${y2}" class="softArrow"/>\n`;
}

function relation(x1, y1, x2, y2, label) {
  const midY = (y1 + y2) / 2;
  return `<path d="M${x1},${y1} L${x1},${midY} L${x2},${midY} L${x2},${y2}" class="softArrow"/>
<rect x="${(x1 + x2) / 2 - 54}" y="${midY - 14}" width="108" height="21" rx="8" fill="#fff" stroke="#d7e2ec"/>
<text x="${(x1 + x2) / 2}" y="${midY + 1}" class="tiny" text-anchor="middle">${esc(label)}</text>\n`;
}

function inheritArrow(childX, childY, apexX, apexY, laneY) {
  const baseY = apexY + 16;
  return `<path d="M${childX},${childY} L${childX},${laneY} L${apexX},${laneY} L${apexX},${baseY}" class="inherit"/>
<path d="M${apexX},${apexY} L${apexX - 12},${baseY} L${apexX + 12},${baseY} Z" class="inheritHead"/>\n`;
}

function band(x1, x2, y, label, reverse) {
  const left = Math.min(x1, x2);
  const right = Math.max(x1, x2);
  const path = reverse ? `M${right - 16},${y} L${left + 16},${y}` : `M${left + 16},${y} L${right - 16},${y}`;
  const cls = reverse ? "softArrow" : "arrow";
  return `<rect x="${left + 32}" y="${y - 28}" width="${right - left - 64}" height="24" rx="8" fill="#fff" stroke="#d7e2ec"/>
<text x="${(left + right) / 2}" y="${y - 11}" class="tiny" text-anchor="middle">${esc(label)}</text>
<path d="${path}" class="${cls}"/>\n`;
}

function note(x, y, w, text) {
  return `<rect x="${x}" y="${y}" width="${w}" height="48" rx="10" fill="#ecfdf5" stroke="#86efac" stroke-width="1.3"/>
<text x="${x + w / 2}" y="${y + 30}" class="detail" text-anchor="middle">${esc(text)}</text>\n`;
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
    } else line = next;
  }
  if (line) out.push(line);
  return out.slice(0, lines);
}

function esc(value) {
  return String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

for (const diagram of diagrams) {
  fs.writeFileSync(diagram.file, shell(diagram));
  const png = diagram.file.replace(/\.svg$/, ".png");
  const result = spawnSync(RSVG, ["-f", "png", "-o", png, diagram.file], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout);
  console.log(png);
}
