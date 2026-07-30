package dev.buildtrace.generation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FallbackGenerator {

    public GenerationResult generate(String prompt) {
        String safePrompt = prompt.replace("<", "&lt;").replace(">", "&gt;");
        String app = """
            import React, { useState } from 'react';
            export default function App() {
              const [items, setItems] = useState(['Review requirements', 'Ship a working preview']);
              const [value, setValue] = useState('');
              const add = () => { if (value.trim()) { setItems([...items, value.trim()]); setValue(''); } };
              return <main className="app"><p className="eyebrow">LOCAL FALLBACK</p><h1>%s</h1>
                <section><div className="row"><input value={value} onChange={e => setValue(e.target.value)} placeholder="Add an item"/><button onClick={add}>Add</button></div>
                <ul>{items.map((item, index) => <li key={item + index}><span>{item}</span><button onClick={() => setItems(items.filter((_, i) => i !== index))}>Remove</button></li>)}</ul></section>
              </main>;
            }
            """.formatted(safePrompt);
        String styles = """
            *{box-sizing:border-box}body{margin:0;background:#f4f5f7;color:#202328;font-family:Inter,system-ui,sans-serif}.app{width:min(720px,calc(100% - 32px));margin:48px auto}.eyebrow{font-size:11px;font-weight:800;color:#2563eb}h1{font-size:34px;margin:8px 0 24px}section{background:white;border:1px solid #dfe2e7;border-radius:8px;padding:20px}.row{display:flex;gap:8px}input{flex:1;padding:10px;border:1px solid #cbd0d8;border-radius:5px}button{border:0;border-radius:5px;background:#202328;color:white;padding:9px 12px}ul{list-style:none;padding:0;margin:18px 0 0}li{display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-top:1px solid #eceef1}li button{background:#eef0f3;color:#4b515a}
            """;
        return new GenerationResult(
            "创建一个本地可运行、支持增删操作的任务清单。",
            List.of("建立可编辑任务状态", "实现添加与删除交互", "补齐响应式产品样式"),
            "已使用本地 fallback 创建可交互的多文件 React 示例。",
            List.of(
                new GenerationResult.FileOperation("write", "/App.jsx", app),
                new GenerationResult.FileOperation("write", "/styles.css", styles)
            ),
            List.of("添加任务有效", "删除任务有效", "窄屏布局可用")
        );
    }
}
