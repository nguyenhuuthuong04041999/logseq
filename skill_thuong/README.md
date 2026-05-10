# skill_thuong — Bộ Agent Skills cho Logseq

Thư mục này chứa toàn bộ agent skills của dự án Logseq, được bóc tách ra để dễ quản lý, tái sử dụng và chia sẻ.

---

## Cấu trúc thư mục

```
skill_thuong/
├── README.md                          ← File này (hướng dẫn setup)
├── esm-cjs-risk-scan/
│   ├── SKILL.md                       ← Mô tả skill
│   └── scripts/
│       └── scan_esm_cjs_risk.mjs      ← Script scan ESM/CJS
├── logseq-cli/
│   └── SKILL.md                       ← Hướng dẫn dùng Logseq CLI
├── logseq-cli-maintenance/
│   └── SKILL.md                       ← Hướng dẫn refactor CLI code
├── logseq-db-agent-guide/
│   └── SKILL.md                       ← Hướng dẫn agent đọc/ghi Logseq graph database
├── logseq-debug-workflow/
│   └── SKILL.md                       ← Quy trình debug Logseq
├── logseq-dependency-upgrade/
│   ├── SKILL.md                       ← Hướng dẫn nâng cấp dependencies
│   └── scripts/
│       └── audit_logseq_dependencies.mjs
├── logseq-i18n/
│   └── SKILL.md                       ← Quy trình i18n/dịch thuật
└── logseq-repl/
    ├── SKILL.md                       ← Hướng dẫn REPL workflow
    ├── scripts/
    │   ├── start-repl.sh              ← Khởi động REPL
    │   ├── start-repl.py              ← Python wrapper
    │   ├── cleanup-repl.sh            ← Dọn dẹp REPL
    │   ├── common.sh                  ← Shared functions
    │   └── verify-repls.sh            ← Kiểm tra REPL targets
    └── tests/
        ├── test-lib.sh
        └── test-logseq-repl.sh
```

---

## Tổng quan các Skills

| Skill | Mục đích | Khi nào dùng |
|-------|----------|--------------|
| **esm-cjs-risk-scan** | Scan rủi ro ESM/CJS cho Electron/Node targets | Thay đổi Electron deps, debug import errors, audit trước khi upgrade |
| **logseq-cli** | Vận hành Logseq CLI để inspect/edit graph, pages, blocks, tasks | Khi cần chạy lệnh `logseq` hoặc đọc CLI output |
| **logseq-cli-maintenance** | Cải thiện readability và maintainability của CLI code | Refactor, clean up, improve structure CLI |
| **logseq-db-agent-guide** | Hướng dẫn agent đọc/ghi Logseq graph database qua CLI | Khi agent cần truy cập data (pages, blocks, tasks, queries) trong graph |
| **logseq-user-guide** | Hướng dẫn sử dụng app Logseq cho người dùng | Tạo page, block, task, journal, template; phím tắt; graph view |
| **logseq-debug-workflow** | Debug bugs Logseq đúng runtime, có bằng chứng before/after | Điều tra bất kỳ bug Logseq nào |
| **logseq-dependency-upgrade** | Audit và lập kế hoạch nâng cấp dependencies | Khi cần kiểm tra outdated/vulnerable packages |
| **logseq-i18n** | Workflow i18n cho thêm/sửa/xét translation keys | Khi thêm/sửa UI text, làm việc với `dicts/`, i18n compliance |
| **logseq-repl** | Khởi động và điều phối Logseq REPL cho `:app`, `:electron`, `:db-worker-node` | Phát triển, debug trực tiếp trong runtime |

---

## Yêu cầu hệ thống

### Bắt buộc

- **Node.js** ≥ 18 (để chạy `.mjs` scripts)
- **pnpm** (package manager của dự án)
- **Babashka** (`bb`) — CLI runner cho Clojure scripts
- **Java** — cần cho ClojureScript compiler

### Kiểm tra

```bash
node --version       # >= 18
pnpm --version
bb --version
java --version
```

### Tùy chọn (cho logseq-repl)

- **Python 3** — `start-repl.py` dùng Python 3
- **shadow-cljs** — cài qua `pnpm install` trong repo

---

## Hướng dẫn Setup từng Skill

---

### 1. esm-cjs-risk-scan

**Yêu cầu:** Node.js ≥ 18, đang đứng ở root repo

```bash
# Chạy từ root của repo logseq
cd /path/to/logseq

# Scan Electron scope (mặc định)
node skill_thuong/esm-cjs-risk-scan/scripts/scan_esm_cjs_risk.mjs

# Scan tất cả Node targets
node skill_thuong/esm-cjs-risk-scan/scripts/scan_esm_cjs_risk.mjs --scope all-node

# Output JSON
node skill_thuong/esm-cjs-risk-scan/scripts/scan_esm_cjs_risk.mjs --format json

# Verbose (xem chi tiết lỗi)
node skill_thuong/esm-cjs-risk-scan/scripts/scan_esm_cjs_risk.mjs --verbose
```

> **Lưu ý:** Script phải chạy từ repo root. Nó tự tìm `package.json` và source dirs tương đối với CWD.

---

### 2. logseq-cli

**Yêu cầu:** `logseq` CLI đã được cài và có trong PATH

```bash
# Kiểm tra CLI có sẵn không
logseq --help

# Xem ví dụ commands
logseq example

# Liệt kê graphs
logseq graph list

# Liệt kê pages trong graph
logseq list page --graph <tên-graph>

# Query
logseq query --graph <tên-graph> --query '[:find ?name :where [?b :block/name ?name]]'
```

Đọc `skill_thuong/logseq-cli/SKILL.md` để hiểu đầy đủ các command groups, options, và pitfalls.

---

### 3. logseq-cli-maintenance

Đây là skill định hướng (không có scripts riêng). Đọc `SKILL.md` trước khi refactor CLI code để biết:

- Các nguyên tắc tách biệt concern (parse / validate / execute / present)
- Checklist refactoring
- Anti-patterns cần tránh

---

### 4. logseq-debug-workflow

Đây là skill quy trình (workflow skill). Đọc `SKILL.md` trước khi debug bất kỳ bug Logseq nào.

**Quy trình tóm tắt:**
1. Chọn đúng runtime REPL (`:app` / `:electron` / `:db-worker-node` / CLI)
2. Reproduce bug trong runtime REPL **trước** khi sửa code
3. Thu thập bằng chứng (logs, REPL output)
4. Áp dụng fix nhỏ nhất
5. Verify lại sau fix

**Vị trí logs quan trọng:**
```
tmp/logseq-repl/shared-shadow-watch.log
tmp/logseq-repl/desktop-electron.log
~/Library/Logs/Logseq/main.log          (macOS)
```

---

### 5. logseq-dependency-upgrade

**Yêu cầu:** Node.js ≥ 18, internet access (fetch npm/Clojars/OSV)

```bash
# Chạy audit từ root repo
cd /path/to/logseq

node skill_thuong/logseq-dependency-upgrade/scripts/audit_logseq_dependencies.mjs \
  --output-json /tmp/deps-audit.json \
  --output-md /tmp/deps-report.md

# Tùy chọn thêm
  --stale-months 24          # Coi là stale nếu không update 24 tháng (mặc định 36)
  --max-update-interval 3    # Bỏ qua nếu latest publish cách current < 3 tháng (mặc định 6)
  --include-prerelease       # Hiển thị thêm pre-release versions
```

Sau khi chạy, đọc file `.md` để xem plan upgrade theo batch.

---

### 6. logseq-i18n

**Yêu cầu:** `bb` (Babashka) đã cài, đứng ở root repo

**Validation commands:**
```bash
# Validate translation keys
bb lang:validate-translations

# Lint hardcoded UI strings (chỉ files đã thay đổi)
bb lang:lint-hardcoded --git-changed

# Format dictionary files
bb lang:format-dicts
```

**Files quan trọng:**
- `src/resources/dicts/en.edn` — source of truth cho English
- `.i18n-lint.toml` — cấu hình lint scope
- `docs/i18n-key-naming.md` — quy tắc đặt tên key
- `src/main/frontend/context/i18n.cljs` — translation helpers

Đọc `skill_thuong/logseq-i18n/SKILL.md` để biết đầy đủ 7 rules quan trọng.

---

### 7. logseq-repl

**Yêu cầu:** `pnpm`, `Python 3`, `bash` (Linux/macOS hoặc WSL trên Windows)

```bash
# Bước 1: Dọn dẹp REPL cũ
./skill_thuong/logseq-repl/scripts/cleanup-repl.sh

# Bước 2: Khởi động tất cả runtimes
./skill_thuong/logseq-repl/scripts/start-repl.sh --repo <tên-graph>

# Bước 3: Attach vào runtime cần thiết
pnpm exec shadow-cljs cljs-repl app          # Renderer UI
pnpm exec shadow-cljs cljs-repl electron     # Electron main process
pnpm exec shadow-cljs cljs-repl db-worker-node  # Worker node

# Bước 4: Khi xong, cleanup
./skill_thuong/logseq-repl/scripts/cleanup-repl.sh
```

**Kiểm tra runtime counts (trước khi attach):**
```bash
pnpm exec shadow-cljs clj-eval "(do (require '[shadow.cljs.devtools.api :as api]) (println {:app (count (api/repl-runtimes :app)) :electron (count (api/repl-runtimes :electron)) :db-worker-node (count (api/repl-runtimes :db-worker-node))}))"
```

> **Lưu ý Windows:** Các scripts `.sh` yêu cầu bash. Dùng **WSL**, **Git Bash**, hoặc **MSYS2**. Scripts `.py` chạy được trực tiếp với `python3`.

---

## Cách tích hợp với Claude Code / AI Agents

### Cấu hình trong AGENTS.md (hoặc CLAUDE.md)

Thêm vào file `AGENTS.md` hoặc `CLAUDE.md` tại root repo:

```markdown
## Agent Skills

- Use repo-local skills discovered under `skill_thuong/`; load the matching `SKILL.md` before editing files or proposing changes.
- **i18n (mandatory)**: Always load `skill_thuong/logseq-i18n/SKILL.md` before any change that adds, edits, or removes user-facing UI text.
```

### Load skill thủ công (trong chat với agent)

```
Đọc skill_thuong/logseq-cli/SKILL.md trước khi làm bất kỳ task nào liên quan đến Logseq CLI.
```

### Thứ tự ưu tiên load skills

| Task | Skill cần load |
|------|---------------|
| Thêm/sửa UI text | `logseq-i18n` (bắt buộc) |
| Chạy `logseq` commands | `logseq-cli` |
| Đọc/ghi graph database | `logseq-db-agent-guide` + `logseq-cli` |
| Refactor CLI code | `logseq-cli-maintenance` + `logseq-cli` |
| Debug bug | `logseq-debug-workflow` + `logseq-repl` + `logseq-cli` |
| Upgrade dependencies | `logseq-dependency-upgrade` |
| Thay đổi Electron deps | `esm-cjs-risk-scan` |
| Dev REPL session | `logseq-repl` |

---

## Nguồn gốc

Skills này được bóc tách từ `.agents/skills/` của repo Logseq. Nội dung gốc giữ nguyên, không chỉnh sửa.

Để cập nhật khi repo thay đổi:
```bash
# Từ root repo
rm -rf skill_thuong && cp -r .agents/skills skill_thuong
# Sau đó copy lại README này nếu đã tùy chỉnh
```
