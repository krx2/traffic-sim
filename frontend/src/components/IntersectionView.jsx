import { useMemo } from 'react'
import './IntersectionView.css'

const DIRS = ['north', 'south', 'east', 'west']

const VEHICLE_COLORS = {
  north: '#ef4444',
  south: '#22c55e',
  east:  '#3b82f6',
  west:  '#f59e0b',
}

const LIGHT_COLORS = { GREEN: '#22c55e', YELLOW: '#fbbf24', RED: '#ef4444' }

// ─── Vehicle state simulation (uses backend response for departures) ────────

function snap(q) {
  return { north: [...q.north], south: [...q.south], east: [...q.east], west: [...q.west] }
}

function computeVehicleStates(commands, response) {
  const vehicleRoad    = {}
  const vehicleEndRoad = {}
  const states = []
  const queues = { north: [], south: [], east: [], west: [] }
  let stepIdx = 0

  for (const cmd of commands) {
    if (cmd.type === 'addVehicle') {
      vehicleRoad[cmd.vehicleId]    = cmd.startRoad
      vehicleEndRoad[cmd.vehicleId] = cmd.endRoad
      queues[cmd.startRoad] = [...queues[cmd.startRoad], cmd.vehicleId]
    } else if (cmd.type === 'step') {
      const stepData = response?.stepStatuses[stepIdx]
      const left = stepData?.leftVehicles ?? []

      for (const dir of DIRS) queues[dir] = queues[dir].filter(id => !left.includes(id))

      const stepExited = { north: [], south: [], east: [], west: [] }
      for (const id of left) {
        const end = vehicleEndRoad[id]
        if (end) stepExited[end] = [...stepExited[end], id]
      }

      // lights come directly from backend (keys are uppercase: NORTH/SOUTH/EAST/WEST)
      const lights = stepData?.lights ?? null

      states.push({ queues: snap(queues), exited: stepExited, leftVehicles: left, lights })
      stepIdx++
    }
  }

  return { states, vehicleRoad }
}

function getDisplayState(commands, response, currentStep, states) {
  const empty = { north: [], south: [], east: [], west: [] }
  if (!response) {
    const queues = { north: [], south: [], east: [], west: [] }
    for (const cmd of commands) {
      if (cmd.type === 'addVehicle') queues[cmd.startRoad] = [...queues[cmd.startRoad], cmd.vehicleId]
    }
    return { queues, exited: empty, leftVehicles: [], lights: null }
  }
  if (states.length === 0) return { queues: empty, exited: empty, leftVehicles: [], lights: null }
  return states[Math.min(currentStep, states.length - 1)]
}

// ─── SVG geometry ──────────────────────────────────────────────────────────

const W = 500, H = 500
const CX = 250, CY = 250
const IW = 110
const IL = CX - IW / 2
const IR = CX + IW / 2
const IT = CY - IW / 2
const IB = CY + IW / 2

function inboundPositions(dir, vehicles) {
  return vehicles.map((id, i) => {
    const g = 24
    switch (dir) {
      case 'north': return { id, x: CX - 16, y: IT - 20 - i * g }
      case 'south': return { id, x: CX + 16, y: IB + 20 + i * g }
      case 'east':  return { id, x: IR + 20 + i * g, y: CY - 16 }
      case 'west':  return { id, x: IL - 20 - i * g, y: CY + 16 }
      default: return { id, x: 0, y: 0 }
    }
  })
}

function outboundPositions(dir, vehicles) {
  return vehicles.map((id, i) => {
    const g = 24
    switch (dir) {
      case 'north': return { id, x: CX + 16, y: IT - 20 - i * g }
      case 'south': return { id, x: CX - 16, y: IB + 20 + i * g }
      case 'east':  return { id, x: IR + 20 + i * g, y: CY + 16 }
      case 'west':  return { id, x: IL - 20 - i * g, y: CY - 16 }
      default: return { id, x: 0, y: 0 }
    }
  })
}

function getLightColor(dir, lights) {
  if (!lights) return '#ef4444'
  const phase = lights[dir.toUpperCase()]
  return LIGHT_COLORS[phase] ?? '#ef4444'
}

function getPhaseSummary(lights) {
  if (!lights) return null
  const green  = DIRS.filter(d => lights[d.toUpperCase()] === 'GREEN').map(d => d[0].toUpperCase())
  const yellow = DIRS.filter(d => lights[d.toUpperCase()] === 'YELLOW').map(d => d[0].toUpperCase())
  if (yellow.length) return { label: `YELLOW ${yellow.join('+')}`, cls: 'phase-yellow' }
  if (green.length)  return { label: `GREEN ${green.join('+')}`,   cls: 'phase-green'  }
  return { label: 'RED', cls: 'phase-red' }
}

// ─── Component ─────────────────────────────────────────────────────────────

export default function IntersectionView({ commands, response, currentStep, setCurrentStep }) {
  const { states, vehicleRoad } = useMemo(
    () => computeVehicleStates(commands, response),
    [commands, response]
  )

  const { queues, exited, leftVehicles, lights } = useMemo(
    () => getDisplayState(commands, response, currentStep, states),
    [commands, response, currentStep, states]
  )

  const stepCount   = states.length
  const hasResponse = response !== null
  const phaseSummary = getPhaseSummary(lights)

  return (
    <div className="intersection-panel">
      <div className="panel-title">Skrzyżowanie</div>

      <div className="svg-wrapper">
        <svg viewBox={`0 0 ${W} ${H}`} className="intersection-svg">
          <rect width={W} height={H} fill="#111827" />

          {/* road arms */}
          <rect x={IL} y={0}  width={IW} height={IT}     fill="#374151" />
          <rect x={IL} y={IB} width={IW} height={H - IB} fill="#374151" />
          <rect x={0}  y={IT} width={IL} height={IW}     fill="#374151" />
          <rect x={IR} y={IT} width={W - IR} height={IW} fill="#374151" />

          {/* intersection box */}
          <rect x={IL} y={IT} width={IW} height={IW} fill="#4b5563" />

          {/* road edge lines */}
          <line x1={IL} y1={0}  x2={IL} y2={IT} stroke="#6b7280" strokeWidth={1} />
          <line x1={IR} y1={0}  x2={IR} y2={IT} stroke="#6b7280" strokeWidth={1} />
          <line x1={IL} y1={IB} x2={IL} y2={H}  stroke="#6b7280" strokeWidth={1} />
          <line x1={IR} y1={IB} x2={IR} y2={H}  stroke="#6b7280" strokeWidth={1} />
          <line x1={0}  y1={IT} x2={IL} y2={IT} stroke="#6b7280" strokeWidth={1} />
          <line x1={0}  y1={IB} x2={IL} y2={IB} stroke="#6b7280" strokeWidth={1} />
          <line x1={IR} y1={IT} x2={W}  y2={IT} stroke="#6b7280" strokeWidth={1} />
          <line x1={IR} y1={IB} x2={W}  y2={IB} stroke="#6b7280" strokeWidth={1} />

          {/* center dividers */}
          <line x1={CX} y1={0}  x2={CX} y2={IT} stroke="#fbbf24" strokeWidth={1.5} strokeDasharray="12,8" opacity={0.5} />
          <line x1={CX} y1={IB} x2={CX} y2={H}  stroke="#fbbf24" strokeWidth={1.5} strokeDasharray="12,8" opacity={0.5} />
          <line x1={0}  y1={CY} x2={IL} y2={CY} stroke="#fbbf24" strokeWidth={1.5} strokeDasharray="12,8" opacity={0.5} />
          <line x1={IR} y1={CY} x2={W}  y2={CY} stroke="#fbbf24" strokeWidth={1.5} strokeDasharray="12,8" opacity={0.5} />

          {/* stop lines */}
          <line x1={IL} y1={IT} x2={CX} y2={IT} stroke="white" strokeWidth={2.5} opacity={0.6} />
          <line x1={CX} y1={IB} x2={IR} y2={IB} stroke="white" strokeWidth={2.5} opacity={0.6} />
          <line x1={IR} y1={IT} x2={IR} y2={CY} stroke="white" strokeWidth={2.5} opacity={0.6} />
          <line x1={IL} y1={CY} x2={IL} y2={IB} stroke="white" strokeWidth={2.5} opacity={0.6} />

          {/* direction labels */}
          <text x={CX}     y={18}     textAnchor="middle" fill="#9ca3af" fontSize="13" fontWeight="bold">N</text>
          <text x={CX}     y={H - 6}  textAnchor="middle" fill="#9ca3af" fontSize="13" fontWeight="bold">S</text>
          <text x={14}     y={CY + 5} textAnchor="middle" fill="#9ca3af" fontSize="13" fontWeight="bold">W</text>
          <text x={W - 14} y={CY + 5} textAnchor="middle" fill="#9ca3af" fontSize="13" fontWeight="bold">E</text>

          {/* traffic lights */}
          {[
            { dir: 'north', x: CX + 28, y: IT + 18 },
            { dir: 'south', x: CX - 28, y: IB - 18 },
            { dir: 'east',  x: IR - 18, y: CY + 28 },
            { dir: 'west',  x: IL + 18, y: CY - 28 },
          ].map(({ dir, x, y }) => {
            const col = getLightColor(dir, lights)
            return (
              <g key={dir}>
                <rect x={x - 9} y={y - 9} width={18} height={18} rx={3} fill="#111827" stroke="#1f2937" strokeWidth={1} />
                {col !== '#ef4444' && <circle cx={x} cy={y} r={9} fill={col} opacity={0.2} />}
                <circle cx={x} cy={y} r={5.5} fill={col} />
              </g>
            )
          })}

          {/* exited vehicles — outbound lane (only current step) */}
          {DIRS.flatMap(dir =>
            outboundPositions(dir, exited[dir] ?? []).map(({ id, x, y }) => {
              const originColor = VEHICLE_COLORS[vehicleRoad[id]] ?? '#94a3b8'
              return (
                <g key={`out-${id}`}>
                  <circle cx={x} cy={y} r={11} fill={originColor} opacity={0.65} stroke="white" strokeWidth={1.5} />
                  <text x={x} y={y + 4} textAnchor="middle" fontSize="7" fill="white" fontWeight="bold" opacity={0.9}>
                    {id.length > 5 ? id.slice(-4) : id}
                  </text>
                </g>
              )
            })
          )}

          {/* queued vehicles — inbound lane */}
          {DIRS.flatMap(dir =>
            inboundPositions(dir, queues[dir] ?? []).map(({ id, x, y }) => (
              <g key={`in-${id}`}>
                <circle cx={x} cy={y} r={11} fill={VEHICLE_COLORS[dir]} opacity={0.92} />
                <text x={x} y={y + 4} textAnchor="middle" fontSize="7" fill="white" fontWeight="bold">
                  {id.length > 5 ? id.slice(-4) : id}
                </text>
              </g>
            ))
          )}
        </svg>
      </div>

      {/* legend */}
      <div className="legend">
        {Object.entries(VEHICLE_COLORS).map(([dir, color]) => (
          <span key={dir} className="legend-item">
            <span className="legend-dot" style={{ background: color }} />{dir}
          </span>
        ))}
        <span className="legend-item"><span className="legend-dot" style={{ background: '#22c55e' }} />green</span>
        <span className="legend-item"><span className="legend-dot" style={{ background: '#fbbf24' }} />yellow</span>
        <span className="legend-item"><span className="legend-dot" style={{ background: '#ef4444' }} />red</span>
      </div>

      {/* step navigation */}
      {hasResponse && stepCount > 0 && (
        <div className="step-nav">
          <button
            className="step-btn"
            onClick={() => setCurrentStep(s => Math.max(0, s - 1))}
            disabled={currentStep === 0}
          >‹</button>
          <div className="step-info">
            <span className="step-label">Krok</span>
            <strong>{currentStep + 1} / {stepCount}</strong>
            {phaseSummary && (
              <span className={`phase-badge ${phaseSummary.cls}`}>{phaseSummary.label}</span>
            )}
            {leftVehicles.length > 0 && (
              <span className="left-badge">wyjechały: {leftVehicles.join(', ')}</span>
            )}
            {leftVehicles.length === 0 && <span className="no-left">brak ruchu</span>}
          </div>
          <button
            className="step-btn"
            onClick={() => setCurrentStep(s => Math.min(stepCount - 1, s + 1))}
            disabled={currentStep === stepCount - 1}
          >›</button>
        </div>
      )}

      {!hasResponse && <div className="hint">Dodaj komendy i uruchom symulację</div>}
    </div>
  )
}
