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
    if (rawDepth >= 0.999999) {
        fragColor = vec4(sharpColor.rgb, 1.0);
        return;
    }

    float sceneDistance = linearizeDepth(rawDepth);

    float nearDelta = max(0.0, (FocusDistance - FocusBand) - sceneDistance);
    float farDelta = max(0.0, sceneDistance - (FocusDistance + FocusBand));

    float nearBlur = clamp(nearDelta / max(Transition, 0.0001), 0.0, 1.0);
    float farBlur = clamp(farDelta / max(Transition, 0.0001), 0.0, 1.0);

    nearBlur = nearBlur * nearBlur;
    farBlur = smoothstep(0.0, 1.0, farBlur);

    float blurAmount = max(nearBlur * NearBlurStrength, farBlur * FarBlurStrength);

    fragColor = mix(sharpColor, blurredColor, clamp(blurAmount, 0.0, 1.0));
    fragColor.a = 1.0;
}
