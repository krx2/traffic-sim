# traffic-sim

Symulator skrzyżowania z sygnalizacją świetlną. Backend w Spring Boot, frontend w React. Projekt akademicki ilustrujący kilka wzorców projektowych.

## Uruchomienie

### Tryb CLI

Zbuduj projekt i uruchom z plikiem wejściowym i wyjściowym:

```bash
./mvnw package -DskipTests
java -jar target/traffic-sim-1.0.0.jar input.json output.json
```

### Tryb serwera web

Backend (port 8080):

```bash
./mvnw spring-boot:run
```

Frontend (port 5173):

```bash
cd frontend
npm install
npm run dev
```

Swagger UI dostępny pod `http://localhost:8080/swagger-ui.html`.

## API

Jeden endpoint:

```
POST /api/simulation/run
```

Przyjmuje listę komend, zwraca stan po każdym kroku.

### Format wejścia

```json
{
  "commands": [
    { "type": "addVehicle", "vehicleId": "v1", "startRoad": "north", "endRoad": "south" },
    { "type": "addVehicle", "vehicleId": "v2", "startRoad": "west", "endRoad": "east", "driverStrategy": "AGGRESSIVE" },
    { "type": "step" },
    { "type": "step" }
  ]
}
```

Dozwolone wartości `startRoad`/`endRoad`: `north`, `south`, `east`, `west`.  
`driverStrategy`: `PASSIVE`, `AGGRESSIVE` lub pominięte (losowy wybór).

### Format wyjścia

```json
{
  "stepStatuses": [
    {
      "leftVehicles": ["v1"],
      "lights": {
        "NORTH": { "STRAIGHT": "GREEN", "RIGHT_TURN": "GREEN", "LEFT_TURN": "RED" },
        "SOUTH": { "STRAIGHT": "GREEN", "RIGHT_TURN": "GREEN", "LEFT_TURN": "RED" },
        "EAST":  { "STRAIGHT": "RED",   "RIGHT_TURN": "RED",   "LEFT_TURN": "RED" },
        "WEST":  { "STRAIGHT": "RED",   "RIGHT_TURN": "RED",   "LEFT_TURN": "RED" }
      }
    }
  ]
}
```

`leftVehicles` — pojazdy, które opuściły skrzyżowanie w danym kroku.  
`lights` — stan każdego pasa przed wykonaniem kroku.

## Fazy świateł

Cykl składa się z sześciu faz:

| Faza | Co ma zielone |
|------|--------------|
| NS_STRAIGHT_RIGHT | Północ i południe: prosto + prawy skręt |
| NS_LEFT | Północ i południe: lewy skręt |
| NS_EW_CLEARANCE | Wszystkie czerwone (1 krok) |
| EW_STRAIGHT_RIGHT | Wschód i zachód: prosto + prawy skręt |
| EW_LEFT | Wschód i zachód: lewy skręt |
| EW_NS_CLEARANCE | Wszystkie czerwone (1 krok) |

Fazy clearance trwają dokładnie jeden krok i nie przechodzą przez żółte.

## Przypisanie pasów

Prawostronne reguły ruchu:

- skręt w prawo → pas RIGHT_TURN
- jazda prosto → pas STRAIGHT
- skręt w lewo → pas LEFT_TURN

## Strategie sterowania sygnalizacją

Strategia jest wstrzykiwana do symulacji i decyduje, kiedy zmienić fazę.

**WeightedQueueStrategy** (domyślna) — obserwuje kolejki pojazdów. Zostaje w bieżącej fazie, jeśli są pojazdy, które mogą przejechać; przechodzi do następnej fazy gdy kolejka aktywnego kierunku się opróżni lub zostanie przekroczony limit kroków.

**FixedTimeStrategy** — zmienia fazę co N kroków bez względu na stan kolejek. Faza clearance zawsze trwa 1 krok.

## Wzorce projektowe

| Wzorzec | Gdzie |
|---------|-------|
| Command | `AddVehicleCommand`, `StepCommand` — komendy wykonywane na `Intersection` |
| Strategy | `TrafficLightStrategy` — wymienne algorytmy sterowania fazami |
| State | `TrafficLight` z `GreenState`, `YellowState`, `RedState` |
| Observer | `LightStateObserver` — rejestruje stan świateł przed każdym krokiem |
| Memento | `IntersectionMemento` — migawka kolejek pojazdów i fazy; używana do przywracania stanu |

## Struktura projektu

```
traffic-sim/
├── src/main/java/com/kzebro/trafficsim/
│   ├── command/          AddVehicleCommand, StepCommand
│   ├── dto/              SimulationRequest, StepStatus, AddVehicleCommandDto, ...
│   ├── factory/          CommandFactory
│   ├── memento/          IntersectionMemento, IntersectionCaretaker
│   ├── model/            Intersection, Road, Lane, Vehicle, LightPhase, LaneType, Direction
│   │   └── light/        TrafficLight, GreenState, YellowState, RedState
│   ├── observer/         LightStateObserver
│   ├── service/          SimulationService
│   └── strategy/         TrafficLightStrategy, WeightedQueueStrategy, FixedTimeStrategy
├── src/test/java/...
│   ├── IntersectionTest.java
│   ├── StrategyTest.java
│   ├── SimulationServiceTest.java
│   └── DriverStrategyTest.java
└── frontend/
    └── src/
        ├── App.jsx
        └── components/
            ├── CommandPanel.jsx   panel komend z GUI i importem JSON
            ├── IntersectionView.jsx  wizualizacja SVG skrzyżowania
            └── ResultsPanel.jsx   wyniki kroku po kroku
```

## Testy

```bash
./mvnw test
```

Testy jednostkowe i integracyjne pokrywają: inicjalny stan świateł, przypisanie pasów, przejścia między fazami, strategie sterowania, zachowanie agresywnego i pasywnego kierowcy, memento oraz poprawność wyjścia serwisu symulacji.
