package dqualizer.dqexec.adapter;

import dqualizer.dqexec.exception.UnknownTermException;
import dqualizer.dqexec.input.ConstantsLoader;
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.LoadTestConstants;
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.loadprofile.ConstantLoad;
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.loadprofile.LoadIncrease;
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.loadprofile.LoadPeak;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.Stage;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.options.*;
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.Stimulus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;

/**
 * Adapts the stimulus to a k6 'options' object
 */
@Component
public class StimulusAdapter {

    @Autowired
    private ConstantsLoader constantsLoader;

    /**
     * Create a k6 'options' objects based on the stimulus for the loadtest
     *
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'options' object
     */
    public Options adaptStimulus(Stimulus stimulus) {
        String loadProfile = stimulus.getLoadProfile();
        Scenario scenario;

        switch (loadProfile) {
            case "LOAD_PEAK" -> scenario = this.getLoadPeakScenario(stimulus);
            case "LOAD_INCREASE" -> scenario = this.getLoadIncreaseScenario(stimulus);
            case "CONSTANT_LOAD" -> scenario = this.getConstantLoadScenario(stimulus);
            default -> throw new UnknownTermException("Unknown Term : " + loadProfile);
        }
        Scenarios scenarios = new Scenarios(scenario);
        Options options = new Options(scenarios);

        return options;
    }

    /**
     * Create a k6 'scenario' object for the load_profile 'LOAD_PEAK'
     *
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'scenario' object with virtual user ramp-up
     */
    public Scenario getLoadPeakScenario(Stimulus stimulus) {
        LoadTestConstants constants = constantsLoader.load();
        LoadPeak loadPeak = constants.getLoadProfile().getLoadPeak();

        String highestLoad = stimulus.getHighestLoad();
        int target;
        switch (highestLoad) {
            case "HIGH" -> target = loadPeak.getHigh();
            case "VERY_HIGH" -> target = loadPeak.getVeryHigh();
            case "EXTREMELY_HIGH" -> target = loadPeak.getExtremelyHigh();
            default -> throw new UnknownTermException(highestLoad);
        }

        String timeToHighestLoad = stimulus.getTimeToHighestLoad();
        String duration;
        switch (timeToHighestLoad) {
            case "SLOW" -> duration = loadPeak.getSlow();
            case "FAST" -> duration = loadPeak.getFast();
            case "VERY_FAST" -> duration = loadPeak.getVeryFast();
            default -> throw new UnknownTermException(timeToHighestLoad);
        }
        String coolDownDuration = loadPeak.getCoolDownDuration();

        Stage stage1 = new Stage(duration, target);
        Stage stage2 = new Stage(coolDownDuration, 0);

        LinkedHashSet<Stage> stages = new LinkedHashSet<>();
        stages.add(stage1);
        stages.add(stage2);
        Scenario scenario = new RampingScenario(stages);
        return scenario;
    }

    /**
     * Create a k6 'scenario' object for the load_profile 'LOAD_INCREASE'
     *
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'scenario' object with increasing virtual user ramp-up
     */
    public Scenario getLoadIncreaseScenario(Stimulus stimulus) {
        LoadTestConstants constants = constantsLoader.load();
        LoadIncrease loadIncrease = constants.getLoadProfile().getLoadIncrease();

        String typeOfIncrease = stimulus.getTypeOfIncrease();
        int exponent;
        switch (typeOfIncrease) {
            case "LINEAR" -> exponent = loadIncrease.getLinear();
            case "QUADRATIC" -> exponent = loadIncrease.getQuadratic();
            case "CUBIC" -> exponent = loadIncrease.getCubic();
            default -> throw new UnknownTermException(typeOfIncrease);
        }

        int endTarget = loadIncrease.getEndTarget();
        int startTarget = loadIncrease.getStartTarget();
        String stageDuration = loadIncrease.getStageDuration();

        LinkedHashSet<Stage> stages = this.getIncreasingStages(stageDuration, startTarget, endTarget, exponent);
        Scenario scenario = new RampingScenario(stages);
        return scenario;
    }

    /**
     * Create an ordered set of stages for the 'INCREASE_LOAD' load_profile
     *
     * @param stageDuration The duration of one single stage
     * @param startTarget   The amount of users at the first stage
     * @param endTarget     The amount of users at the last stage
     * @param exponent      How fast should the amount of users increase
     * @return An ordered set of stages
     */
    private LinkedHashSet<Stage> getIncreasingStages(String stageDuration, int startTarget, int endTarget, int exponent) {
        LinkedHashSet<Stage> stages = new LinkedHashSet<>();
        int stageCounter = 1;
        int currentTarget = startTarget;

        while (currentTarget < endTarget) {
            currentTarget = startTarget * (int) Math.pow(stageCounter, exponent);
            Stage currentStage = new Stage(stageDuration, currentTarget);
            stages.add(currentStage);

            stageCounter++;
        }
        Stage lastStage = new Stage(stageDuration, 0);
        stages.add(lastStage);

        return stages;
    }

    /**
     * Create a k6 'scenario' object for the load_profile 'CONSTANT_LOAD'
     *
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'scenario' object with constant virtual users
     */
    public Scenario getConstantLoadScenario(Stimulus stimulus) {
        LoadTestConstants constants = constantsLoader.load();
        ConstantLoad constantLoad = constants.getLoadProfile().getConstantLoad();

        String baseLoad = stimulus.getBaseLoad();
        int vus;
        switch (baseLoad) {
            case "LOW" -> vus = constantLoad.getLow();
            case "MEDIUM" -> vus = constantLoad.getMedium();
            case "HIGH" -> vus = constantLoad.getHigh();
            default -> throw new UnknownTermException(baseLoad);
        }

        int accuracy = stimulus.getAccuracy();
        int maxDuration = constantLoad.getMaxDuration();
        int minDuration = constantLoad.getMinDuration();

        int duration = (int) (maxDuration * (accuracy / 100.0));
        int trueDuration = Math.max(minDuration, duration);

        Scenario scenario = new ConstantScenario(vus, trueDuration);
        return scenario;
    }
}