import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import { promisify } from 'node:util';
import test from 'node:test';

const execute = promisify(execFile);
const root = new URL('../../', import.meta.url);

async function validate() {
  return execute('node', ['scripts/validate-visual-companions.mjs'], { cwd: root });
}

async function html(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

function values(content, pattern) {
  return [...content.matchAll(pattern)].map((match) => match[1]).sort();
}

test('approved bilingual visual companions satisfy the repository contract', async () => {
  const { stdout } = await validate();
  assert.match(stdout, /2 documents \/ 4 locale files/);
});

test('cache companion explains the implemented Exposed and Redis path', async () => {
  const content = await html(
    'docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.html',
  );
  for (const marker of [
    'JdbcCacheRepository',
    'AbstractJdbcRedissonRepository',
    'RLocalCachedMap',
    'EntityMapLoader',
    'EntityMapWriter',
    'READ_WRITE_THROUGH_WITH_NEAR_CACHE',
    'READ_ONLY_WITH_NEAR_CACHE',
    'WRITE_BEHIND_WITH_NEAR_CACHE',
    '요청 수락',
    'DB 반영 완료',
    'deleteFromDBOnInvalidate',
  ]) {
    assert.match(content, new RegExp(marker));
  }
  for (const strategy of ['read-through', 'write-through', 'read-only', 'write-behind']) {
    assert.match(content, new RegExp(`data-strategy=["']${strategy}["']`));
  }
});

test('cache locale documents expose the same structure and interactions', async () => {
  const ko = await html(
    'docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.html',
  );
  const en = await html(
    'docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.en.html',
  );
  assert.deepEqual(values(en, /<section\b[^>]*id=["']([^"']+)/gi), values(ko, /<section\b[^>]*id=["']([^"']+)/gi));
  assert.deepEqual(values(en, /data-strategy=["']([^"']+)/gi), values(ko, /data-strategy=["']([^"']+)/gi));
  assert.equal((en.match(/\.\/gradlew /g) ?? []).length, (ko.match(/\.\/gradlew /g) ?? []).length);
  assert.match(en, /data-baseline=["']a2edd9af77188f814ccae10917a9e6ad574402f9["']/);
});

test('DDD companion separates the accepted event path from a rejected module reference', async () => {
  const content = await html(
    'docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.html',
  );
  for (const marker of [
    'AcceptOrderCommand',
    'OrderApplicationService',
    'OrderAcceptedEvent',
    'ShippingReservationHandler',
    'orders :: events',
    'ApplicationModules\\.verify\\(\\)',
    'Violations',
    'orders\\.internal',
    'DDD가 필요한 상황',
    'DDD 적용 효과',
  ]) {
    assert.match(content, new RegExp(marker));
  }
  for (const scenario of ['normal', 'violation']) {
    assert.match(content, new RegExp(`data-scenario=["']${scenario}["']`));
  }
});

test('DDD locale documents expose the same structure and scenarios', async () => {
  const ko = await html(
    'docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.html',
  );
  const en = await html(
    'docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.en.html',
  );
  assert.deepEqual(values(en, /<section\b[^>]*id=["']([^"']+)/gi), values(ko, /<section\b[^>]*id=["']([^"']+)/gi));
  assert.deepEqual(values(en, /data-scenario=["']([^"']+)/gi), values(ko, /data-scenario=["']([^"']+)/gi));
  assert.equal((en.match(/\.\/gradlew /g) ?? []).length, (ko.match(/\.\/gradlew /g) ?? []).length);
  assert.match(en, /data-baseline=["']a2edd9af77188f814ccae10917a9e6ad574402f9["']/);
});
