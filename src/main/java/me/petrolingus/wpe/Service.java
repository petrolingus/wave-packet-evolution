package me.petrolingus.wpe;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import javax.sound.sampled.Line;
import java.util.Random;

public class Service extends javafx.concurrent.Service<Void> {

    private  LineChart<Number, Number> wavePacketChart;

    public Service(LineChart<Number, Number> wavePacketChart) {
        this.wavePacketChart = wavePacketChart;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {

                MathLogic mathLogic = new MathLogic();
                mathLogic.setInitState(0, Math.sqrt(0.15));
                System.out.println("MathLogic was created");

                int iteration = 0;
                int clock = 1;

                while (!isCancelled()) {

                    if (iteration++ > clock) {
                        XYChart.Series<Number, Number> series = mathLogic.getSeries();
                        iteration = 0;
                        Platform.runLater(() -> {
                            wavePacketChart.getData().clear();
                            wavePacketChart.getData().add(series);
                        });
                        mathLogic.isUsing = false;
                    }

                    mathLogic.step();
                    Thread.yield();
//                    Thread.sleep(100);
                }

                return null;
            }
        };
    }
}
