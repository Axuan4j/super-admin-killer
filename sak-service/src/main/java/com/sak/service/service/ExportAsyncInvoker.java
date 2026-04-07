package com.sak.service.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ExportAsyncInvoker {

    @Async("exportTaskExecutor")
    public void runAsync(Runnable task) {
        task.run();
    }
}
