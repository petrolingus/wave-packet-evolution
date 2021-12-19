package me.petrolingus.wpe;

import javafx.scene.chart.XYChart;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MathLogic {

    private static final double R = 3; // Максимальное значение по оси Х для волнового пакета
    private static final double MODEL_SIZE = 2.0 * R;
    private static final double TAU = 0.05; // Временной шаг симуляции
    private static final int POINTS = 500;
    private static final int POINTS_LESS = POINTS - 1;
    private static final double STEP = MODEL_SIZE / (POINTS - 1.0);

    private static final double U0 = 30.0;
    private static final double A = 2.1;
    private static final double B = 2.5;
    private static final double K = 0.5;
    private static final double GAMMA = 1.0;
    private static final double INF = 50;

    /**
     * Потенциал задачи.
     */
    private final List<Complex> vec_U;
    /**
     * Поглощающие слои.
     */
    private final List<Complex> vec_sigm;
    /**
     * Поглощающие слои (производная).
     */
    private final List<Complex> vec_d_sigm;


    private final List<Complex> vec_wave_packet;
    private final List<Complex> vec_wave_packet_prev;

    /**
     * Вектор коэффициентов Ak.
     */
    private final List<Complex> vectorA;
    /**
     * Вектор коэффициентов Bk.
     */
    private final List<Complex> vectorB;
    /**
     * Вектор коэффициентов Ck.
     */
    private final List<Complex> vectorC;
    /**
     * Вектор коэффициентов Dk.
     */
    private final List<Complex> vectorD;

    /**
     * Вектор значений фукнции alpha.
     */
    private final List<Complex> vec_alpha;
    /**
     * Вектор значений фукнции beta.
     */
    private final List<Complex> vec_beta;

    private final Complex c0 = Complex.I.multiply(TAU).divide(2.0);

    List<Double> xis;

    XYChart.Series<Number, Number> wavePacketSeries;

    boolean isUsing = false;

    public MathLogic() {

        vec_U = new ArrayList<>(POINTS);
        vec_sigm = new ArrayList<>(POINTS);
        vec_d_sigm = new ArrayList<>(POINTS);

        vectorA = new ArrayList<>(POINTS);
        vectorB = new ArrayList<>(POINTS);
        vectorC = new ArrayList<>(POINTS);
        vectorD = new ArrayList<>(POINTS);

        vec_alpha = new ArrayList<>(POINTS);
        vec_beta = new ArrayList<>(POINTS);

        vec_wave_packet = new ArrayList<>(POINTS);
        vec_wave_packet_prev = new ArrayList<>(POINTS);

        for (int i = 0; i < POINTS; i++) {
            vectorA.add(Complex.ZERO);
            vectorB.add(Complex.ZERO);
            vectorC.add(Complex.ZERO);
            vectorD.add(Complex.ZERO);
            vec_alpha.add(Complex.ZERO);
            vec_beta.add(Complex.ZERO);
            vec_wave_packet.add(Complex.ZERO);
        }

        xis = new ArrayList<>(POINTS);
        wavePacketSeries = new XYChart.Series<>();
    }

    public void calculateCoefficient() {
        for (int i = 1; i < vectorA.size() - 1; i++) {
            Complex b = vec_sigm.get(i).divide(STEP * STEP);
            Complex c = vec_d_sigm.get(i).divide(2.0 * STEP);
            vectorA.set(i, c0.multiply(vec_sigm.get(i)).multiply(b.subtract(c))).multiply(-1);
        }
        for (int i = 1; i < vectorB.size() - 1; i++) {
            Complex b = vec_sigm.get(i).divide(STEP * STEP);
            Complex c = vec_d_sigm.get(i).divide(2.0 * STEP);
            vectorB.set(i, c0.multiply(vec_sigm.get(i)).multiply(b.add(c)).multiply(-1));
        }
//        vec_C[i] = 1.0 + (image_j * tau / 2.0) * (vec_U[i] + 2.0 * vec_sigm[i] * vec_sigm[i] / step_r / step_r);
        for (int i = 1; i < POINTS_LESS; i++) {
            Complex a = vec_sigm.get(i).multiply(vec_sigm.get(i)).multiply(2.0 / Math.pow(STEP, 2));
            Complex b = vec_U.get(i).add(a);
            Complex c = c0.multiply(b);
            vectorC.set(i, Complex.ONE.add(c));
        }
//        vec_D[i] = vec_wave_packet_prev[i] +
//                c0 * (- vec_U[i] * vec_wave_packet_prev[i]) +
//                c0 * (vec_sigm[i] * vec_d_sigm[i] * (vec_wave_packet_prev[i + 1] - vec_wave_packet_prev[i - 1]) /(2.0 * step_r)) +
//                c0 * (vec_sigm[i] * vec_sigm[i] * (vec_wave_packet_prev[i + 1] - 2.0 * vec_wave_packet_prev[i] + vec_wave_packet_prev[i - 1]) / step_r / step_r);
        for (int i = 1; i < vectorD.size() - 1; i++) {
            Complex wp = vec_wave_packet_prev.get(i);
            Complex wpn = vec_wave_packet_prev.get(i + 1);
            Complex wpp = vec_wave_packet_prev.get(i - 1);
            Complex b = vec_U.get(i).negate().multiply(wp);
            Complex c = vec_sigm.get(i).multiply(vec_d_sigm.get(i).multiply(wpn.subtract(wpp))).divide(2.0 * STEP);
            Complex d = vec_sigm.get(i).multiply(vec_sigm.get(i).multiply(wpn.subtract(wp.multiply(2.0)).add(wp))).divide(STEP).divide(STEP);
            vectorD.set(i, wp.add(c0.multiply(b)).add(c0.multiply(c)).add(c0.multiply(d)));
        }
    }

    /**
     * Прогонка вперед для функций alpha и beta.
     */
//    vec_alpha[1] = 0;
//    vec_beta[1] = 0;
//		for (size_t i = 2; i < vec_alpha.size(); i++)
//    {
//        complex<double> div = vec_C[i - 1] + vec_A[i - 1] * vec_alpha[i - 1];
//
//        vec_alpha[i] = - vec_B[i - 1] / div;
//        vec_beta[i] = (vec_D[i - 1] - vec_A[i - 1] * vec_beta[i - 1]) / div;
//    }
    public void forward() {
        vec_alpha.set(1, Complex.ZERO);
        vec_beta.set(1, Complex.ZERO);
        for (int i = 2; i < POINTS; i++) {
            Complex div = vectorC.get(i - 1).add(vectorA.get(i - 1).multiply(vec_alpha.get(i - 1)));
            vec_alpha.set(i, vectorB.get(i - 1).negate().divide(div));
            vec_beta.set(i, vectorD.get(i - 1).subtract(vectorA.get(i - 1).multiply(vec_beta.get(i - 1))).divide(div));
        }
    }

    /**
     * Прогонка назад для волнового пакета.
     */
//    void backward()
//    {
//        vec_wave_packet[vec_wave_packet.size() - 1] = 0;
//
//        for (size_t i = vec_wave_packet.size() - 1; i > 0; i--)
//        {
//            vec_wave_packet[i - 1] = vec_alpha[i] * vec_wave_packet[i] + vec_beta[i];
//        }
//    }
    public void backward() {
        vec_wave_packet.set(POINTS_LESS, Complex.ZERO);
        for (int i = POINTS_LESS; i > 0; i--) {
            vec_wave_packet.set(i - 1, vec_alpha.get(i).multiply(vec_wave_packet.get(i)).add(vec_beta.get(i)));
        }
    }

    public void step() {
        if (!isUsing) {
            calculateCoefficient();
            forward();
            backward();
            Collections.copy(vec_wave_packet_prev, vec_wave_packet);
        }
    }

    /**
     * Проинициализировать состояние волнового пакета.
     */
    public void setInitState(double mean, double sigma) {
        Gaussian gaussian = new Gaussian(mean, sigma);
        for (int i = 0; i < POINTS; i++) {
            double x = i * STEP - R;
            double value = gaussian.value(x);
            vec_wave_packet_prev.add(Complex.valueOf(value));
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

            if (x < -B) {
                Complex div = Complex.ONE.add(Complex.I.multiply(GAMMA * Math.pow(x + B, 2)));
                vec_sigm.add(Complex.ONE.divide(div));
                vec_d_sigm.add(Complex.I.divide(div.pow(2)).multiply(-2.0 * GAMMA * (x + B)));
            } else if (x <= B) {
                vec_sigm.add(Complex.ONE);
                vec_d_sigm.add(Complex.ZERO);
            } else {
                Complex div = Complex.ONE.add(Complex.I.multiply(GAMMA * Math.pow(x - B, 2)));
                vec_sigm.add(Complex.ONE.divide(div));
                vec_d_sigm.add(Complex.I.divide(div.pow(2)).multiply(-2.0 * GAMMA * (x - B)));
            }
        }
        System.out.println();
    }

    public XYChart.Series<Number, Number> getSeries() {
        isUsing = true;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
//        double max = 0;
//        for (int i = 0; i < POINTS; i++) {
//            double value = vec_wave_packet.get(i).abs();
//            if (max < value) {
//                max = value;
//            }
//        }
        for (int i = 0; i < POINTS; i++) {
            double x = xis.get(i);
            double y = vec_wave_packet.get(i).abs();
            series.getData().add(new XYChart.Data<>(x, y));
        }
        isUsing = false;
        return series;
    }
}
