package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.*;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildActionProps;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceActionProps;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketImportProps;
import software.amazon.awscdk.services.s3.IBucket;

import java.util.Arrays;

/**
 *  Prerequisites:
 *
 *  1. Fork this project in github into your own account.
 *  2. Create a personal github token that has permisssions to add webhooks to the project
 *  3. Store your github token in Secrets Manager (in your target region). Make note of the
 *     secret id and json field.
 *  4. Create an s3 bucket for the code build cache (in your target region).
 *  5. Run the cdk bootstrap process in your target region.
 *  4. Update the variables below to reflect the github project you
 */
public class PipelineStack extends Stack {

    private static final String REGIONAL_ARTIFACT_CACHE_BUCKET_NAME = "joappdevone-ap-northeast-1-codebuild-cache";

    private static final String SECRET_ID = "jousby/github";
    private static final String SECRET_ID_JSON_FIELD = "oauthToken";

    private static final String DOCKER_BUILD_ENV_IMAGE = "jousby/aws-buildbox:1.4.0";

    private static final String GITHUB_OWNER = "jousby";
    private static final String GITHUB_REPO = "aws-java-appdev-lab";
    private static final String GITHUB_BRANCH = "lab/1-aws-basics";
    private static final String GITHUB_LAB1_PATH = "lab1-aws-basics";

    public PipelineStack(final App parent, final String name) {
        this(parent, name, null);
    }

    public PipelineStack(final App parent, final String name, final StackProps props) {
        super(parent, name, props);

        IBucket regionalArtifactCache = Bucket.import_(this, "artifactCache", BucketImportProps.builder()
            .withBucketName(REGIONAL_ARTIFACT_CACHE_BUCKET_NAME).build());

        SecretValue githubToken = SecretValue.secretsManager(SECRET_ID, SecretsManagerSecretOptions.builder()
            .withJsonField(SECRET_ID_JSON_FIELD)
            .build());

        // Create pipeline
        Pipeline pipeline = new Pipeline(this, "PetClinicPipeline", PipelineProps.builder().build());

        Artifact sourceArtifact = Artifact.artifact("SourceArtifact");

        // Add source stage
        GitHubSourceAction sourceAction = new GitHubSourceAction(GitHubSourceActionProps.builder()
            .withActionName("GithubSourceAction")
            .withOwner(GITHUB_OWNER)
            .withRepo(GITHUB_REPO)
            .withOauthToken(githubToken)
            .withBranch(GITHUB_BRANCH)
            .withOutput(sourceArtifact)
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
                .withBuildSpec(GITHUB_LAB1_PATH + "/buildspec.yml")
                .withCacheBucket(regionalArtifactCache)
                .build());

        buildProject.getRole()
                    .attachManagedPolicy("arn:aws:iam::aws:policy/AdministratorAccess");

        CodeBuildAction buildAction = new CodeBuildAction(CodeBuildActionProps.builder()
                .withActionName("PipelineBuildAction")
                .withInput(sourceArtifact)
                .withProject(buildProject)
                .build());

//        buildAction.getRole()
//                   .attachManagedPolicy("arn:aws:iam::aws:policy/AdministratorAccess");

        pipeline.addStage(StageAddToPipelineProps.builder()
            .withName("BuildStage")
            .withPlacement(StagePlacement.builder().withAtIndex(1).build())
            .withActions(Arrays.asList(buildAction))
            .build());
    }
}
