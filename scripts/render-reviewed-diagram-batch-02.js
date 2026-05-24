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
    file: "docs/images/readme-diagrams/01-spring-boot-spring-webflux-exposed-class-01.svg",
    title: "Spring WebFlux With Exposed Architecture",
    subtitle: "Suspend controllers isolate blocking Exposed JDBC work behind coroutine transactions",
    width: 1520,
    height: 760,
    body: webfluxArchitecture,
  },
  {
    file: "docs/images/readme-diagrams/01-spring-boot-spring-webflux-exposed-sequence-02.svg",
    title: "Spring WebFlux Request Processing Flow",
    subtitle: "Actor API request through suspend controller, suspended transaction, repository, and DTO mapping",
    width: 1320,
    height: 780,
    body: webfluxSequence,
  },
  {
    file: "docs/images/readme-diagrams/01-spring-boot-spring-webflux-exposed-erd-03.svg",
    title: "Spring WebFlux Domain Model",
    subtitle: "Shared Movie/Actor tables, Exposed DAO entities, and immutable API records",
    width: 1460,
    height: 860,
    body: webfluxDomain,
  },
  {
    file: "docs/images/readme-diagrams/02-alternatives-to-jpa-architecture-01.svg",
    title: "Alternatives To JPA Architecture Flow",
    subtitle: "Three asynchronous data-access styles compared against the same Team/Member learning target",
    width: 1420,
    height: 760,
    body: alternativesArchitecture,
  },
  {
    file: "docs/images/readme-diagrams/02-alternatives-to-jpa-hibernate-reactive-example-sequence-01.svg",
    title: "Hibernate Reactive Request Flow",
    subtitle: "Coroutine controller delegates to Mutiny SessionFactory and Hibernate Reactive repositories",
    width: 1340,
    height: 780,
    body: hibernateReactiveSequence,
  },
];

function webfluxArchitecture() {
  let b = panel(42, 118, 288, 410, "HTTP entry");
  b += card(66, 144, 226, 54, "Client / Swagger UI", 0, "REST caller");
  b += apiDocument(88, 218, 182, 62, "OpenAPI", "Swagger config", 0);
  b += card(66, 304, 226, 54, "Actor Suspend Controller", 1, "/actors");
  b += card(66, 374, 226, 54, "Movie Suspend Controller", 2, "/movies");
  b += card(66, 444, 226, 54, "Movie Actors Controller", 3);

  b += panel(382, 118, 248, 410, "Reactive runtime");
  b += card(410, 150, 192, 58, "Netty EventLoop", 4, "non-blocking HTTP");
  b += card(410, 270, 192, 58, "Coroutine suspend bridge", 1, "handler resumes later");
  b += card(410, 392, 192, 58, "Backpressure boundary", 7, "reactive server path");

  b += panel(690, 118, 300, 410, "Transaction and SQL");
  b += card(722, 144, 236, 58, "newSuspendedTransaction", 6, "readOnly/write boundary");
  b += card(722, 258, 236, 66, "Actor Repository", 4, "DSL search + DAO create");
  b += card(722, 398, 236, 66, "Movie Repository", 5, "join + eager loading");

  b += panel(1050, 118, 220, 410, "Exposed model");
  b += card(1074, 144, 172, 58, "Movie Entity / Actor Entity", 5);
  b += card(1074, 252, 172, 58, "Actor Table", 1, "actors");
  b += card(1074, 342, 172, 58, "Movie Table", 0, "movies");
  b += card(1074, 432, 172, 58, "Actor In Movie Table", 2, "bridge");

  b += panel(1290, 118, 190, 410, "Data");
  b += card(1318, 178, 134, 58, "HikariCP", 7, "connection pool");
  b += dbCylinder(1310, 342, 150, 104, "H2 / MySQL / Postgres", 3);

  b += orthArrow(270, 249, 292, 171, 314, "softArrow");
  b += orthArrow(292, 331, 410, 299, 358);
  b += orthArrow(292, 401, 410, 299, 358);
  b += orthArrow(292, 471, 410, 299, 358);
  b += orthArrow(602, 299, 722, 173, 654);
  b += orthArrow(958, 173, 1074, 173, 1020, "softArrow");
  b += orthArrow(958, 291, 1074, 281, 1020);
  b += `<path d="M958,431 L1018,431 L1018,371 L1074,371" class="arrow"/>\n`;
  b += `<path d="M958,431 L1034,431 L1034,461 L1074,461" class="arrow"/>\n`;
  b += `<path d="M958,431 L1034,431 L1034,281 L1074,281" class="softArrow"/>\n`;
  b += orthArrow(1246, 281, 1320, 207, 1288, "softArrow");
  b += orthArrow(1246, 371, 1320, 207, 1288, "softArrow");
  b += orthArrow(1246, 461, 1320, 207, 1288, "softArrow");
  b += `<path d="M1385,236 L1385,342" class="arrow"/>\n`;
  b += note(94, 590, 1260, "WebFlux keeps the HTTP path non-blocking; suspended Exposed transactions contain JDBC work and return immutable DTO records.");
  return b;
}

function webfluxSequence() {
  const xs = [118, 310, 520, 730, 940, 1130];
  const names = ["Client", "Suspend Controller", "newSuspended Transaction", "Actor Repository", "Actor Table", "Actor Record"];
  let b = "";
  xs.forEach((x, i) => {
    b += card(x - 68, 122, 136, 52, names[i], i, "");
    b += `<path d="M${x},184 L${x},704" class="lifeline"/>\n`;
  });
  [
    [0, 1, "GET /actors/{id}", 224, false],
    [1, 2, "enter readOnly suspend transaction", 286, false],
    [2, 3, "call repository inside transaction", 348, false],
    [3, 4, "select ActorTable by id", 410, false],
    [4, 5, "map row to ActorRecord", 472, false],
    [5, 3, "nullable DTO", 534, true],
    [3, 2, "transaction returns DTO", 596, true],
    [2, 1, "resume suspend handler", 658, true],
    [1, 0, "200 OK or 404 Not Found", 704, true],
  ].forEach(([from, to, label, y, reverse], i) => {
    b += band(xs[from], xs[to], y, `${i + 1}. ${label}`, reverse);
  });
  b += note(76, 714, 1168, "Search/create/delete use the same shape: suspend controller opens a suspended transaction, then repositories compose Exposed DSL or DAO work.");
  return b;
}

function webfluxDomain() {
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
  b += note(180, 760, 1100, "The WebFlux module shares the Movie/Actor schema with MVC; coroutine controllers return records instead of Exposed entities.");
  return b;
}

function alternativesArchitecture() {
  let b = panel(58, 118, 276, 410, "Learning target");
  b += card(86, 150, 220, 58, "Team / Member domain", 0, "same scenario");
  b += card(86, 252, 220, 58, "Controller tests", 1, "CRUD + search");
  b += card(86, 354, 220, 58, "Mapping comparison", 6, "entity to DTO");

  b += panel(392, 118, 276, 410, "Hibernate Reactive");
  b += card(420, 150, 220, 58, "JPA Entity model", 2, "@Entity relations");
  b += card(420, 252, 220, 58, "Mutiny SessionFactory", 4, "withSession");
  b += card(420, 354, 220, 58, "Uni / awaitSuspending", 5, "reactive driver");

  b += panel(724, 118, 276, 410, "Spring Data R2DBC");
  b += card(752, 150, 220, 58, "R2DBC Repository", 0, "suspend APIs");
  b += card(752, 252, 220, 58, "DatabaseClient", 3, "manual joins");
  b += card(752, 354, 220, 58, "TransactionalOperator", 6, "reactive tx");

  b += panel(1056, 118, 276, 410, "Vert.x SQL Client");
  b += card(1084, 150, 220, 58, "SqlClient / Pool", 5, "event loop");
  b += card(1084, 252, 220, 58, "SqlTemplate", 2, "direct SQL");
  b += card(1084, 354, 220, 58, "RowSet mapping", 7, "manual mapper");

  b += `<rect x="252" y="560" width="916" height="124" rx="12" class="panel"/>
<text x="286" y="580" class="panelTitle">DATABASE COMPARISON RESULT</text>\n`;
  b += dbCylinder(314, 588, 156, 82, "PostgreSQL", 3);
  b += dbCylinder(632, 588, 156, 82, "R2DBC DBs", 0);
  b += dbCylinder(950, 588, 156, 82, "SQL clients", 5);
  b += `<path d="M530,528 L530,546 L392,546 L392,588" class="softArrow"/>\n`;
  b += `<path d="M862,528 L862,546 L710,546 L710,588" class="softArrow"/>\n`;
  b += `<path d="M1194,528 L1194,546 L1028,546 L1028,588" class="softArrow"/>\n`;
  b += note(142, 690, 1136, "The chapter compares persistence style, transaction boundary, async result type, and relationship loading across the same domain.");
  return b;
}

function hibernateReactiveSequence() {
  const xs = [116, 306, 496, 686, 876, 1066, 1224];
  const names = ["Client", "TeamController", "SessionFactory", "Mutiny Session", "Team Repository", "PostgreSQL", "DTO Record"];
  let b = "";
  xs.forEach((x, i) => {
    b += card(x - 70, 122, 140, 52, names[i], i, "");
    b += `<path d="M${x},184 L${x},704" class="lifeline"/>\n`;
  });
  [
    [0, 1, "GET /teams?memberName=", 224, false],
    [1, 2, "withSessionSuspending()", 286, false],
    [2, 3, "open Mutiny session", 348, false],
    [3, 4, "findAllByMemberName()", 410, false],
    [4, 5, "Criteria query + awaitSuspending", 472, false],
    [4, 3, "fetch team.members when needed", 534, "call-left"],
    [5, 4, "reactive rows", 596, true],
    [4, 1, "map entities to records", 658, true],
    [1, 0, "List<TeamAndMemberRecord>", 704, true],
  ].forEach(([from, to, label, y, reverse], i) => {
    b += reverse === "call-left"
      ? callBand(xs[from], xs[to], y, `${i + 1}. ${label}`)
      : band(xs[from], xs[to], y, `${i + 1}. ${label}`, reverse);
  });
  b += note(82, 714, 1176, "Hibernate Reactive keeps database IO in Mutiny sessions; controllers await coroutine-friendly results before returning DTO records.");
  return b;
}

function shell(diagram) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${diagram.width} ${diagram.height}" width="${diagram.width}" height="${diagram.height}" role="img" aria-label="${esc(diagram.title)}">
<defs>
<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
<marker id="arrow" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#64748b"/></marker>
<marker id="arrowGreen" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#16a34a"/></marker>
<marker id="arrowPurple" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#7c3aed"/></marker>
<style>${style}</style>
</defs>
<rect width="${diagram.width}" height="${diagram.height}" class="canvas"/>
<rect x="20" y="20" width="${diagram.width - 40}" height="${diagram.height - 40}" rx="16" class="frame"/>
<text x="48" y="58" class="title">${esc(diagram.title)}</text>
<text x="48" y="80" class="subtitle">${esc(diagram.subtitle)}</text>
${diagram.body()}</svg>
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
<text x="${x + w / 2}" y="${y + 18}" class="tiny" text-anchor="middle">&lt;&lt;${esc(stereotype)}&gt;&gt;</text>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${y + 36 + i * 17}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  out += `<path d="M${x},${y + headerH} H${x + w}" stroke="${stroke}" stroke-width="1.1"/>\n`;
  rows.slice(0, 3).forEach((r, i) => (out += `<text x="${x + 14}" y="${y + headerH + 18 + i * 15}" class="tiny">${esc(r)}</text>\n`));
  return out;
}

function dbCylinder(x, y, w, h, title, c) {
  const [fill, stroke] = colors[c % colors.length];
  const detail = title.length > 18 ? wrap(title, Math.floor(w / 8), 2) : [title];
  let out = `<path d="M${x},${y + 14} C${x},${y - 2} ${x + w},${y - 2} ${x + w},${y + 14} L${x + w},${y + h - 14} C${x + w},${y + h + 2} ${x},${y + h + 2} ${x},${y + h - 14} Z" fill="${fill}" stroke="${stroke}" class="card"/>
<path d="M${x},${y + 14} C${x},${y + 30} ${x + w},${y + 30} ${x + w},${y + 14}" fill="none" stroke="${stroke}" stroke-width="1.5"/>
<text x="${x + w / 2}" y="${y + 42}" class="label" text-anchor="middle">Database</text>\n`;
  detail.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${y + 62 + i * 14}" class="detail" text-anchor="middle">${esc(line)}</text>\n`));
  return out;
}

function apiDocument(x, y, w, h, title, detail, c) {
  const [fill, stroke] = colors[c % colors.length];
  return `<path d="M${x},${y} H${x + w - 22} L${x + w},${y + 22} V${y + h} H${x} Z" fill="${fill}" stroke="${stroke}" class="card"/>
<path d="M${x + w - 22},${y} V${y + 22} H${x + w}" fill="#fff" stroke="${stroke}" stroke-width="1.2"/>
<text x="${x + w / 2}" y="${y + 30}" class="label" text-anchor="middle">${esc(title)}</text>
<text x="${x + w / 2}" y="${y + 49}" class="detail" text-anchor="middle">${esc(detail)}</text>\n`;
}

function orthArrow(x1, y1, x2, y2, laneX, cls = "arrow") {
  return `<path d="M${x1},${y1} L${laneX},${y1} L${laneX},${y2} L${x2},${y2}" class="${cls}"/>\n`;
}

function softArrow(x1, y1, x2, y2) {
  return `<path d="M${x1},${y1} C${(x1 + x2) / 2},${y1} ${(x1 + x2) / 2},${y2} ${x2},${y2}" class="softArrow"/>\n`;
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

function callBand(x1, x2, y, label) {
  const left = Math.min(x1, x2);
  const right = Math.max(x1, x2);
  const path = x2 < x1 ? `M${right - 16},${y} L${left + 16},${y}` : `M${left + 16},${y} L${right - 16},${y}`;
  return `<rect x="${left + 32}" y="${y - 28}" width="${right - left - 64}" height="24" rx="8" fill="#fff" stroke="#d7e2ec"/>
<text x="${(left + right) / 2}" y="${y - 11}" class="tiny" text-anchor="middle">${esc(label)}</text>
<path d="${path}" class="arrow"/>\n`;
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
