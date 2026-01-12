# HTTP Protocol Flow — SDE-2 Interview Notes

This document explains how HTTP works over TCP, including connection setup, sequence numbers, acknowledgements, retries, flow control, congestion control, and a comparison with UDP.
The depth is intentionally tuned for SDE-2 / SysDev-2 interviews.

---
# Application Layer vs Transport Layer (HTTP Focus) — SDE-2 Notes

---

## Big Picture

Application Layer and Transport Layer solve **different problems**.

- **Application Layer** → *What is being said?*  
- **Transport Layer (Layer 4)** → *How is it delivered?*

This separation is intentional and fundamental to networking.

---

## Application Layer Protocol (Layer 7)

### What the Application Layer Does

The application layer defines:

- Message format
- Request / response semantics
- Commands and their meanings
- Application-level behavior

It answers the question:

> **“What does this data mean to the application?”**

---

### Examples of Application Layer Protocols

- HTTP / HTTPS
- DNS
- FTP
- SMTP
- Kafka protocol (Kafka has its own application-layer protocol)

---

### HTTP Example



## 1. Protocol Stack Overview

HTTP is an application-layer protocol.
It does NOT handle reliability, ordering, or retries on its own.

HTTP relies on TCP for:
- Reliable delivery
- Ordered delivery
- Retransmissions
- Flow control
- Congestion control

Protocol layering:

HTTP  
└── TCP  
    └── IP  
        └── Network  

---

## 2. TCP Connection Setup (3-Way Handshake)

Before any HTTP data is exchanged, TCP establishes a connection.

Client → Server: SYN (seq = x)  
Server → Client: SYN + ACK (seq = y, ack = x + 1)  
Client → Server: ACK (ack = y + 1)

This handshake:
- Synchronizes sequence numbers
- Confirms both sides are ready
- Establishes a reliable connection

Only after this does HTTP begin.

---

## 3. TCP Sequence Numbers (Core Concept)

TCP sequence numbers represent byte positions, not packet numbers.

Example:
Data = "HELLO" (5 bytes)

Segment 1: seq = 1000, data = "HEL"  
Segment 2: seq = 1003, data = "LO"  

Receiver sends:
ACK = 1005

Meaning:
"I have successfully received all bytes up to 1004."

This mechanism guarantees:
- Correct ordering
- Detection of missing data
- No duplication

---

## 4. HTTP Request / Response Flow

Once TCP is established, HTTP messages flow as raw bytes.

Client sends:
GET /index.html  
Host: example.com  

Server responds:
HTTP/1.1 200 OK  
<response body>

Important:
- HTTP does not manage packet loss
- HTTP does not manage ordering
- TCP transparently handles all of that

---

## 5. Reliability: ACKs and Retransmissions

TCP ensures reliability using acknowledgements.

If data is lost:
- Receiver does not ACK missing bytes
- Sender waits for timeout
- Sender retransmits missing data

Example:
Client sends seq=1005 → packet lost  
Server ACKs only up to 1004  
Client retransmits seq=1005  

HTTP never knows this happened.

---

## 6. Flow Control (High-Level Only)

TCP prevents overwhelming the receiver using a sliding window.

Concept:
- Receiver advertises how much data it can handle
- Sender limits in-flight data accordingly

This protects slow receivers from buffer overflow.

Interview note:
Knowing that flow control exists and why it exists is enough.
Exact window size calculations are NOT required for SDE-2.

---

## 7. Congestion Control (High-Level Only)

TCP also protects the network itself.

High-level behavior:
- Start sending slowly
- Increase rate when network is healthy
- Reduce rate when packet loss occurs

This allows TCP to adapt automatically to changing network conditions.

Interview note:
You only need conceptual understanding, not algorithmic details (Reno, Cubic, BBR).

---

## 8. TCP Retries vs HTTP Retries

TCP Retries:
- Automatic
- Handle packet loss, duplication, reordering
- Transparent to HTTP

HTTP Retries:
- Application-level decision
- Safe for idempotent requests (GET)
- Careful or avoided for non-idempotent requests (POST)

TCP guarantees delivery of bytes.
HTTP decides whether a request should be retried logically.

---

## 9. Connection Termination (Graceful Close)

TCP uses a 4-step close to ensure no data loss.

Client → Server: FIN  
Server → Client: ACK  
Server → Client: FIN  
Client → Server: ACK  

This ensures all data is fully transmitted before closure.

---

## 10. Why HTTP Uses TCP

HTTP requires:
- Reliable delivery
- Ordered delivery
- No data corruption

Losing or reordering bytes in HTML or JSON is unacceptable.

Therefore:
HTTP chooses TCP because correctness is more important than raw speed.

---

## 11. UDP Overview

UDP is connectionless and best-effort.

UDP provides:
- No handshake
- No ACKs
- No retries
- No ordering
- No flow control
- No congestion control

Client sends data without knowing if it arrived.

---

## 12. TCP vs UDP Comparison

Feature            | TCP (HTTP)            | UDP
------------------|-----------------------|---------------------
Connection         | Yes (handshake)       | No
Reliability        | Guaranteed            | No
Ordering           | Guaranteed            | No
Retransmissions    | Automatic             | No
Flow control       | Yes                   | No
Congestion control | Yes                   | No
Latency            | Higher                | Lower
Typical use cases  | HTTP, HTTPS, DBs      | DNS, video, gaming

---

## 13. Why Some Protocols Choose UDP

Protocols like:
- DNS
- Video streaming
- VoIP
- Gaming
- QUIC (HTTP/3)

Prefer UDP because:
- Low latency matters more than reliability
- Occasional packet loss is acceptable
- Applications can handle retries selectively

---

## 14. Do I Need to Know TCP Window Size in Detail?

For SDE-2 / SysDev-2 interviews:
NO.

Expected level:
- Know that sliding window exists
- Know it prevents receiver overload
- Know it limits in-flight data

NOT expected:
- Window size math
- TCP state machines
- Congestion algorithm internals

---

## 15. Interview-Perfect Closing Explanation

"HTTP relies on TCP for reliable, ordered delivery. TCP uses sequence numbers, acknowledgements, retransmissions, flow control, and congestion control to guarantee correctness. HTTP itself is unaware of packet loss. UDP trades reliability for lower latency, which is why it is used in real-time systems."

This level of understanding is sufficient and correct for SDE-2 interviews.



