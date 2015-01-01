#version 300 es

precision mediump float;

in vec2 vTexCoord;
in float vNormalizedDistFromTouch;

layout(location=0) out vec4 fragColor;

uniform sampler2D uTexture;
uniform highp vec2 uPointSizeInTexCoord;

const float MAX_DIST_FROM_CENTER = 0.7071;

void main() {
    vec2 halfTCFactor = uPointSizeInTexCoord * 0.5;
    vec2 texCoord = vTexCoord - halfTCFactor + gl_PointCoord * uPointSizeInTexCoord;
    vec4 color = texture(uTexture, texCoord);

    // [0.0, 1.0] -> [0.5, MAX_DIST_FROM_CENTER]
    float distOffset = vNormalizedDistFromTouch * (MAX_DIST_FROM_CENTER - 0.5) + 0.5;
    vec2 distFromCenter = gl_PointCoord - vec2(0.5, 0.5);

    if (length(distFromCenter) > distOffset) {
        color.a = 0.0;
    }
    fragColor = color;
}
