package dev.buildtrace.generation;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ShowcaseProject {

    public Map<String, String> files() {
        return Map.ofEntries(
            Map.entry("/package.json", """
                {"scripts":{"dev":"vite","build":"vite build","preview":"vite preview"},"dependencies":{"lucide-react":"^0.468.0","react":"^18.2.0","react-dom":"^18.2.0"},"devDependencies":{"@vitejs/plugin-react":"3.1.0","vite":"4.1.4","esbuild-wasm":"0.17.12"}}
                """.trim()),
            Map.entry("/index.html", """
                <!doctype html><html lang="zh-CN"><head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/><title>LaunchBoard</title></head><body><div id="root"></div><script type="module" src="/index.jsx"></script></body></html>
                """.trim()),
            Map.entry("/index.jsx", """
                import React from 'react';
                import { createRoot } from 'react-dom/client';
                import App from './App';
                import './styles.css';
                createRoot(document.getElementById('root')).render(<App />);
                """.trim()),
            Map.entry("/vite.config.js", """
                import { defineConfig } from 'vite';
                import react from '@vitejs/plugin-react';
                export default defineConfig({ plugins: [react()] });
                """.trim()),
            Map.entry("/data.js", """
                export const stages = [
                  { id: 'screening', label: '简历筛选', tone: 'blue' },
                  { id: 'interview', label: '面试中', tone: 'amber' },
                  { id: 'offer', label: 'Offer', tone: 'green' },
                ];

                export const seedCandidates = [
                  { id: 'c1', name: '林知夏', role: 'Senior Product Designer', stage: 'screening', score: 92, source: 'Referral', updated: '今天 10:24', tags: ['B2B', 'Design System'] },
                  { id: 'c2', name: '周叙', role: 'Frontend Engineer', stage: 'screening', score: 88, source: 'LinkedIn', updated: '昨天 18:40', tags: ['React', 'TypeScript'] },
                  { id: 'c3', name: '陈予安', role: 'AI Product Manager', stage: 'interview', score: 95, source: 'Career site', updated: '今天 09:12', tags: ['Agent', 'Growth'] },
                  { id: 'c4', name: '宋闻', role: 'Backend Engineer', stage: 'interview', score: 90, source: 'Referral', updated: '周二 16:08', tags: ['Java', 'Platform'] },
                  { id: 'c5', name: '许栩', role: 'Data Analyst', stage: 'offer', score: 93, source: 'LinkedIn', updated: '周一 14:32', tags: ['SQL', 'BI'] },
                ];
                """.trim()),
            Map.entry("/hooks/usePersistentState.js", """
                import { useEffect, useState } from 'react';

                export function usePersistentState(key, initialValue) {
                  const [value, setValue] = useState(() => {
                    try {
                      const saved = localStorage.getItem(key);
                      return saved ? JSON.parse(saved) : initialValue;
                    } catch {
                      return initialValue;
                    }
                  });
                  useEffect(() => { localStorage.setItem(key, JSON.stringify(value)); }, [key, value]);
                  return [value, setValue];
                }
                """.trim()),
            Map.entry("/components/CandidateCard.jsx", """
                import React from 'react';
                import { ArrowRight, BriefcaseBusiness, Clock3, MoreHorizontal, Trash2 } from 'lucide-react';

                export default function CandidateCard({ candidate, canAdvance, onAdvance, onRemove }) {
                  const initials = candidate.name.slice(0, 2);
                  return <article className="candidate-card">
                    <div className="candidate-top">
                      <span className="avatar" aria-hidden="true">{initials}</span>
                      <div className="candidate-identity"><strong>{candidate.name}</strong><span><BriefcaseBusiness size={13}/>{candidate.role}</span></div>
                      <button className="icon-button quiet" aria-label={`更多 ${candidate.name}`}><MoreHorizontal size={17}/></button>
                    </div>
                    <div className="tag-row">{candidate.tags.map(tag => <span key={tag}>{tag}</span>)}</div>
                    <div className="candidate-meta"><span className="score">{candidate.score}<small>/100</small></span><span>{candidate.source}</span></div>
                    <div className="candidate-footer"><span><Clock3 size={13}/>{candidate.updated}</span><div>
                      <button className="icon-button danger" onClick={() => onRemove(candidate.id)} aria-label={`删除 ${candidate.name}`}><Trash2 size={14}/></button>
                      {canAdvance && <button className="advance" onClick={() => onAdvance(candidate.id)}>推进<ArrowRight size={14}/></button>}
                    </div></div>
                  </article>;
                }
                """.trim()),
            Map.entry("/App.jsx", """
                import React, { useMemo, useState } from 'react';
                import { Bell, BriefcaseBusiness, ChevronDown, Moon, Plus, Search, Sparkles, Sun, UsersRound, X } from 'lucide-react';
                import CandidateCard from './components/CandidateCard';
                import { seedCandidates, stages } from './data';
                import { usePersistentState } from './hooks/usePersistentState';

                const blankCandidate = { name: '', role: '', score: 85, source: 'Career site', tags: ['New'] };

                export default function App() {
                  const [candidates, setCandidates] = usePersistentState('launchboard.candidates', seedCandidates);
                  const [dark, setDark] = usePersistentState('launchboard.dark', false);
                  const [query, setQuery] = useState('');
                  const [stageFilter, setStageFilter] = useState('all');
                  const [showAdd, setShowAdd] = useState(false);
                  const [draft, setDraft] = useState(blankCandidate);

                  const visible = useMemo(() => candidates.filter(candidate => {
                    const matchesQuery = `${candidate.name} ${candidate.role} ${candidate.tags.join(' ')}`.toLowerCase().includes(query.toLowerCase());
                    return matchesQuery && (stageFilter === 'all' || candidate.stage === stageFilter);
                  }), [candidates, query, stageFilter]);

                  const advance = id => setCandidates(items => items.map(candidate => {
                    if (candidate.id !== id) return candidate;
                    const index = stages.findIndex(stage => stage.id === candidate.stage);
                    return { ...candidate, stage: stages[Math.min(index + 1, stages.length - 1)].id, updated: '刚刚' };
                  }));
                  const remove = id => setCandidates(items => items.filter(candidate => candidate.id !== id));
                  const add = event => {
                    event.preventDefault();
                    if (!draft.name.trim() || !draft.role.trim()) return;
                    setCandidates(items => [{ ...draft, id: crypto.randomUUID(), name: draft.name.trim(), role: draft.role.trim(), stage: 'screening', updated: '刚刚' }, ...items]);
                    setDraft(blankCandidate);
                    setShowAdd(false);
                  };

                  const average = candidates.length ? Math.round(candidates.reduce((sum, item) => sum + Number(item.score), 0) / candidates.length) : 0;

                  return <div className="product" data-theme={dark ? 'dark' : 'light'}>
                    <aside className="sidebar">
                      <div className="brand"><span><Sparkles size={17}/></span><strong>LaunchBoard</strong></div>
                      <nav><button className="active"><UsersRound size={17}/>候选人</button><button><BriefcaseBusiness size={17}/>职位</button></nav>
                      <div className="sidebar-note"><span>本周目标</span><strong>完成 8 场面试</strong><div><i style={{width:'62%'}}/></div><small>5 / 8 已完成</small></div>
                    </aside>

                    <main className="main">
                      <header className="topbar"><div><p>人才招聘</p><h1>候选人看板</h1></div><div className="top-actions"><button className="icon-button" onClick={() => setDark(!dark)} aria-label="切换主题">{dark ? <Sun size={17}/> : <Moon size={17}/>}</button><button className="icon-button" aria-label="通知"><Bell size={17}/></button><button className="primary" onClick={() => setShowAdd(true)}><Plus size={16}/>添加候选人</button></div></header>

                      <section className="metrics">
                        <article><span>活跃候选人</span><strong>{candidates.length}</strong><small className="positive">较上周 +12%</small></article>
                        <article><span>平均匹配度</span><strong>{average}<em>%</em></strong><small>基于岗位要求</small></article>
                        <article><span>进入 Offer</span><strong>{candidates.filter(item => item.stage === 'offer').length}</strong><small className="positive">转化率 20%</small></article>
                      </section>

                      <section className="filters"><label><Search size={16}/><input value={query} onChange={event => setQuery(event.target.value)} placeholder="搜索姓名、岗位或技能"/></label><div className="select-wrap"><select value={stageFilter} onChange={event => setStageFilter(event.target.value)} aria-label="筛选阶段"><option value="all">全部阶段</option>{stages.map(stage => <option value={stage.id} key={stage.id}>{stage.label}</option>)}</select><ChevronDown size={15}/></div></section>

                      {visible.length === 0 ? <section className="empty"><Search size={24}/><strong>没有匹配的候选人</strong><p>调整搜索或阶段筛选条件。</p><button onClick={() => {setQuery('');setStageFilter('all')}}>清除筛选</button></section> : <section className="board">{stages.map((stage, index) => {
                        const items = visible.filter(candidate => candidate.stage === stage.id);
                        if (stageFilter !== 'all' && stageFilter !== stage.id) return null;
                        return <div className="column" key={stage.id}><header><span className={`stage-dot ${stage.tone}`}/><strong>{stage.label}</strong><small>{items.length}</small></header><div className="cards">{items.map(candidate => <CandidateCard key={candidate.id} candidate={candidate} canAdvance={index < stages.length - 1} onAdvance={advance} onRemove={remove}/>)}</div></div>;
                      })}</section>}
                    </main>

                    {showAdd && <div className="modal-backdrop" onMouseDown={() => setShowAdd(false)}><form className="modal" onSubmit={add} onMouseDown={event => event.stopPropagation()}><header><div><span>人才库</span><h2>添加候选人</h2></div><button type="button" className="icon-button" onClick={() => setShowAdd(false)} aria-label="关闭"><X size={17}/></button></header><label>姓名<input autoFocus value={draft.name} onChange={event => setDraft({...draft,name:event.target.value})} placeholder="例如：方予" required/></label><label>应聘岗位<input value={draft.role} onChange={event => setDraft({...draft,role:event.target.value})} placeholder="例如：Product Engineer" required/></label><label>初始匹配度<input type="number" min="50" max="100" value={draft.score} onChange={event => setDraft({...draft,score:Number(event.target.value)})}/></label><footer><button type="button" onClick={() => setShowAdd(false)}>取消</button><button className="primary" type="submit">添加到筛选阶段</button></footer></form></div>}
                  </div>;
                }
                """.trim()),
            Map.entry("/styles.css", """
                :root{font-family:Inter,ui-sans-serif,system-ui,-apple-system,sans-serif;color:#202329;background:#f4f5f6;font-synthesis:none}*{box-sizing:border-box}body{margin:0}button,input,select{font:inherit}.product{--bg:#f5f6f7;--panel:#fff;--ink:#202329;--muted:#727780;--line:#e1e4e7;--soft:#f0f2f3;--accent:#225d4b;--accent-ink:#fff;min-height:100vh;background:var(--bg);color:var(--ink);display:grid;grid-template-columns:190px minmax(0,1fr)}.product[data-theme=dark]{--bg:#17191b;--panel:#212427;--ink:#f3f5f4;--muted:#a3aaa6;--line:#34383a;--soft:#2b2f31;--accent:#87d4b1;--accent-ink:#152019}.sidebar{background:#191c1d;color:#f2f4f3;padding:22px 16px;display:flex;flex-direction:column;min-height:100vh}.brand{display:flex;align-items:center;gap:9px;padding:0 6px 24px}.brand>span{width:28px;height:28px;display:grid;place-items:center;background:#d8ff72;color:#20251d;border-radius:6px}.brand strong{font-size:15px}.sidebar nav{display:grid;gap:5px}.sidebar nav button{display:flex;gap:9px;align-items:center;border:0;background:transparent;color:#aeb5b2;padding:10px;border-radius:6px;text-align:left}.sidebar nav button.active{background:#2a2e2f;color:#fff}.sidebar-note{margin-top:auto;border-top:1px solid #303435;padding:18px 6px 0;display:grid;gap:6px}.sidebar-note span,.sidebar-note small{font-size:11px;color:#9ba29f}.sidebar-note strong{font-size:12px}.sidebar-note div{height:4px;background:#34393a;border-radius:4px;overflow:hidden}.sidebar-note i{display:block;height:100%;background:#d8ff72}.main{padding:28px 30px;min-width:0}.topbar{display:flex;align-items:center;justify-content:space-between;gap:16px}.topbar p{margin:0 0 4px;color:var(--muted);font-size:11px;font-weight:700;text-transform:uppercase}.topbar h1{font-size:26px;line-height:1.2;margin:0;letter-spacing:0}.top-actions{display:flex;gap:8px;align-items:center}.icon-button{width:36px;height:36px;display:inline-grid;place-items:center;border:1px solid var(--line);border-radius:6px;background:var(--panel);color:var(--ink)}.icon-button.quiet{border:0;background:transparent;color:var(--muted)}.icon-button.danger{width:29px;height:29px;color:#b64a4a;background:transparent}.primary{display:inline-flex;align-items:center;justify-content:center;gap:7px;border:0;border-radius:6px;background:var(--accent);color:var(--accent-ink);font-weight:700;padding:10px 14px}.metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin:24px 0 16px}.metrics article{background:var(--panel);border:1px solid var(--line);border-radius:7px;padding:16px;display:grid;grid-template-columns:1fr auto;gap:5px}.metrics span{font-size:12px;color:var(--muted)}.metrics strong{grid-row:2;font-size:25px}.metrics strong em{font-size:13px;font-style:normal;color:var(--muted)}.metrics small{grid-row:2;align-self:end;text-align:right;color:var(--muted);font-size:10px}.metrics small.positive{color:#27805c}.filters{display:flex;justify-content:space-between;gap:10px;margin-bottom:16px}.filters label{height:38px;width:min(360px,60%);display:flex;align-items:center;gap:8px;border:1px solid var(--line);background:var(--panel);border-radius:6px;padding:0 11px;color:var(--muted)}.filters input{width:100%;border:0;outline:0;background:transparent;color:var(--ink)}.select-wrap{position:relative}.select-wrap select{height:38px;appearance:none;border:1px solid var(--line);background:var(--panel);color:var(--ink);border-radius:6px;padding:0 34px 0 12px}.select-wrap svg{position:absolute;right:10px;top:12px;pointer-events:none}.board{display:grid;grid-template-columns:repeat(3,minmax(180px,1fr));gap:12px;align-items:start;overflow-x:auto}.column{min-width:0}.column>header{display:flex;align-items:center;gap:7px;padding:8px 3px 10px}.column>header strong{font-size:12px}.column>header small{margin-left:auto;background:var(--soft);color:var(--muted);padding:2px 7px;border-radius:10px}.stage-dot{width:7px;height:7px;border-radius:50%}.stage-dot.blue{background:#528fe0}.stage-dot.amber{background:#d99a36}.stage-dot.green{background:#48a379}.cards{display:grid;gap:9px}.candidate-card{background:var(--panel);border:1px solid var(--line);border-radius:7px;padding:13px}.candidate-top{display:flex;align-items:center;gap:9px}.avatar{width:34px;height:34px;display:grid;place-items:center;border-radius:7px;background:#e4eee9;color:#255b49;font-size:11px;font-weight:800}.candidate-identity{min-width:0;display:grid;gap:3px}.candidate-identity strong{font-size:13px}.candidate-identity span{display:flex;align-items:center;gap:4px;color:var(--muted);font-size:10px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.candidate-top>.icon-button{margin-left:auto}.tag-row{display:flex;flex-wrap:wrap;gap:5px;margin:12px 0}.tag-row span{font-size:9px;background:var(--soft);color:var(--muted);padding:4px 6px;border-radius:4px}.candidate-meta{display:flex;align-items:center;justify-content:space-between;border-top:1px solid var(--line);padding-top:10px;color:var(--muted);font-size:10px}.candidate-meta .score{font-size:16px;color:var(--ink);font-weight:800}.score small{font-size:9px;color:var(--muted)}.candidate-footer{display:flex;align-items:center;justify-content:space-between;margin-top:10px}.candidate-footer>span{display:flex;align-items:center;gap:4px;font-size:9px;color:var(--muted)}.candidate-footer>div{display:flex;align-items:center;gap:4px}.advance{display:flex;align-items:center;gap:4px;border:0;border-radius:5px;background:var(--soft);color:var(--ink);font-size:10px;padding:7px 8px}.empty{background:var(--panel);border:1px dashed var(--line);min-height:240px;display:grid;place-content:center;justify-items:center;text-align:center;border-radius:7px}.empty strong{margin-top:10px}.empty p{font-size:12px;color:var(--muted)}.empty button{border:0;background:var(--soft);color:var(--ink);padding:8px 10px;border-radius:5px}.modal-backdrop{position:fixed;inset:0;background:#0c1113a8;display:grid;place-items:center;padding:18px}.modal{width:min(420px,100%);background:var(--panel);border:1px solid var(--line);border-radius:8px;padding:20px;box-shadow:0 24px 80px #0004}.modal header{display:flex;justify-content:space-between;align-items:start;margin-bottom:18px}.modal header span{font-size:10px;text-transform:uppercase;color:var(--muted);font-weight:800}.modal h2{font-size:19px;margin:3px 0 0}.modal>label{display:grid;gap:6px;margin:11px 0;font-size:11px;font-weight:700}.modal input{border:1px solid var(--line);background:var(--bg);color:var(--ink);border-radius:6px;padding:10px}.modal footer{display:flex;justify-content:flex-end;gap:8px;margin-top:18px}.modal footer button:not(.primary){border:1px solid var(--line);background:var(--panel);color:var(--ink);border-radius:6px;padding:9px 12px}button{cursor:pointer}button:focus-visible,input:focus-visible,select:focus-visible{outline:2px solid #4a8f78;outline-offset:2px}@media(max-width:760px){.product{grid-template-columns:1fr}.sidebar{min-height:auto;padding:13px 16px;display:flex;flex-direction:row;align-items:center}.brand{padding:0}.sidebar nav{margin-left:auto;display:flex}.sidebar nav button{padding:8px}.sidebar nav button:not(.active),.sidebar-note{display:none}.main{padding:20px 14px}.topbar{align-items:flex-start}.top-actions .icon-button:nth-child(2){display:none}.primary{font-size:0;padding:10px}.primary svg{margin:0}.metrics{grid-template-columns:1fr 1fr}.metrics article:last-child{grid-column:1/-1}.filters{align-items:stretch}.filters label{width:100%}.board{grid-template-columns:repeat(3,180px);padding-bottom:8px}}
                """.trim())
        );
    }
}
