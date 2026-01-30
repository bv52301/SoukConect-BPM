package com.soukconect.bpm.order;

import com.soukconect.bpm.common.activity.OrderActivities;
import com.soukconect.bpm.common.workflow.OrderWorkflow;
import com.soukconect.bpm.order.workflow.OrderWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class OrderWorkerApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderWorkerApplication.class);

    @Value("${temporal.server.address:localhost:7233}")
    private String temporalAddress;

    private final OrderActivities orderActivities;

    public OrderWorkerApplication(OrderActivities orderActivities) {
        this.orderActivities = orderActivities;
    }

    public static void main(String[] args) {
        SpringApplication.run(OrderWorkerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Starting Order Worker, connecting to Temporal at: {}", temporalAddress);

        // Create Temporal service stubs
        WorkflowServiceStubs serviceStubs = WorkflowServiceStubs.newLocalServiceStubs();

        // Create workflow client
        WorkflowClient client = WorkflowClient.newInstance(serviceStubs);

        // Create worker factory
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Create worker for order task queue
        Worker worker = factory.newWorker(OrderWorkflow.TASK_QUEUE);

        // Register workflow implementation
        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);

        // Register activity implementation
        worker.registerActivitiesImplementations(orderActivities);

        // Start the worker
        factory.start();

        log.info("Order Worker started, listening on task queue: {}", OrderWorkflow.TASK_QUEUE);
    }

}
