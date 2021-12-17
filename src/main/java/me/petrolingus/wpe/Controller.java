package me.petrolingus.wpe;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;

import java.util.Random;

public class Controller {

    public Button startButton;
    public Button stopButton;

    public LineChart<Number, Number> wavePacketChart;

    private Service service;

    public void initialize() {

        // Random initialization
        Random random = new Random();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < 1024; i++) {
            series.getData().add(new XYChart.Data<>(i, random.nextGaussian()));
        }
        wavePacketChart.getData().add(series);

        // Gaussian initialization

        service = new Service(wavePacketChart);



    }

    public void onStartButton() {
        service.start();
    }

    public void onStopButton() {
        service.cancel();
        service.reset();
    }

}
