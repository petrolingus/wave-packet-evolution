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

    public LineChart<Number, Number> wavePacketChart;

    private Service service;

    public void initialize() {

        // Random initialization
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < 1024; i++) {
            series.getData().add(new XYChart.Data<>(i, 0));
        }
        wavePacketChart.getData().add(series);

//        wavePacketChart.getYAxis().

        // Gaussian initialization

        sliderWavePacket.disableProperty().bind(startButton.disableProperty().not());
        stopButton.disableProperty().bind(startButton.disableProperty().not());


        sliderWavePacket.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                double upper = ((NumberAxis)wavePacketChart.getXAxis()).getUpperBound();
                double lower = ((NumberAxis)wavePacketChart.getXAxis()).getLowerBound();
                double value = newValue.doubleValue() * upper;
                series.getData().add(new XYChart.Data<>(value, 0));
                series.getData().add(new XYChart.Data<>(value, 150));
                wavePacketChart.getData().clear();
                wavePacketChart.getData().add(series);
            }
        });

        service = new Service(wavePacketChart);

    }

    public void onStartButton() {
        startButton.setDisable(true);
        service.start();
    }

    public void onStopButton() {
        service.cancel();
        service.reset();
    }

    public void foo() {
        System.out.println("foo");
    }

}
