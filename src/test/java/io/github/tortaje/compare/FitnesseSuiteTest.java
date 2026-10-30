package io.github.tortaje.compare;

import fitnesse.junit.FitNesseRunner;
import org.junit.runner.RunWith;

/** Runs the CompareSuite wiki pages in-process with FitNesse (quiet runner), against both comparison fixtures. */
@RunWith(QuietFitNesseRunner.class)
@FitNesseRunner.Suite("CompareSuite")
@FitNesseRunner.FitnesseDir("src/test/fitnesse")
@FitNesseRunner.OutputDir("target/fitnesse-results")
@FitNesseRunner.PreventSystemExit(false)
public class FitnesseSuiteTest {
}
