import { useState } from "react";

type Page = "config" | "apps" | "keys";

export default function App() {
  const [page, setPage] = useState<Page>("config");
  const [selectedKey, setSelectedKey] = useState(1);
  const [appTarget, setAppTarget] = useState("music");
  const [keyFunction, setKeyFunction] = useState("shortcut");
  const [volume, setVolume] = useState(65);
  const [demo, setDemo] = useState(true);

  const pageTitle = page === "config" ? "Configure seu controle" : page === "apps" ? "Aplicativos" : "Atalhos das teclas";
  const nav = [
    { id: "config" as const, icon: "⚙", label: "Configuração" },
    { id: "apps" as const, icon: "▦", label: "Aplicativos" },
    { id: "keys" as const, icon: "▤", label: "Teclas" },
  ];

  return <main className="deckra-window">
    <header className="window-bar">
      <div className="window-brand">DECKRA</div><span className="brand-divider" />
      <span className="window-caption">Prévia do software</span>
      <div className="window-actions" aria-label="Controles da janela"><span>−</span><span>□</span><span>×</span></div>
    </header>
    <div className="window-body">
      <aside className="sidebar">
        <nav>{nav.map(item => <button key={item.id} className={"nav-item "+(page===item.id?"active":"")} onClick={()=>setPage(item.id)}><span className="nav-icon">{item.icon}</span>{item.label}</button>)}</nav>
        <div className="sidebar-footer"><span>{demo ? "Interface demonstrativa" : "Modo de demonstração pausado"}</span><button onClick={()=>setDemo(!demo)} aria-label="Alternar demonstração" className={"demo-indicator "+(demo?"on":"")} /></div>
      </aside>
      <section className="main-content">
        <h1>{pageTitle}</h1>
        {page === "config" && <div className="config-grid">
          <article className="panel volume-panel">
            <div className="panel-title"><span className="purple-icon">◖))</span><h2>Volume por aplicativo</h2></div>
            <div className="panel-content">
              <label className="accent-label" htmlFor="app-target">Potenciômetro 01</label>
              <label className="field-label" htmlFor="app-target">Aplicativo</label>
              <div className="select-wrap"><span>♫</span><select id="app-target" value={appTarget} onChange={e=>setAppTarget(e.target.value)}><option value="music">Reprodutor de música</option><option value="browser">Navegador</option><option value="game">Jogo</option><option value="system">Volume do sistema</option></select><span className="chevron">⌄</span></div>
              <div className="dial-wrap">
                <svg viewBox="0 0 200 200" className="dial" role="img" aria-label={`Volume ${volume}%`}>
                  <circle cx="100" cy="100" r="76" className="dial-track" />
                  <circle cx="100" cy="100" r="76" className="dial-progress" style={{strokeDasharray:478,strokeDashoffset:478*(1-volume/100)}} />
                  <circle cx="100" cy="100" r="60" className="dial-inner" />
                  <line x1="100" y1="23" x2="100" y2="42" className="dial-pointer" transform={`rotate(${-135+volume*2.7} 100 100)`}/>
                  <text x="100" y="108" className="dial-number" textAnchor="middle">{volume}<tspan className="dial-percent">%</tspan></text>
                </svg>
              </div>
              <div className="volume-slider"><span>◖))</span><input aria-label="Volume demonstrativo" type="range" min="0" max="100" value={volume} onChange={e=>setVolume(Number(e.target.value))}/></div>
              <p className="helper-text">Gire para ajustar o volume</p>
            </div>
          </article>
          <article className="panel keys-panel">
            <div className="panel-title"><span className="purple-icon keyboard-icon">▦</span><h2>Atalhos das teclas</h2></div>
            <div className="panel-content">
              <div className="key-grid">{Array.from({length:6},(_,i)=><button key={i} className={"hardware-key "+(selectedKey===i+1?"selected":"")} onClick={()=>setSelectedKey(i+1)} aria-pressed={selectedKey===i+1}>{String(i+1).padStart(2,"0")}</button>)}</div>
              <label className="accent-label key-label">Tecla {String(selectedKey).padStart(2,"0")}</label>
              <label className="field-label" htmlFor="key-function">Função</label>
              <div className="select-wrap"><span>▤</span><select id="key-function" value={keyFunction} onChange={e=>setKeyFunction(e.target.value)}><option value="shortcut">Atalho de teclado</option><option value="media-play">Reproduzir / pausar</option><option value="mute">Silenciar</option><option value="next">Próxima faixa</option><option value="previous">Faixa anterior</option><option value="disabled">Desativada</option></select><span className="chevron">⌄</span></div>
              <label className="field-label shortcut-label">Atalho</label>
              {keyFunction==="shortcut" ? <div className="shortcut-keys"><kbd>Ctrl</kbd><span>+</span><kbd>Shift</kbd><span>+</span><kbd>M</kbd></div> : <div className="shortcut-description">{keyFunction==="disabled"?"Esta tecla ficará desativada.":"A ação selecionada será atribuída à tecla."}</div>}
              <p className="helper-text">Escolha a função de cada tecla</p>
            </div>
          </article>
        </div>}
        {page === "apps" && <section className="simple-page panel"><div className="panel-title"><span className="purple-icon">♫</span><h2>Volume por aplicativo</h2></div><p className="page-description">Escolha quais aplicativos poderão ser controlados pelos potenciômetros do DECKRA.</p><label className="field-label" htmlFor="app-target-page">Aplicativo associado ao Potenciômetro 01</label><div className="select-wrap"><span>♫</span><select id="app-target-page" value={appTarget} onChange={e=>setAppTarget(e.target.value)}><option value="music">Reprodutor de música</option><option value="browser">Navegador</option><option value="game">Jogo</option><option value="system">Volume do sistema</option></select><span className="chevron">⌄</span></div><p className="helper-text left-helper">A seleção nesta prévia é demonstrativa. O controle de volume por aplicativo do Windows ainda precisa ser integrado.</p></section>}
        {page === "keys" && <section className="simple-page panel"><div className="panel-title"><span className="purple-icon keyboard-icon">▦</span><h2>Configuração das teclas</h2></div><div className="key-grid large-key-grid">{Array.from({length:6},(_,i)=><button key={i} className={"hardware-key "+(selectedKey===i+1?"selected":"")} onClick={()=>setSelectedKey(i+1)}>{String(i+1).padStart(2,"0")}</button>)}</div><label className="accent-label key-label">Tecla {String(selectedKey).padStart(2,"0")}</label><label className="field-label" htmlFor="key-function-page">Função</label><div className="select-wrap"><span>▤</span><select id="key-function-page" value={keyFunction} onChange={e=>setKeyFunction(e.target.value)}><option value="shortcut">Atalho de teclado</option><option value="media-play">Reproduzir / pausar</option><option value="mute">Silenciar</option><option value="next">Próxima faixa</option><option value="previous">Faixa anterior</option><option value="disabled">Desativada</option></select><span className="chevron">⌄</span></div><p className="helper-text left-helper">As alterações são apenas demonstrativas até a integração com o controlador.</p></section>}
      </section>
    </div>
    <footer className="window-footer"><span>Prévia ilustrativa do software · A aparência da interface final pode variar.</span><span className="footer-version">DECKRA CONTROL CENTER</span></footer>
  </main>;
}
