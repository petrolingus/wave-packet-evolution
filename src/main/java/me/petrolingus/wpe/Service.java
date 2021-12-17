package me.petrolingus.wpe;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import javax.sound.sampled.Line;
import java.util.Random;

public class Service extends javafx.concurrent.Service<Void> {

    private int points = 1024;

    private  LineChart<Number, Number> wavePacketChart;

    // private MathLogic mathLogic;

    public Service(LineChart<Number, Number> wavePacketChart) {
        this.wavePacketChart = wavePacketChart;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {

                MathLogic mathLogic = new MathLogic(points);
                mathLogic.initWavePacket(0, Math.sqrt(0.15));

                int iteration = 0;
                int clock = 1;

                while (!isCancelled()) {

                    if (iteration++ > clock) {
                        iteration = 0;
                        Platform.runLater(() -> {
                            wavePacketChart.getData().clear();
                            wavePacketChart.getData().add(mathLogic.wavePacketSeries);
                        });
                    }
                    Thread.yield();
                }

                return null;
            }
        };
    }
}
