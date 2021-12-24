package me.petrolingus.wpe;

import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MathLogic {

    private static final double R = 3;
    private static final double MODEL_SIZE = 2.0 * R;
    private static final double TAU = 0.05;
    public static final int POINTS = 500;
    public static final int POINTS_LESS = POINTS - 1;
    private static final double STEP = MODEL_SIZE / (POINTS - 1.0);
    private static final double MEAN = 0.5;
    private static final double SIGMA = 0.25;

    private static final double A = 2.1;
    private static final double B = 2.5;
    private static final double K = 10;
    private static final double GAMMA = 1.0;

    private final List<Complex> vectorU;
    private final List<Complex> vectorSigma;
    private final List<Complex> vectorSigmaDerivative;

    private final List<Complex> vectorWavePacket;
    private final List<Complex> vectorWavePacketPrevious;

    private final List<Complex> vectorA;
    private final List<Complex> vectorB;
    private final List<Complex> vectorC;
    private final List<Complex> vectorD;

    private final List<Complex> vectorAlpha;
    private final List<Complex> vectorBeta;

    private final Complex c0 = Complex.I.multiply(TAU).divide(2.0);

    public static final List<Double> XIS = new ArrayList<>(POINTS);

    public final static int WAVE_PACKETS_COUNT = 512;
    int psiElementsCount = 0;
    Complex[][] psi = new Complex[WAVE_PACKETS_COUNT][POINTS];

    private static final FastFourierTransformer FFT = new FastFourierTransformer(DftNormalization.STANDARD);

    public static int wavePacketLineIndex = 250;
    public static final XYChart.Series<Number, Number> wavePacketSeries = new XYChart.Series<>();
    public static final XYChart.Series<Number, Number> wavePacketSeriesOrigin = new XYChart.Series<>();
    public static final XYChart.Series<Number, Number> wavePacketLineSeries = new XYChart.Series<>();

    boolean isPsiReady = false;
    public static int psiLineIndex = 250;
    public static final XYChart.Series<Number, Number> psiLineSeries = new XYChart.Series<>();
    public static final XYChart.Series<Number, Number> psiSeries = new XYChart.Series<>();

    public static final XYChart.Series<Number, Number> stationarySeries = new XYChart.Series<>();

    public MathLogic() {

        vectorU = new ArrayList<>(POINTS);
        vectorSigma = new ArrayList<>(POINTS);
        vectorSigmaDerivative = new ArrayList<>(POINTS);

        vectorA = new ArrayList<>(POINTS);
        vectorB = new ArrayList<>(POINTS);
        vectorC = new ArrayList<>(POINTS);
        vectorD = new ArrayList<>(POINTS);

        vectorAlpha = new ArrayList<>(POINTS);
        vectorBeta = new ArrayList<>(POINTS);

        vectorWavePacket = new ArrayList<>(POINTS);
        vectorWavePacketPrevious = new ArrayList<>(POINTS);

        for (int i = 0; i < POINTS; i++) {
            vectorA.add(Complex.ZERO);
            vectorB.add(Complex.ZERO);
            vectorC.add(Complex.ZERO);
            vectorD.add(Complex.ZERO);
            vectorAlpha.add(Complex.ZERO);
            vectorBeta.add(Complex.ZERO);
            vectorWavePacket.add(Complex.ZERO);
        }
    }

    public void calculateCoefficient() {

        for (int i = 1; i < vectorA.size() - 1; i++) {
            Complex b = vectorSigma.get(i).divide(Math.pow(STEP, 2));
            Complex c = vectorSigmaDerivative.get(i).divide(2.0 * STEP);
            vectorA.set(i, c0.negate().multiply(vectorSigma.get(i)).multiply(b.subtract(c)));
        }

        for (int i = 1; i < vectorB.size() - 1; i++) {
            Complex b = vectorSigma.get(i).divide(STEP * STEP);
            Complex c = vectorSigmaDerivative.get(i).divide(2.0 * STEP);
            vectorB.set(i, c0.negate().multiply(vectorSigma.get(i)).multiply(b.add(c)));
        }

        for (int i = 1; i < POINTS_LESS; i++) {
            Complex a = vectorSigma.get(i).multiply(vectorSigma.get(i)).multiply(2.0 / Math.pow(STEP, 2));
            Complex b = vectorU.get(i).add(a);
            Complex c = c0.multiply(b);
            vectorC.set(i, Complex.ONE.add(c));
        }

        for (int i = 1; i < POINTS_LESS; i++) {
            Complex wp = vectorWavePacketPrevious.get(i);
            Complex wpn = vectorWavePacketPrevious.get(i + 1);
            Complex wpp = vectorWavePacketPrevious.get(i - 1);
            Complex b = vectorU.get(i).negate().multiply(wp);
            Complex c = vectorSigma.get(i).multiply(vectorSigmaDerivative.get(i)).multiply(wpn.subtract(wpp)).divide(2.0 * STEP);
            Complex d = vectorSigma.get(i).pow(2).multiply(wpn.subtract(wp.multiply(2.0)).add(wpp)).divide(STEP * STEP);
            vectorD.set(i, wp.add(c0.multiply(b)).add(c0.multiply(c)).add(c0.multiply(d)));
        }
    }

    public void forward() {
        vectorAlpha.set(1, Complex.ZERO);
        vectorBeta.set(1, Complex.ZERO);
        for (int i = 2; i < POINTS; i++) {
            Complex div = vectorC.get(i - 1).add(vectorA.get(i - 1).multiply(vectorAlpha.get(i - 1)));
            vectorAlpha.set(i, vectorB.get(i - 1).negate().divide(div));
            vectorBeta.set(i, vectorD.get(i - 1).subtract(vectorA.get(i - 1).multiply(vectorBeta.get(i - 1))).divide(div));
        }
    }

    public void backward() {
        vectorWavePacket.set(POINTS_LESS, Complex.ZERO);
        for (int i = POINTS_LESS; i > 0; i--) {
            vectorWavePacket.set(i - 1, vectorAlpha.get(i).multiply(vectorWavePacket.get(i)).add(vectorBeta.get(i)));
        }
    }

    public void step() {
        calculateCoefficient();
        forward();
        backward();
        Collections.copy(vectorWavePacketPrevious, vectorWavePacket);
        createPhi();
        createSeriesAndDrawIt();
    }

    public void setInitState() {

        for (int i = 0; i < POINTS; i++) {
            double x = XIS.get(i);
            double value = wavePacketSeriesOrigin.getData().get(i).getYValue().doubleValue();
            vectorWavePacketPrevious.add(Complex.valueOf(value));

            if (x > -A && x < A) {
                double c0 = K * A * A / 2.0;
                double u = K * Math.pow(x, 2) / 2.0 - c0;
                vectorU.add(Complex.valueOf(u));
            } else {
                vectorU.add(Complex.valueOf(0));
            }

            if (x < -B) {
                Complex div = Complex.ONE.add(Complex.I.multiply(GAMMA * Math.pow(x + B, 2)));
                vectorSigma.add(Complex.ONE.divide(div));
                vectorSigmaDerivative.add(Complex.I.divide(div.pow(2)).multiply(-2.0 * GAMMA * (x + B)));
            } else if (x <= B) {
                vectorSigma.add(Complex.ONE);
                vectorSigmaDerivative.add(Complex.ZERO);
            } else {
                Complex div = Complex.ONE.add(Complex.I.multiply(GAMMA * Math.pow(x - B, 2)));
                vectorSigma.add(Complex.ONE.divide(div));
                vectorSigmaDerivative.add(Complex.I.divide(div.pow(2)).multiply(-2.0 * GAMMA * (x - B)));
            }
        }

    }

    private void createSeriesAndDrawIt() {
        Platform.runLater(() -> {
            wavePacketSeries.getData().clear();
            for (int i = 0; i < POINTS; i++) {
                wavePacketSeries.getData().add(new XYChart.Data<>(XIS.get(i), vectorWavePacket.get(i).abs()));
            }
            if (isPsiReady) {
                psiSeries.getData().clear();
                for (int i = 0; i < POINTS; i++) {
                    psiSeries.getData().add(new XYChart.Data<>(XIS.get(i), psi[i][wavePacketLineIndex].abs()));
                }
                stationarySeries.getData().clear();
                for (int i = 0; i < POINTS; i++) {
                    stationarySeries.getData().add(new XYChart.Data<>(XIS.get(i), psi[psiLineIndex][i].abs()));
                }
            }
        });
    }

    private void createPhi() {
        if (psiElementsCount < WAVE_PACKETS_COUNT) {
            for (int i = 0; i < vectorWavePacket.size(); i++) {
                psi[psiElementsCount][i] = vectorWavePacket.get(i);
            }
            psiElementsCount++;
        } else if (!isPsiReady) {

            for (int i = 0; i < POINTS; i++) {
                Complex[] complexes = new Complex[WAVE_PACKETS_COUNT];
                for (int j = 0; j < WAVE_PACKETS_COUNT; j++) {
                    complexes[j] = psi[j][i];
                }
                Complex[] result = FFT.transform(complexes, TransformType.FORWARD);
                for (int j = 0; j < WAVE_PACKETS_COUNT; j++) {
                    psi[j][i] = result[j];
                }
            }

            System.out.println("READY!!");
            isPsiReady = true;
        }
    }

    public static void init() {
        initWavePacketSeries();
        initVerticalLineSeries();
        initPsiSeries();
        initStationarySeries();
    }

    private static void initWavePacketSeries() {
        Gaussian gaussian = new Gaussian(1, MEAN, SIGMA);
        for (int i = 0; i < POINTS; i++) {
            double x = i * STEP - R;
            double value = gaussian.value(x);
            XIS.add(x);
            wavePacketSeries.getData().add(new XYChart.Data<>(x, value));
            wavePacketSeriesOrigin.getData().add(new XYChart.Data<>(x, value));
        }
    }

    private static void initVerticalLineSeries() {
        wavePacketLineSeries.getData().add(new XYChart.Data<>(0, 0));
        wavePacketLineSeries.getData().add(new XYChart.Data<>(0, 1));
        psiLineSeries.getData().add(new XYChart.Data<>(0, 0));
        psiLineSeries.getData().add(new XYChart.Data<>(0, 200));
    }

    private static void initPsiSeries() {
        for (int i = 0; i < POINTS; i++) {
            psiSeries.getData().add(new XYChart.Data<>(XIS.get(i), 0));
        }
    }

    private static void initStationarySeries() {
        for (int i = 0; i < POINTS; i++) {
            stationarySeries.getData().add(new XYChart.Data<>(XIS.get(i), 0));
        }
    }
}
