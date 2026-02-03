# 🌐 SysDev II: Networking & Troubleshooting Battle Map

## 1. The "Golden Commands" Cheat Sheet
*Use these to isolate if a problem is Process, Local Network, or Remote Routing.*

| Command | Why it’s the L5 Choice | Common Flag / Usage |
| :--- | :--- | :--- |
| **`ss -tunlp`** | Faster and more detailed than `netstat`. | `-p` to see the **PID** owning the socket. |
| **`ip route get <IP>`** | Shows exactly which interface and gateway the kernel will use. | `ip route get 10.144.35.116` |
| **`nc -zv <IP> <Port>`** | Quickest way to check reachability. | Success = Port Open; Timeout = Firewall Drop. |
| **`tcpdump`** | The "Truth" on the wire. | `tcpdump -i any host <IP> and port <Port> -nn` |
| **`arp -an`** | Checks Layer 2 resolution. | If `<incomplete>`, the host is physically gone or down. |
| **`traceroute -n -T`**| Find where the packet dies. | `-T` uses **TCP SYN** to bypass ICMP-blocking firewalls. |
| **`strace -p <PID>`** | See if a process is stuck on a `read()`, `write()`, or `connect()`. | `strace -e network -p 1234` |
| **`lsof -i :<Port>`** | Identifies the process blocking a specific port. | `lsof -i :6443` |

---

## 2. Troubleshooting: Failure Code Interpretation
*When a command fails, the error message tells you where to look.*

### **A. "Connection Refused"**
* **Meaning:** The packet reached the host, but nothing is listening.
* **The Culprit:** Process is crashed, not started, or bound to the wrong IP (e.g., `127.0.0.1` vs `0.0.0.0`).
* **Next Move:** `ps aux` and `ss -tunlp` on the destination node.

### **B. "Connection Timeout"**
* **Meaning:** The packet was sent, but no response (not even a rejection) came back.
* **The Culprit:** Usually a **Firewall** (Security Group, `iptables`, `nftables`) silently dropping packets.
* **Next Move:** Check Cloud Security Groups and local `iptables -L -n`.

### **C. "No Route to Host" / `!H`**
* **Meaning:** The network stack doesn't know how to deliver the packet.
* **The Culprit:** 1. Routing table is missing the destination subnet.
  2. **ARP Failure:** The gateway for that subnet cannot find the MAC address of the host.
* **Next Move:** `ip route` and `arping`.

---

## 3. The SysDev "Deep Dive" Flowchart
*If an interviewer says "The API is down," follow these layers:*

1.  **Process Layer:** Is the binary running? (`ps`, `systemctl`)
2.  **Socket Layer:** Is it bound to the correct port and **External IP**? (`ss -tunlp`)
3.  **Local Firewall:** Is the host's own firewall blocking the port? (`iptables`)
4.  **Network Path:** Is there a route? (`ip route`, `traceroute`)
5.  **Layer 2:** Can the gateway see the hardware? (`arp -an`)
6.  **The Wire:** Use `tcpdump`. If you see **SYN** going out but no **SYN-ACK** coming back, it’s a drop in the middle.

---

## 4. Key "L5" Scenarios to Master

### **Scenario: The "Silent Drop"**
* **Symptoms:** `ping` works, but `nc` or `curl` times out.
* **Explanation:** High-level firewalls often allow ICMP (ping) for debugging but block TCP on specific ports (6443/80/443).
* **Fix:** Verify Security Group rules for the specific TCP port.

### **Scenario: Port Exhaustion**
* **Symptoms:** "Cannot assign requested address" or "Connection refused" under high load.
* **Explanation:** The system has too many sockets in `TIME_WAIT` and ran out of ephemeral ports.
* **Fix:** Check `cat /proc/sys/net/ipv4/ip_local_port_range`. Suggest **Connection Pooling** in the application logic.

### **Scenario: Asymmetric Routing**
* **Symptoms:** `tcpdump` shows the packet arriving at the Node, but the client never gets a response.
* **Explanation:** The packet comes in via `Gateway A`, but the Node's routing table sends the response back via `Gateway B`, which the client or firewall rejects as invalid.
* **Fix:** Ensure symmetric routing or check "Reverse Path Filtering" (`rp_filter`) settings in sysctl.
