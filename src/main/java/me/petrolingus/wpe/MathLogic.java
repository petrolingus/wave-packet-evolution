package me.petrolingus.wpe;

import javafx.scene.chart.XYChart;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MathLogic {

    private static final double R = 3;
    private static final double MODEL_SIZE = 2.0 * R;
    private static final double TAU = 0.01;
    private static final int POINTS = 500;
    private static final int POINTS_LESS = POINTS - 1;
    private static final double STEP = MODEL_SIZE / (POINTS - 1.0);

    private static final double U0 = 30.0;
    private static final double A = 2.1;
    private static final double B = 2.5;
    private static final double K = 0.1;
    private static final double GAMMA = 1.0;
    private static final double INF = 50;

    private final List<Complex> vec_U;
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

    List<Double> xis;

    XYChart.Series<Number, Number> wavePacketSeries;

    boolean isUsing = false;

    public MathLogic() {

        vec_U = new ArrayList<>(POINTS);
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

        xis = new ArrayList<>(POINTS);
        wavePacketSeries = new XYChart.Series<>();
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
            Complex b = vec_U.get(i).add(a);
            Complex c = c0.multiply(b);
            vectorC.set(i, Complex.ONE.add(c));
        }

        for (int i = 1; i < POINTS_LESS; i++) {
            Complex wp = vectorWavePacketPrevious.get(i);
            Complex wpn = vectorWavePacketPrevious.get(i + 1);
            Complex wpp = vectorWavePacketPrevious.get(i - 1);
            Complex b = vec_U.get(i).negate().multiply(wp);
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
        if (!isUsing) {
            calculateCoefficient();
            forward();
            backward();
            Collections.copy(vectorWavePacketPrevious, vectorWavePacket);
        }
    }

    public void setInitState(double mean, double sigma) {
        Gaussian gaussian = new Gaussian(mean, sigma);
        for (int i = 0; i < POINTS; i++) {
            double x = i * STEP - R;
            double value = gaussian.value(x);
            vectorWavePacketPrevious.add(Complex.valueOf(value));
            xis.add(x);
            wavePacketSeries.getData().add(new XYChart.Data<>(x, value));

            if (x < A && x > -A) {
                if (x <= 0) {
                    vec_U.add(Complex.valueOf(INF));
                } else {
                    vec_U.add(Complex.valueOf(K * x));
                }
            } else {
                vec_U.add(Complex.valueOf(0));
            }

//            double u = K * Math.pow(x, 2) / 2;
//            vec_U.add(Complex.valueOf(u));

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
        System.out.println();
    }

    public XYChart.Series<Number, Number> getSeries() {
        isUsing = true;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
//        double max = 0;
//        for (int i = 0; i < POINTS; i++) {
//            double value = vectorWavePacket.get(i).abs();
//            if (max < value) {
//                max = value;
//            }
//        }
        for (int i = 0; i < POINTS; i++) {
            double x = xis.get(i);
            double y = vectorWavePacket.get(i).abs();
            series.getData().add(new XYChart.Data<>(x, y));
        }
        isUsing = false;
        return series;
    }
}
