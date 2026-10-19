import { useMemo, useState } from "react";
import { encodeRgbCommand, parseDeviceLine, type DeviceMessage } from "./protocol";

type ControlValue = { id: string; kind: string; value: number | boolean };
const demoMessages = [
  '{"type":"hello","protocol":1,"device":"deckra","firmware":"0.1.0-demo","capabilities":["potentiometers","buttons","rgb"]}',
  '{"type":"control","control":"pot_1","kind":"potentiometer","value":512,"min":0,"max":1023}',
  '{"type":"control","control":"pot_2","kind":"potentiometer","value":760,"min":0,"max":1023}',
  '{"type":"control","control":"button_1","kind":"button","pressed":false}'
];

export default function App() {
  const [connected, setConnected] = useState(false);
  const [port, setPort] = useState("COM3");
  const [messages, setMessages] = useState<DeviceMessage[]>([]);
  const [controls, setControls] = useState<ControlValue[]>([]);
  const [rgb, setRgb] = useState("#8b5cf6");
  const [notice, setNotice] = useState("Modo demonstração ativo. Nenhum hardware foi conectado.");
  const [log, setLog] = useState<string[]>(["Aguardando eventos do dispositivo."]);
  const device = useMemo(() => messages.find((m) => m.type === "hello") as Extract<DeviceMessage, {type:"hello"}> | undefined, [messages]);

  function addLine(line: string) {
    const message = parseDeviceLine(line);
    if (!message) { setLog((items) => ["Mensagem inválida ignorada.", ...items].slice(0, 8)); return; }
    setMessages((items) => [message, ...items].slice(0, 30));
    if (message.type === "control" && (message.kind === "potentiometer" || message.kind === "button")) {
      const next = {id:message.control,kind:message.kind,value:message.kind==="potentiometer"?message.value:message.pressed};
      setControls((items) => [next, ...items.filter((item)=>item.id!==message.control)]);
    }
    setLog((items) => ["RX "+JSON.stringify(message), ...items].slice(0, 8));
  }
  function connectDemo() {
    setConnected(true); setNotice("Conectado ao dispositivo simulado. Porta COM real ainda não está ligada ao frontend.");
    demoMessages.forEach(addLine);
  }
  function disconnect() { setConnected(false); setNotice("Demonstração desconectada."); }
  function sendRgb() {
    const hex=rgb.slice(1), r=parseInt(hex.slice(0,2),16), g=parseInt(hex.slice(2,4),16), b=parseInt(hex.slice(4,6),16);
    const command=encodeRgbCommand("rgb_1",r,g,b,crypto.randomUUID());
    setLog((items)=>["TX "+command.trim(),...items].slice(0,8));
    setNotice("Comando RGB preparado em modo de demonstração. Nenhum LED físico foi alterado.");
  }

  return <main className="shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark">D</div><div><strong>DECKRA</strong><span>CONTROL CENTER</span></div></div>
      <div className="side-label">WORKSPACE</div>
      <button className="nav-item active"><span>◈</span>Visão geral</button>
      <button className="nav-item" onClick={()=>setNotice("Editor de controles será habilitado após validar o hardware.")}><span>⌘</span>Controles</button>
      <button className="nav-item" onClick={()=>setNotice("Perfis serão implementados na próxima etapa.")}><span>▤</span>Perfis</button>
      <div className="sidebar-bottom"><span className="status-dot"/> PROTÓTIPO 0.1.0</div>
    </aside>
    <section className="main">
      <header className="topbar"><div><span className="eyebrow">DEVICE MANAGEMENT</span><h1>Visão geral</h1></div><div className="top-right"><span className="platform">WINDOWS 10 / 11</span><div className="avatar">D</div></div></header>
      <section className="welcome">
        <div className="welcome-copy"><span className="eyebrow purple">YOUR HARDWARE. YOUR RULES.</span><h2>Seu setup,<br/><em>do seu jeito.</em></h2><p>Configure os controles DECKRA e acompanhe o estado do dispositivo em um só lugar.</p><div className="welcome-actions"><button className="primary" onClick={connected?disconnect:connectDemo}>{connected?"Desconectar demonstração":"Iniciar demonstração"} <span>↗</span></button><span className="hint">Hardware real ainda não conectado</span></div></div>
        <div className="device-art"><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="device"><div className="device-top"><span>DECKRA</span><span className="device-led"/></div><div className="device-knobs"><div className="knob"><i/></div><div className="knob"><i/></div><div className="knob"><i/></div></div><div className="device-buttons"><b/><b/><b/><b/><b/><b/></div><div className="device-bottom">CONTROL INTERFACE</div></div><div className="float-tag tag-top">USB SERIAL <span>●</span></div><div className="float-tag tag-bottom">CUSTOM PROFILE <span>↗</span></div></div>
      </section>
      <div className="section-heading"><div><span className="eyebrow">LIVE STATUS</span><h2>Seu dispositivo</h2></div><span className={"connection-pill "+(connected?"online":"")}><i/>{connected?"Conectado (demo)":"Desconectado"}</span></div>
      <div className="grid">
        <article className="panel"><div className="panel-head"><div className="panel-icon">⌁</div><div><h3>Conexão USB Serial</h3><p>Arduino Leonardo / Micro</p></div><span className="panel-kebab">•••</span></div><label className="field-label" htmlFor="port">PORTA SERIAL</label><div className="port-row"><select id="port" value={port} onChange={(e)=>setPort(e.target.value)}><option>COM3</option><option>COM4</option><option>COM5</option><option>COM6</option></select><span className="port-placeholder">Lista ilustrativa</span></div><div className="notice">{notice}</div><button className="secondary full" onClick={()=>setNotice("A seleção de porta é visual neste scaffold. Backend serial e leitura de eventos ainda precisam ser conectados.")}>Configurar conexão <span>→</span></button></article>
        <article className="panel"><div className="panel-head"><div className="panel-icon purple-icon">◉</div><div><h3>Entradas detectadas</h3><p>Eventos do controlador</p></div><span className="count">{String(controls.length).padStart(2,"0")}</span></div>{controls.length?<div className="control-list">{controls.map((c)=><div className="control-row" key={c.id}><div className="control-glyph">{c.kind==="potentiometer"?"◉":"●"}</div><div className="control-name"><strong>{c.id}</strong><span>{c.kind==="potentiometer"?"Potenciômetro":"Botão"}</span></div><div className="control-value">{typeof c.value==="boolean"?(c.value?"Pressionado":"Livre"):c.value}</div></div>)}</div>:<div className="empty-state"><span>⌁</span><p>Os controles aparecerão aqui quando forem detectados.</p></div>}<button className="text-button" onClick={connectDemo}>Carregar dados de demonstração <span>↗</span></button></article>
        <article className="panel rgb-panel"><div className="panel-head"><div className="panel-icon rgb-icon">✳</div><div><h3>Iluminação RGB</h3><p>Prévia de cor do LED</p></div><span className="live-label"><i/> PREVIEW</span></div><div className="rgb-preview" style={{"--led-color":rgb} as React.CSSProperties}><div className="rgb-glow"/><div className="rgb-ring"><div className="rgb-core"/></div><span>RGB_1</span></div><div className="color-row"><label htmlFor="rgb">COR PERSONALIZADA</label><div className="color-picker"><input id="rgb" type="color" value={rgb} onChange={(e)=>setRgb(e.target.value)}/><code>{rgb.toUpperCase()}</code></div></div><button className="primary full" onClick={sendRgb}>Preparar comando RGB <span>↗</span></button></article>
      </div>
      <section className="log-panel"><div className="log-heading"><div><span className="eyebrow">DEBUG CONSOLE</span><h3>Atividade recente</h3></div><button className="text-button" onClick={()=>setLog([])}>Limpar log</button></div><div className="logs">{log.map((line,i)=><div className="log-line" key={line+i}><span className="log-time">{new Date().toLocaleTimeString("pt-BR",{hour12:false})}</span><code>{line}</code></div>)}</div></section>
      <footer><span>DECKRA CONTROL CENTER <b>·</b> PROTÓTIPO 0.1.0</span><span>{device?"Firmware "+device.firmware:"Aguardando identificação do dispositivo"}</span></footer>
    </section>
  </main>;
}
