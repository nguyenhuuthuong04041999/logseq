---
name: logseq-user-guide
description: Hướng dẫn sử dụng Logseq app dành cho người dùng — tạo page, block, tag, task, journal, template; dùng graph view; phím tắt; và kết nối các ghi chú. Dựa trên dữ liệu thực từ graph database.
---

# Hướng dẫn sử dụng Logseq App

> Dữ liệu thực từ database người dùng (đọc bằng CLI):
> - **3 graphs:** "trí nhớ của tôi ( note )", "đồ thị mới tiếng việt", "đồ thị tiếng việt 2"
> - **Tags người dùng tạo:** Game Hacking
> - **Journal gần nhất:** May 8th, 2026

---

## 1. KHÁI NIỆM CƠ BẢN

### Cấu trúc dữ liệu (từ database thực tế)

```
Graph ("trí nhớ của tôi ( note )")
├── Pages (Trang)
│   ├── Journal: "May 8th, 2026"   ← tự tạo hàng ngày
│   └── Page: "Game Hacking"       ← người dùng tạo
├── Tags (Thẻ)
│   ├── Built-in: Task, Query, Card, Template, Journal...
│   └── User: Game Hacking
└── Properties (Thuộc tính)
    ├── Status, Priority, Deadline, Scheduled  ← cho Task
    ├── Due, State                              ← cho Card (thẻ học)
    └── Description, Icon, External URL...
```

### 3 đơn vị cơ bản

| Đơn vị | Là gì | Ví dụ thực tế trong DB |
|--------|-------|------------------------|
| **Graph** | Kho dữ liệu toàn bộ | "trí nhớ của tôi ( note )" |
| **Page** | Trang ghi chú | "Game Hacking", "May 8th, 2026" |
| **Block** | Dòng/đoạn nội dung | "1. Kênh cho người bắt đầu..." |

---

## 2. TẠO NỘI DUNG MỚI

### Tạo Page (Trang ghi chú)

**Cách 1 — Phím tắt `Ctrl+K` (nhanh nhất)**
```
1. Nhấn Ctrl+K
2. Gõ tên trang muốn tạo
3. Chọn "Tạo trang tên '...'" → Enter
```

**Cách 2 — Gõ `[[` trong bất kỳ block nào**
```
Gõ [[Game Hacking]]  →  popup hiện ra
→ Chọn "Trang mới: Game Hacking"
```

**Cách 3 — CLI**
```bash
logseq upsert page --name "Tên trang" --graph "trí nhớ của tôi ( note )"
```

---

### Tạo Block (Ghi chú/Nội dung)

Block là **đơn vị nhỏ nhất** — mỗi dòng là 1 block.

```
Trong bất kỳ trang nào, click vào vùng trắng rồi gõ.

Enter       →  Block mới ngang hàng
Tab         →  Block con (thụt vào, thành child)
Shift+Tab   →  Block cha (thụt ra)
```

**Ví dụ thực tế** (từ page "Game Hacking" trong DB của bạn):
```
Page: Game Hacking
├── 1. Kênh cho người bắt đầu          ← block cha
│   ├── • Tên kênh: Stephen Chapman    ← block con (Tab)
│   ├── • Tại sao xem: ...
│   └── • Playlist phải xem: ...
└── 2. Kênh cho người muốn làm Trùm
    └── • Tên kênh: Guided Hacking
```

**CLI:**
```bash
# Thêm block vào page
logseq upsert block \
  --page "Game Hacking" \
  --content "Nội dung mới" \
  --graph "trí nhớ của tôi ( note )"
```

---

### Tạo Tag (Thẻ phân loại)

**Trong block — gõ `#`:**
```
Học [[Clean Code]] xong rồi   #HọcTập   #Sách
```

**Tạo tag mới qua `Ctrl+K`:**
```
Ctrl+K → gõ tên → chọn "Tạo thẻ tên '...'"
```

**CLI:**
```bash
logseq upsert tag --name "HọcTập" --graph "trí nhớ của tôi ( note )"
```

> **Lưu ý:** Tag phải tồn tại trước khi gắn vào block.

---

### Tạo Task (Công việc)

**Cách 1 — Gõ `/` trong block:**
```
/ → tìm "TODO" hoặc "Task" → Enter
```

**Cách 2 — Gắn tag #Task:**
```
Mua sữa đi chợ   #Task
```

**Trạng thái Task** (từ database thực tế):

| Trạng thái | Ý nghĩa |
|-----------|---------|
| `Backlog` | Chưa lên kế hoạch |
| `Todo` | Cần làm |
| `Doing` | Đang làm |
| `In Review` | Đang xem lại |
| `Done` | Xong |
| `Canceled` | Đã hủy |

**CLI:**
```bash
# Tạo task
logseq upsert task \
  --content "Đọc sách Clean Code" \
  --status Todo \
  --graph "trí nhớ của tôi ( note )"

# Liệt kê tasks
logseq list task --graph "trí nhớ của tôi ( note )"

# Đổi trạng thái
logseq upsert task --id <id> --status Done --graph "trí nhớ của tôi ( note )"
```

---

### Tạo Journal (Nhật ký hàng ngày)

Journal **tự động tạo mỗi ngày**. Không cần tạo thủ công.

```
Phím tắt:  Alt+J    →  Mở journal hôm nay
```

Mỗi ngày có 1 trang journal. Ví dụ thực tế trong DB của bạn:
- **May 8th, 2026** — có blocks: "asd" và 2 blocks trống

---

### Tạo Card / Thẻ học (Flashcard)

```
1. Tạo page hoặc block
2. Gắn tag #Card
3. Logseq tự thêm thuộc tính: Due (Hạn ôn tập), State (Trạng thái FSRS)
4. Dùng tính năng Review để ôn tập theo thuật toán FSRS
```

---

### Tạo Template (Mẫu có sẵn)

```
1. Tạo page → gắn tag #Template
2. Viết nội dung mẫu trong page đó
3. Khi muốn dùng: gõ /Template trong bất kỳ page nào
4. Nếu muốn tự động áp dụng cho một tag:
   → Thêm thuộc tính: Apply template to tags:: #TênTag
```

---

## 3. KẾT NỐI CÁC GHI CHÚ

### Liên kết trang (`[[]]`)
```
Hôm nay học về [[Game Hacking]]
→ Tạo liên kết 2 chiều tới page Game Hacking
```

### Gắn Tag
```
Phương pháp hay     #HọcTập   #GameHacking
→ Block này nằm trong cả 2 tag
```

### Thuộc tính (Properties)
```
Alias:: Bí danh của trang
Description:: Mô tả ngắn
Deadline:: 2026-05-15
```

---

## 4. GRAPH VIEW (XEM ĐỒ THỊ)

Graph view hiển thị **mạng lưới kết nối** giữa các pages.

**Để page xuất hiện trong graph:**
1. Page phải có **ít nhất 1 block có nội dung**
2. Có **kết nối** với page khác qua `[[link]]` hoặc `#Tag`
3. Kiểm tra **Filters** — bật hiển thị các loại node cần thiết

**Ví dụ thực tế** (trong graph "trí nhớ của tôi"):
```
Template ──── Tags ──── Journal
                  └──── May 8th, 2026 ──── Code
                                      └──── Card
                                      └──── Game Hacking ← page của bạn
```

---

## 5. PHÍM TẮT QUAN TRỌNG

| Tác vụ | Phím tắt |
|--------|----------|
| Command Palette (làm tất cả mọi thứ) | `Ctrl+K` |
| Journal hôm nay | `Alt+J` |
| Block mới | `Enter` |
| Block con | `Tab` |
| Block cha | `Shift+Tab` |
| Slash commands | `/` (trong block) |
| Tạo page reference | `[[` |
| Thêm tag | `#` |
| Tìm kiếm toàn bộ | `Ctrl+U` |
| Di chuyển block lên/xuống | `Alt+Shift+↑/↓` |

---

## 6. SLASH COMMANDS (`/`)

Gõ `/` trong block để mở menu:

| Lệnh | Kết quả |
|------|---------|
| `/TODO` | Task trạng thái Todo |
| `/Today` | Chèn ngày hôm nay |
| `/Template` | Áp dụng template |
| `/Code` | Block mã nguồn |
| `/Quote` | Block trích dẫn |
| `/Math` | Block toán học |
| `/Upload` | Tải file lên |

---

## 7. ĐỌC/GHI DATABASE BẰNG CLI

### Xem graph có sẵn
```bash
logseq graph list
# → trí nhớ của tôi ( note )
# → đồ thị mới tiếng việt
# → đồ thị tiếng việt 2
```

### Đọc pages
```bash
logseq list page --graph "trí nhớ của tôi ( note )" --output json
logseq search page --query "Game" --graph "trí nhớ của tôi ( note )"
logseq show --id 223 --graph "trí nhớ của tôi ( note )"
```

### Đọc tags
```bash
logseq list tag --graph "trí nhớ của tôi ( note )"
```

### Ghi dữ liệu
```bash
# Tạo page mới
logseq upsert page --name "Tên trang" --graph "trí nhớ của tôi ( note )"

# Thêm block có tag
logseq upsert block \
  --page "Tên trang" \
  --content "Nội dung block" \
  --update-tags '["Game Hacking"]' \
  --graph "trí nhớ của tôi ( note )"

# Query Datascript
logseq query \
  --graph "trí nhớ của tôi ( note )" \
  --query '[:find ?title :where [?b :block/title ?title] [?b :block/tags ?t] [?t :block/name "game hacking"]]'
```

---

## 8. TÀI LIỆU LIÊN QUAN

- [Hướng dẫn Agent dùng database](../logseq-db-agent-guide/SKILL.md) — đọc/ghi qua CLI dành cho AI agent
- [Hướng dẫn CLI đầy đủ](../logseq-cli/SKILL.md) — tất cả lệnh CLI
