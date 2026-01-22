package com.soukconect.bpm.general;

import com.soukconect.bpm.general.activity.NotificationActivities;
import com.soukconect.bpm.general.activity.VendorActivities;
import com.soukconect.bpm.general.workflow.VendorPayoutWorkflowImpl;
import com.soukconect.bpm.general.workflow.NotificationWorkflowImpl;
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
public class GeneralWorkerApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GeneralWorkerApplication.class);

    public static final String TASK_QUEUE = "general-queue";

    @Value("${temporal.server.address:localhost:7233}")
    private String temporalAddress;

    private final VendorActivities vendorActivities;
    private final NotificationActivities notificationActivities;

    public GeneralWorkerApplication(VendorActivities vendorActivities,
                                    NotificationActivities notificationActivities) {
        this.vendorActivities = vendorActivities;
        this.notificationActivities = notificationActivities;
    }

    public static void main(String[] args) {
        SpringApplication.run(GeneralWorkerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Starting General Worker, connecting to Temporal at: {}", temporalAddress);

        WorkflowServiceStubs serviceStubs = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(serviceStubs);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker(TASK_QUEUE);

        // Register workflow implementations
        worker.registerWorkflowImplementationTypes(
                VendorPayoutWorkflowImpl.class,
                NotificationWorkflowImpl.class
        );

        // Register activity implementations
        worker.registerActivitiesImplementations(vendorActivities, notificationActivities);

        factory.start();

        log.info("General Worker started, listening on task queue: {}", TASK_QUEUE);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
