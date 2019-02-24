package software.amazon.samples;

import software.amazon.awscdk.App;

public class PipelineApp {
    public static void main(final String argv[]) {
        App app = new App();

        new PipelineStack(app, "petclinic-pipeline");

        app.run();
    }
}
