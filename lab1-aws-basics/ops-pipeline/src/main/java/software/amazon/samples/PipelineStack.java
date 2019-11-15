package software.amazon.samples;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.*;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildActionProps;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceActionProps;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.s3.Bucket;
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

    private static final String REGIONAL_ARTIFACT_CACHE_BUCKET_NAME = "joappdevone-ap-southeast-2-codebuild-cache";

    private static final String SECRET_ID = "jousby/github";
    private static final String SECRET_ID_JSON_FIELD = "oauthToken";

    private static final String DOCKER_BUILD_ENV_IMAGE = "jousby/aws-buildbox:1.4.0";

    private static final String GITHUB_OWNER = "jousby";
    private static final String GITHUB_REPO = "aws-java-appdev-lab";
    private static final String GITHUB_BRANCH = "demo";
    private static final String GITHUB_LAB1_PATH = "lab1-aws-basics";

    public PipelineStack(final App parent, final String name) {
        this(parent, name, null);
    }

    public PipelineStack(final App parent, final String name, final StackProps props) {
        super(parent, name, props);

        IBucket regionalArtifactCache = Bucket.fromBucketName(this, "artifactCache", REGIONAL_ARTIFACT_CACHE_BUCKET_NAME);

        SecretValue githubToken = SecretValue.secretsManager(SECRET_ID, SecretsManagerSecretOptions.builder()
            .jsonField(SECRET_ID_JSON_FIELD)
            .build());

        // Create a pipeline
        Pipeline pipeline = new Pipeline(this, "PetClinicPipeline", PipelineProps.builder().build());

        Artifact sourceArtifact = Artifact.artifact("SourceArtifact");

        // Add a source stage that retrieves our source from github on each commit to a specific branch
        GitHubSourceAction sourceAction = new GitHubSourceAction(GitHubSourceActionProps.builder()
            .actionName("GithubSourceAction")
            .owner(GITHUB_OWNER)
            .repo(GITHUB_REPO)
            .oauthToken(githubToken)
            .branch(GITHUB_BRANCH)
            .output(sourceArtifact)
            .build());

        pipeline.addStage(StageOptions.builder()
            .stageName("SourceStage")
            .actions(Arrays.asList(sourceAction))
            .build());

        // Add a build stage that takes the source from the previous stage and runs the build commands in our
        // buildspec.yml file (located in the base of this project).

        // Leverage a custom docker image that has a specific build toolchain
        BuildEnvironment buildEnvironment = BuildEnvironment.builder()
            .buildImage(LinuxBuildImage.fromDockerRegistry(DOCKER_BUILD_ENV_IMAGE))
            .build();

        PipelineProject buildProject = new PipelineProject(this, "PipelineProject",
            PipelineProjectProps.builder()
                .environment(buildEnvironment)
                .buildSpec(BuildSpec.fromSourceFilename(GITHUB_LAB1_PATH + "/buildspec.yml"))
                .cache(Cache.bucket(regionalArtifactCache))
                .build());

        // Adding AdministratorAccess allows the build agent to provision any type of resources but if looking to use
        // in a production context you would want to tighten this up with a custom policy that only allows the build
        // agent to provision those resources that you use in your environment stack.
        buildProject.getRole()
                .addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AdministratorAccess"));

        CodeBuildAction buildAction = new CodeBuildAction(CodeBuildActionProps.builder()
                .actionName("PipelineBuildAction")
                .input(sourceArtifact)
                .project(buildProject)
                .build());

        pipeline.addStage(StageOptions.builder()
            .stageName("BuildStage")
            .actions(Arrays.asList(buildAction))
            .build());
    }
}
