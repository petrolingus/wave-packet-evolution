package me.petrolingus.wpe;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;

import java.util.Random;

public class Controller {

    public Button startButton;
    public Button stopButton;

    public Slider sliderWavePacket;
    public Slider sliderStationary;

    public LineChart<Number, Number> wavePacketChart;
    public LineChart<Number, Number> psiChart;
    public LineChart<Number, Number> stationaryChart;

    private Service service;

    public static XYChart.Series<Number, Number> series = new XYChart.Series<>();

    public static double linePos = 0;
    public static int lineIndex = 250;
    public static boolean lineChanged = false;

    public static int stationaryIndex = 0;

    public void initialize() {

        // Random initialization
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
        XYChart.Series<Number, Number> series3 = new XYChart.Series<>();
        for (int i = 0; i < 1024; i++) {
            series.getData().add(new XYChart.Data<>(i, 0));
            series2.getData().add(new XYChart.Data<>(i, 0));
            series3.getData().add(new XYChart.Data<>(i, 0));
        }
        wavePacketChart.getData().add(series);
        psiChart.getData().add(series2);
        stationaryChart.getData().add(series2);

        stopButton.setDisable(false);

        sliderWavePacket.valueProperty().addListener((observable, oldValue, newValue) -> {
            double upper = ((NumberAxis)wavePacketChart.getXAxis()).getUpperBound();
            double value = newValue.doubleValue();
            linePos = upper * value;
            lineIndex = (int) (Math.round(512 * (value + 1) / 2));
            lineChanged = true;
        });

        sliderStationary.valueProperty().addListener((observable, oldValue, newValue) -> {
//            double upper = ((NumberAxis)psiChart.getXAxis()).getUpperBound();
//            double value = newValue.doubleValue();
//            linePos = upper * value;
//            lineIndex = (int) (Math.round(500 * (value + 1) / 2));
//            lineChanged = true;
            stationaryIndex = (int) Math.round(512 * newValue.doubleValue());
        });

        service = new Service(wavePacketChart, psiChart, stationaryChart);

    }

    public void onStartButton() {
        service.start();
    }

    public void onStopButton() {
        service.cancel();
        service.reset();
    }

}
