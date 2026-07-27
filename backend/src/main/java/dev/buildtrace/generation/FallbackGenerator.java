package dev.buildtrace.generation;

import org.springframework.stereotype.Component;

@Component
public class FallbackGenerator {

    public String generate(String prompt) {
        String title = escape(prompt.length() > 70 ? prompt.substring(0, 70) + "..." : prompt);
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>智能待办</title>
              <style>
                * { box-sizing: border-box; }
                body { margin: 0; min-height: 100vh; font-family: Inter, system-ui, sans-serif; background: #f4f5f7; color: #17191c; display: grid; place-items: center; }
                main { width: min(680px, calc(100%% - 32px)); background: white; border: 1px solid #dde0e5; border-radius: 8px; padding: 28px; box-shadow: 0 12px 36px rgba(26,31,38,.08); }
                .eyebrow { color: #2563eb; font-weight: 700; font-size: 12px; text-transform: uppercase; }
                h1 { margin: 8px 0 6px; font-size: 30px; }
                .brief { color: #656b76; margin: 0 0 22px; line-height: 1.5; }
                form { display: flex; gap: 10px; }
                input { flex: 1; border: 1px solid #cfd4dc; border-radius: 6px; padding: 12px 14px; font: inherit; }
                button { border: 0; border-radius: 6px; padding: 11px 16px; font: inherit; font-weight: 700; cursor: pointer; }
                form button { background: #17191c; color: white; }
                ul { list-style: none; padding: 0; margin: 22px 0 0; display: grid; gap: 8px; }
                li { display: flex; align-items: center; gap: 10px; border: 1px solid #e2e5e9; border-radius: 6px; padding: 12px; }
                li span { flex: 1; }
                li.done span { color: #8a9099; text-decoration: line-through; }
                li button { background: #f0f1f3; color: #5f6570; padding: 7px 10px; }
                .count { margin-top: 18px; color: #656b76; font-size: 13px; }
              </style>
            </head>
            <body>
              <main>
                <div class="eyebrow">BuildTrace offline fallback</div>
                <h1>智能待办</h1>
                <p class="brief">根据需求“%s”生成。添加任务后可完成或删除，所有控件都是真实可交互的。</p>
                <form id="form"><input id="task" placeholder="输入任务，按回车添加" /><button>添加</button></form>
                <ul id="list"></ul>
                <div class="count" id="count">0 个待办</div>
              </main>
              <script>
                const form = document.querySelector('#form');
                const input = document.querySelector('#task');
                const list = document.querySelector('#list');
                const count = document.querySelector('#count');
                let tasks = [];
                const render = () => {
                  list.innerHTML = tasks.map((task, index) => `<li class="${task.done ? 'done' : ''}"><input type="checkbox" ${task.done ? 'checked' : ''} data-toggle="${index}"/><span>${task.text}</span><button data-delete="${index}">删除</button></li>`).join('');
                  count.textContent = `${tasks.filter(task => !task.done).length} 个待办`;
                };
                form.addEventListener('submit', event => { event.preventDefault(); const text = input.value.trim(); if (!text) return; tasks.unshift({ text, done: false }); input.value = ''; render(); });
                list.addEventListener('click', event => { const toggle = event.target.dataset.toggle; const remove = event.target.dataset.delete; if (toggle !== undefined) tasks[toggle].done = !tasks[toggle].done; if (remove !== undefined) tasks.splice(remove, 1); render(); });
                render();
              </script>
            </body>
            </html>
            """.formatted(title);
    }

    private String escape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
