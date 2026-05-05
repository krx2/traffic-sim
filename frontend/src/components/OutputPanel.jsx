import './OutputPanel.css'

export default function OutputPanel({ response, currentStep, setCurrentStep }) {
  if (!response) {
    return (
      <div className="panel output-panel">
        <div className="panel-title">Wyniki</div>
        <div className="empty-output">
          <div className="empty-icon">output</div>
          <div>Uruchom symulację,<br />aby zobaczyć wyniki</div>
        </div>
      </div>
    )
  }

  const { stepStatuses } = response

  return (
    <div className="panel output-panel">
      <div className="panel-title">
        Wyniki
        <span className="badge">{stepStatuses.length} kroków</span>
      </div>

      <div className="step-statuses">
        {stepStatuses.map((status, i) => (
          <button
            key={i}
            className={`step-card ${i === currentStep ? 'active' : ''}`}
            onClick={() => setCurrentStep(i)}
          >
            <div className="step-card-header">
              <span className="step-num">Krok {i + 1}</span>
              <span className="vehicle-count">
                {status.leftVehicles.length} pojazd{vehicleSuffix(status.leftVehicles.length)}
              </span>
            </div>
            <div className="vehicle-list">
              {status.leftVehicles.length === 0 ? (
                <span className="no-vehicles">— brak ruchu —</span>
              ) : (
                status.leftVehicles.map(v => (
                  <span key={v} className="vehicle-chip">{v}</span>
                ))
              )}
            </div>
          </button>
        ))}
      </div>

      <details className="json-section">
        <summary>Pełny JSON</summary>
        <pre className="json-pre">{JSON.stringify(response, null, 2)}</pre>
      </details>
    </div>
  )
}

function vehicleSuffix(n) {
  if (n === 1) return ''
  if (n >= 2 && n <= 4) return 'y'
  return 'ów'
}
