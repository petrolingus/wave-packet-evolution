package me.petrolingus.wpe;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

public class Service extends javafx.concurrent.Service<Void> {

    private  LineChart<Number, Number> wavePacketChart;
    private  LineChart<Number, Number> psiChart;
    private  LineChart<Number, Number> stationaryChart;

    public Service(LineChart<Number, Number> wavePacketChart, LineChart<Number, Number> psiChart, LineChart<Number, Number> stationaryChart) {
        this.wavePacketChart = wavePacketChart;
        this.psiChart = psiChart;
        this.stationaryChart = stationaryChart;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {

                MathLogic mathLogic = new MathLogic();
                mathLogic.setInitState(0.5, 0.25);
                System.out.println("MathLogic was created");

                int iteration = 0;
                int clock = 1;

                while (!isCancelled()) {

                    if (iteration++ > clock) {

                        XYChart.Series<Number, Number> series = mathLogic.getSeries();
                        XYChart.Series<Number, Number> origin = mathLogic.getOrigin();

                        XYChart.Series<Number, Number> psi = mathLogic.getFftSeries();

                        XYChart.Series<Number, Number> stationary = mathLogic.getStationary();
                        XYChart.Series<Number, Number> line = new XYChart.Series<>();
                        line.getData().add(new XYChart.Data<>(Controller.linePos, 0));
                        line.getData().add(new XYChart.Data<>(Controller.linePos, 1));
                        iteration = 0;
                        Platform.runLater(() -> {
                            wavePacketChart.getData().clear();
                            wavePacketChart.getData().add(origin);
                            wavePacketChart.getData().add(line);
                            wavePacketChart.getData().add(series);
                            psiChart.getData().clear();
                            psiChart.getData().add(psi);
                            stationaryChart.getData().clear();
                            stationaryChart.getData().add(stationary);

                        });
                        mathLogic.isUsing = false;
                    }

                    mathLogic.step();
//                    Thread.yield();
                    Thread.sleep(8);
                }

                return null;
            }
        };
    }
}
