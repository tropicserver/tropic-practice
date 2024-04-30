package gg.tropic.practice.games.ranked;

import gg.scala.commons.annotations.inject.AutoBind;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author Colin McDonald
 * @since 2/18/2017
 */
@AutoBind
public final class PotPvPEloCalculator implements EloCalculator {

    public static final PotPvPEloCalculator INSTANCE =
            new PotPvPEloCalculator(35.0, 7, 25, 7, 25);

    private final double kPower;
    private final int minEloGain;
    private final int maxEloGain;
    private final int minEloLoss;
    private final int maxEloLoss;

    public PotPvPEloCalculator(double kPower, int minEloGain, int maxEloGain, int minEloLoss, int maxEloLoss) {
        this.kPower = kPower;
        this.minEloGain = minEloGain;
        this.maxEloGain = maxEloGain;
        this.minEloLoss = minEloLoss;
        this.maxEloLoss = maxEloLoss;
    }

    @Contract("_, _ -> new")
    public @NotNull CalculationResult calculate(int winnerElo, int loserElo) {
        double winnerQ = Math.pow(10, ((double) winnerElo) / 300D);
        double loserQ = Math.pow(10, ((double) loserElo) / 300D);

        double winnerE = winnerQ / (winnerQ + loserQ);
        double loserE = loserQ / (winnerQ + loserQ);

        int winnerGain = (int) (kPower * (1 - winnerE));
        int loserGain = (int) (kPower * (0 - loserE));

        winnerGain = Math.min(winnerGain, maxEloGain);
        winnerGain = Math.max(winnerGain, minEloGain);

        // loserGain will be negative so pay close attention here
        loserGain = Math.min(loserGain, -minEloLoss);
        loserGain = Math.max(loserGain, -maxEloLoss);

        return new CalculationResult(winnerElo, winnerGain, loserElo, loserGain);
    }

    @NotNull
    @Override
    public CalculationResult getNewRating(int winnerElo, int loserElo) {
        double winnerQ = Math.pow(10, ((double) winnerElo) / 300D);
        double loserQ = Math.pow(10, ((double) loserElo) / 300D);

        double winnerE = winnerQ / (winnerQ + loserQ);
        double loserE = loserQ / (winnerQ + loserQ);

        int winnerGain = (int) (kPower * (1 - winnerE));
        int loserGain = (int) (kPower * (0 - loserE));

        winnerGain = Math.min(winnerGain, maxEloGain);
        winnerGain = Math.max(winnerGain, minEloGain);

        // loserGain will be negative so pay close attention here
        loserGain = Math.min(loserGain, -minEloLoss);
        loserGain = Math.max(loserGain, -maxEloLoss);

        return new CalculationResult(winnerElo, winnerGain, loserElo, loserGain);
    }
}
