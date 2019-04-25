package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Secret;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.*;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketImportProps;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.secretsmanager.SecretString;
import software.amazon.awscdk.services.secretsmanager.SecretStringProps;

import java.util.Arrays;

public class PipelineStack extends Stack {

    private static final String REGIONAL_ARTIFACT_CACHE_BUCKET_NAME = "joappdevone-us-east-1-codebuild-cache";

    private static final String SECRET_ID = "jousby/github";
    private static final String SECRET_ID_JSON_FIELD = "oauthToken";

    private static final String DOCKER_BUILD_ENV_IMAGE = "jousby/aws-buildbox:1.2.0";

    private static final String GITHUB_OWNER = "jousby";
    private static final String GITHUB_REPO = "aws-java-appdev-lab";
    private static final String GITHUB_BRANCH = "lab/1-aws-basics";

    public PipelineStack(final App parent, final String name) {
        this(parent, name, null);
    }

    public PipelineStack(final App parent, final String name, final StackProps props) {
        super(parent, name, props);

        IBucket regionalArtifactCache = Bucket.import_(this, "artifactCache", BucketImportProps.builder()
            .withBucketName(REGIONAL_ARTIFACT_CACHE_BUCKET_NAME).build());

        SecretString secretString = new SecretString(this, "oauth", SecretStringProps.builder()
            .withSecretId(SECRET_ID)
            .build());
        Secret githubToken = new Secret(secretString.jsonFieldValue(SECRET_ID_JSON_FIELD));

        // Create pipeline
        Pipeline pipeline = new Pipeline(this, "PetClinicPipeline", PipelineProps.builder().build());

        // Add source stage
        GitHubSourceAction sourceAction = new GitHubSourceAction(GitHubSourceActionProps.builder()
            .withActionName("GithubSourceAction")
            .withOwner(GITHUB_OWNER)
            .withRepo(GITHUB_REPO)
            .withOauthToken(githubToken)
            .withBranch(GITHUB_BRANCH)
            .withOutputArtifactName("SourceArtifact")
            .build());

        pipeline.addStage(StageAddToPipelineProps.builder()
            .withName("SourceStage")
            .withPlacement(StagePlacement.builder().withAtIndex(0).build())
            .withActions(Arrays.asList(sourceAction))
            .build());

        // Add build stage
        BuildEnvironment buildEnvironment = BuildEnvironment.builder()
            .withBuildImage(LinuxBuildImage.fromDockerHub(DOCKER_BUILD_ENV_IMAGE))
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
