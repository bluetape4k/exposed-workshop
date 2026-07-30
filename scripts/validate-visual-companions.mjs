#!/usr/bin/env node

import { readFile, realpath } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const BASELINE = 'a2edd9af77188f814ccae10917a9e6ad574402f9';
const DESIGN = '2026-07-30-exposed-workshop-visual-companions-design.md';
const forbidden = [
  /<script\b[^>]*\bsrc\s*=/i,
  /<link\b[^>]*\brel\s*=\s*["']?stylesheet\b/i,
  /<(?:img|iframe|audio|video|source)\b[^>]*\bsrc\s*=\s*["'](?!data:|#)[^"']+["']/i,
  /<form\b/i,
  /\bfetch\s*\(/,
  /\bXMLHttpRequest\b/,
  /\bWebSocket\s*\(/,
  /\bnavigator\.sendBeacon\s*\(/,
];

const documentContracts = {
  'exposed-redis-cache-strategies': {
    links: [
      '01-cache-strategies/README.ko.md',
      'UserCacheRepository.kt',
      'UserCacheRepositoryTest.kt',
    ],
    commands: [
      './gradlew :01-cache-strategies:test',
      './gradlew :01-cache-strategies:bootRun',
      './gradlew :02-cache-strategies-coroutines:test',
    ],
    markers: [
      'RLocalCachedMap',
      'EntityMapLoader',
      'EntityMapWriter',
      'deleteFromDBOnInvalidate',
    ],
  },
  'ddd-modulith-boundaries': {
    links: [
      '08-ddd-modulith-boundaries/README.ko.md',
      'OrderApplicationService.kt',
      'BoundaryVerificationApplicationTest.kt',
    ],
    commands: ['./gradlew :08-ddd-modulith-boundaries:test'],
    markers: [
      'OrderAcceptedEvent',
      'ShippingReservationHandler',
      'ApplicationModules.verify()',
      'orders :: events',
    ],
  },
};

function requireMatch(errors, value, pattern, message) {
  if (!pattern.test(value)) errors.push(message);
}

function contained(root, relative) {
  const absolute = path.resolve(root, relative);
  if (!absolute.startsWith(`${root}${path.sep}`)) {
    throw new Error(`Path escapes repository: ${relative}`);
  }
  return absolute;
}

export async function validateRepository(inputRoot = process.cwd()) {
  const root = await realpath(inputRoot);
  const manifestPath = path.join(root, 'docs/visual-companions/manifest.json');
  const manifest = JSON.parse(await readFile(manifestPath, 'utf8'));
  const errors = [];

  if (manifest.schemaVersion !== 1) errors.push('manifest.schemaVersion must be 1');
  if (manifest.repository !== 'bluetape4k/exposed-workshop') {
    errors.push('manifest.repository must be bluetape4k/exposed-workshop');
  }
  if (!Array.isArray(manifest.documents) || manifest.documents.length !== 2) {
    errors.push('manifest.documents must contain the two approved documents');
  }

  const ids = new Set();
  let localeFileCount = 0;
  for (const [index, document] of (manifest.documents ?? []).entries()) {
    const field = `documents[${index}]`;
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(document.id)) errors.push(`${field}.id is invalid`);
    if (ids.has(document.id)) errors.push(`${field}.id is duplicated`);
    ids.add(document.id);
    if (document.status !== 'approved' || document.public !== true) {
      errors.push(`${field} must be approved and public`);
    }
    if (
      document.presentation?.mode !== 'simulation'
      || document.presentation?.defaultView !== 'simulation'
      || document.presentation?.views?.length !== 1
      || document.presentation.views[0] !== 'simulation'
    ) {
      errors.push(`${field}.presentation must expose the simulation view`);
    }

    await readFile(contained(root, document.source), 'utf8').catch(() => {
      errors.push(`${field}.source does not exist`);
    });

    const contract = documentContracts[document.id];
    if (!contract) errors.push(`${field}.id has no validation contract`);

    for (const locale of ['en', 'ko']) {
      const localeEntry = document.locales?.[locale];
      if (!localeEntry?.title || !localeEntry?.html) {
        errors.push(`${field}.locales.${locale} is required`);
        continue;
      }
      const content = await readFile(contained(root, localeEntry.html), 'utf8').catch(() => null);
      if (content === null) {
        errors.push(`${field}.locales.${locale}.html does not exist`);
        continue;
      }

      localeFileCount += 1;
      const prefix = `${document.id}.${locale}`;
      const firstStyle = content.search(/<style\b/i);
      const themeRead = content.indexOf('localStorage.getItem(storageKey)');
      requireMatch(errors, content, /^\s*<!doctype html>/i, `${prefix} must start with doctype`);
      requireMatch(
        errors,
        content,
        new RegExp(`<html\\b[^>]*lang=["']${locale}["']`, 'i'),
        `${prefix} must set lang=${locale}`,
      );
      requireMatch(
        errors,
        content,
        /<meta\b[^>]*name=["']color-scheme["'][^>]*content=["']light dark["']/i,
        `${prefix} must support light dark color schemes`,
      );
      if (themeRead < 0 || firstStyle < 0 || themeRead > firstStyle) {
        errors.push(`${prefix} must resolve starlight-theme before CSS`);
      }
      requireMatch(errors, content, /:root\[data-theme=["']light["']\]/i, `${prefix} needs light tokens`);
      requireMatch(errors, content, /:root\[data-theme=["']dark["']\]/i, `${prefix} needs dark tokens`);
      requireMatch(
        errors,
        content,
        /<button\b[^>]*class=["'][^"']*theme-toggle[^"']*["'][^>]*aria-label=["'][^"']+["']/i,
        `${prefix} needs an accessible theme toggle`,
      );
      requireMatch(
        errors,
        content,
        /localStorage\.setItem\(themeStorageKey,/i,
        `${prefix} must persist the selected theme`,
      );
      requireMatch(errors, content, /<section\b[^>]*id=["']simulation["']/i, `${prefix} needs #simulation`);
      requireMatch(
        errors,
        content,
        new RegExp(`data-source=["']${DESIGN}["']`, 'i'),
        `${prefix} must identify its design source`,
      );
      requireMatch(
        errors,
        content,
        new RegExp(`data-baseline=["']${BASELINE}["']`, 'i'),
        `${prefix} must pin the approved design baseline`,
      );
      requireMatch(
        errors,
        content,
        new RegExp(`href=["'][^"']*${DESIGN}["']`, 'i'),
        `${prefix} must link to its design source`,
      );
      requireMatch(
        errors,
        content,
        new RegExp(`href=["'][^"']*${document.id === 'exposed-redis-cache-strategies' ? 'ddd-modulith-boundaries' : 'exposed-redis-cache-strategies'}[^"']*["']`, 'i'),
        `${prefix} must link to its sibling document`,
      );
      const oppositeHtml = path.posix.basename(document.locales[locale === 'en' ? 'ko' : 'en'].html);
      requireMatch(
        errors,
        content,
        new RegExp(`href=["'][^"']*${oppositeHtml.replaceAll('.', '\\.')}["']`, 'i'),
        `${prefix} must link to its opposite locale`,
      );
      for (const marker of contract?.markers ?? []) {
        if (!content.includes(marker)) errors.push(`${prefix} must include ${marker}`);
      }
      for (const command of contract?.commands ?? []) {
        if (!content.includes(command)) errors.push(`${prefix} must include command ${command}`);
      }
      for (const link of contract?.links ?? []) {
        requireMatch(
          errors,
          content,
          new RegExp(`href=["'][^"']*${link.replaceAll('.', '\\.')}[^"']*["']`, 'i'),
          `${prefix} must link ${link}`,
        );
      }
      if (forbidden.some((pattern) => pattern.test(content))) {
        errors.push(`${prefix} contains a forbidden surface`);
      }
    }
  }

  if (errors.length > 0) throw new Error(errors.join('\n'));
  return { documentCount: manifest.documents.length, localeFileCount };
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isMain) {
  try {
    const result = await validateRepository();
    console.log(
      `Visual companion validation passed: ${result.documentCount} documents / ${result.localeFileCount} locale files`,
    );
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
