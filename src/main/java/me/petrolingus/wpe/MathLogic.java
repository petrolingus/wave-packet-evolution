package me.petrolingus.wpe;

import javafx.scene.chart.XYChart;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final double K = 10;
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
    XYChart.Series<Number, Number> wavePacketSeriesOrigin;
    XYChart.Series<Number, Number> fftSeries;
    XYChart.Series<Number, Number> stationary;
    XYChart.Series<Number, Number> fftLine;

    int counter = 512;
    int index = 250;
    List<List<Complex>> psi = new ArrayList<>(counter);
    int idpsi2 = 0;
    Complex[][] psi2 = new Complex[counter][POINTS];
    boolean isPsiReady = false;
    boolean psiReady = false;
    Complex[] result;
    FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

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
        wavePacketSeriesOrigin = new XYChart.Series<>();
        fftSeries = new XYChart.Series<>();
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

            if(idpsi2 < counter) {
                for (int i = 0; i < vectorWavePacket.size(); i++) {
                    psi2[idpsi2][i] = vectorWavePacket.get(i);
                }
                idpsi2++;
            }

            if (idpsi2 == counter && !isPsiReady) {
                psiReady = true;
                isPsiReady = true;
                System.out.println("Psi ready to process!!!!");

                for (int i = 0; i < POINTS; i++) {
                    Complex[] complexes = new Complex[counter];
                    for (int j = 0; j < counter; j++) {
                        complexes[j] = psi2[j][i];
                    }
                    Complex[] result = fft.transform(complexes, TransformType.FORWARD);
                    for (int j = 0; j < counter; j++) {
                        psi2[j][i] = result[j];
                    }
                }

                System.out.println("Psi ready to use!!!!");
            }

//            if (psi.size() < counter) {
//                List<Complex> complexes = new ArrayList<>(POINTS);
//                for (int i = 0; i < POINTS; i++) {
//                    complexes.add(vectorWavePacket.get(i));
//                }
//                psi.add(complexes);
//            } else if (!isPsiReady) {
//                isPsiReady = true;
//                psiReady = true;
//
//                for (int i = 0; i < 512; i++) {
//                    Complex[] complexes = new Complex[counter];
//                    for (int j = 0; j < counter; j++) {
//                        complexes[j] = psi.get(j).get(i);
//                    }
//                    Complex[] r = fft.transform(complexes, TransformType.FORWARD);
//                    for (int j = 0; j < counter; j++) {
//                        List<Complex> curr = psi.get()
//                        psi.set
//                    }
//                }
//            }

//            if (isPsiReady) {
//                calculate();
//                stationary.getData().clear();
//                for (int i = 0; i < 100; i++) {
//                    double value = 1;
//                    stationary.getData().add(new XYChart.Data<>(xis.get(i), value));
//                }
//            }

        }
    }

    public void calculate() {
        Complex[] complexes = new Complex[counter];
        for (int i = 0; i < counter; i++) {
            complexes[i] = psi.get(i).get(Controller.lineIndex);
        }
        result = fft.transform(complexes, TransformType.FORWARD);
//        System.out.println(Arrays.toString(complexes));
//        System.out.println("fft is ready111!");
    }

    public void setInitState(double mean, double sigma) {
        Gaussian gaussian = new Gaussian(1, mean, sigma);
        for (int i = 0; i < POINTS; i++) {
            double x = i * STEP - R;
            double value = gaussian.value(x);
            vectorWavePacketPrevious.add(Complex.valueOf(value));
            xis.add(x);
            wavePacketSeries.getData().add(new XYChart.Data<>(x, value));
            wavePacketSeriesOrigin.getData().add(new XYChart.Data<>(x, value));

            if (x > -A && x < A) {
                double c0 = K * A * A / 2.0;
                double u = K * Math.pow(x, 2) / 2.0 - c0;
                vec_U.add(Complex.valueOf(u));
            } else {
                vec_U.add(Complex.valueOf(0));
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
        System.out.println();
    }

    public XYChart.Series<Number, Number> getSeries() {
        isUsing = true;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < POINTS; i++) {
            double x = xis.get(i);
            double y = vectorWavePacket.get(i).abs();
            series.getData().add(new XYChart.Data<>(x, y));
        }
        isUsing = false;
        return series;
    }

    public XYChart.Series<Number, Number> getFftSeries() {
        isUsing = true;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        if (psiReady) {
            for (int i = 0; i < POINTS; i++) {
                double x = xis.get(i);
                double y = psi2[i][Controller.lineIndex].abs();
                series.getData().add(new XYChart.Data<>(x, y));
            }
        } else {
            for (int i = 0; i < POINTS; i++) {
                double x = xis.get(i);
                series.getData().add(new XYChart.Data<>(x, 0));
            }
        }
        isUsing = false;
        return series;
    }

    public XYChart.Series<Number, Number> getOrigin() {
        return wavePacketSeriesOrigin;
    }

    public XYChart.Series<Number, Number> getStationary() {
        isUsing = true;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        if (psiReady) {
            for (int i = 0; i < POINTS; i++) {
                double x = xis.get(i);
                double y = psi2[Controller.stationaryIndex][i].abs();
                series.getData().add(new XYChart.Data<>(x, y));
            }
        } else {
            for (int i = 0; i < POINTS; i++) {
                double x = xis.get(i);
                series.getData().add(new XYChart.Data<>(x, 0));
            }
        }
        isUsing = false;
        return series;
    }
}
