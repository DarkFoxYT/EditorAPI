#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BlurredSampler;
uniform sampler2D DiffuseDepthSampler;

uniform float FocusDistance;
uniform float FocusBand;
uniform float Transition;
uniform float NearBlurStrength;
uniform float FarBlurStrength;
uniform float NearPlane;
uniform float FarPlane;

in vec2 texCoord;

out vec4 fragColor;

float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * NearPlane * FarPlane) / (FarPlane + NearPlane - z * (FarPlane - NearPlane));
}

void main() {
    vec4 sharpColor = texture(DiffuseSampler, texCoord);
    vec4 blurredColor = texture(BlurredSampler, texCoord);

    float rawDepth = texture(DiffuseDepthSampler, texCoord).r;
    float sceneDistance = rawDepth >= 1.0 ? FarPlane : linearizeDepth(rawDepth);

    float nearStart = max(0.0, FocusDistance - FocusBand - Transition);
    float nearEnd = max(0.0, FocusDistance - FocusBand);
    float farStart = FocusDistance + FocusBand;
    float farEnd = FocusDistance + FocusBand + Transition;

    float nearBlur = 1.0 - smoothstep(nearStart, nearEnd, sceneDistance);
    float farBlur = smoothstep(farStart, farEnd, sceneDistance);
    float blurAmount = max(nearBlur * NearBlurStrength, farBlur * FarBlurStrength);

    fragColor = mix(sharpColor, blurredColor, clamp(blurAmount, 0.0, 1.0));
}
