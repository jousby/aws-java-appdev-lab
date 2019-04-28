# Lab 1 - AWS Basics

The goal for this lab is to move the Spring Petclinic application to 
a simple but highly effective web architecture on AWS leveraging some of
our original lower level services. This approach will be familiar to 
developers deploying applications in an on premises environment, 
consisting of a load balancer and an autoscaling fleet of web servers. 


## Prerequisites

Make sure you have completed [lab0-getting-started](../README.md)

## Steps

#### Provision the devops pipeline

1. Navigate to your local clone of the lab project from github. 

2. Changed into the lab 1 directory 
   ```
   cd lab1-aws-basics
   ```
3. Open this folder in your favourite IDE or whenever we say to edit 
a file in subsequent steps feel free to use Vim or another editor of
choice. 

4. Edit the file ```ops-pipeline/src/main/java/software/amazon/samples/PipelineStack``` 
At the top of this file is a collection of constants that you need
to update to reflect the notes you have made during the getting started
lab. In particular you need to update:
   1. The regional code build cache bucket name
   2. The github secret name and key
   3. The github owner details 
Take the time to review the rest of the file. This is where we are 
defining the CI/CD pipeline that will be used to incrementally build 
and update our application plus its environment on every commit. 
   
5. Build the project with  
   ```
   mvn package
   ``` 
   
6. Commit the changes and push the commit back to your remote copy in
github with:

   ```
   git commit -am "localised lab1 pipeline configuration"
   ```
   and
   ```
   git push origin master
   ```

7. Change into the ops-pipeline directory
   ```
   cd ops-pipeline
   ```
   
8. Do a test generation of the cloudformation template for the 
pipeline by running:
   ```
   cdk synth
   ```
   This should print a cloudformation template to std out that you can 
review and correlate with the ```PipelineStack.java``` file that you 
edited earlier. 

9. Deploy our pipeline cloudformation stack using the cdk cli by running: 
   ```
   cdk deploy
   ```
   
10. If everything lines up then this will create a cloudformation stack
that provisions an AWS CodePipeline pipeline and a AWS CodeBuild build 
project. Once complete the pipeline will do an intial run using the 
latest commit in your repository on github. The pipeline will in turn
use the cdk cli to provision the environment for this project which is
defined in ```<project>/ops-environment/src/main/java/software/amazon/samples/InfrastructureStack.java```. 
At the end of the pipeline run you should have another cloudformation
stack for petclinic environment which you view in the Cloudformation 
service in the AWS Console. If you click on the outputs tab for this
stack you can see the URL that our application should now be live on.
Make a copy of this url and open it in a web browser. If all is 
successful then you should see the Petclinic welcome page. If not
click on the pipeline in the AWS CodePipeline service in the console and
look at any failed steps and their associated logs for clues on where 
things might have been misconfigured. 

#### Test your pipeline by making a change

Lets rollout both a cosmetic change to the petclinic application and a 
change to the environment in a single commit. 

2. Edit ```<project>/ops-environment/src/main/java/software/amazon/samples/InfrastructureStack.java```
and change the instance type size in our autoscaling group to ```InstanceSize.Micro```. 

3. Edit ```<project>/spring-petclinic/src/main/resources/messages/messages.properties``` 
and change the welcome message from 'Welcome' to 'Hello'

4. Navigate back to the lab1 parent folder if not already there. Commit
and push the current changes with 

   ```
   git commit -am "Optimising instance type and fixing greeting"
   ```
   and
   ```
   git push origin master
   ```
   
5. Watch the pipeline process this change in the AWS CodePipeline 
console page. Once the pipeline completes it will still take some time
for the autoscaling group to spin up a new instance and tear down the 
old one. You can watch this process in action on the Amazon EC2 console
page. Once the new instance is live you should be able to refresh the 
petclinic url in your browser and see the changed greeting message. 

## Key Learnings

TODO
