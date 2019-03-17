package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Secret;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.*;
import software.amazon.awscdk.services.codepipeline.api.Artifact;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketImportProps;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.secretsmanager.SecretString;
import software.amazon.awscdk.services.secretsmanager.SecretStringProps;

import java.util.Arrays;

public class PipelineStack extends Stack {

    private static final String REGIONAL_ARTIFACT_CACHE_BUCKET_NAME = "joappdevone-ap-southeast-2-codebuild-cache";

    public PipelineStack(final App parent, final String name) {
        this(parent, name, null);
    }

    public PipelineStack(final App parent, final String name, final StackProps props) {
        super(parent, name, props);

        IBucket regionalArtifactCache = Bucket.import_(this, "artifactCache", BucketImportProps.builder()
            .withBucketName(REGIONAL_ARTIFACT_CACHE_BUCKET_NAME).build());

        Pipeline pipeline = new Pipeline(this, "PetClinicPipeline", PipelineProps.builder().build());

        // Source stage

        // Prerequisite: You need to grab your oauth token from github, log into the AWS Console and add it
        // as a secret key in SecretsManager
        SecretString secretString = new SecretString(this, "oauth", SecretStringProps.builder()
            .withSecretId("jousby/github")
            .build());

        Secret githubToken = new Secret(secretString.jsonFieldValue("oauthToken"));

        GitHubSourceAction sourceAction = new GitHubSourceAction(GitHubSourceActionProps.builder()
            .withActionName("GithubSourceAction")
            .withOwner("jousby")
            .withRepo("aws-java-appdev-lab")
            .withOauthToken(githubToken)
            .withBranch("lab/1-aws-basics")
            .withOutputArtifactName("SourceArtifact")
            .build());

        pipeline.addStage(StageAddToPipelineProps.builder()
            .withName("SourceStage")
            .withPlacement(StagePlacement.builder().withAtIndex(0).build())
            .withActions(Arrays.asList(sourceAction))
            .build());

        // Build stage
        BuildEnvironment buildEnvironment = BuildEnvironment.builder()
            .withBuildImage(LinuxBuildImage.fromDockerHub("jousby/aws-buildbox:latest"))
            .build();

        PipelineProject buildProject = new PipelineProject(this, "PipelineProject",
            PipelineProjectProps.builder()
                .withEnvironment(buildEnvironment)
                .withCacheBucket(regionalArtifactCache)
                .build());

        PipelineBuildAction buildAction = new PipelineBuildAction(PipelineBuildActionProps.builder()
                .withActionName("PipelineBuildAction")
                .withInputArtifact(sourceAction.getOutputArtifact())
                .withProject(buildProject)
                .build());

        pipeline.addStage(StageAddToPipelineProps.builder()
            .withName("BuildStage")
            .withPlacement(StagePlacement.builder().withAtIndex(1).build())
            .withActions(Arrays.asList(buildAction))
            .build());

    }
}
