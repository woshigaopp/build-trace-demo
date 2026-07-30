# Frontend Mode Leakage Negative Matrix

| Surface/action | Mode/state | Forbidden DOM/text | Forbidden payload fields | Forbidden route/API | Assertion method | Owner issue |
|---|---|---|---|---|---|---|
| All project requests | authenticated | guest identity controls | `guestId`, `currentHtml`, password | `X-Guest-Id` header | network/browser assertion | T-FE-AUTH |
| Code workspace | React snapshot | single HTML-only label or truncated source | `html` | legacy HTML save endpoint | DOM/network assertion | T-FE-CODE |
| Generated scope | React snapshot | backend deployment/publish claims | arbitrary commands | publish/deploy API | DOM absence assertion | T-FE-CODE |

There is no alternate runtime mode. This matrix prevents leakage from the superseded v1 guest/single-HTML mode.
