# Memory Hybrid Enforcement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 테스트 누락을 우선적으로 줄이고, 코딩 스타일 forgetting을 보조적으로 줄이기 위해 hook + memory + mempalace/wiki/obsidian 분류 체계를 구현한다.

**Architecture:** 기존 `~/.claude/settings.json`의 hook 체인을 유지한 채, `post-edit-dispatcher.sh`에서 테스트 강제 체크를 추가하고 `SessionStart`/`UserPromptSubmit`에 짧은 priority reminder를 주입한다. 저장 계층은 auto-memory를 행동 규칙의 단일 원천으로 두고, mempalace/wiki/obsidian은 검색·아카이브 용도로 분리한다.

**Tech Stack:** Claude Code hooks, Bash, jq, Python 3, auto-memory files, mempalace ChromaDB, project docs markdown

---

## File Structure

- Modify: `/Users/debop/.claude/hooks/post-edit-dispatcher.sh` — Edit/Write 후 파일 유형별 검사를 라우팅하는 entrypoint
- Create: `/Users/debop/.claude/hooks/test-enforcement-check.sh` — 코드 변경 시 테스트/실행/testlog 누락을 판정
- Modify: `/Users/debop/.claude/settings.json` — SessionStart/UserPromptSubmit/Stop hooks에 reminder 및 classifier 연결
- Create: `/Users/debop/.claude/hooks/session-priority-reminder.sh` — 짧은 협업 규칙 상기 메시지 출력
- Create: `/Users/debop/.claude/hooks/knowledge-routing-reminder.sh` — Stop 시 mempalace/wiki/obsidian 저장 기준 안내
- Modify: `/Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/MEMORY.md` — 핵심 행동 규칙 인덱스 유지
- Create or Modify: `/Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/feedback_test_enforcement.md` — 테스트 우선 규칙 저장
- Create or Modify: `/Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/feedback_response_verification.md` — 확인 후 응답 규칙 저장
- Create: `/Users/debop/work/bluetape4k/exposed-workshop/docs/testlogs/2026-04.md` — 테스트 로그 월간 파일
- Create: `/Users/debop/work/bluetape4k/exposed-workshop/.omc/wiki/memory-hybrid-enforcement.md` — 재사용 가치 있는 운영 패턴 정리
- Create: `/Users/debop/Library/Mobile Documents/com~apple~CloudDocs/Obsidian/<vault>/claude-memory-hybrid-enforcement.md` 또는 기존 적절한 노트 수정 — 사람 중심 허브 노트

### Task 1: 테스트 강제 훅 스크립트 추가

**Files:**
- Create: `/Users/debop/.claude/hooks/test-enforcement-check.sh`
- Test: `/private/tmp/test-enforcement-input.json`

- [ ] **Step 1: failing path를 정의하는 테스트 입력 JSON 작성**

```json
{
  "tool_name": "Edit",
  "tool_input": {
    "file_path": "/Users/debop/work/bluetape4k/exposed-workshop/05-exposed-dml/01-dml/src/testable/SampleService.kt"
  },
  "tool_response": {
    "filePath": "/Users/debop/work/bluetape4k/exposed-workshop/05-exposed-dml/01-dml/src/testable/SampleService.kt"
  }
}
```

- [ ] **Step 2: 입력 파일 생성 후 스크립트 없이 실패 조건을 문서화**

Run: `printf '%s\n' '<JSON above>' > /private/tmp/test-enforcement-input.json`
Expected: `/private/tmp/test-enforcement-input.json` 파일 생성

- [ ] **Step 3: 최소 구현으로 스크립트 초안 작성**

```bash
#!/usr/bin/env bash
set -euo pipefail

INPUT=$(cat)
FILE_PATH=$(printf '%s' "$INPUT" | jq -r '.tool_input.file_path // .tool_response.filePath // empty')
[[ -z "$FILE_PATH" ]] && exit 0

case "$FILE_PATH" in
  *.kt|*.kts|*.java)
    ;;
  *)
    exit 0
    ;;
esac

if printf '%s' "$FILE_PATH" | grep -q '/src/test/'; then
  exit 0
fi

if printf '%s' "$FILE_PATH" | grep -qE 'README|\.md$'; then
  exit 0
fi

echo "──────────────────────────────────────────" >&2
echo "⚠️ 테스트 강제 체크: 코드 파일 변경 감지" >&2
echo "   파일: $FILE_PATH" >&2
echo "   다음 항목을 확인하세요:" >&2
echo "   1) 관련 테스트 작성/수정" >&2
echo "   2) 관련 Gradle test 실행" >&2
echo "   3) docs/testlogs/$(date +%Y-%m).md 기록" >&2
echo "──────────────────────────────────────────" >&2
exit 0
```

- [ ] **Step 4: 스크립트 실행으로 기본 경고 출력 확인**

Run: `bash /Users/debop/.claude/hooks/test-enforcement-check.sh < /private/tmp/test-enforcement-input.json`
Expected: stderr에 `⚠️ 테스트 강제 체크` 포함

- [ ] **Step 5: 테스트 파일 변경은 통과하도록 필터 추가**

```bash
if printf '%s' "$FILE_PATH" | grep -qE '/src/test/|/src/testFixtures/'; then
  exit 0
fi
```

- [ ] **Step 6: 문서 파일 변경은 통과하도록 필터 확인**

Run: `printf '%s\n' '{"tool_input":{"file_path":"/Users/debop/work/bluetape4k/exposed-workshop/README.md"}}' | bash /Users/debop/.claude/hooks/test-enforcement-check.sh`
Expected: 출력 없음, exit 0

- [ ] **Step 7: 실행 권한 부여**

Run: `chmod +x /Users/debop/.claude/hooks/test-enforcement-check.sh`
Expected: 권한 부여 성공

- [ ] **Step 8: Commit**

```bash
git add /Users/debop/.claude/hooks/test-enforcement-check.sh
git commit -m "feat: add test enforcement hook"
```

### Task 2: post-edit-dispatcher에 테스트 강제 체크 연결

**Files:**
- Modify: `/Users/debop/.claude/hooks/post-edit-dispatcher.sh:11-28`
- Test: `/private/tmp/test-enforcement-input.json`

- [ ] **Step 1: 연결 전 failing expectation 명시**

Run: `bash /Users/debop/.claude/hooks/post-edit-dispatcher.sh < /private/tmp/test-enforcement-input.json`
Expected: 아직 `⚠️ 테스트 강제 체크`가 없거나 라우팅되지 않음

- [ ] **Step 2: kt 분기에서 새 스크립트 호출 추가**

```bash
  kt)
    bash /Users/debop/.claude/hooks/test-enforcement-check.sh <<< "$INPUT"
    bash /Users/debop/.claude/hooks/bluetape4k-patterns-checker.sh <<< "$INPUT"
    bash /Users/debop/.claude/hooks/kotlin-detekt.sh <<< "$INPUT"
```

- [ ] **Step 3: kts/java도 검사 대상에 포함하도록 case 확장**

```bash
  kt|kts|java)
    bash /Users/debop/.claude/hooks/test-enforcement-check.sh <<< "$INPUT"
```

- [ ] **Step 4: dispatcher를 직접 실행해 경고가 라우팅되는지 확인**

Run: `bash /Users/debop/.claude/hooks/post-edit-dispatcher.sh < /private/tmp/test-enforcement-input.json`
Expected: stderr에 `⚠️ 테스트 강제 체크` 포함

- [ ] **Step 5: shell, markdown 기존 동작이 깨지지 않는지 확인**

Run: `printf '%s\n' '{"tool_input":{"file_path":"/Users/debop/.claude/hooks/testlog-reminder.sh"}}' | bash /Users/debop/.claude/hooks/post-edit-dispatcher.sh`
Expected: shell validator만 실행, fatal error 없음

- [ ] **Step 6: Commit**

```bash
git add /Users/debop/.claude/hooks/post-edit-dispatcher.sh
git commit -m "feat: route test enforcement in post edit dispatcher"
```

### Task 3: SessionStart/UserPromptSubmit reminder 추가

**Files:**
- Create: `/Users/debop/.claude/hooks/session-priority-reminder.sh`
- Modify: `/Users/debop/.claude/settings.json`
- Test: `/private/tmp/empty-hook-input.json`

- [ ] **Step 1: 빈 hook 입력 파일 작성**

```json
{}
```

- [ ] **Step 2: 입력 파일 생성**

Run: `printf '{}\n' > /private/tmp/empty-hook-input.json`
Expected: `/private/tmp/empty-hook-input.json` 생성

- [ ] **Step 3: reminder 스크립트 최소 구현 작성**

```bash
#!/usr/bin/env bash
set -euo pipefail
cat <<'EOF'
<priority_reminder>
- 테스트 우선: 코드 변경 후 테스트/실행/testlog 기록
- 확인 우선: 실제 확인 후 응답, 추측 금지
- 범위 준수: 요청한 것만 구현
- 모듈 변경 시 README/README.ko 동기화
</priority_reminder>
EOF
```

- [ ] **Step 4: reminder 스크립트 단독 실행 확인**

Run: `bash /Users/debop/.claude/hooks/session-priority-reminder.sh < /private/tmp/empty-hook-input.json`
Expected: stdout에 `<priority_reminder>` 블록 출력

- [ ] **Step 5: settings.json의 SessionStart hooks에 command hook 추가**

```json
{
  "type": "command",
  "command": "bash /Users/debop/.claude/hooks/session-priority-reminder.sh"
}
```

- [ ] **Step 6: settings.json의 UserPromptSubmit hooks에도 동일 hook 추가**

```json
{
  "type": "command",
  "command": "bash /Users/debop/.claude/hooks/session-priority-reminder.sh"
}
```

- [ ] **Step 7: jq로 설정 JSON 유효성 검증**

Run: `jq -e '.hooks.SessionStart, .hooks.UserPromptSubmit' /Users/debop/.claude/settings.json >/dev/null`
Expected: exit 0

- [ ] **Step 8: Commit**

```bash
git add /Users/debop/.claude/hooks/session-priority-reminder.sh /Users/debop/.claude/settings.json
git commit -m "feat: add session priority reminders"
```

### Task 4: auto-memory 규칙 압축 정리

**Files:**
- Modify: `/Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/MEMORY.md`
- Create: `/Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/feedback_test_enforcement.md`
- Create: `/Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/feedback_response_verification.md`

- [ ] **Step 1: 테스트 우선 규칙 memory 파일 작성**

```markdown
---
name: 테스트 누락 방지 우선 규칙
description: 코드 변경 후 테스트 작성, 실행, testlog 기록을 우선적으로 강제해야 하는 사용자 선호
type: feedback
---
테스트 관련 작업에서는 구현보다 테스트 누락 방지를 더 중요하게 다룬다.

**Why:** 사용자가 가장 자주 체감하는 forgetting이 테스트 코드 누락, 테스트 실행 누락, testlog 기록 누락이기 때문이다.
**How to apply:** 코드 파일을 수정하면 테스트 작성/실행/testlog 기록 여부를 먼저 점검하고, 누락 시 즉시 보완을 유도한다.
```

- [ ] **Step 2: 확인 후 응답 규칙 memory 파일 작성**

```markdown
---
name: 확인 후 응답 규칙
description: 일반론보다 실제 파일과 설정을 먼저 확인하고 답해야 하는 협업 규칙
type: feedback
---
실제 파일, 설정, 훅, 저장 상태를 확인한 뒤 답한다.

**Why:** 사용자는 확인 없는 일반론 답변을 가장 불필요하게 느끼며 신뢰를 떨어뜨린다.
**How to apply:** 설정, 저장, 동작 여부 질문에는 먼저 파일이나 현재 상태를 확인하고, 확인 결과를 근거로 대답한다.
```

- [ ] **Step 3: MEMORY.md 인덱스에 두 파일 포인터 추가**

```markdown
- [feedback_test_enforcement.md](feedback_test_enforcement.md) — 테스트 코드/실행/testlog 누락 방지를 최우선으로 다룬다
- [feedback_response_verification.md](feedback_response_verification.md) — 일반론보다 실제 확인 결과를 근거로 답한다
```

- [ ] **Step 4: memory 파일 형식 검토**

Run: `python3 - <<'PY'
from pathlib import Path
for p in [
Path('/Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/feedback_test_enforcement.md'),
Path('/Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/feedback_response_verification.md')]:
    txt=p.read_text()
    assert '---' in txt and 'Why:' in txt and 'How to apply:' in txt
print('OK')
PY`
Expected: `OK`

- [ ] **Step 5: Commit**

```bash
git add /Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/MEMORY.md \
  /Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/feedback_test_enforcement.md \
  /Users/debop/.claude/projects/-Users-debop-work-bluetape4k-exposed-workshop/memory/feedback_response_verification.md
git commit -m "docs: tighten memory rules for test enforcement"
```

### Task 5: testlog 기본 파일 준비 및 검사 경로 명확화

**Files:**
- Create: `/Users/debop/work/bluetape4k/exposed-workshop/docs/testlogs/2026-04.md`
- Modify: `/Users/debop/.claude/hooks/test-enforcement-check.sh`

- [ ] **Step 1: testlogs 디렉터리 생성**

Run: `mkdir -p /Users/debop/work/bluetape4k/exposed-workshop/docs/testlogs`
Expected: 디렉터리 생성

- [ ] **Step 2: 월간 testlog 파일 기본 템플릿 작성**

```markdown
# 2026-04 Test Logs

| 날짜 | 작업 | 대상 | 테스트 항목 | 결과 | 소요 | 비고 |
|------|------|------|-------------|------|------|------|
```

- [ ] **Step 3: test-enforcement-check에 testlog 존재 확인 추가**

```bash
TESTLOG_FILE="/Users/debop/work/bluetape4k/exposed-workshop/docs/testlogs/$(date +%Y-%m).md"
if [[ ! -f "$TESTLOG_FILE" ]]; then
  echo "   3) testlog 파일 없음: $TESTLOG_FILE" >&2
fi
```

- [ ] **Step 4: 직접 실행으로 testlog 경고가 보이는지 확인**

Run: `bash /Users/debop/.claude/hooks/test-enforcement-check.sh < /private/tmp/test-enforcement-input.json`
Expected: testlog 파일이 없던 경우 경고, 파일 생성 후에는 해당 줄 사라짐

- [ ] **Step 5: Commit**

```bash
git add /Users/debop/work/bluetape4k/exposed-workshop/docs/testlogs/2026-04.md \
  /Users/debop/.claude/hooks/test-enforcement-check.sh
git commit -m "test: add monthly testlog template"
```

### Task 6: mempalace/wiki/obsidian 분류 가이드 구현

**Files:**
- Create: `/Users/debop/.claude/hooks/knowledge-routing-reminder.sh`
- Modify: `/Users/debop/.claude/hooks/mempalace-save-hook.sh`
- Create: `/Users/debop/work/bluetape4k/exposed-workshop/.omc/wiki/memory-hybrid-enforcement.md`
- Create or Modify: `/Users/debop/Library/Mobile Documents/com~apple~CloudDocs/Obsidian/<vault>/claude-memory-hybrid-enforcement.md`

- [ ] **Step 1: 분류 reminder 스크립트 작성**

```bash
#!/usr/bin/env bash
set -euo pipefail
cat <<'EOF'
<knowledge_routing>
- 행동 규칙/사용자 선호: auto-memory
- 환경 설정/도구 설치/장기 회고: mempalace chats
- 반복 재사용 패턴/결정사항: project wiki
- 장문 연결 노트/개인 지식 허브: obsidian
- 같은 내용을 모든 저장소에 복제하지 말고 원문 1개 + 요약/포인터로 유지
</knowledge_routing>
EOF
```

- [ ] **Step 2: mempalace-save-hook 출력문에 분류 기준 한 줄 추가**

```bash
저장 전 분류 기준:
- 행동 규칙은 auto-memory 우선
- 환경/회고는 chats + mempalace
- 재사용 패턴은 wiki
- 장문 연결은 obsidian
```

- [ ] **Step 3: wiki 문서 작성**

```markdown
---
title: Memory Hybrid Enforcement
tags: [claude, memory, hooks, testing]
category: pattern
created: 2026-04-10
---

## 목적

테스트 누락을 hook으로 줄이고, 행동 규칙은 auto-memory로 유지한다.

## 저장소 역할

- auto-memory: 협업 규칙
- mempalace: 환경/회고/장기 검색
- wiki: 반복 패턴
- obsidian: 사람 중심 허브
```

- [ ] **Step 4: obsidian 노트 초안 작성 또는 기존 허브에 링크 추가**

```markdown
# Claude Memory Hybrid Enforcement

- Spec: `docs/superpowers/specs/2026-04-10-memory-hybrid-enforcement-design.md`
- Plan: `docs/superpowers/plans/2026-04-10-memory-hybrid-enforcement.md`
- Wiki: `.omc/wiki/memory-hybrid-enforcement.md`
- mempalace: chats 기반 장기 검색

## 운영 원칙
- 테스트 강제는 hook
- 행동 규칙은 auto-memory
- 문서 자산은 wiki/obsidian
```

- [ ] **Step 5: Commit**

```bash
git add /Users/debop/.claude/hooks/knowledge-routing-reminder.sh \
  /Users/debop/.claude/hooks/mempalace-save-hook.sh \
  /Users/debop/work/bluetape4k/exposed-workshop/.omc/wiki/memory-hybrid-enforcement.md
git commit -m "docs: add knowledge routing guidance"
```

### Task 7: Hook 실제 동작 검증

**Files:**
- Modify: `/private/tmp/test-enforcement-input.json`
- Test: `/Users/debop/.claude/settings.json`

- [ ] **Step 1: 테스트 강제 훅 단독 검증**

Run: `bash /Users/debop/.claude/hooks/test-enforcement-check.sh < /private/tmp/test-enforcement-input.json 2>&1 | tee /private/tmp/test-enforcement-check.log`
Expected: 경고 메시지와 testlog 항목 출력

- [ ] **Step 2: dispatcher 경유 검증**

Run: `bash /Users/debop/.claude/hooks/post-edit-dispatcher.sh < /private/tmp/test-enforcement-input.json 2>&1 | tee /private/tmp/post-edit-dispatcher.log`
Expected: dispatcher를 통해 동일 경고 출력

- [ ] **Step 3: session reminder 단독 검증**

Run: `bash /Users/debop/.claude/hooks/session-priority-reminder.sh < /private/tmp/empty-hook-input.json`
Expected: `<priority_reminder>` 출력

- [ ] **Step 4: knowledge routing reminder 단독 검증**

Run: `bash /Users/debop/.claude/hooks/knowledge-routing-reminder.sh < /private/tmp/empty-hook-input.json`
Expected: `<knowledge_routing>` 출력

- [ ] **Step 5: settings.json hook schema 검증**

Run: `jq -e '.hooks.PostToolUse[]?.hooks, .hooks.SessionStart[]?.hooks, .hooks.UserPromptSubmit[]?.hooks, .hooks.Stop[]?.hooks' /Users/debop/.claude/settings.json >/dev/null`
Expected: exit 0

- [ ] **Step 6: exposed-workshop 관련 테스트 실행**

Run: `./bin/repo-test-summary -- ./gradlew :01-spring-boot:test`
Expected: 테스트 요약 성공 또는 현재 상태 기준 의미 있는 실패 원인 출력

- [ ] **Step 7: testlog 기록**

```markdown
| 2026-04-10 | memory hybrid enforcement hook 검증 | hooks/settings | session-priority-reminder, test-enforcement-check | PASS 또는 실제 결과 | 수동기록 | initial verification |
```

- [ ] **Step 8: Commit**

```bash
git add /Users/debop/.claude/settings.json \
  /Users/debop/work/bluetape4k/exposed-workshop/docs/testlogs/2026-04.md
git commit -m "test: verify memory hybrid enforcement hooks"
```

## Self-Review

- Spec coverage: 테스트 강제, memory 규칙 압축, mempalace/wiki/obsidian 역할 분리가 모두 Task 1~7에 매핑되어 있다.
- Placeholder scan: TODO/TBD/implement later 없음. 각 코드/명령/파일 경로를 명시했다.
- Type consistency: hook 파일명, settings 경로, memory 경로, testlog 경로를 문서 전체에서 동일하게 유지했다.
