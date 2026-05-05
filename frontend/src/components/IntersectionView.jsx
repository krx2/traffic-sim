import { useMemo } from 'react'
import './IntersectionView.css'

const DIRS = ['north', 'south', 'east', 'west']
const LANE_TYPES = ['RIGHT_TURN', 'STRAIGHT', 'LEFT_TURN']

const VEHICLE_COLORS = {
  north: '#ef4444',
  south: '#22c55e',
  east:  '#3b82f6',
  west:  '#f59e0b',
}

// ─── Lane routing (mirrors Java LaneType.forMovement) ──────────────────────

const LANE_MAP = {
  north: { west: 'RIGHT_TURN', south: 'STRAIGHT', east: 'LEFT_TURN' },
  south: { east: 'RIGHT_TURN', north: 'STRAIGHT', west: 'LEFT_TURN' },
  east:  { north: 'RIGHT_TURN', west: 'STRAIGHT', south: 'LEFT_TURN' },
  west:  { south: 'RIGHT_TURN', east: 'STRAIGHT', north: 'LEFT_TURN' },
}

function getLaneType(from, to) {
  return LANE_MAP[from]?.[to] ?? 'STRAIGHT'
}

// ─── SVG geometry ──────────────────────────────────────────────────────────

const W = 500, H = 500
const CX = 250, CY = 250
const IW = 120                   // intersection box width
const IL = CX - IW / 2          // 190
const IR = CX + IW / 2          // 310
const IT = CY - IW / 2          // 190
const IB = CY + IW / 2          // 310

// Per-lane x/y centers inside each half-arm (right-hand Polish traffic)
// NORTH inbound (going south) → west half x=[IL..CX], RIGHT_TURN leftmost on screen
const NX = { RIGHT_TURN: IL + 10, STRAIGHT: IL + 27, LEFT_TURN: IL + 44 }
// SOUTH inbound (going north) → east half x=[CX..IR], RIGHT_TURN rightmost on screen
const SX = { RIGHT_TURN: IR - 10, STRAIGHT: IR - 27, LEFT_TURN: IR - 44 }
// EAST inbound (going west) → top half y=[IT..CY], RIGHT_TURN topmost on screen
const EY = { RIGHT_TURN: IT + 10, STRAIGHT: IT + 27, LEFT_TURN: IT + 44 }
// WEST inbound (going east) → bottom half y=[CY..IB], RIGHT_TURN bottommost on screen
const WY = { RIGHT_TURN: IB - 10, STRAIGHT: IB - 27, LEFT_TURN: IB - 44 }

function laneX(dir, laneType) {
  if (dir === 'north') return NX[laneType]
  if (dir === 'south') return SX[laneType]
  return null
}
function laneY(dir, laneType) {
  if (dir === 'east') return EY[laneType]
  if (dir === 'west') return WY[laneType]
  return null
}

function inboundPositions(dir, laneType, vehicles) {
  const g = 22
  return vehicles.map((id, i) => {
    switch (dir) {
      case 'north': return { id, x: NX[laneType], y: IT - 16 - i * g }
      case 'south': return { id, x: SX[laneType], y: IB + 16 + i * g }
      case 'east':  return { id, x: IR + 16 + i * g, y: EY[laneType] }
      case 'west':  return { id, x: IL - 16 - i * g, y: WY[laneType] }
      default:      return { id, x: 0, y: 0 }
    }
  })
}

function outboundPositions(dir, vehicles) {
  const g = 22
  // Outbound uses center of the outbound (exit) half
  const outX = { north: IR - 27, south: IL + 27 }
  const outY = { east: IB - 27, west: IT + 27 }
  return vehicles.map((id, i) => {
    switch (dir) {
      case 'north': return { id, x: outX.north, y: IT - 16 - i * g }
      case 'south': return { id, x: outX.south, y: IB + 16 + i * g }
      case 'east':  return { id, x: IR + 16 + i * g, y: outY.east }
      case 'west':  return { id, x: IL - 16 - i * g, y: outY.west }
      default:      return { id, x: 0, y: 0 }
    }
  })
}

// ─── Vehicle state simulation ───────────────────────────────────────────────

function emptyQueues() {
  const q = {}
  for (const d of DIRS) {
    q[d] = { RIGHT_TURN: [], STRAIGHT: [], LEFT_TURN: [] }
  }
  return q
}

function snapQueues(q) {
  const s = {}
  for (const d of DIRS) {
    s[d] = { RIGHT_TURN: [...q[d].RIGHT_TURN], STRAIGHT: [...q[d].STRAIGHT], LEFT_TURN: [...q[d].LEFT_TURN] }
  }
  return s
}

function computeVehicleStates(commands, response) {
  const vehicleRoad    = {}
  const vehicleEndRoad = {}
  const states = []
  const queues = emptyQueues()
  let stepIdx = 0

  for (const cmd of commands) {
    if (cmd.type === 'addVehicle') {
      vehicleRoad[cmd.vehicleId]    = cmd.startRoad
      vehicleEndRoad[cmd.vehicleId] = cmd.endRoad
      const lane = getLaneType(cmd.startRoad, cmd.endRoad)
      queues[cmd.startRoad][lane] = [...queues[cmd.startRoad][lane], cmd.vehicleId]
    } else if (cmd.type === 'step') {
      const stepData = response?.stepStatuses[stepIdx]
      const left = stepData?.leftVehicles ?? []

      for (const dir of DIRS)
        for (const lane of LANE_TYPES)
          queues[dir][lane] = queues[dir][lane].filter(id => !left.includes(id))

      const stepExited = { north: [], south: [], east: [], west: [] }
      for (const id of left) {
        const end = vehicleEndRoad[id]
        if (end) stepExited[end] = [...stepExited[end], id]
      }

      const lights = stepData?.lights ?? null
      states.push({ queues: snapQueues(queues), exited: stepExited, leftVehicles: left, lights })
      stepIdx++
    }
  }

  return { states, vehicleRoad }
}

function getDisplayState(commands, response, currentStep, states) {
  const empty = { north: [], south: [], east: [], west: [] }
  if (!response) {
    const queues = emptyQueues()
    for (const cmd of commands) {
      if (cmd.type === 'addVehicle') {
        const lane = getLaneType(cmd.startRoad, cmd.endRoad)
        queues[cmd.startRoad][lane] = [...queues[cmd.startRoad][lane], cmd.vehicleId]
      }
    }
    return { queues, exited: empty, leftVehicles: [], lights: null }
  }
  if (states.length === 0) return { queues: emptyQueues(), exited: empty, leftVehicles: [], lights: null }
  return states[Math.min(currentStep, states.length - 1)]
}

// ─── Light helpers ──────────────────────────────────────────────────────────

const LIGHT_ON  = { GREEN: '#22c55e', YELLOW: '#fbbf24', RED: '#ef4444' }
const LIGHT_OFF = '#1c1c1c'

function getLaneColor(dir, laneType, lights) {
  if (!lights) return 'RED'
  const dl = lights[dir.toUpperCase()]
  return dl?.[laneType] ?? 'RED'
}

function getPhaseSummary(lights) {
  if (!lights) return null
  const anyGreenDirs = DIRS.filter(d => {
    const dl = lights[d.toUpperCase()]
    return dl && Object.values(dl).some(c => c === 'GREEN')
  })
  const anyYellow = DIRS.some(d => {
    const dl = lights[d.toUpperCase()]
    return dl && Object.values(dl).some(c => c === 'YELLOW')
  })
  if (anyYellow) return { label: 'PRZEJŚCIE FAZY', cls: 'phase-yellow' }
  if (anyGreenDirs.length === 0) return { label: 'WSZYSTKIE CZERWONE', cls: 'phase-red' }

  const ns = anyGreenDirs.filter(d => d === 'north' || d === 'south').length === 2
  const ew = anyGreenDirs.filter(d => d === 'east' || d === 'west').length === 2
  const axis = ns ? 'NS' : ew ? 'EW' : anyGreenDirs.map(d => d[0].toUpperCase()).join('+')

  // Detect left-only phase (no STRAIGHT or RIGHT_TURN green)
  const leftOnly = DIRS.every(d => {
    const dl = lights[d.toUpperCase()]
    if (!dl) return true
    return !Object.entries(dl).some(([lt, c]) => c === 'GREEN' && lt !== 'LEFT_TURN')
  })
  const label = leftOnly ? `${axis} LEWY SKRĘT` : `${axis} PROSTO/PRAWY`
  return { label, cls: 'phase-green' }
}

// ─── Polish-style traffic light head ───────────────────────────────────────

function PolishLight({ cx, cy, color, rotate = 0 }) {
  const w = 11, h = 28, r = 3.5
  const top    = cy - 9
  const mid    = cy
  const bottom = cy + 9
  return (
    <g transform={`rotate(${rotate},${cx},${cy})`}>
      <rect x={cx - w / 2} y={cy - h / 2} width={w} height={h} rx={2}
            fill="#0f172a" stroke="#334155" strokeWidth={0.8} />
      {/* glow halos */}
      {color === 'RED'    && <circle cx={cx} cy={top}    r={r + 2} fill="#ef4444" opacity={0.25} />}
      {color === 'YELLOW' && <circle cx={cx} cy={mid}    r={r + 2} fill="#fbbf24" opacity={0.25} />}
      {color === 'GREEN'  && <circle cx={cx} cy={bottom} r={r + 2} fill="#22c55e" opacity={0.25} />}
      {/* circles */}
      <circle cx={cx} cy={top}    r={r} fill={color === 'RED'    ? LIGHT_ON.RED    : LIGHT_OFF} />
      <circle cx={cx} cy={mid}    r={r} fill={color === 'YELLOW' ? LIGHT_ON.YELLOW : LIGHT_OFF} />
      <circle cx={cx} cy={bottom} r={r} fill={color === 'GREEN'  ? LIGHT_ON.GREEN  : LIGHT_OFF} />
    </g>
  )
}

// ─── Traffic light placement config ────────────────────────────────────────
// Two signal heads per direction: STRAIGHT+RIGHT and LEFT_TURN
// Placed just inside the intersection box near each stop line

const LIGHT_CONFIGS = [
  // NORTH (near top of box, left half)
  { dir: 'north', laneType: 'STRAIGHT',   cx: NX.STRAIGHT,   cy: IT + 15, rotate: 0 },
  { dir: 'north', laneType: 'RIGHT_TURN', cx: NX.RIGHT_TURN,  cy: IT + 15, rotate: 0 },
  { dir: 'north', laneType: 'LEFT_TURN',  cx: NX.LEFT_TURN,   cy: IT + 15, rotate: 0 },
  // SOUTH (near bottom of box, right half)
  { dir: 'south', laneType: 'STRAIGHT',   cx: SX.STRAIGHT,   cy: IB - 15, rotate: 0 },
  { dir: 'south', laneType: 'RIGHT_TURN', cx: SX.RIGHT_TURN,  cy: IB - 15, rotate: 0 },
  { dir: 'south', laneType: 'LEFT_TURN',  cx: SX.LEFT_TURN,   cy: IB - 15, rotate: 0 },
  // EAST (near right of box, top half) — rotated 90° (driver approaches from right)
  { dir: 'east',  laneType: 'STRAIGHT',   cx: IR - 15, cy: EY.STRAIGHT,   rotate: -90 },
  { dir: 'east',  laneType: 'RIGHT_TURN', cx: IR - 15, cy: EY.RIGHT_TURN,  rotate: -90 },
  { dir: 'east',  laneType: 'LEFT_TURN',  cx: IR - 15, cy: EY.LEFT_TURN,   rotate: -90 },
  // WEST (near left of box, bottom half) — rotated 90°
  { dir: 'west',  laneType: 'STRAIGHT',   cx: IL + 15, cy: WY.STRAIGHT,   rotate: 90 },
  { dir: 'west',  laneType: 'RIGHT_TURN', cx: IL + 15, cy: WY.RIGHT_TURN,  rotate: 90 },
  { dir: 'west',  laneType: 'LEFT_TURN',  cx: IL + 15, cy: WY.LEFT_TURN,   rotate: 90 },
]

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

  const stepCount    = states.length
  const hasResponse  = response !== null
  const phaseSummary = getPhaseSummary(lights)

  return (
    <div className="intersection-panel">
      <div className="panel-title">Skrzyżowanie</div>

      <div className="svg-wrapper">
        <svg viewBox={`0 0 ${W} ${H}`} className="intersection-svg">
          <rect width={W} height={H} fill="#111827" />

          {/* road arms */}
          <rect x={IL} y={0}   width={IW} height={IT}     fill="#374151" />
          <rect x={IL} y={IB}  width={IW} height={H - IB} fill="#374151" />
          <rect x={0}  y={IT}  width={IL} height={IW}     fill="#374151" />
          <rect x={IR} y={IT}  width={W - IR} height={IW} fill="#374151" />

          {/* intersection box */}
          <rect x={IL} y={IT} width={IW} height={IW} fill="#4b5563" />

          {/* road edge lines */}
          {[
            [IL, 0, IL, IT], [IR, 0, IR, IT],
            [IL, IB, IL, H], [IR, IB, IR, H],
            [0, IT, IL, IT], [0, IB, IL, IB],
            [IR, IT, W, IT], [IR, IB, W, IB],
          ].map(([x1, y1, x2, y2], i) => (
            <line key={i} x1={x1} y1={y1} x2={x2} y2={y2} stroke="#6b7280" strokeWidth={1} />
          ))}

          {/* lane dividers — NORTH/SOUTH arm */}
          {[NX.RIGHT_TURN + (NX.STRAIGHT - NX.RIGHT_TURN) / 2,
            NX.STRAIGHT  + (NX.LEFT_TURN  - NX.STRAIGHT)  / 2].map((x, i) => (
            <line key={`nd${i}`} x1={x} y1={0} x2={x} y2={IT}
                  stroke="#4b5563" strokeWidth={1} strokeDasharray="6,6" opacity={0.5} />
          ))}
          {[SX.LEFT_TURN + (SX.STRAIGHT - SX.LEFT_TURN) / 2,
            SX.STRAIGHT + (SX.RIGHT_TURN - SX.STRAIGHT) / 2].map((x, i) => (
            <line key={`sd${i}`} x1={x} y1={IB} x2={x} y2={H}
                  stroke="#4b5563" strokeWidth={1} strokeDasharray="6,6" opacity={0.5} />
          ))}

          {/* lane dividers — EAST/WEST arm */}
          {[EY.RIGHT_TURN + (EY.STRAIGHT - EY.RIGHT_TURN) / 2,
            EY.STRAIGHT  + (EY.LEFT_TURN  - EY.STRAIGHT)  / 2].map((y, i) => (
            <line key={`ed${i}`} x1={IR} y1={y} x2={W} y2={y}
                  stroke="#4b5563" strokeWidth={1} strokeDasharray="6,6" opacity={0.5} />
          ))}
          {[WY.LEFT_TURN + (WY.STRAIGHT - WY.LEFT_TURN) / 2,
            WY.STRAIGHT + (WY.RIGHT_TURN - WY.STRAIGHT) / 2].map((y, i) => (
            <line key={`wd${i}`} x1={0} y1={y} x2={IL} y2={y}
                  stroke="#4b5563" strokeWidth={1} strokeDasharray="6,6" opacity={0.5} />
          ))}

          {/* center dividers */}
          <line x1={CX} y1={0}  x2={CX} y2={IT} stroke="#fbbf24" strokeWidth={1.5} strokeDasharray="12,8" opacity={0.5} />
          <line x1={CX} y1={IB} x2={CX} y2={H}  stroke="#fbbf24" strokeWidth={1.5} strokeDasharray="12,8" opacity={0.5} />
          <line x1={0}  y1={CY} x2={IL} y2={CY}  stroke="#fbbf24" strokeWidth={1.5} strokeDasharray="12,8" opacity={0.5} />
          <line x1={IR} y1={CY} x2={W}  y2={CY}  stroke="#fbbf24" strokeWidth={1.5} strokeDasharray="12,8" opacity={0.5} />

          {/* stop lines */}
          <line x1={IL} y1={IT} x2={CX} y2={IT} stroke="white" strokeWidth={2.5} opacity={0.6} />
          <line x1={CX} y1={IB} x2={IR} y2={IB} stroke="white" strokeWidth={2.5} opacity={0.6} />
          <line x1={IR} y1={IT} x2={IR} y2={CY} stroke="white" strokeWidth={2.5} opacity={0.6} />
          <line x1={IL} y1={CY} x2={IL} y2={IB} stroke="white" strokeWidth={2.5} opacity={0.6} />

          {/* direction labels */}
          <text x={CX} y={16}     textAnchor="middle" fill="#9ca3af" fontSize="13" fontWeight="bold">N</text>
          <text x={CX} y={H - 4}  textAnchor="middle" fill="#9ca3af" fontSize="13" fontWeight="bold">S</text>
          <text x={12} y={CY + 5} textAnchor="middle" fill="#9ca3af" fontSize="13" fontWeight="bold">W</text>
          <text x={W - 12} y={CY + 5} textAnchor="middle" fill="#9ca3af" fontSize="13" fontWeight="bold">E</text>

          {/* lane type labels on arms */}
          {[
            { x: NX.RIGHT_TURN, y: IT - 4, label: 'R', anchor: 'middle' },
            { x: NX.STRAIGHT,   y: IT - 4, label: 'P', anchor: 'middle' },
            { x: NX.LEFT_TURN,  y: IT - 4, label: 'L', anchor: 'middle' },
            { x: SX.RIGHT_TURN, y: IB + 10, label: 'R', anchor: 'middle' },
            { x: SX.STRAIGHT,   y: IB + 10, label: 'P', anchor: 'middle' },
            { x: SX.LEFT_TURN,  y: IB + 10, label: 'L', anchor: 'middle' },
          ].map(({ x, y, label, anchor }, i) => (
            <text key={`lbl${i}`} x={x} y={y} textAnchor={anchor}
                  fill="#6b7280" fontSize="7" fontWeight="bold">{label}</text>
          ))}
          {[
            { x: IR + 4, y: EY.RIGHT_TURN + 3, label: 'R' },
            { x: IR + 4, y: EY.STRAIGHT   + 3, label: 'P' },
            { x: IR + 4, y: EY.LEFT_TURN  + 3, label: 'L' },
            { x: IL - 4, y: WY.RIGHT_TURN + 3, label: 'R' },
            { x: IL - 4, y: WY.STRAIGHT   + 3, label: 'P' },
            { x: IL - 4, y: WY.LEFT_TURN  + 3, label: 'L' },
          ].map(({ x, y, label }, i) => (
            <text key={`lbl2${i}`} x={x} y={y} textAnchor="middle"
                  fill="#6b7280" fontSize="7" fontWeight="bold">{label}</text>
          ))}

          {/* Polish traffic lights (12 heads: 3 per direction) */}
          {LIGHT_CONFIGS.map(({ dir, laneType, cx, cy, rotate }) => (
            <PolishLight
              key={`light-${dir}-${laneType}`}
              cx={cx} cy={cy}
              color={getLaneColor(dir, laneType, lights)}
              rotate={rotate}
            />
          ))}

          {/* exited vehicles — outbound lane (only current step) */}
          {DIRS.flatMap(dir =>
            outboundPositions(dir, exited[dir] ?? []).map(({ id, x, y }) => {
              const originColor = VEHICLE_COLORS[vehicleRoad[id]] ?? '#94a3b8'
              return (
                <g key={`out-${id}`}>
                  <circle cx={x} cy={y} r={9} fill={originColor} opacity={0.6} stroke="white" strokeWidth={1.5} />
                  <text x={x} y={y + 3.5} textAnchor="middle" fontSize="6" fill="white" fontWeight="bold" opacity={0.9}>
                    {id.length > 5 ? id.slice(-4) : id}
                  </text>
                </g>
              )
            })
          )}

          {/* queued vehicles — inbound lanes */}
          {DIRS.flatMap(dir =>
            LANE_TYPES.flatMap(lane =>
              inboundPositions(dir, lane, queues[dir]?.[lane] ?? []).map(({ id, x, y }) => (
                <g key={`in-${id}`}>
                  <circle cx={x} cy={y} r={9} fill={VEHICLE_COLORS[dir]} opacity={0.92} />
                  <text x={x} y={y + 3.5} textAnchor="middle" fontSize="6" fill="white" fontWeight="bold">
                    {id.length > 5 ? id.slice(-4) : id}
                  </text>
                </g>
              ))
            )
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
        <span className="legend-item legend-sep">|</span>
        <span className="legend-item"><span className="legend-dot" style={{ background: '#22c55e' }} />zielone</span>
        <span className="legend-item"><span className="legend-dot" style={{ background: '#fbbf24' }} />żółte</span>
        <span className="legend-item"><span className="legend-dot" style={{ background: '#ef4444' }} />czerwone</span>
        <span className="legend-item legend-sep">|</span>
        <span className="legend-item" style={{ color: '#9ca3af' }}>L=lewy P=prosto P=prawy</span>
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
