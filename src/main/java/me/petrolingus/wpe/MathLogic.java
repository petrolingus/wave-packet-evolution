package me.petrolingus.wpe;

import javafx.scene.chart.XYChart;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MathLogic {

    private static final double R = 3; // Максимальное значение по оси Х для волнового пакета
    private static final double TAU = 3; // Временной шаг симуляции
    private static final int POINTS = 1024; // Временной шаг симуляции

    private double step;

    /** Потенциал задачи.*/
    private List<Complex> vec_U;
    /** Поглощающие слои.*/
    private List<Complex> vec_sigm;
    /** Поглощающие слои (производная).*/
    private List<Complex> vec_d_sigm;

    /* Ширина области моделирования без учета поглощающих слоев.*/
    double b;
    // Временной шаг
    double tau;
    /** Пространственный шаг.*/
    double step_r;
//    int _N_Time;
//    int _t;
//    bool stop_flag;

    private List<Complex> vec_wave_packet;
    private List<Complex> vec_wave_packet_prev;

    /** Вектор коэффициентов Ak.*/
    List<Complex> vectorA;
    /** Вектор коэффициентов Bk.*/
    List<Complex> vectorB;
    /** Вектор коэффициентов Ck.*/
    List<Complex> vectorC;
    /** Вектор коэффициентов Dk.*/
    List<Complex> vectorD;

    /** Вектор значений фукнции alpha.*/
    List<Complex> vec_alpha;
    /** Вектор значений фукнции beta.*/
    List<Complex> vec_beta;

//    List<Double> wavePacket;

    private Complex c0 = Complex.I.multiply(-tau).divide(2.0);

    XYChart.Series<Number, Number> wavePacketSeries;

    public MathLogic() {
        this.step = (2 * R) / (POINTS - 1);

        vectorA = new ArrayList<>(POINTS);
        vectorB = new ArrayList<>(POINTS);
        vectorC = new ArrayList<>(POINTS);
        vectorD = new ArrayList<>(POINTS);

        vec_alpha = new ArrayList<>(POINTS);
        vec_beta = new ArrayList<>(POINTS);

        vec_wave_packet = new ArrayList<>(POINTS);
        vec_wave_packet_prev = new ArrayList<>(POINTS);

        wavePacketSeries = new XYChart.Series<>();
    }

    public void calculateCoefficient() {
        for (int i = 1; i < vectorA.size() - 1; i++)
        {
            Complex a = c0; // Complex.I.multiply(-tau / 2.0);
            Complex b = vec_sigm.get(i).divide(step_r * step_r);
            Complex c = vec_d_sigm.get(i).divide(2.0 * step_r);
            vectorA.set(i, a.multiply(vec_sigm.get(i).multiply(b.subtract(c))));
        }
        for (int i = 1; i < vectorB.size() - 1; i++)
        {
            Complex a = c0; // Complex.I.multiply(-tau / 2.0);
            Complex b = vec_sigm.get(i).divide(step_r * step_r);
            Complex c = vec_d_sigm.get(i).divide(2.0 * step_r);
            vectorB.set(i, a.multiply(vec_sigm.get(i).multiply(b.add(c))));
        }
        for (int i = 1; i < vectorC.size() - 1; i++)
        {
            Complex a = c0; // Complex.I.multiply(-tau / 2.0);
            Complex b = vec_sigm.get(i).divide(step_r * step_r);
            Complex c = vec_U.get(i).add(Complex.valueOf(2).multiply(vec_sigm.get(i).multiply(b)));
            vectorC.set(i, Complex.ONE.add(a.multiply(c)));
        }
        for (int i = 1; i < vectorD.size() - 1; i++)
        {
            Complex wp = vec_wave_packet_prev.get(i);
            Complex wpnext = vec_wave_packet_prev.get(i + 1);
            Complex wpprev = vec_wave_packet_prev.get(i - 1);
            Complex a = c0; // Complex.I.multiply(-tau / 2.0);
            Complex b = vec_U.get(i).multiply(-1).multiply(wp);
            Complex c = vec_sigm.get(i).multiply(vec_d_sigm.get(i).multiply(wpnext.subtract(wpprev).divide(2.0 * step_r)));
            Complex d = vec_sigm.get(i).multiply(vec_sigm.get(i).multiply(wpnext.subtract(wp.multiply(2.0).add(wpnext).divide(step_r * step_r))));
            vectorD.set(i, wp.add(a.multiply(b)).add(a.multiply(c)).add(a.multiply(d)));
        }
    }

    /** Прогонка вперед для функций alpha и beta.*/
//    void forward()
//    {
//        vec_alpha[1] = 0;
//        vec_beta[1] = 0;
//        for (size_t i = 2; i < vec_alpha.size(); i++)
//        {
//            complex<double> div = vec_C[i - 1] + vec_A[i - 1] * vec_alpha[i - 1];
//
//            vec_alpha[i] = - vec_B[i - 1] / div;
//            vec_beta[i] = (vec_D[i - 1] - vec_A[i - 1] * vec_beta[i - 1]) / div;
//        }
//    }
    public void forward() {
        vec_alpha.set(1, Complex.ZERO);
        vec_beta.set(1, Complex.ZERO);
        for (int i = 2; i < POINTS; i++) {
            Complex div = vectorC.get(i - 1).add(vectorA.get(i - 1).multiply(vec_alpha.get(i - 1)));
            vec_alpha.set(i, vectorB.get(i - 1).multiply(-1.0).divide(div));
            vec_beta.set(i, vectorD.get(i - 1).subtract(vectorA.get(i - 1).multiply(vec_beta.get(i - 1))).divide(div));
        }
    }

    /** Прогонка назад для волнового пакета.*/
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
        vec_wave_packet.set(POINTS - 1, Complex.ZERO);
        for (int i = POINTS - 1; i > 0; i--) {
            vec_wave_packet.set(i - 1, vec_alpha.get(i).multiply(vec_wave_packet.get(i)).add(vec_beta.get(i)));
        }
    }

    public void step() {
        calculateCoefficient();
        forward();
        backward();
        Collections.copy(vec_wave_packet_prev, vec_wave_packet);
    }

    public void init() {
//        , _Uo(30)
//                , _R(3)
//                , _a(2.1)
//                , _b(2.5)
//                , _mu(0)
//                , _sigma(0.15)
//                , _tau(0.05)
//                , TimerID(0)
//                , _N_Time(5000)
//                , _n_FFT(256)
//                , FFT_enable(false)
//                , cstrStatus(_T("Status"))
//                , K(0.5)
//                , Beckon(50)
    }

    public void initWavePacket(double mean, double sigma) {
        Gaussian gaussian = new Gaussian(mean, sigma);
        for (int i = 0; i < POINTS; i++) {
            double x = -R + step * i;
            wavePacketSeries.getData().add(new XYChart.Data<>(x, gaussian.value(x)));
        }
    }
}
