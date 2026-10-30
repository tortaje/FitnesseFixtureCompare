package io.github.tortaje.compare;

import fitnesse.junit.FitNesseRunner;
import fitnesse.testrunner.MultipleTestsRunner;
import fitnesse.testsystems.ConsoleExecutionLogListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

/**
 * FitNesseRunner without the test-system start banner ({@code slim:in-process} and the command line) that
 * FitNesse prints on System.out; test-system output, stderr and exceptions are still forwarded to the console.
 */
public class QuietFitNesseRunner extends FitNesseRunner {

    /**
     * Creates the runner.
     *
     * @param suiteClass the class carrying the FitNesseRunner annotations
     * @throws InitializationError if the annotations are invalid
     */
    public QuietFitNesseRunner(Class<?> suiteClass) throws InitializationError {
        super(suiteClass);
    }

    @Override
    protected void addExecutionLogListener(RunNotifier notifier, MultipleTestsRunner testRunner, Class<?> suiteClass) {
        testRunner.addExecutionLogListener(new ConsoleExecutionLogListener() {
            @Override
            public void commandStarted(ExecutionContext context) {
                // banner dropped: it is printed on every successful run
            }
        });
    }
}
