package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.VpcNetwork;
import software.amazon.awscdk.services.ec2.VpcNetworkProps;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final App parent, final String name) {
        this(parent, name, null);
    }

    public InfrastructureStack(final App parent, final String name, final StackProps props) {
        super(parent, name, props);

        // vpc
        VpcNetwork vpc = new VpcNetwork(this, "PetclinicVPC", VpcNetworkProps.builder().build());

        // alb

        // ec2

        //

    }
}
