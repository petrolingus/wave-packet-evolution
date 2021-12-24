package me.petrolingus.wpe;

import javafx.concurrent.Task;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Service extends javafx.concurrent.Service<Void> {

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {

                MathLogic mathLogic = new MathLogic();
                mathLogic.setInitState();
                System.out.println("MathLogic was created");

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(mathLogic::step, 0, 16, TimeUnit.MILLISECONDS);

                return null;
            }
        };
    }
}
