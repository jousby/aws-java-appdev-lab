package software.amazon.samples;

import software.amazon.awscdk.App;

public class InfrastructureApp {
    public static void main(final String argv[]) {
        App app = new App();

        new InfrastructureStack(app, "petclinic-environment");

        app.run();
    }
}
