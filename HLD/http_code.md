| Category | Code | Name | SDE-2 System Design Context | Technical Behavior & Headers (What happens next?) |
| :--- | :--- | :--- | :--- | :--- |
| **2xx** | **200** | OK | Standard for successful GET, PUT, or PATCH. | **Parse Body:** The client reads the payload (JSON/HTML) immediately and assumes the operation is complete. |
| | **201** | Created | POST success. Use this when a resource is written to the DB. | **Look for `Location`:** The client expects a `Location` header containing the URL of the newly created resource. |
| | **202** | Accepted | Async/Queue. The request is in the queue; the job isn't done yet. | **Start Polling:** The client receives a "Status Monitor URL" in the body to check back later. |
| | **204** | No Content | DELETE success. Request worked, but there's no body to return. | **Stay Put:** The browser does not navigate away or refresh; used for background "autosave" or deletions. |
| **3xx** | **301** | Moved Perm. | Permanent Redirection. Best for SEO and link shortening. | **Hard Cache:** The browser remembers this redirect forever and won't ask the old URL again. |
| | **302** | Found (Temp) | Temporary Redirection. Use if you need to track every click. | **Follow `Location`:** The browser jumps to the new URL but will still check the old one next time. |
| | **304** | Not Modified | Caching. "Your browser's copy is still good; don't re-download." | **Load from Disk:** The server sends no body; the browser uses its local cached version (via `ETag`). |
| | **307 / 308**| Temp / Perm | Same as 301/302, but guarantees the HTTP method won't change. | **Preserve Method:** Ensures a POST request stays a POST request after the redirect. |
| **4xx** | **400** | Bad Request | Client-side validation failure (e.g., bad JSON format). | **Do Not Retry:** The client must fix the data before sending it again. |
| | **401** | Unauthorized | Identity. "I don't know who you are (No Login)." | **Re-Authenticate:** The app should redirect the user to the Login screen or prompt for credentials. |
| | **403** | Forbidden | Permission. "I know you, but you can't touch this file." | **Show Error:** Logging in won't help; the client must show an "Access Denied" message. |
| | **404** | Not Found | Resource ID does not exist in the database. | **Handle Null:** Confirms the URL or ID is invalid; the client displays a "Not Found" page. |
| | **409** | Conflict | Concurrency. Two people edited the same row at once. | **Resolve & Retry:** The client must fetch the latest data, resolve the clash, and then try the write again. |
| | **429** | Too Many Req. | Rate Limiting. The user hit the API too fast. | **Wait:** The client looks for a `Retry-After` header to know how many seconds to wait before trying again. |
| **5xx** | **500** | Internal Error | Server-side crash or unhandled code exception. | **Generic Failure:** The client displays a "Something went wrong" message. |
| | **502** | Bad Gateway | Infra. Load Balancer is up, but the Service behind it is dead. | **Check Infra:** Indicates a communication failure between backend servers/proxies. |
| | **503** | Service Unavail.| Server is down for maintenance or is overloaded. | **Backoff:** The client should use "exponential backoff" (wait 1s, 2s, 4s...) to retry. |
| | **504** | Gateway Timeout| Latency. The Service took too long to answer the Load Balancer. | **Risky Retry:** The request might have actually processed; retrying a payment here is dangerous. |
