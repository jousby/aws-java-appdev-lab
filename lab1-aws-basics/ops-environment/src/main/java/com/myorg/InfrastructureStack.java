package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
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

        // ec2 role for copying assets from s3
//        Role ec2Role = new Role(this, "Ec2Role", RoleProps.builder()
//            .withAssumedBy(new ServicePrincipal("ec2.amazonaws.com"))
//            .build());

        // pet clinic jar (the Asset construct takes the local file and stores it in S3)
        Asset asset = new Asset(this, "PetClinicJar", AssetProps.builder()
            .withPath(PETCLINIC_JAR_PATH)
            .withPackaging(AssetPackaging.File)
            .build());
//        asset.grantRead(ec2Role);

        // create a vpc (software defined network)
        VpcNetwork vpc = new VpcNetwork(this, "PetclinicVPC", VpcNetworkProps.builder().build());

        // create an autoscaling group and place it within the vpc
        AutoScalingGroup asg = new AutoScalingGroup(this, "PetClinicAutoScale",
            AutoScalingGroupProps.builder()
                .withVpc(vpc)
//                .withRole(ec2Role)
                .withInstanceType(new InstanceTypePair(InstanceClass.Burstable2, InstanceSize.Small))
                .withMachineImage(new AmazonLinuxImage(AmazonLinuxImageProps.builder()
                    .withGeneration(AmazonLinuxGeneration.AmazonLinux2)
                    .build()))
                .withUpdateType(UpdateType.RollingUpdate)
                .build());

        asset.grantRead(asg.getRole());

        // install the petclinic application on the instances in our autoscaling group
        asg.addUserData(
            "# Install Corretto 8 (Java 8)",
            "amazon-linux-extras enable corretto8",
            "yum -y update",
            "yum -y install java-1.8.0-amazon-corretto",
            "",
            "# Download and run the petclinic jar",
            String.format("aws s3 cp s3://%s/%s /tmp/%s", asset.getS3BucketName(), asset.getS3ObjectKey(), PETCLINIC_JAR_NAME),
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
    }
}

//    Policy policy = new Policy(this, "Ec2Policy", PolicyProps.builder()
//        .withStatements(Arrays.asList(
//            new PolicyStatement()
//                .addServicePrincipal("ec2.amazonaws.com")
//        )).build());
//
//        policy.attachToRole(ec2Role);
