---
name: logseq-db-agent-guide
description: Guide for AI agents to read and write the Logseq graph database via the CLI. Covers graph discovery, reading pages/blocks/tasks, writing data, querying with Datascript, and common pitfalls.
---

# Hướng dẫn cho Agent: Sử dụng Logseq Graph Database

## Tổng quan

Logseq lưu dữ liệu trong một **graph database** dựa trên Datascript. Agent truy cập database này thông qua **`logseq` CLI**. Không có REST API hay SQL — tất cả đọc/ghi đều qua CLI.

---

## Bước 1: Kiểm tra CLI có sẵn

```bash
logseq --version
logseq --help
```

Nếu không tìm thấy, kiểm tra `PATH` hoặc đặt biến môi trường:
```bash
export LOGSEQ_CLI_ROOT_DIR=~/logseq   # thư mục chứa graphs
```

---

## Bước 2: Khám phá graphs có sẵn

```bash
# Liệt kê tất cả graphs
logseq graph list

# Thông tin chi tiết về graph hiện tại
logseq graph info --graph <tên-graph>

# Xác thực tính toàn vẹn của graph
logseq graph validate --graph <tên-graph>
```

> **Quy tắc**: Luôn dùng `--graph <tên>` để chỉ rõ graph mục tiêu. Không dựa vào graph mặc định.

---

## Bước 3: Đọc dữ liệu

### Pages

```bash
# Liệt kê pages (tối đa 3 dòng/row trong bảng, dùng --output json cho đầy đủ)
logseq list page --graph <tên-graph>
logseq list page --graph <tên-graph> --output json

# Xem nội dung một page (cây blocks)
logseq show --id <db-id-hoặc-uuid> --graph <tên-graph>
```

### Blocks

```bash
# Liệt kê blocks
logseq list node --graph <tên-graph>

# Tìm kiếm blocks theo nội dung
logseq search block --query "từ khóa" --graph <tên-graph>
```

### Tasks

```bash
# Liệt kê tất cả tasks
logseq list task --graph <tên-graph>

# Lọc theo trạng thái (TODO, DOING, DONE, LATER, WAITING, CANCELLED)
logseq list task --graph <tên-graph> --output json | jq '.[] | select(.status == "TODO")'
```

### Tags và Properties

```bash
logseq list tag --graph <tên-graph>
logseq list property --graph <tên-graph>
```

---

## Bước 4: Query với Datascript

Datascript là query engine của Logseq (cú pháp Datalog).

```bash
# Query tất cả page names
logseq query --graph <tên-graph> \
  --query '[:find ?name :where [?b :block/name ?name]]'

# Query blocks có tag cụ thể
logseq query --graph <tên-graph> \
  --query '[:find ?content :where [?b :block/content ?content] [?b :block/tags ?t] [?t :block/name "meeting"]]'

# Xem danh sách saved queries
logseq query list --graph <tên-graph>
```

> **Tip**: Chạy `logseq example query` để xem ví dụ mẫu.

---

## Bước 5: Ghi dữ liệu

### Tạo / cập nhật block

```bash
# Tạo block mới trong một page
logseq upsert block \
  --page "Tên Page" \
  --content "Nội dung block" \
  --graph <tên-graph>

# Cập nhật block hiện có (chế độ update khi có --id)
logseq upsert block \
  --id <db-id> \
  --content "Nội dung mới" \
  --graph <tên-graph>

# Tạo block có tag
logseq upsert block \
  --page "Tên Page" \
  --content "Nội dung" \
  --update-tags '["TagName"]' \
  --graph <tên-graph>
```

> **Quy tắc cấu trúc**: Với nội dung có cấu trúc (danh sách, outline), tạo **nhiều blocks** thay vì một block dài. Mỗi bullet/mục là một block riêng.

### Tạo / cập nhật page

```bash
logseq upsert page \
  --name "Tên Page Mới" \
  --graph <tên-graph>
```

### Tạo task

```bash
# Xem options trước
logseq upsert task --help

# Tạo task mới
logseq upsert task \
  --content "Tên task" \
  --status TODO \
  --graph <tên-graph>
```

### Xóa

```bash
logseq remove block --id <db-id> --graph <tên-graph>
logseq remove page --name "Tên Page" --graph <tên-graph>
```

---

## Mô hình dữ liệu (Data Model)

```
Graph
└── Pages (mỗi page có :block/name, :block/uuid)
    └── Blocks (mỗi block có :block/content, :db/id, :block/uuid)
        ├── Child blocks (parent-child bằng :block/parent)
        ├── Tags ([:block/tags] → list tag entities)
        └── Properties (:block/properties map)

Tasks = blocks có :block/marker (TODO/DOING/DONE/...)
Tags = pages đặc biệt có :block/type "tag"
Properties = khai báo kiểu dữ liệu cho blocks/pages
```

---

## Workflow điển hình cho Agent

### Đọc context trước khi ghi

```bash
# 1. Tìm page liên quan
logseq search page --query "keyword" --graph <tên>

# 2. Xem nội dung page
logseq show --id <page-id> --graph <tên>

# 3. Ghi dữ liệu vào đúng vị trí
logseq upsert block --page "Tên Page" --content "..." --graph <tên>
```

### Xử lý tasks

```bash
# 1. Liệt kê tasks cần làm
logseq list task --graph <tên> --output json

# 2. Cập nhật trạng thái task
logseq upsert task --id <task-id> --status DONE --graph <tên>
```

---

## Quy tắc bắt buộc cho Agent

1. **Luôn chạy `logseq <cmd> --help` trước** khi dùng command mới — options có thể thay đổi.
2. **Luôn dùng `--graph`** — không giả định graph mặc định.
3. **Không hardcode IDs** — discover bằng `list` hoặc `search` trước.
4. **Tags phải tồn tại trước** khi gắn vào block. Tạo tag với `upsert tag --name "X"` nếu cần.
5. **Dùng `--output json`** khi cần parse output trong code/pipeline.
6. **Không nhúng hashtag vào `--content`** thay cho `--update-tags` — đây là pitfall phổ biến.

---

## Dữ liệu thực từ database người dùng

Graphs hiện có (đọc bằng `logseq graph list`):
- **"trí nhớ của tôi ( note )"** — graph chính
- **"đồ thị mới tiếng việt"**
- **"đồ thị tiếng việt 2"**

Pages người dùng đã tạo: `Game Hacking` (tag + query), Journal tự động theo ngày.

---

## Tài liệu tham khảo

- `skill_thuong/logseq-user-guide/SKILL.md` — **Hướng dẫn sử dụng app** cho người dùng (tạo page, block, task, journal, shortcut...)
- `skill_thuong/logseq-cli/SKILL.md` — Tài liệu đầy đủ về CLI
- `logseq example` — Ví dụ commands có thể chạy trực tiếp
- `docs/agent-guide/logseq-cli/` — Ghi chú kỹ thuật chuyên sâu
