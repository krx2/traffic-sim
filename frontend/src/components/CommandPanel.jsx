import { useState } from 'react'
import './CommandPanel.css'

const DIRECTIONS = ['north', 'south', 'east', 'west']
const DIR_LABEL = { north: 'Północ', south: 'Południe', east: 'Wschód', west: 'Zachód' }

function pickRandom(directions) {
  return directions[Math.floor(Math.random() * directions.length)]
}

let vehicleCounter = 1

// ─── JSON import helper ─────────────────────────────────────────────────────

function parseJson(text) {
  const parsed = JSON.parse(text.trim())
  const commands = parsed.commands ?? parsed
  if (!Array.isArray(commands)) throw new Error('Oczekiwano tablicy "commands"')
  return commands
}

// ─── Component ──────────────────────────────────────────────────────────────

export default function CommandPanel({ commands, setCommands, onRun, onReset, loading }) {
  const [vehicleId, setVehicleId]       = useState('vehicle1')
  const [startRoad, setStartRoad]       = useState('random')
  const [endRoad, setEndRoad]           = useState('random')
  const [driverStrategy, setDriverStrategy] = useState('')

  const [jsonOpen, setJsonOpen]   = useState(false)
  const [jsonText, setJsonText]   = useState('')
  const [jsonError, setJsonError] = useState(null)

  // ── GUI add helpers ───────────────────────────────────────────────────────

  const addVehicle = () => {
    if (!vehicleId.trim()) return
    const resolvedStart = startRoad === 'random' ? pickRandom(DIRECTIONS) : startRoad
    const resolvedEnd = endRoad === 'random'
      ? pickRandom(DIRECTIONS.filter(d => d !== resolvedStart))
      : endRoad === resolvedStart
        ? pickRandom(DIRECTIONS.filter(d => d !== resolvedStart))
        : endRoad
    setCommands(prev => [
      ...prev,
      {
        type: 'addVehicle',
        vehicleId: vehicleId.trim(),
        startRoad: resolvedStart,
        endRoad: resolvedEnd,
        ...(driverStrategy ? { driverStrategy } : {}),
      },
    ])
    vehicleCounter++
    setVehicleId(`vehicle${vehicleCounter}`)
  }

  const addStep = () => setCommands(prev => [...prev, { type: 'step' }])

  const removeCommand = (index) => setCommands(prev => prev.filter((_, i) => i !== index))

  // ── JSON import ───────────────────────────────────────────────────────────

  const loadJson = () => {
    try {
      setCommands(parseJson(jsonText))
      setJsonText('')
      setJsonError(null)
      setJsonOpen(false)
    } catch (e) {
      setJsonError(e.message)
    }
  }

  // ─────────────────────────────────────────────────────────────────────────

  const stepCount = commands.filter(c => c.type === 'step').length
  const canRun = commands.length > 0 && stepCount > 0

  return (
    <div className="panel command-panel">
      <div className="panel-title">Komendy</div>

      {/* ── GUI form ── */}
      <div className="section">
        <div className="section-label">Dodaj pojazd</div>
        <input
          className="text-input"
          placeholder="ID pojazdu"
          value={vehicleId}
          onChange={e => setVehicleId(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && addVehicle()}
        />
        <div className="field-row">
          <label>Start</label>
          <select value={startRoad} onChange={e => setStartRoad(e.target.value)}>
            <option value="random">Losowy</option>
            {DIRECTIONS.map(d => <option key={d} value={d}>{DIR_LABEL[d]}</option>)}
          </select>
        </div>
        <div className="field-row">
          <label>Cel</label>
          <select value={endRoad} onChange={e => setEndRoad(e.target.value)}>
            <option value="random">Losowy</option>
            {DIRECTIONS.filter(d => startRoad === 'random' || d !== startRoad).map(d => (
              <option key={d} value={d}>{DIR_LABEL[d]}</option>
            ))}
          </select>
        </div>
        <div className="field-row">
          <label>Styl</label>
          <select value={driverStrategy} onChange={e => setDriverStrategy(e.target.value)}>
            <option value="">Losowy</option>
            <option value="PASSIVE">Pasywny</option>
            <option value="AGGRESSIVE">Agresywny</option>
          </select>
        </div>
        <button className="btn btn-add-vehicle" onClick={addVehicle}>+ Dodaj pojazd</button>
      </div>

      <div className="section">
        <button className="btn btn-add-step" onClick={addStep}>+ Dodaj krok symulacji</button>
      </div>

      {/* ── JSON import ── */}
      <div className="section editor-section">
        <button
          className="editor-toggle"
          onClick={() => { setJsonOpen(o => !o); setJsonError(null) }}
        >
          <span className={`editor-chevron ${jsonOpen ? 'open' : ''}`}>›</span>
          Wklej JSON
        </button>

        {jsonOpen && (
          <div className="editor-body">
            <textarea
              className="editor-textarea"
              value={jsonText}
              onChange={e => { setJsonText(e.target.value); setJsonError(null) }}
              spellCheck={false}
              rows={8}
              placeholder={'{\n  "commands": [\n    {"type":"addVehicle","vehicleId":"v1","startRoad":"north","endRoad":"south"},\n    {"type":"step"}\n  ]\n}'}
            />
            {jsonError && <pre className="editor-error">{jsonError}</pre>}
            <button className="btn btn-editor-replace" onClick={loadJson}>
              Załaduj
            </button>
          </div>
        )}
      </div>

      {/* ── Command list ── */}
      <div className="command-list-wrapper">
        <div className="section-label">
          Lista komend
          <span className="badge">{commands.length}</span>
        </div>
        <div className="command-list">
          {commands.length === 0 && (
            <div className="empty-list">Brak komend</div>
          )}
          {commands.map((cmd, i) => (
            <div key={i} className={`cmd-item cmd-${cmd.type}`}>
              <span className="cmd-index">{i + 1}</span>
              <span className="cmd-body">
                {cmd.type === 'addVehicle' ? (
                  <>
                    <span className="cmd-tag">pojazd</span>
                    <strong>{cmd.vehicleId}</strong>
                    <span className="cmd-route">{cmd.startRoad} → {cmd.endRoad}</span>
                    {cmd.driverStrategy && (
                      <span className="cmd-strategy">{cmd.driverStrategy}</span>
                    )}
                  </>
                ) : (
                  <span className="cmd-tag cmd-tag-step">KROK</span>
                )}
              </span>
              <button className="btn-remove" onClick={() => removeCommand(i)}>×</button>
            </div>
          ))}
        </div>
      </div>

      <div className="panel-actions">
        <button className="btn btn-reset" onClick={onReset}>Resetuj</button>
        <button
          className="btn btn-run"
          onClick={onRun}
          disabled={loading || !canRun}
        >
          {loading ? 'Symulowanie…' : 'Uruchom'}
        </button>
      </div>
    </div>
  )
}
