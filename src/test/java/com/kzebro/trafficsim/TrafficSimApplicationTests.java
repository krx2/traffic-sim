package com.kzebro.trafficsim;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Master test suite — uruchomienie tej klasy powoduje wykonanie wszystkich
 * klas testowych projektu.
 *
 * <p>Aby dodać nową klasę testową do suite, wystarczy dopisać ją do {@code @SelectClasses}.</p>
 *
 * <p>Mechanizm: JUnit Platform uruchamia dwa silniki równolegle:
 * <ul>
 *   <li><b>SuiteTestEngine</b>   – odkrywa i wykonuje klasy z {@code @SelectClasses}</li>
 *   <li><b>JupiterTestEngine</b> – wykonuje metodę {@code contextLoads()} poniżej</li>
 * </ul>
 * </p>
 */
@Suite
@SelectClasses({
        DriverStrategyTest.class,
        IntersectionTest.class,
        StrategyTest.class,
        SimulationServiceTest.class
})
@SpringBootTest
class TrafficSimApplicationTests {

    @Test
    void contextLoads() {
        // Weryfikuje że Spring ApplicationContext ładuje się bez błędów
    }

}
