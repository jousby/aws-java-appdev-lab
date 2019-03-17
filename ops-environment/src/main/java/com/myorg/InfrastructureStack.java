package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assets.Asset;
import software.amazon.awscdk.assets.AssetPackaging;
import software.amazon.awscdk.assets.GenericAssetProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroupProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.iam.PolicyPrincipal;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.Arrays;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final App parent, final String name) {
        this(parent, name, null);
    }

    public InfrastructureStack(final App parent, final String name, final StackProps props) {
        super(parent, name, props);


        // ec2 role for copying assets from s3
        Role ec2Role = new Role(this, "Ec2Role", RoleProps.builder()
            .withAssumedBy(new ServicePrincipal("ec2.amazonaws.com"))
            .withManagedPolicyArns(Arrays.asList("arn:aws:iam::aws:policy/AdministratorAccess"))
            .build());

        // pet clinic jar
        String filename = "spring-petclinic-2.1.0.BUILD-SNAPSHOT.jar";

        Asset asset = new Asset(this, "PetClinicJar", GenericAssetProps.builder()
            .withPath(String.format("./../petclinic-app/target/%s", filename))
            .withPackaging(AssetPackaging.File)
            .build());

        asset.grantRead(ec2Role);

        // vpc
        VpcNetwork vpc = new VpcNetwork(this, "PetclinicVPC", VpcNetworkProps.builder().build());

        // alb
        ApplicationLoadBalancer alb = new ApplicationLoadBalancer(this, "PetClinicLB",
            ApplicationLoadBalancerProps.builder()
                .withVpc(vpc)
                .withInternetFacing(true)
                .build());

        // listener
        ApplicationListener listener = new ApplicationListener(this, "PetClinicListener",
            ApplicationListenerProps.builder()
                .withPort(80)
                .withOpen(true)
                .withLoadBalancer(alb)
                .build());

        // autoscaling group
        AutoScalingGroup asg = new AutoScalingGroup(this, "PetClinicAutoScale",
            AutoScalingGroupProps.builder()
                .withVpc(vpc)
                .withRole(ec2Role)
                .withInstanceType(new InstanceTypePair(InstanceClass.Burstable2, InstanceSize.Small))
                .withMachineImage(new AmazonLinuxImage(AmazonLinuxImageProps.builder()
                    .withGeneration(AmazonLinuxGeneration.AmazonLinux2)
                    .build()))
                .withAssociatePublicIpAddress(true)
                .withKeyName("joappdevone-us-east-1")
                .build());

        // listener target
        listener.addTargets("PetClinicFleet", AddApplicationTargetsProps.builder()
            .withTargets(Arrays.asList(asg))
            .withPort(8080)
            .build());

        // asg userdata
        asg.addUserData(
            "# Install Corretto 8 (Java 8)",
            "amazon-linux-extras enable corretto8",
            "yum -y update",
            "yum -y install java-1.8.0-amazon-corretto",
            "",
            "# Download and run the petclinic jar",
            String.format("aws s3 cp s3://%s/%s /tmp/%s", asset.getS3BucketName(), asset.getS3ObjectKey(), filename),
            String.format("java -jar /tmp/%s &>> /tmp/petclinic.log", filename)
        );
    }
}
