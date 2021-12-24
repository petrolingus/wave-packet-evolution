package me.petrolingus.wpe;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;

public class Controller {

    public Button startButton;
    public Button stopButton;

    public Slider sliderWavePacket;
    public Slider sliderPsi;

    public LineChart<Number, Number> wavePacketChart;
    public LineChart<Number, Number> psiChart;
    public LineChart<Number, Number> stationaryChart;

    private Service service;

    public void initialize() {

        service = new Service();

        MathLogic.init();

        sliderWavePacket.valueProperty().addListener((observable, oldValue, newValue) -> {
            MathLogic.wavePacketLineSeries.getData().clear();
            MathLogic.wavePacketLineSeries.getData().add(new XYChart.Data<>(newValue, 0));
            MathLogic.wavePacketLineSeries.getData().add(new XYChart.Data<>(newValue, 1));
            MathLogic.wavePacketLineIndex = (int) Math.round(499 * (newValue.doubleValue() + 3) / 6.0);
        });

        wavePacketChart.getData().add(MathLogic.wavePacketLineSeries);
        wavePacketChart.getData().add(MathLogic.wavePacketSeriesOrigin);
        wavePacketChart.getData().add(MathLogic.wavePacketSeries);

        sliderPsi.valueProperty().addListener((observable, oldValue, newValue) -> {
            MathLogic.psiLineSeries.getData().clear();
            MathLogic.psiLineSeries.getData().add(new XYChart.Data<>(newValue, 0));
            MathLogic.psiLineSeries.getData().add(new XYChart.Data<>(newValue, 200));
            MathLogic.psiLineIndex = (int) Math.round((MathLogic.WAVE_PACKETS_COUNT - 1) * (newValue.doubleValue() + 3) / 6.0);
        });

        psiChart.getData().add(MathLogic.psiLineSeries);
        psiChart.getData().add(MathLogic.psiSeries);

        stationaryChart.getData().add(MathLogic.stationarySeries);
    }

    public void onStartButton() {
        service.start();
    }

    public void onStopButton() {
        service.cancel();
        service.reset();
    }

}
