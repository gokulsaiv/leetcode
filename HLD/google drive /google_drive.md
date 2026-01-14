# Google Drive – Interview-Ready Hybrid System Design

> **Mental model:** Google Drive is a *metadata system* with *blob storage underneath*.
> Control plane = metadata. Data plane = file blobs.

---

## 1. Scope & Assumptions

### Functional Requirements

* Upload / download files (small + large)
* Folder hierarchy (create, move, rename)
* File versioning
* File & folder sharing (read / write)
* Sync across multiple devices

### Non‑Functional Requirements

* High availability
* Scales to tens of millions of DAU
* Supports GB‑scale files
* Strong consistency for metadata
* Eventual consistency for file content & sync

---

## 2. High‑Level Architecture

```
Client
  ├── API Server (Metadata / Control Plane)
  │      ├── Metadata DB
  │      ├── Cache (Redis)
  │      └── Notification / Sync Service
  │
  └── Blob Storage (S3 / GFS‑like)  ← CDN
         └── Background Workers
```

**Key principle:** API servers never carry large files.

---

## 3. Core Data Model (Metadata)

### File

```
file_id
owner_id
parent_folder_id
current_version
permissions
size
checksum
state (UPLOADING | AVAILABLE | FAILED)
created_at
updated_at
```

### FileVersion

```
file_id
version
blob_pointer
checksum
created_at
```

**DB choice:**

* Document DB for folder trees & per‑user metadata
* Strong consistency required for metadata updates

---

## 4. Upload Path (Correct & Scalable)

### Why two requests are correct

* Metadata ≠ file data
* Large files must bypass API servers

### Upload Flow

1. **Client → API Server**

   * `POST /files/create`
   * Authenticate user
   * Create metadata entry with state = `UPLOADING`
   * Generate **pre‑signed multipart upload URLs**

2. **Client → Blob Storage (Direct)**

   * Upload file in chunks
   * Supports resume on failure

3. **Upload Completion Event**

   * Blob storage or client notifies backend

4. **Background Workers**

   * Validate checksum
   * Virus scan (optional)
   * Encryption verification

5. **Metadata Commit Service**

   * Atomically update metadata
   * Mark file as `AVAILABLE`

**Invariant:**

> A file is never visible until the blob is durable.

---

## 5. Download Path (Optimized)

### Download Flow

1. **Client → API Server**

   * Request file metadata
   * Permission check

2. **API → Client**

   * Return signed download URL

3. **Client → CDN / Blob Storage**

   * Stream file directly

**Important:**

* Workers are NOT on the hot download path
* Decryption handled transparently at storage/edge

---

## 6. Notification & Sync System

### Problem

Keep multiple devices in sync efficiently.

### Solution: Delta‑based sync

* Client tracks `last_seen_version`
* API exposes:

```
GET /changes?since=version
```

### Implementation

* Metadata changes emit events
* Notification service:

  * Push (WebSockets) OR
  * Pull (long polling / periodic sync)

**Key idea:**

> Sync deltas, not full folder trees.

---

## 7. Metadata Update Responsibility (Clarified)

* Upload workers do NOT update metadata
* Download workers do NOT update metadata

**Dedicated Metadata Commit Service**:

* Consumes completion events
* Idempotent updates
* Retry‑safe

---

## 8. Background Workers (Responsibilities)

Workers handle:

* Validation & scanning
* Compression (optional)
* Cold‑storage migration
* Garbage collection

Workers do NOT:

* Serve downloads
* Perform permission checks

---

## 9. Cold Storage Strategy

* Files not accessed for N months → cold storage
* Metadata updated with `storage_tier`
* On access:

  * Restore to hot tier
  * Inform user of delay

**Benefit:** Major cost reduction at scale.

---

## 10. Consistency Model

| Component         | Consistency |
| ----------------- | ----------- |
| Metadata          | Strong      |
| File blobs        | Eventual    |
| Cross‑device sync | Eventual    |
| Permissions       | Strong      |

> Metadata correctness > immediate blob visibility.

---

## 11. Failure Handling

* Upload fails → metadata remains `UPLOADING`
* Client crash → resumable uploads
* Worker failure → idempotent retries

**Rule:** Every step is retry‑safe.

---

## 12. How to Explain This in an Interview (Cheat Code)

**Say this first:**

> I separate metadata from blob storage.

Then walk through:

1. Upload path
2. Download path
3. Sync & notification
4. Scaling & consistency

You do NOT need to explain everything unless asked.

---

## One‑Line Summary (Memorize This)

> Google Drive is a strongly consistent metadata system with asynchronously managed blob storage underneath.
