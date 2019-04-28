# AWS Java Development Labs

In this series of labs we explore different architectures for running 
Java based workloads on AWS. In each lab we will use the [AWS Cloud
Development Kit](https://github.com/awslabs/aws-cdk) (AWS CDK) to provision both a devops pipeline and 
the application environment in Java. In this sense it also serves as an
introduction to AWS CDK but the primary focus is on helping Java
developers to understand how they might build and run applications on
AWS. 

We have chosen the [Spring Framework](https://spring.io/) demo application [Spring Petclinic](https://github.com/spring-projects/spring-petclinic) as 
the source material for our labs. Most Java developers are familiar with 
the Spring framework and this particular demo application comes in 
several variants that makes it easier to evolve our architectures across
the labs. 

## Instructions

The first lab [0. Getting Started](lab0-getting-started) is mandatory in 
order to ensure you have all the necessary prerequisites in place. Apart
from this though there is no required progression in moving through the 
labs. Each lab is self contained and can be completed in isolation and
in any order. The order laid out here however does trend from "on 
premises style architectures" to "cloud native architectures" and for 
lab participants new to AWS there will be a natural progression outside 
your comfort zone as you work through the list.

<br/>
<br/>


| Lab | Description |
|-----|-------------|
| [0. Getting Started](lab0-getting-started) | Install the necessary tools and bootstrap your AWS account to run these labs |
| [1. AWS Basics](lab1-aws-basics) | Migrating a [Spring Boot](https://spring.io/guides/gs/spring-boot)  application to a cloud native variation of a classic web architecture (Load Balancer -> Virtual Machines) |
| 2. Containers (ECS) | TODO - Migrate a dockerised Spring Boot application to run on Amazon Elastic Container Service (ECS) |
| 3. Containers (EKS) | TODO - Migrate a dockerised Spring Boot application to run on Amazon Elastic Container Service for Kubernetes (EKS) |
| 4. Serverless (Basic) | TODO - Migrate to Serverless based microservices with AWS Lambda and Amazon API Gateway |
| 5. Serverless (Graphql) | TODO - Migrate to Serverless based microservices with AWS Appsync and AWS Amplify |


## Feedback Welcome

The above labs are still a work in progress but I would appreciate any 
early feedback via the [issues](https://github.com/jousby/aws-java-appdev-lab/issues) section in this repository.

 
