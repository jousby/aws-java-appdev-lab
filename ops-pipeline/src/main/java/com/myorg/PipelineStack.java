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

        Artifact sourceArtifact = createSourceStage(pipeline);

        Stage buildStage = createBuildStage(
            pipeline,
            "jousby/aws-buildbox:latest",
            regionalArtifactCache,
            sourceArtifact
        );
    }

    private Artifact createSourceStage(Pipeline pipeline) {
        // Source stage
        Stage sourceStage = new Stage(this, "SourceStage", StageProps.builder()
            .withPipeline(pipeline)
            .withPlacement(StagePlacement.builder().withAtIndex(0).build())
            .build());

        // Prerequisite: You need to grab your oauth token from github, log into the AWS Console and add it
        // as a secret key in SecretsManager
        SecretString secretString = new SecretString(this, "oauth", SecretStringProps.builder()
            .withSecretId("jousby/github")
            .build());
        Secret githubToken = new Secret(secretString.jsonFieldValue("oauthToken"));

        GitHubSourceAction sourceAction = new GitHubSourceAction(this, "githubRepo", GitHubSourceActionProps
            .builder()
            .withOwner("jousby")
            .withRepo("aws-java-appdev-lab")
            .withOauthToken(githubToken)
            .withBranch("lab/1-aws-basics")
            .withStage(sourceStage)
            .withOutputArtifactName("SourceArtifact")
            .build());

        return sourceAction.getOutputArtifact();
    }

    private Stage createBuildStage(
        Pipeline pipeline,
        String dockerHubBuildImage,
        IBucket artifactCacheBucket,
        Artifact sourceArtifact
    ) {
        // Build stage
        Stage buildStage = new Stage(this, "BuildStage", StageProps.builder()
            .withPipeline(pipeline)
            .withPlacement(StagePlacement.builder().withAtIndex(1).build())
            .build());

        BuildEnvironment buildEnvironment = BuildEnvironment.builder()
            .withBuildImage(LinuxBuildImage.fromDockerHub(dockerHubBuildImage))
            .build();

        PipelineProject buildProject = new PipelineProject(this, "PipelineProject",
            PipelineProjectProps.builder()
                .withEnvironment(buildEnvironment)
                .withCacheBucket(artifactCacheBucket)
                .build());

        PipelineBuildAction buildAction = new PipelineBuildAction(this, "buildAction",
            PipelineBuildActionProps.builder()
                .withInputArtifact(sourceArtifact)
                .withProject(buildProject)
                .withStage(buildStage)
                .build());

        return buildStage;
    }
}
