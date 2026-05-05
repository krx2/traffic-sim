import { useState } from 'react'
import CommandPanel from './components/CommandPanel'
import IntersectionView from './components/IntersectionView'
import OutputPanel from './components/OutputPanel'
import './App.css'

export default function App() {
  const [commands, setCommands] = useState([])
  const [response, setResponse] = useState(null)
  const [currentStep, setCurrentStep] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const runSimulation = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch('/api/simulation/run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ commands }),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.json()
      setResponse(data)
      setCurrentStep(0)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const reset = () => {
    setCommands([])
    setResponse(null)
    setCurrentStep(0)
    setError(null)
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>Traffic Simulator</h1>
        {error && <span className="error-badge">Błąd: {error}</span>}
      </header>
      <main className="app-body">
        <CommandPanel
          commands={commands}
          setCommands={setCommands}
          onRun={runSimulation}
          onReset={reset}
          loading={loading}
        />
        <IntersectionView
          commands={commands}
          response={response}
          currentStep={currentStep}
          setCurrentStep={setCurrentStep}
        />
        <OutputPanel
          response={response}
          currentStep={currentStep}
          setCurrentStep={setCurrentStep}
        />
      </main>
    </div>
  )
}
