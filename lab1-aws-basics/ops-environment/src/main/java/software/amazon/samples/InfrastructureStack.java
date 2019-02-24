package software.amazon.samples;

import software.amazon.awscdk.*;
import software.amazon.awscdk.assets.Asset;
import software.amazon.awscdk.assets.AssetPackaging;
import software.amazon.awscdk.assets.AssetProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroupProps;
import software.amazon.awscdk.services.autoscaling.UpdateType;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.iam.*;

import java.util.Arrays;

public class InfrastructureStack extends Stack {

    public static final String PETCLINIC_JAR_NAME = "spring-petclinic-2.1.0.BUILD-SNAPSHOT.jar";
    public static final String PETCLINIC_JAR_PATH = "./../petclinic-app/target/" + PETCLINIC_JAR_NAME;

    public InfrastructureStack(final App parent, final String name) {
        this(parent, name, null);
    }

    public InfrastructureStack(final App parent, final String name, final StackProps props) {
        super(parent, name, props);

        // pet clinic jar (the Asset construct takes the local file and stores it in S3)
        Asset petClinicJar = new Asset(this, "PetClinicJar", AssetProps.builder()
            .withPath(PETCLINIC_JAR_PATH)
            .withPackaging(AssetPackaging.File)
            .build());

        // create a vpc (software defined network)
        VpcNetwork vpc = new VpcNetwork(this, "PetclinicVPC", VpcNetworkProps.builder().build());

        // create an autoscaling group and place it within the vpc
        AutoScalingGroup asg = new AutoScalingGroup(this, "PetClinicAutoScale",
            AutoScalingGroupProps.builder()
                .withVpc(vpc)
                .withInstanceType(new InstanceTypePair(InstanceClass.Burstable2, InstanceSize.Small))
                .withMachineImage(new AmazonLinuxImage(AmazonLinuxImageProps.builder()
                    .withGeneration(AmazonLinuxGeneration.AmazonLinux2)
                    .build()))
                .withUpdateType(UpdateType.RollingUpdate)
                .build());

        // grant our ec2 instance roles the permission to read the petclinic jar from s3
        petClinicJar.grantRead(asg.getRole());

        // install the petclinic application on the instances in our autoscaling group
        asg.addUserData(
            "# Install Corretto 8 (Java 8)",
            "amazon-linux-extras enable corretto8",
            "yum -y update",
            "yum -y install java-1.8.0-amazon-corretto",
            "",
            "# Download and run the petclinic jar",
            String.format("aws s3 cp s3://%s/%s /tmp/%s", petClinicJar.getS3BucketName(), petClinicJar.getS3ObjectKey(), PETCLINIC_JAR_NAME),
            String.format("java -jar /tmp/%s &>> /tmp/petclinic.log", PETCLINIC_JAR_NAME)
        );

        // create an internet facing load balancer and place it within the the vpc
        ApplicationLoadBalancer alb = new ApplicationLoadBalancer(this, "PetClinicLB",
            ApplicationLoadBalancerProps.builder()
                .withVpc(vpc)
                .withInternetFacing(true)
                .build());

        ApplicationListener listener = new ApplicationListener(this, "PetClinicListener",
            ApplicationListenerProps.builder()
                .withPort(80)
                .withOpen(true)
                .withLoadBalancer(alb)
                .build());

        // connect the autoscaling group running petclinic with the load balancer
        listener.addTargets("PetClinicFleet", AddApplicationTargetsProps.builder()
            .withTargets(Arrays.asList(asg))
            .withPort(8080)
            .build());

        // output the load balancer url to make tracking down our applications new address easier
        CfnOutput output = new CfnOutput(this, "petclinicUrl", CfnOutputProps.builder()
            .withValue(alb.getDnsName())
            .build());
    }
}

