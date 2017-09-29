package ca.blackacorn.dev.springboot;


import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.lib.client.RestClient;
import net.es.nsi.dds.lib.constants.Nsi;
import net.es.sense.rm.driver.api.Driver;
import net.es.sense.rm.driver.api.Model;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Slf4j
@SpringBootApplication
@EnableSwagger2
@EnableTransactionManagement
@ComponentScan("net.es.sense.rm")
@EntityScan("net.es.sense.rm")
@EnableJpaRepositories("net.es.sense.rm")
public class Application {
  // Keep running while true.
  private static boolean keepRunning = true;

  /**
   * This is the Springboot main for this application.
   *
   * @param args
   * @throws java.util.concurrent.ExecutionException
   * @throws java.lang.InterruptedException
   */
  public static void main(String[] args) throws ExecutionException, InterruptedException, ClassNotFoundException {
    log.info("[main] Starting.");

    ApplicationContext context = SpringApplication.run(Application.class, args);

    // Dump some runtime information.
    RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
    log.info("Name: " + mxBean.getName());
    log.info("Uptime: " + mxBean.getUptime() + " ms");
    log.info("BootClasspath: " + mxBean.getBootClassPath());
    log.info("Classpath: " + mxBean.getClassPath());
    log.info("Library Path: " + mxBean.getLibraryPath());
    for (String argument : mxBean.getInputArguments()) {
      log.info("Input Argument: " + argument);
    }
    // Listen for a shutdown event so we can clean up.
    Runtime.getRuntime().addShutdownHook(
            new Thread() {
      @Override
      public void run() {
        log.info("Shutting down NSI RA...");
        log.info("...Shutdown complete.");
        Application.setKeepRunning(false);
      }
    }
    );

    // Test the DDS client.
    RestClient client = new RestClient();
    Client httpClient = client.get();
    WebTarget webTarget = httpClient.target("http://localhost:8401/dds").path("subscriptions");
    Response response = webTarget.request(Nsi.NSI_DDS_V1_XML).get();
    log.info("[main] response " + response.getStatusInfo());

    // Test the NSI Driver.
    log.info("[main]: get NSI properties.");
    NsiProperties properties = context.getBean("nsiProperties", NsiProperties.class);
    log.info("[main] DDS url: {}", properties.getDdsUrl());

    Class<?> forName = Class.forName("net.es.sense.rm.driver.nsi.NsiDriver");
    Driver driver = context.getBean(forName.asSubclass(Driver.class));

    log.info("[main]: sleeping 10 seconds.");
    Thread.sleep(10000);

    log.info("[main]: querying model.");
    Future<Collection<Model>> models = driver.getModels(0, false, "turtle");
    Collection<Model> results = models.get();

    log.info("Model: {}", results.size());

    // Loop until we are told to shutdown.
    while (keepRunning) {
      Thread.sleep(1000);
    }
    log.info("Shutdown complete with uptime: " + mxBean.getUptime() + " ms");
    System.exit(0);
  }

  /**
   * Returns a boolean indicating whether the PCE should continue running (true) or should terminate (false).
   *
   * @return true if the PCE should be running, false otherwise.
   */
  public static boolean isKeepRunning() {
    return keepRunning;
  }

  /**
   * Set whether the PCE should be running (true) or terminated (false).
   *
   * @param keepRunning true if the PCE should be running, false otherwise.
   */
  public static void setKeepRunning(boolean keepRunning) {
    Application.keepRunning = keepRunning;
  }
}
